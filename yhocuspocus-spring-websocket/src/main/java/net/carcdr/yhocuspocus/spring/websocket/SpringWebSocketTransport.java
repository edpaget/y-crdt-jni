package net.carcdr.yhocuspocus.spring.websocket;

import net.carcdr.yhocuspocus.transport.ReceiveListener;
import net.carcdr.yhocuspocus.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring WebSocket transport implementation.
 *
 * <p>This class adapts a Spring {@link WebSocketSession} to the
 * {@link Transport} interface, allowing YHocuspocus to work with
 * Spring WebSocket connections.</p>
 *
 * <p>Thread-safe implementation that handles:</p>
 * <ul>
 *   <li>Binary message transmission</li>
 *   <li>Connection state tracking</li>
 *   <li>Graceful closure with WebSocket close codes</li>
 *   <li>Remote address extraction</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class SpringWebSocketTransport implements Transport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringWebSocketTransport.class);

    private final WebSocketSession session;
    private final String connectionId;
    private final String remoteAddress;
    private final AtomicBoolean closed;
    private volatile ReceiveListener receiveListener;

    /**
     * Creates a new Spring WebSocket transport from a session.
     *
     * @param session the WebSocket session (must not be null)
     * @throws IllegalArgumentException if session is null
     */
    public SpringWebSocketTransport(WebSocketSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        this.session = session;
        this.connectionId = generateConnectionId(session);
        this.remoteAddress = extractRemoteAddress();
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public CompletableFuture<Void> send(byte[] message) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Transport is closed")
            );
        }

        if (!session.isOpen()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("WebSocket session is not open")
            );
        }

        return CompletableFuture.runAsync(() -> {
            try {
                session.sendMessage(new BinaryMessage(ByteBuffer.wrap(message)));
            } catch (IOException e) {
                LOGGER.error("Failed to send message on connection {}", connectionId, e);
                throw new RuntimeException("Failed to send WebSocket message", e);
            }
        });
    }

    @Override
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public void close(int code, String reason) {
        if (closed.compareAndSet(false, true)) {
            try {
                CloseStatus status = mapToCloseStatus(code, reason);
                session.close(status);
                LOGGER.debug("Closed WebSocket connection {} with code {}: {}",
                    connectionId, code, reason);
            } catch (IOException e) {
                LOGGER.error("Error closing WebSocket connection {}", connectionId, e);
            }
        }
    }

    @Override
    public boolean isOpen() {
        return !closed.get() && session.isOpen();
    }

    @Override
    public String getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public void setReceiveListener(ReceiveListener listener) {
        this.receiveListener = listener;
    }

    /**
     * Called by {@link SpringWebSocketHandler} when a binary message is received.
     *
     * <p>This method is package-private as it should only be called by
     * SpringWebSocketHandler within the same package.</p>
     *
     * @param data the received message bytes
     */
    void receiveMessage(byte[] data) {
        ReceiveListener listener = this.receiveListener;
        if (listener != null) {
            listener.onMessage(data);
        } else {
            LOGGER.warn("Received message on connection {} but no listener registered",
                connectionId);
        }
    }

    /**
     * Gets the underlying Spring WebSocket session.
     *
     * @return the WebSocket session
     */
    public WebSocketSession getSession() {
        return session;
    }

    /**
     * Generates a unique connection ID from the session.
     */
    private String generateConnectionId(WebSocketSession session) {
        return "spring-ws-" + session.getId() + "-" + System.currentTimeMillis();
    }

    /**
     * Extracts the remote address from the session.
     */
    private String extractRemoteAddress() {
        try {
            var remoteAddress = session.getRemoteAddress();
            if (remoteAddress != null) {
                return remoteAddress.toString();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get remote address for connection {}", connectionId, e);
        }
        return "unknown";
    }

    /**
     * Maps custom close codes to Spring WebSocket CloseStatus.
     *
     * <p>WebSocket spec defines codes in ranges:</p>
     * <ul>
     *   <li>1000-1015: Standard codes</li>
     *   <li>3000-3999: Registered codes (libraries/frameworks)</li>
     *   <li>4000-4999: Private use codes (applications)</li>
     * </ul>
     *
     * @param code the custom close code
     * @param reason the close reason
     * @return Spring CloseStatus
     */
    private CloseStatus mapToCloseStatus(int code, String reason) {
        // Application codes (4000-4999) are already valid
        if (code >= 4000 && code <= 4999) {
            return new CloseStatus(code, reason);
        }

        // Map common codes to Spring CloseStatus constants
        return switch (code) {
            case 1000 -> CloseStatus.NORMAL.withReason(reason);
            case 1001 -> CloseStatus.GOING_AWAY.withReason(reason);
            case 1003 -> CloseStatus.NOT_ACCEPTABLE.withReason(reason);
            case 1008 -> CloseStatus.POLICY_VIOLATION.withReason(reason);
            case 1011 -> CloseStatus.SERVER_ERROR.withReason(reason);
            default -> CloseStatus.NORMAL.withReason(reason);
        };
    }
}
