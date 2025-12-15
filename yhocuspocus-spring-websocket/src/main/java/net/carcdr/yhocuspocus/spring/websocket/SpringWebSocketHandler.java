package net.carcdr.yhocuspocus.spring.websocket;

import net.carcdr.yhocuspocus.core.ClientConnection;
import net.carcdr.yhocuspocus.core.YHocuspocus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring WebSocket handler for YHocuspocus connections.
 *
 * <p>This class extends Spring's {@link BinaryWebSocketHandler} and creates
 * a {@link SpringWebSocketTransport} for each incoming connection, integrating
 * it with the YHocuspocus server.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Handle WebSocket connection lifecycle (open/close/error)</li>
 *   <li>Receive binary messages and forward to YHocuspocus</li>
 *   <li>Create and manage SpringWebSocketTransport instances</li>
 *   <li>Maintain connection context from session attributes</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class SpringWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringWebSocketHandler.class);

    private final YHocuspocus server;

    // Track transport and connection per session
    private final ConcurrentHashMap<String, SpringWebSocketTransport> transports;
    private final ConcurrentHashMap<String, ClientConnection> connections;

    /**
     * Creates a new Spring WebSocket handler.
     *
     * @param server the YHocuspocus server instance (must not be null)
     * @throws IllegalArgumentException if server is null
     */
    public SpringWebSocketHandler(YHocuspocus server) {
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }
        this.server = server;
        this.transports = new ConcurrentHashMap<>();
        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        LOGGER.info("WebSocket connection opened: {}", sessionId);

        try {
            // Create transport wrapper
            SpringWebSocketTransport transport = new SpringWebSocketTransport(session);
            transports.put(sessionId, transport);

            // Create initial context (can be enriched by handshake interceptor)
            Map<String, Object> context = new ConcurrentHashMap<>();
            context.put("remoteAddress", transport.getRemoteAddress());
            context.put("sessionId", sessionId);

            // Copy session attributes to context (set by HandshakeInterceptor)
            session.getAttributes().forEach(context::put);

            // Register connection with YHocuspocus
            // ClientConnection registers itself as a receive listener on the transport
            ClientConnection connection = server.handleConnection(transport, context);
            connections.put(sessionId, connection);

            LOGGER.info("Successfully registered connection: {}", transport.getConnectionId());
        } catch (Exception e) {
            LOGGER.error("Failed to handle WebSocket connection", e);
            transports.remove(sessionId);
            session.close(CloseStatus.SERVER_ERROR.withReason("Server error: " + e.getMessage()));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        SpringWebSocketTransport transport = transports.get(sessionId);

        if (transport != null) {
            byte[] payload = message.getPayload().array();
            transport.receiveMessage(payload);
        } else {
            LOGGER.warn("Received message for unknown session: {}", sessionId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        LOGGER.info("WebSocket connection closed: {} ({})", sessionId, status);

        // Clean up resources
        ClientConnection connection = connections.remove(sessionId);
        if (connection != null) {
            try {
                String reason = status.getReason() != null ? status.getReason() : "Connection closed";
                connection.close(status.getCode(), reason);
            } catch (Exception e) {
                LOGGER.error("Error closing connection for session {}", sessionId, e);
            }
        }
        transports.remove(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        LOGGER.error("WebSocket transport error for session: {}", sessionId, exception);

        // Clean up resources on error
        ClientConnection connection = connections.remove(sessionId);
        if (connection != null) {
            try {
                connection.close(1011, "Transport error: " + exception.getMessage());
            } catch (Exception e) {
                LOGGER.error("Error closing connection after transport error", e);
            }
        }
        transports.remove(sessionId);
    }

    /**
     * Gets the number of active connections.
     *
     * @return the number of active connections
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * Gets the YHocuspocus server instance.
     *
     * @return the server instance
     */
    public YHocuspocus getServer() {
        return server;
    }
}
