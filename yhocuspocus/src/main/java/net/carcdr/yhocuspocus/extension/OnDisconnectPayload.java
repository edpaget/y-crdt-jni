package net.carcdr.yhocuspocus.extension;

import java.util.Map;

/**
 * Payload for the onDisconnect hook.
 *
 * <p>Called when a client connection is closed.</p>
 *
 * @since 1.0.0
 */
public class OnDisconnectPayload {

    private final String connectionId;
    private final Map<String, Object> context;

    /**
     * Creates a new disconnect payload.
     *
     * @param connectionId unique connection identifier
     * @param context connection context
     */
    public OnDisconnectPayload(String connectionId, Map<String, Object> context) {
        this.connectionId = connectionId;
        this.context = context;
    }

    /**
     * Gets the connection identifier.
     *
     * @return connection ID
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Gets the connection context.
     *
     * <p>This context is read-only. Attempts to modify it will throw
     * {@link UnsupportedOperationException}. Context can only be modified
     * during the onConnect and onAuthenticate hooks.</p>
     *
     * @return read-only context map
     */
    public Map<String, Object> getContext() {
        return context;
    }
}
