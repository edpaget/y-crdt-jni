package net.carcdr.yhocuspocus.transport;

/**
 * Handler for incoming transport messages.
 *
 * <p>Implementations of this interface receive binary messages from
 * transport connections and process them according to the Y-CRDT
 * synchronization protocol.</p>
 */
@FunctionalInterface
public interface TransportMessageHandler {

    /**
     * Handles an incoming message from a transport.
     *
     * @param transport the transport that received the message
     * @param message the binary message data
     */
    void onMessage(Transport transport, byte[] message);
}
