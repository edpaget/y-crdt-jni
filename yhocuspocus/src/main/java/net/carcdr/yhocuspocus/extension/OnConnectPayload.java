package net.carcdr.yhocuspocus.extension;

import java.util.Map;

/**
 * Payload for the onConnect hook.
 *
 * <p>Provides information about a newly established client connection.</p>
 *
 * @since 1.0.0
 */
public class OnConnectPayload {

    private final String connectionId;
    private final Map<String, Object> context;

    /**
     * Creates a new connection payload.
     *
     * @param connectionId unique connection identifier
     * @param context mutable context map for storing connection data
     */
    public OnConnectPayload(String connectionId, Map<String, Object> context) {
        this.connectionId = connectionId;
        this.context = context;
    }

    /**
     * Gets the unique connection identifier.
     *
     * @return connection ID
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Gets the connection context.
     *
     * <p>Extensions can add information to this context that will be
     * available in subsequent hooks.</p>
     *
     * @return mutable context map
     */
    public Map<String, Object> getContext() {
        return context;
    }
}
