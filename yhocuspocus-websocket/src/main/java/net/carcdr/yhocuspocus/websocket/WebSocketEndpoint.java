package net.carcdr.yhocuspocus.websocket;

import net.carcdr.yhocuspocus.core.ClientConnection;
import net.carcdr.yhocuspocus.core.YHocuspocus;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Frame;
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
public class WebSocketEndpoint implements Session.Listener.AutoDemanding {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEndpoint.class);

    private final YHocuspocus server;
    private Session session;
    private WebSocketTransport transport;
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
        LOGGER.info("WebSocket connection opened from: {}", session.getRemoteSocketAddress());
        LOGGER.info("Session ID: {}", session.hashCode());
        LOGGER.info("Protocol version: {}", session.getProtocolVersion());

        try {
            // Create transport wrapper
            this.transport = new WebSocketTransport(session);

            // Create initial context (can be enriched by extensions)
            Map<String, Object> context = new ConcurrentHashMap<>();
            context.put("remoteAddress", transport.getRemoteAddress());

            // Register connection with YHocuspocus
            // The ClientConnection will register itself as a receive listener on the transport
            this.connection = server.handleConnection(transport, context);

            LOGGER.info("Successfully registered connection: {}", transport.getConnectionId());
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

            LOGGER.info("Received binary message, size: {} bytes", data.length);

            // Log first few bytes for debugging
            if (data.length > 0) {
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < Math.min(20, data.length); i++) {
                    hex.append(String.format("%02x ", data[i]));
                }
                LOGGER.info("First bytes: {}", hex.toString());
            }

            // Forward to transport, which will notify its registered listener
            if (transport != null) {
                transport.receiveMessage(data);
                callback.succeed();
            } else {
                LOGGER.warn("Received message before transport initialized");
                callback.fail(new IllegalStateException("Transport not initialized"));
            }
        } catch (Exception e) {
            LOGGER.error("Error processing WebSocket message", e);
            callback.fail(e);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        LOGGER.info("WebSocket connection closed: code={}, reason={}", statusCode, reason);

        // Clean up the connection when WebSocket closes
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

        // Clean up the connection on error
        if (connection != null) {
            try {
                connection.close(1011, "Error: " + cause.getMessage());
            } catch (Exception e) {
                LOGGER.error("Error closing connection after error", e);
            }
        }
    }
}
