package net.carcdr.yhocuspocus.core;

import net.carcdr.yhocuspocus.transport.Transport;
import net.carcdr.yhocuspocus.protocol.MessageDecoder;
import net.carcdr.yhocuspocus.protocol.IncomingMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages a client transport connection.
 *
 * <p>Handles authentication, message routing, and document multiplexing
 * over a single transport connection. Transport-agnostic design allows
 * use with WebSocket, HTTP, or other protocols.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Authentication flow per document</li>
 *   <li>Message queueing during authentication</li>
 *   <li>Document multiplexing (multiple documents per connection)</li>
 *   <li>Keepalive mechanism to detect dead connections</li>
 *   <li>Clean shutdown and resource cleanup</li>
 * </ul>
 */
public class ClientConnection implements AutoCloseable {

    private final YHocuspocus server;
    private final Transport transport;
    private final Map<String, Object> context;
    private final ConcurrentHashMap<String, DocumentConnection> documentConnections;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<byte[]>> messageQueues;

    private final ScheduledExecutorService keepaliveScheduler;
    private ScheduledFuture<?> keepaliveFuture;

    /**
     * Creates a new client connection.
     *
     * @param server the YHocuspocus server instance
     * @param transport the transport connection
     * @param context initial context (user data, etc.)
     */
    ClientConnection(YHocuspocus server, Transport transport, Map<String, Object> context) {
        this.server = server;
        this.transport = transport;
        this.context = new ConcurrentHashMap<>(context);
        this.documentConnections = new ConcurrentHashMap<>();
        this.messageQueues = new ConcurrentHashMap<>();
        this.keepaliveScheduler = Executors.newSingleThreadScheduledExecutor();

        setupKeepalive();
    }

    /**
     * Handles an incoming message from transport.
     *
     * @param data the raw message data
     */
    public void handleMessage(byte[] data) {
        try {
            IncomingMessage message = MessageDecoder.decode(data);
            String documentName = message.getDocumentName();

            // Check if authenticated for this document
            DocumentConnection docConn = documentConnections.get(documentName);
            if (docConn == null) {
                // First message for this document - authenticate
                handleAuthentication(message);
            } else {
                // Already authenticated - process message
                docConn.handleMessage(message);
            }
        } catch (IllegalArgumentException e) {
            // Invalid message format - close connection
            close(1003, "Invalid message format: " + e.getMessage());
        }
    }

    /**
     * Handles authentication flow.
     *
     * <p>Messages are queued until authentication completes. If authentication
     * succeeds, queued messages are processed. If it fails, the connection is
     * closed.</p>
     *
     * @param message the authentication message
     */
    private void handleAuthentication(IncomingMessage message) {
        String documentName = message.getDocumentName();

        // Queue message until authenticated
        messageQueues.computeIfAbsent(documentName, k -> new ConcurrentLinkedQueue<>())
            .add(message.getRawData());

        // Run authentication hooks (simplified - no hooks yet)
        // For Phase 2, we'll do simple authentication
        authenticateDocument(documentName)
            .thenAccept(document -> {
                // Authentication succeeded - create document connection
                DocumentConnection docConn = new DocumentConnection(
                    this,
                    document,
                    documentName,
                    context
                );
                documentConnections.put(documentName, docConn);

                // Process queued messages
                ConcurrentLinkedQueue<byte[]> queue = messageQueues.remove(documentName);
                if (queue != null) {
                    queue.forEach(msg -> {
                        try {
                            IncomingMessage queuedMsg = MessageDecoder.decode(msg);
                            docConn.handleMessage(queuedMsg);
                        } catch (Exception e) {
                            // Log and skip invalid message
                        }
                    });
                }
            })
            .exceptionally(error -> {
                // Authentication failed
                close(4403, "Authentication failed: " + error.getMessage());
                messageQueues.remove(documentName);
                return null;
            });
    }

    /**
     * Authenticates access to a document.
     *
     * <p>For Phase 2, this is simplified. Full hook integration will be
     * added in Phase 5.</p>
     *
     * @param documentName the document name
     * @return future that completes with the document
     */
    private java.util.concurrent.CompletableFuture<YDocument> authenticateDocument(
        String documentName
    ) {
        // For now, all requests are authenticated
        // Phase 5 will add extension hooks here
        return server.getOrCreateDocument(documentName, context);
    }

    /**
     * Sets up keepalive/ping mechanism.
     *
     * <p>Checks every 30 seconds if the transport is still alive.
     * If not, closes the connection.</p>
     */
    private void setupKeepalive() {
        keepaliveFuture = keepaliveScheduler.scheduleAtFixedRate(
            this::sendKeepalive,
            30, // initial delay
            30, // period
            TimeUnit.SECONDS
        );
    }

    /**
     * Sends keepalive check.
     */
    private void sendKeepalive() {
        // Send ping or check if transport is still alive
        if (!transport.isOpen()) {
            close(1001, "Transport closed");
        }
    }

    /**
     * Sends a message via transport.
     *
     * @param message the message bytes
     */
    public void send(byte[] message) {
        if (transport.isOpen()) {
            transport.send(message);
        }
    }

    /**
     * Closes this connection.
     *
     * @param code close code
     * @param reason close reason
     */
    public void close(int code, String reason) {
        if (keepaliveFuture != null) {
            keepaliveFuture.cancel(false);
        }
        keepaliveScheduler.shutdown();

        // Close all document connections
        documentConnections.values().forEach(DocumentConnection::close);
        documentConnections.clear();

        // Clear message queues
        messageQueues.clear();

        // Close transport
        transport.close(code, reason);
    }

    @Override
    public void close() {
        close(1000, "Normal closure");
    }

    /**
     * Gets the connection ID.
     *
     * @return connection ID
     */
    public String getConnectionId() {
        return transport.getConnectionId();
    }

    /**
     * Gets the connection context.
     *
     * @return mutable context map
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Gets the transport.
     *
     * @return transport instance
     */
    public Transport getTransport() {
        return transport;
    }
}
