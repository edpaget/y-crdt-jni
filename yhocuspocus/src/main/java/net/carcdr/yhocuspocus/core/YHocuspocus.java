package net.carcdr.yhocuspocus.core;

import net.carcdr.ycrdt.UpdateObserver;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.yhocuspocus.extension.AfterLoadDocumentPayload;
import net.carcdr.yhocuspocus.extension.AfterStoreDocumentPayload;
import net.carcdr.yhocuspocus.extension.AfterUnloadDocumentPayload;
import net.carcdr.yhocuspocus.extension.BeforeUnloadDocumentPayload;
import net.carcdr.yhocuspocus.extension.Extension;
import net.carcdr.yhocuspocus.extension.OnChangePayload;
import net.carcdr.yhocuspocus.extension.OnConnectPayload;
import net.carcdr.yhocuspocus.extension.OnCreateDocumentPayload;
import net.carcdr.yhocuspocus.extension.OnDestroyPayload;
import net.carcdr.yhocuspocus.extension.OnDisconnectPayload;
import net.carcdr.yhocuspocus.extension.OnLoadDocumentPayload;
import net.carcdr.yhocuspocus.extension.OnStoreDocumentPayload;
import net.carcdr.yhocuspocus.transport.Transport;
import net.carcdr.yhocuspocus.util.DebouncedDocumentSaver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main collaborative editing server orchestrator.
 *
 * <p>YHocuspocus manages document lifecycle, connection handling,
 * and extension execution. It is transport-agnostic and can work
 * with WebSocket, HTTP, or any other transport mechanism.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * YHocuspocus server = YHocuspocus.builder()
 *     .extension(new InMemoryDatabaseExtension())
 *     .debounce(Duration.ofSeconds(2))
 *     .maxDebounce(Duration.ofSeconds(10))
 *     .build();
 *
 * // Handle incoming transport connection
 * server.handleConnection(transport, Map.of());
 * }</pre>
 *
 * @since 1.0.0
 */
public final class YHocuspocus implements AutoCloseable {

    // Document management
    private final ConcurrentHashMap<String, YDocument> documents;
    private final ConcurrentHashMap<String, CompletableFuture<YDocument>> loadingDocuments;

    // Extension system
    private final List<Extension> extensions;

    // Persistence
    private final DebouncedDocumentSaver documentSaver;

    // Configuration
    private final Duration debounce;
    private final Duration maxDebounce;

    // Executors
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executor;

    private volatile boolean closed = false;

    /**
     * Private constructor - use {@link #builder()} instead.
     */
    private YHocuspocus(Builder builder) {
        this.documents = new ConcurrentHashMap<>();
        this.loadingDocuments = new ConcurrentHashMap<>();
        this.extensions = new ArrayList<>(builder.extensions);
        this.debounce = builder.debounce;
        this.maxDebounce = builder.maxDebounce;
        this.scheduler = Executors.newScheduledThreadPool(builder.schedulerThreads);
        this.executor = Executors.newCachedThreadPool();
        this.documentSaver = new DebouncedDocumentSaver(
            scheduler,
            debounce,
            maxDebounce
        );

        // Sort extensions by priority (higher = earlier)
        this.extensions.sort(Comparator.comparingInt(Extension::priority).reversed());
    }

    /**
     * Handles a new transport connection.
     *
     * @param transport the transport connection
     * @param initialContext initial context (user data, etc.)
     * @return connection instance
     */
    public ClientConnection handleConnection(
        Transport transport,
        Map<String, Object> initialContext
    ) {
        ClientConnection connection = new ClientConnection(
            this,
            transport,
            initialContext
        );

        // Run onConnect hooks asynchronously
        OnConnectPayload payload = new OnConnectPayload(
            transport.getConnectionId(),
            initialContext
        );

        runHooks(payload, Extension::onConnect)
            .exceptionally(error -> {
                connection.close(4403, "Connection rejected: " + error.getMessage());
                return null;
            });

        System.out.println("HERE");
        return connection;
    }

    /**
     * Creates or retrieves a document.
     *
     * <p>Thread-safe document loading with race condition prevention.
     * If multiple connections request the same document simultaneously,
     * only one will load it.</p>
     *
     * @param documentName document name
     * @param context connection context
     * @return future that completes with document
     */
    public CompletableFuture<YDocument> getOrCreateDocument(
        String documentName,
        Map<String, Object> context
    ) {
        // Check if already loaded
        YDocument existing = documents.get(documentName);
        if (existing != null && existing.getState() == YDocument.State.ACTIVE) {
            return CompletableFuture.completedFuture(existing);
        }

        // Check if currently loading (race condition prevention)
        return loadingDocuments.computeIfAbsent(documentName, name -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return loadDocument(name, context);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load document: " + name, e);
                }
            }, executor).whenComplete((doc, error) -> {
                loadingDocuments.remove(name);
                if (doc != null && error == null) {
                    documents.put(name, doc);
                    // Run afterLoadDocument hooks
                    AfterLoadDocumentPayload afterPayload = new AfterLoadDocumentPayload(doc, context);
                    runHooksSync(afterPayload, Extension::afterLoadDocument);
                }
            });
        });
    }

    /**
     * Loads a document from storage via extensions.
     */
    private YDocument loadDocument(String documentName, Map<String, Object> context) {
        YDoc ydoc = new YDoc();
        YDocument document = new YDocument(documentName, ydoc, this);

        // Run onCreateDocument hooks
        OnCreateDocumentPayload createPayload = new OnCreateDocumentPayload(document, context);
        runHooksSync(createPayload, Extension::onCreateDocument);

        // Run onLoadDocument hooks (extensions load data here)
        OnLoadDocumentPayload loadPayload = new OnLoadDocumentPayload(document, context);
        runHooksSync(loadPayload, Extension::onLoadDocument);

        // Apply loaded state if provided
        // Wrap in transaction for atomicity and to batch observer notifications
        if (loadPayload.getState() != null && loadPayload.getState().length > 0) {
            try (YTransaction txn = ydoc.beginTransaction()) {
                ydoc.applyUpdate(loadPayload.getState());
            }
        }

        // Set up change tracking for onChange and debounced save
        // Create a context copy for the observer closure to avoid capturing mutable reference
        Map<String, Object> observerContext = new ConcurrentHashMap<>(context);
        UpdateObserver observer = (update, origin) -> {
            handleDocumentChange(document, observerContext);
        };

        YSubscription subscription = ydoc.observeUpdateV1(observer);
        document.setUpdateSubscription(subscription);

        document.setState(YDocument.State.ACTIVE);

        return document;
    }

    /**
     * Handles document changes (triggers onChange and schedules save).
     *
     * @param document the document that changed
     * @param context connection context
     */
    public void handleDocumentChange(YDocument document, Map<String, Object> context) {
        if (closed || document.getState() != YDocument.State.ACTIVE) {
            return;
        }

        // Get current state as update
        byte[] update = document.getDoc().encodeStateAsUpdate();

        // Run onChange hooks
        OnChangePayload payload = new OnChangePayload(document, context, update);

        runHooks(payload, Extension::onChange)
            .thenRun(() -> {
                // Schedule debounced save
                if (!closed) {
                    documentSaver.scheduleSave(document.getName(), () -> {
                        storeDocument(document, context);
                    });
                }
            });
    }

    /**
     * Stores a document via hooks.
     */
    private void storeDocument(YDocument document, Map<String, Object> context) {
        if (closed) {
            return;
        }

        document.withSaveLock(() -> {
            byte[] state = document.getDoc().encodeStateAsUpdate();

            OnStoreDocumentPayload storePayload = new OnStoreDocumentPayload(
                document,
                context,
                state
            );

            try {
                runHooksSync(storePayload, Extension::onStoreDocument);

                AfterStoreDocumentPayload afterPayload = new AfterStoreDocumentPayload(
                    document,
                    context
                );
                runHooksSync(afterPayload, Extension::afterStoreDocument);
            } catch (Exception e) {
                // Log error but don't propagate
                System.err.println("Error storing document " + document.getName() + ": " + e);
            }
        });
    }

    /**
     * Unloads a document from memory.
     *
     * <p>Called when the last connection to a document is closed.
     * Ensures the document is persisted before unloading.</p>
     *
     * @param documentName document name
     * @return future that completes when unload is done
     */
    public CompletableFuture<Void> unloadDocument(String documentName) {
        YDocument document = documents.remove(documentName);
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            // Mark as unloading
            document.setState(YDocument.State.UNLOADING);

            // Wait for all connections to close (with timeout)
            int maxWait = 50; // 5 seconds
            int waited = 0;
            while (document.getConnectionCount() > 0 && waited < maxWait) {
                try {
                    Thread.sleep(100);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Store immediately (bypass debounce)
            documentSaver.saveImmediately(documentName, () -> {
                storeDocument(document, new ConcurrentHashMap<>());
            });

            // Run beforeUnloadDocument hooks
            BeforeUnloadDocumentPayload beforePayload = new BeforeUnloadDocumentPayload(document);
            runHooksSync(beforePayload, Extension::beforeUnloadDocument);

            // Close Y.Doc
            document.close();

            // Run afterUnloadDocument hooks
            AfterUnloadDocumentPayload afterPayload = new AfterUnloadDocumentPayload(documentName);
            runHooksSync(afterPayload, Extension::afterUnloadDocument);
        }, executor);
    }

    /**
     * Handles client disconnection.
     *
     * @param connectionId connection identifier
     * @param context connection context
     */
    public void handleDisconnect(String connectionId, Map<String, Object> context) {
        OnDisconnectPayload payload = new OnDisconnectPayload(connectionId, context);
        runHooks(payload, Extension::onDisconnect);
    }

    /**
     * Runs hooks asynchronously.
     *
     * @param payload hook payload
     * @param hookMethod method reference to the hook
     * @param <T> payload type
     * @return future that completes when all hooks finish
     */
    <T> CompletableFuture<Void> runHooks(
        T payload,
        java.util.function.BiFunction<Extension, T, CompletableFuture<Void>> hookMethod
    ) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (Extension ext : extensions) {
            chain = chain.thenCompose(prev -> hookMethod.apply(ext, payload));
        }

        return chain;
    }

    /**
     * Runs hooks synchronously (blocking).
     *
     * @param payload hook payload
     * @param hookMethod method reference to the hook
     * @param <T> payload type
     */
    <T> void runHooksSync(
        T payload,
        java.util.function.BiFunction<Extension, T, CompletableFuture<Void>> hookMethod
    ) {
        try {
            runHooks(payload, hookMethod).get();
        } catch (Exception e) {
            throw new RuntimeException("Hook execution failed", e);
        }
    }

    /**
     * Gets a document by name (if loaded).
     *
     * @param documentName document name
     * @return document, or null if not loaded
     */
    public YDocument getDocument(String documentName) {
        return documents.get(documentName);
    }

    /**
     * Gets the number of loaded documents.
     *
     * @return document count
     */
    public int getDocumentCount() {
        return documents.size();
    }

    /**
     * Gets the list of extensions.
     *
     * @return unmodifiable list of extensions
     */
    public List<Extension> getExtensions() {
        return List.copyOf(extensions);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        // Run onDestroy hooks
        OnDestroyPayload payload = new OnDestroyPayload();
        runHooksSync(payload, Extension::onDestroy);

        // Close all documents
        documents.values().forEach(YDocument::close);
        documents.clear();

        // Shutdown executors
        scheduler.shutdown();
        executor.shutdown();
    }

    /**
     * Builder for YHocuspocus.
     */
    public static class Builder {
        private final List<Extension> extensions = new ArrayList<>();
        private Duration debounce = Duration.ofSeconds(2);
        private Duration maxDebounce = Duration.ofSeconds(10);
        private int schedulerThreads = 2;

        /**
         * Adds an extension to the server.
         *
         * @param extension extension instance
         * @return this builder
         */
        public Builder extension(Extension extension) {
            if (extension != null) {
                this.extensions.add(extension);
            }
            return this;
        }

        /**
         * Sets the debounce duration (quiet period before save).
         *
         * @param debounce debounce duration
         * @return this builder
         */
        public Builder debounce(Duration debounce) {
            this.debounce = debounce;
            return this;
        }

        /**
         * Sets the maximum debounce duration (force save after).
         *
         * @param maxDebounce max debounce duration
         * @return this builder
         */
        public Builder maxDebounce(Duration maxDebounce) {
            this.maxDebounce = maxDebounce;
            return this;
        }

        /**
         * Sets the number of scheduler threads.
         *
         * @param threads thread count
         * @return this builder
         */
        public Builder schedulerThreads(int threads) {
            this.schedulerThreads = threads;
            return this;
        }

        /**
         * Builds the YHocuspocus instance.
         *
         * @return new YHocuspocus server
         */
        public YHocuspocus build() {
            return new YHocuspocus(this);
        }
    }

    /**
     * Creates a new builder.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
