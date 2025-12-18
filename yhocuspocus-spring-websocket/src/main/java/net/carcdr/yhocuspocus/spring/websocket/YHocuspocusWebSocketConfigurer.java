package net.carcdr.yhocuspocus.spring.websocket;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configures WebSocket handling for YHocuspocus.
 *
 * <p>Use the builder to configure paths, allowed origins, and interceptors:</p>
 *
 * <pre>{@code
 * YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
 *     .server(hocuspocus)
 *     .path("/collaboration")
 *     .allowedOrigins("http://localhost:3000")
 *     .build();
 * }</pre>
 *
 * <p>The configurer implements {@link WebSocketConfigurer} and can be used
 * directly with Spring's {@code @EnableWebSocket} annotation.</p>
 *
 * @since 1.0.0
 */
public final class YHocuspocusWebSocketConfigurer implements WebSocketConfigurer {

    private final YHocuspocus server;
    private final String path;
    private final String[] allowedOrigins;
    private final List<HandshakeInterceptor> interceptors;
    private final SpringWebSocketHandler handler;

    private YHocuspocusWebSocketConfigurer(Builder builder) {
        this.server = builder.server;
        this.path = builder.path;
        this.allowedOrigins = builder.allowedOrigins.toArray(new String[0]);
        this.interceptors = new ArrayList<>(builder.interceptors);
        this.handler = new SpringWebSocketHandler(server);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry.addHandler(handler, path)
            .setAllowedOrigins(allowedOrigins);

        // Add interceptors if configured
        if (!interceptors.isEmpty()) {
            registration.addInterceptors(interceptors.toArray(new HandshakeInterceptor[0]));
        }
    }

    /**
     * Gets the configured WebSocket path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the YHocuspocus server instance.
     *
     * @return the server
     */
    public YHocuspocus getServer() {
        return server;
    }

    /**
     * Gets the WebSocket handler.
     *
     * @return the handler
     */
    public SpringWebSocketHandler getHandler() {
        return handler;
    }

    /**
     * Gets the allowed origins.
     *
     * @return copy of allowed origins array
     */
    public String[] getAllowedOrigins() {
        return Arrays.copyOf(allowedOrigins, allowedOrigins.length);
    }

    /**
     * Creates a new builder.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for YHocuspocusWebSocketConfigurer.
     */
    public static class Builder {
        private YHocuspocus server;
        private String path = "/";
        private List<String> allowedOrigins = new ArrayList<>(List.of("*"));
        private List<HandshakeInterceptor> interceptors = new ArrayList<>();

        /**
         * Sets the YHocuspocus server (required).
         *
         * @param server the server instance
         * @return this builder
         */
        public Builder server(YHocuspocus server) {
            this.server = server;
            return this;
        }

        /**
         * Sets the WebSocket endpoint path.
         *
         * @param path the path (must start with '/')
         * @return this builder
         * @throws IllegalArgumentException if path is null or doesn't start with '/'
         */
        public Builder path(String path) {
            if (path == null || !path.startsWith("/")) {
                throw new IllegalArgumentException("Path must start with '/'");
            }
            this.path = path;
            return this;
        }

        /**
         * Sets the allowed origins for CORS.
         *
         * @param origins allowed origins (use "*" for all)
         * @return this builder
         */
        public Builder allowedOrigins(String... origins) {
            this.allowedOrigins = new ArrayList<>(List.of(origins));
            return this;
        }

        /**
         * Adds a handshake interceptor.
         *
         * <p>Interceptors can be used for authentication, adding session attributes,
         * and other pre-connection logic.</p>
         *
         * @param interceptor the interceptor to add
         * @return this builder
         */
        public Builder addInterceptor(HandshakeInterceptor interceptor) {
            if (interceptor != null) {
                this.interceptors.add(interceptor);
            }
            return this;
        }

        /**
         * Builds the configurer.
         *
         * @return new YHocuspocusWebSocketConfigurer instance
         * @throws IllegalStateException if server is not set
         */
        public YHocuspocusWebSocketConfigurer build() {
            if (server == null) {
                throw new IllegalStateException("YHocuspocus server is required");
            }
            return new YHocuspocusWebSocketConfigurer(this);
        }
    }
}
