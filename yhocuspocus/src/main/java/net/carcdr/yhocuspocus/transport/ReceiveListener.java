package net.carcdr.yhocuspocus.transport;

/**
 * Listener for receiving messages from a transport.
 *
 * <p>Transport implementations invoke this listener when they receive
 * binary messages from the underlying protocol (WebSocket, HTTP, etc.).</p>
 *
 * <p>This allows the transport layer to remain decoupled from the
 * connection management layer - transports simply notify their listener
 * when data arrives.</p>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ReceiveListener {

    /**
     * Called when the transport receives a binary message.
     *
     * <p>Implementations should handle the message asynchronously if needed,
     * as this method may be called from transport-specific I/O threads.</p>
     *
     * @param data the received message bytes (never null)
     */
    void onMessage(byte[] data);
}
