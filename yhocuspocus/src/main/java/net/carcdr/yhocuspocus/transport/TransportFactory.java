package net.carcdr.yhocuspocus.transport;

/**
 * Factory for creating transport instances from raw connections.
 *
 * <p>Implementations of this interface wrap protocol-specific connections
 * (WebSocket sessions, HTTP requests, etc.) in the transport abstraction
 * layer used by YHocuspocus.</p>
 *
 * @param <T> the type of raw connection (e.g., WebSocket Session, HTTP Exchange)
 */
public interface TransportFactory<T> {

    /**
     * Creates a transport from a raw connection.
     *
     * @param connection the raw connection object
     * @param messageHandler handler for incoming messages
     * @param eventHandler handler for lifecycle events
     * @return transport instance wrapping the connection
     * @throws IllegalArgumentException if connection is null or invalid
     */
    Transport createTransport(
        T connection,
        TransportMessageHandler messageHandler,
        TransportEventHandler eventHandler
    );
}
