package net.carcdr.yhocuspocus.transport;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for transport mechanisms (WebSocket, HTTP, SSE, etc.).
 *
 * <p>Transport implementations handle the low-level details of sending
 * and receiving binary messages between the server and clients.</p>
 *
 * <p>This interface is transport-agnostic, allowing YHocuspocus to work
 * with any protocol layer including WebSocket, HTTP long-polling, Server-Sent
 * Events, or custom protocols.</p>
 *
 * <p>Receiving messages: Transport implementations should invoke the registered
 * {@link ReceiveListener} when binary messages are received from the underlying
 * protocol. This decouples the transport from connection management.</p>
 */
public interface Transport extends AutoCloseable {

    /**
     * Sends a binary message to the client.
     *
     * @param message the message bytes
     * @return future that completes when message is sent
     * @throws IllegalStateException if transport is closed
     */
    CompletableFuture<Void> send(byte[] message);

    /**
     * Sets the listener for receiving messages from this transport.
     *
     * <p>The transport implementation should invoke {@link ReceiveListener#onMessage(byte[])}
     * when binary messages are received from the underlying protocol.</p>
     *
     * <p>Only one listener can be registered at a time. Setting a new listener
     * replaces any previously registered listener.</p>
     *
     * @param listener the receive listener (may be null to unregister)
     */
    void setReceiveListener(ReceiveListener listener);

    /**
     * Gets a unique identifier for this transport connection.
     *
     * @return connection ID (never null)
     */
    String getConnectionId();

    /**
     * Closes the transport connection.
     *
     * @param code close code (protocol-specific)
     * @param reason close reason message
     */
    void close(int code, String reason);

    /**
     * Checks if the transport is open and ready to send messages.
     *
     * @return true if open, false if closed
     */
    boolean isOpen();

    /**
     * Gets the remote address/identifier for this connection.
     *
     * <p>The format depends on the transport implementation:
     * <ul>
     *   <li>WebSocket: IP address</li>
     *   <li>HTTP: Client IP or proxy header</li>
     *   <li>Custom: Implementation-defined</li>
     * </ul>
     *
     * @return remote address (never null, may be "unknown")
     */
    String getRemoteAddress();

    /**
     * Closes the transport with default parameters.
     *
     * <p>Equivalent to {@code close(1000, "Normal closure")}.</p>
     */
    @Override
    default void close() {
        close(1000, "Normal closure");
    }
}
