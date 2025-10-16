package net.carcdr.yhocuspocus.transport;

/**
 * Handler for transport lifecycle events.
 *
 * <p>Implementations receive notifications about transport connection
 * state changes including opens, closes, and errors.</p>
 */
public interface TransportEventHandler {

    /**
     * Called when a transport connection opens.
     *
     * @param transport the newly opened transport
     */
    void onOpen(Transport transport);

    /**
     * Called when a transport connection closes.
     *
     * @param transport the closed transport
     * @param code close code (protocol-specific)
     * @param reason close reason message
     */
    void onClose(Transport transport, int code, String reason);

    /**
     * Called when a transport error occurs.
     *
     * @param transport the transport that encountered an error
     * @param error the error that occurred
     */
    void onError(Transport transport, Throwable error);
}
