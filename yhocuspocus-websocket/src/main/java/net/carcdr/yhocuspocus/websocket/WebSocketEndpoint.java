package net.carcdr.yhocuspocus.websocket;

import net.carcdr.yhocuspocus.core.ClientConnection;
import net.carcdr.yhocuspocus.core.YHocuspocus;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint that bridges Jetty WebSocket with YHocuspocus.
 *
 * <p>This class implements the Jetty WebSocket {@link Session.Listener}
 * interface and creates a {@link WebSocketTransport} for each incoming
 * connection, integrating it with the YHocuspocus server.</p>
 *
 * <p>Each WebSocket connection gets its own listener instance, allowing
 * proper session tracking and message routing.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Handle WebSocket connection lifecycle (open/close/error)</li>
 *   <li>Receive binary messages and forward to YHocuspocus</li>
 *   <li>Create and manage WebSocketTransport instances</li>
 *   <li>Maintain connection context</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class WebSocketEndpoint implements Session.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEndpoint.class);

    private final YHocuspocus server;
    private Session session;
    private ClientConnection connection;

    /**
     * Creates a new WebSocket endpoint for a single connection.
     *
     * @param server the YHocuspocus server instance (must not be null)
     * @throws IllegalArgumentException if server is null
     */
    public WebSocketEndpoint(YHocuspocus server) {
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }
        this.server = server;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        this.session = session;
        LOGGER.info("WebSocket connection opened: {}", session.getRemoteSocketAddress());

        try {
            // Create transport wrapper
            WebSocketTransport transport = new WebSocketTransport(session);

            // Create initial context (can be enriched by extensions)
            Map<String, Object> context = new ConcurrentHashMap<>();
            context.put("remoteAddress", transport.getRemoteAddress());

            // Register connection with YHocuspocus
            this.connection = server.handleConnection(transport, context);

            LOGGER.debug("Registered connection: {}", transport.getConnectionId());
        } catch (Exception e) {
            LOGGER.error("Failed to handle WebSocket connection", e);
            session.close(1011, "Server error: " + e.getMessage(), Callback.NOOP);
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        try {
            // Convert ByteBuffer to byte array
            byte[] data = new byte[payload.remaining()];
            payload.get(data);

            // Forward to connection
            if (connection != null) {
                connection.handleMessage(data);
                callback.succeed();
            } else {
                LOGGER.warn("Received message before connection initialized");
                callback.fail(new IllegalStateException("Connection not initialized"));
            }
        } catch (Exception e) {
            LOGGER.error("Error processing WebSocket message", e);
            callback.fail(e);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        LOGGER.info("WebSocket connection closed: code={}, reason={}", statusCode, reason);

        if (connection != null) {
            try {
                connection.close(statusCode, reason != null ? reason : "Connection closed");
            } catch (Exception e) {
                LOGGER.error("Error closing connection", e);
            }
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOGGER.error("WebSocket error", cause);

        if (connection != null) {
            try {
                connection.close(1011, "Error: " + cause.getMessage());
            } catch (Exception e) {
                LOGGER.error("Error closing connection after error", e);
            }
        }
    }

    /**
     * Gets the associated client connection.
     *
     * @return the client connection, or null if not yet initialized
     */
    public ClientConnection getConnection() {
        return connection;
    }
}
