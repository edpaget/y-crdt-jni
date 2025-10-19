package net.carcdr.yhocuspocus.websocket;

import net.carcdr.yhocuspocus.transport.Transport;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket transport implementation using Jetty WebSocket.
 *
 * <p>This class adapts a Jetty WebSocket {@link Session} to the
 * {@link Transport} interface, allowing YHocuspocus to work with
 * WebSocket connections.</p>
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
public class WebSocketTransport implements Transport {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketTransport.class);

    private final Session session;
    private final String connectionId;
    private final String remoteAddress;
    private final AtomicBoolean closed;

    /**
     * Creates a new WebSocket transport from a Jetty session.
     *
     * @param session the WebSocket session (must not be null)
     * @throws IllegalArgumentException if session is null
     */
    public WebSocketTransport(Session session) {
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
                ByteBuffer buffer = ByteBuffer.wrap(message);
                session.sendBinary(buffer, null);
            } catch (Exception e) {
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
                // Map custom codes to WebSocket close codes
                int wsCode = mapToWebSocketCode(code);
                session.close(wsCode, reason, null);
                LOGGER.debug("Closed WebSocket connection {} with code {} ({}): {}",
                    connectionId, code, wsCode, reason);
            } catch (Exception e) {
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

    /**
     * Gets the underlying Jetty WebSocket session.
     *
     * @return the WebSocket session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Generates a unique connection ID from the session.
     */
    private String generateConnectionId(Session session) {
        // Use session hashCode and timestamp for uniqueness
        return "ws-" + Integer.toHexString(session.hashCode()) +
               "-" + System.currentTimeMillis();
    }

    /**
     * Extracts the remote address from the session.
     */
    private String extractRemoteAddress() {
        try {
            var remoteSocketAddress = session.getRemoteSocketAddress();
            if (remoteSocketAddress != null) {
                return remoteSocketAddress.toString();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get remote address for connection {}", connectionId, e);
        }
        return "unknown";
    }

    /**
     * Maps custom close codes to WebSocket standard close codes.
     *
     * <p>WebSocket spec defines codes in ranges:</p>
     * <ul>
     *   <li>1000-1015: Standard codes</li>
     *   <li>3000-3999: Registered codes (libraries/frameworks)</li>
     *   <li>4000-4999: Private use codes (applications)</li>
     * </ul>
     *
     * @param code the custom close code
     * @return WebSocket standard close code
     */
    private int mapToWebSocketCode(int code) {
        // Application codes (4000-4999) are already valid
        if (code >= 4000 && code <= 4999) {
            return code;
        }

        // Map common codes to WebSocket standards
        return switch (code) {
            case 1000 -> StatusCode.NORMAL;           // Normal closure
            case 1001 -> StatusCode.SHUTDOWN;         // Going away
            case 1003 -> StatusCode.BAD_DATA;         // Unsupported data
            case 1008 -> StatusCode.POLICY_VIOLATION; // Policy violation
            case 1011 -> StatusCode.SERVER_ERROR;     // Internal error
            default -> StatusCode.NORMAL;             // Default to normal
        };
    }
}
