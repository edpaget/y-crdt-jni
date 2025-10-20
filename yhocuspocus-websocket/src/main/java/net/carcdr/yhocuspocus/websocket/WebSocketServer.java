package net.carcdr.yhocuspocus.websocket;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * WebSocket server for YHocuspocus collaborative editing.
 *
 * <p>This class provides a complete WebSocket server implementation using
 * Jetty, configured for hosting YHocuspocus instances. It handles:</p>
 * <ul>
 *   <li>HTTP server setup and lifecycle</li>
 *   <li>WebSocket upgrade and routing</li>
 *   <li>Connection management</li>
 *   <li>Graceful shutdown</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * YHocuspocus hocuspocus = YHocuspocus.builder()
 *     .extension(new InMemoryDatabaseExtension())
 *     .build();
 *
 * WebSocketServer server = WebSocketServer.builder()
 *     .server(hocuspocus)
 *     .port(1234)
 *     .path("/collaboration")
 *     .build();
 *
 * server.start();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WebSocketServer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);

    private final YHocuspocus hocuspocus;
    private final int port;
    private final String host;
    private final String path;
    private final Duration pingInterval;
    private final Server jettyServer;

    private boolean started = false;

    /**
     * Private constructor - use {@link #builder()} instead.
     */
    private WebSocketServer(Builder builder) {
        this.hocuspocus = builder.hocuspocus;
        this.port = builder.port;
        this.host = builder.host;
        this.path = builder.path;
        this.pingInterval = builder.pingInterval;
        this.jettyServer = createJettyServer();
    }

    /**
     * Creates and configures the underlying Jetty server.
     */
    private Server createJettyServer() {
        Server server = new Server();

        // Create HTTP connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        if (host != null) {
            connector.setHost(host);
        }
        server.addConnector(connector);

        // Create WebSocket container and configure
        ServerWebSocketContainer container = ServerWebSocketContainer.ensure(server);

        // Set idle timeout for connection keepalive
        // Jetty will automatically send WebSocket ping frames when idle
        // and close connections that don't respond with pong
        Duration idleTimeout = pingInterval.multipliedBy(2); // Allow time for pong response
        container.setIdleTimeout(idleTimeout);
        LOGGER.debug("WebSocket idle timeout (keepalive): {}", idleTimeout);

        container.setMaxBinaryMessageSize(10 * 1024 * 1024); // 10MB max message
        container.setAutoFragment(false);

        // Set up handler that upgrades WebSocket connections
        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                String requestPath = Request.getPathInContext(request);

                // Check if this is a WebSocket upgrade request for our path
                if (requestPath.equals(path)) {
                    // Attempt WebSocket upgrade - create new endpoint for each connection
                    if (container.upgrade((upgradeRequest, upgradeResponse, upgradeCallback) -> {
                                System.out.println("HEREHERE");
                                LOGGER.debug("Passing to the endpoint");
                        return new WebSocketEndpoint(hocuspocus);
                    }, request, response, callback)) {
                        return true; // Upgrade successful
                    }
                }

                // Not a WebSocket request or wrong path
                response.setStatus(404);
                response.write(true, null, callback);
                return true;
            }
        });

        return server;
    }

    /**
     * Starts the WebSocket server.
     *
     * <p>This method blocks until the server is fully started and ready
     * to accept connections.</p>
     *
     * @throws Exception if the server fails to start
     * @throws IllegalStateException if the server is already started
     */
    public void start() throws Exception {
        if (started) {
            throw new IllegalStateException("Server is already started");
        }

        LOGGER.info("Starting WebSocket server on {}:{}{}",
            host != null ? host : "0.0.0.0", port, path);

        jettyServer.start();
        started = true;

        // Get actual listening address from connector
        ServerConnector connector = (ServerConnector) jettyServer.getConnectors()[0];
        String listenHost = connector.getHost() != null ? connector.getHost() : "0.0.0.0";
        int listenPort = connector.getLocalPort();

        LOGGER.info("WebSocket server started successfully on ws://{}:{}{}",
            listenHost, listenPort, path);
    }

    /**
     * Stops the WebSocket server gracefully.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Stops accepting new connections</li>
     *   <li>Closes all existing WebSocket connections</li>
     *   <li>Shuts down the HTTP server</li>
     * </ol>
     *
     * @throws Exception if the server fails to stop cleanly
     */
    public void stop() throws Exception {
        if (!started) {
            return;
        }

        LOGGER.info("Stopping WebSocket server");

        // Stop Jetty server (will close all WebSocket connections)
        jettyServer.stop();
        started = false;

        LOGGER.info("WebSocket server stopped");
    }

    /**
     * Checks if the server is currently running.
     *
     * @return true if started, false otherwise
     */
    public boolean isStarted() {
        return started && jettyServer.isStarted();
    }

    /**
     * Gets the port the server is listening on.
     *
     * <p>If the server was configured with port 0 (random port), this method
     * returns the actual port assigned by the OS after the server starts.</p>
     *
     * @return port number (actual listening port if started, configured port otherwise)
     */
    public int getPort() {
        if (started && jettyServer != null && jettyServer.getConnectors().length > 0) {
            ServerConnector connector = (ServerConnector) jettyServer.getConnectors()[0];
            return connector.getLocalPort();
        }
        return port;
    }

    /**
     * Gets the WebSocket path.
     *
     * @return path (e.g., "/collaboration")
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the YHocuspocus server instance.
     *
     * @return the hocuspocus server
     */
    public YHocuspocus getHocuspocus() {
        return hocuspocus;
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    /**
     * Creates a new builder for WebSocketServer.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for WebSocketServer.
     */
    public static class Builder {
        private YHocuspocus hocuspocus;
        private int port = 1234;
        private String host = null; // null = bind to all interfaces
        private String path = "/";
        private Duration pingInterval = Duration.ofSeconds(30);

        /**
         * Sets the YHocuspocus server instance.
         *
         * @param hocuspocus the server (required)
         * @return this builder
         */
        public Builder server(YHocuspocus hocuspocus) {
            this.hocuspocus = hocuspocus;
            return this;
        }

        /**
         * Sets the port to listen on.
         *
         * @param port port number (0 = random port, default: 1234)
         * @return this builder
         */
        public Builder port(int port) {
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 0 and 65535");
            }
            this.port = port;
            return this;
        }

        /**
         * Sets the host/interface to bind to.
         *
         * @param host hostname or IP (null = all interfaces)
         * @return this builder
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the WebSocket path.
         *
         * @param path the path (default: "/")
         * @return this builder
         */
        public Builder path(String path) {
            if (path == null || !path.startsWith("/")) {
                throw new IllegalArgumentException("Path must start with '/'");
            }
            this.path = path;
            return this;
        }

        /**
         * Sets the WebSocket ping interval for keepalive.
         *
         * <p>The server will automatically send WebSocket ping frames at this
         * interval to detect broken connections. Set to {@link Duration#ZERO}
         * to disable automatic pings.</p>
         *
         * @param pingInterval the ping interval (default: 30 seconds)
         * @return this builder
         */
        public Builder pingInterval(Duration pingInterval) {
            if (pingInterval == null || pingInterval.isNegative()) {
                throw new IllegalArgumentException("Ping interval must be non-negative");
            }
            this.pingInterval = pingInterval;
            return this;
        }

        /**
         * Builds the WebSocketServer instance.
         *
         * @return new WebSocketServer
         * @throws IllegalStateException if required fields are missing
         */
        public WebSocketServer build() {
            if (hocuspocus == null) {
                throw new IllegalStateException("YHocuspocus server is required");
            }
            return new WebSocketServer(this);
        }
    }
}
