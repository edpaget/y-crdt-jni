package net.carcdr.yhocuspocus.core;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.yhocuspocus.transport.Transport;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main collaborative editing server orchestrator.
 *
 * <p>YHocuspocus manages document lifecycle, connection handling,
 * and is transport-agnostic, working with WebSocket, HTTP, or any
 * other transport mechanism.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * YHocuspocus server = new YHocuspocus();
 *
 * // Handle incoming transport connection
 * server.handleConnection(transport, Map.of());
 * }</pre>
 *
 * <p><b>Phase 2 Implementation:</b> This is a simplified version for Phase 2.
 * Extension system, debounced persistence, and hook execution will be
 * added in later phases.</p>
 */
public class YHocuspocus implements AutoCloseable {

    // Document management
    private final ConcurrentHashMap<String, YDocument> documents;
    private final ConcurrentHashMap<String, CompletableFuture<YDocument>> loadingDocuments;

    // Executor for async operations
    private final ExecutorService executor;

    /**
     * Creates a new YHocuspocus server.
     */
    public YHocuspocus() {
        this.documents = new ConcurrentHashMap<>();
        this.loadingDocuments = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool();
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

        // Phase 2: No hooks yet, connection is immediately ready
        // Phase 5 will add onConnect hook execution

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
                }
            });
        });
    }

    /**
     * Loads a document from storage.
     *
     * <p><b>Phase 2:</b> Creates a new empty document.
     * <b>Phase 5:</b> Will add extension hooks for loading from database.</p>
     *
     * @param documentName document name
     * @param context connection context
     * @return loaded document
     */
    private YDocument loadDocument(String documentName, Map<String, Object> context) {
        YDoc ydoc = new YDoc();
        YDocument document = new YDocument(documentName, ydoc, this);

        // Phase 5 will add:
        // - onCreateDocument hooks
        // - onLoadDocument hooks (extensions load data here)
        // - afterLoadDocument hooks
        // - Observer setup for onChange hooks

        document.setState(YDocument.State.ACTIVE);
        return document;
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

            // Phase 6 will add:
            // - Store immediately (save to database)
            // - beforeUnloadDocument hooks
            // - afterUnloadDocument hooks

            // Close Y.Doc
            document.close();
        }, executor);
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

    @Override
    public void close() {
        // Phase 5 will add onDestroy hooks

        // Close all documents
        documents.values().forEach(YDocument::close);
        documents.clear();

        // Shutdown executor
        executor.shutdown();
    }
}
