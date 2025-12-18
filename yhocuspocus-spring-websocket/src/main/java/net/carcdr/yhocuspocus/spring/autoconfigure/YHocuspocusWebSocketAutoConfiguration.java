package net.carcdr.yhocuspocus.spring.autoconfigure;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.spring.websocket.YHocuspocusWebSocketConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;

/**
 * Auto-configuration for YHocuspocus WebSocket support.
 *
 * <p>Creates a {@link YHocuspocusWebSocketConfigurer} bean that registers
 * WebSocket handlers with Spring. Runs after {@link YHocuspocusAutoConfiguration}
 * to ensure the server is available.</p>
 *
 * <p>Conditions:</p>
 * <ul>
 *   <li>Servlet-based web application</li>
 *   <li>WebSocketConfigurer class available</li>
 *   <li>YHocuspocus bean exists</li>
 * </ul>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = YHocuspocusAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebSocketConfigurer.class)
@ConditionalOnBean(YHocuspocus.class)
@EnableConfigurationProperties(YHocuspocusProperties.class)
@EnableWebSocket
public class YHocuspocusWebSocketAutoConfiguration {

    /**
     * Creates a YHocuspocusWebSocketConfigurer bean.
     *
     * @param server YHocuspocus server instance
     * @param properties configuration properties
     * @param interceptors list of handshake interceptors from Spring context
     * @return configured WebSocket configurer
     */
    @Bean
    @ConditionalOnMissingBean
    public YHocuspocusWebSocketConfigurer yHocuspocusWebSocketConfigurer(
            YHocuspocus server,
            YHocuspocusProperties properties,
            List<HandshakeInterceptor> interceptors) {

        YHocuspocusWebSocketConfigurer.Builder builder =
            YHocuspocusWebSocketConfigurer.builder()
                .server(server)
                .path(properties.getPath())
                .allowedOrigins(properties.getAllowedOrigins().toArray(new String[0]));

        // Add all interceptors from Spring context
        interceptors.forEach(builder::addInterceptor);

        return builder.build();
    }
}
