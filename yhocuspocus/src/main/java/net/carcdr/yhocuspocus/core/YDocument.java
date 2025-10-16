package net.carcdr.yhocuspocus.core;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.yhocuspocus.protocol.Awareness;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps a Y.Doc with connection management and awareness.
 *
 * <p>Extends functionality beyond the base YDoc to include:</p>
 * <ul>
 *   <li>Connection tracking - knows which clients are connected</li>
 *   <li>Awareness protocol - user presence and cursor tracking</li>
 *   <li>Document lifecycle state - loading, active, unloading, closed</li>
 *   <li>Save coordination - mutex for persistence operations</li>
 *   <li>Broadcasting - send messages to all connected clients</li>
 * </ul>
 *
 * <p>Thread-safe implementation using concurrent collections and locks.</p>
 */
public class YDocument implements AutoCloseable {

    /**
     * Document lifecycle states.
     */
    public enum State {
        /** Document is being loaded from storage. */
        LOADING,
        /** Document is active and accepting connections. */
        ACTIVE,
        /** Document is being unloaded (no new connections). */
        UNLOADING,
        /** Document is closed and cannot be used. */
        CLOSED
    }

    private final String name;
    private final YDoc doc;
    private final YHocuspocus server;
    private final Awareness awareness;
    private final ReentrantLock saveLock;

    private final ConcurrentHashMap<String, DocumentConnection> connections;
    private volatile State state;

    /**
     * Creates a new YDocument wrapper.
     *
     * @param name document name
     * @param doc the underlying YDoc instance
     * @param server the YHocuspocus server instance
     */
    YDocument(String name, YDoc doc, YHocuspocus server) {
        this.name = name;
        this.doc = doc;
        this.server = server;
        this.awareness = new Awareness(doc);
        this.saveLock = new ReentrantLock();
        this.connections = new ConcurrentHashMap<>();
        this.state = State.ACTIVE;
    }

    /**
     * Adds a connection to this document.
     *
     * @param connection the connection to add
     */
    public void addConnection(DocumentConnection connection) {
        connections.put(connection.getConnectionId(), connection);
    }

    /**
     * Removes a connection from this document.
     *
     * <p>If this is the last connection and the document is active,
     * schedules the document for unloading.</p>
     *
     * @param connection the connection to remove
     */
    public void removeConnection(DocumentConnection connection) {
        connections.remove(connection.getConnectionId());

        // Unload if no more connections
        if (connections.isEmpty() && state == State.ACTIVE) {
            server.unloadDocument(name);
        }
    }

    /**
     * Broadcasts a message to all connections except sender.
     *
     * @param message the message to broadcast
     * @param exceptConnectionId connection ID to exclude (null to broadcast to all)
     */
    public void broadcast(byte[] message, String exceptConnectionId) {
        connections.values().stream()
            .filter(conn -> !conn.getConnectionId().equals(exceptConnectionId))
            .forEach(conn -> conn.send(message));
    }

    /**
     * Broadcasts to all connections including sender.
     *
     * @param message the message to broadcast
     */
    public void broadcastToAll(byte[] message) {
        connections.values().forEach(conn -> conn.send(message));
    }

    /**
     * Broadcasts a stateless message.
     *
     * @param payload the stateless message payload
     * @param exceptConnectionId connection ID to exclude
     */
    public void broadcastStateless(String payload, String exceptConnectionId) {
        // Implementation will be added in Phase 4 (Awareness & Stateless Messages)
        // For now, this is a placeholder
    }

    /**
     * Gets connection count.
     *
     * @return number of active connections
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * Executes code with save lock held.
     *
     * <p>Ensures that only one save operation can occur at a time,
     * preventing race conditions during persistence.</p>
     *
     * @param task the task to execute with lock held
     */
    public void withSaveLock(Runnable task) {
        saveLock.lock();
        try {
            task.run();
        } finally {
            saveLock.unlock();
        }
    }

    /**
     * Gets the document name.
     *
     * @return document name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the underlying YDoc.
     *
     * @return YDoc instance
     */
    public YDoc getDoc() {
        return doc;
    }

    /**
     * Gets the awareness instance.
     *
     * @return awareness protocol handler
     */
    public Awareness getAwareness() {
        return awareness;
    }

    /**
     * Gets the current lifecycle state.
     *
     * @return current state
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the lifecycle state.
     *
     * @param state new state
     */
    void setState(State state) {
        this.state = state;
    }

    @Override
    public void close() {
        state = State.CLOSED;

        // Close all connections
        connections.values().forEach(DocumentConnection::close);
        connections.clear();

        // Close the underlying YDoc
        doc.close();
    }
}
