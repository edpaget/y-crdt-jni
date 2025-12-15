package net.carcdr.yhocuspocus.spring.websocket;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for YHocuspocusWebSocketConfigurer.
 */
public class YHocuspocusWebSocketConfigurerTest {

    private YHocuspocus mockServer;

    @Before
    public void setUp() {
        mockServer = mock(YHocuspocus.class);
    }

    @Test
    public void testBuilderWithDefaults() {
        YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .build();

        assertEquals("/", configurer.getPath());
        assertEquals(mockServer, configurer.getServer());
        assertNotNull(configurer.getHandler());
        assertArrayEquals(new String[]{"*"}, configurer.getAllowedOrigins());
    }

    @Test
    public void testBuilderWithCustomPath() {
        YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .path("/collaboration")
            .build();

        assertEquals("/collaboration", configurer.getPath());
    }

    @Test
    public void testBuilderWithAllowedOrigins() {
        YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .allowedOrigins("http://localhost:3000", "https://example.com")
            .build();

        assertArrayEquals(
            new String[]{"http://localhost:3000", "https://example.com"},
            configurer.getAllowedOrigins()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderPathMustStartWithSlash() {
        YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .path("invalid")
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderPathCannotBeNull() {
        YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .path(null)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderRequiresServer() {
        YHocuspocusWebSocketConfigurer.builder()
            .path("/test")
            .build();
    }

    @Test
    public void testRegisterWebSocketHandlers() {
        YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .path("/ws")
            .allowedOrigins("http://localhost:3000")
            .build();

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(any(WebSocketHandler.class), eq("/ws")))
            .thenReturn(registration);
        when(registration.setAllowedOrigins(any(String[].class)))
            .thenReturn(registration);

        configurer.registerWebSocketHandlers(registry);

        verify(registry).addHandler(any(SpringWebSocketHandler.class), eq("/ws"));
        verify(registration).setAllowedOrigins("http://localhost:3000");
    }

    @Test
    public void testRegisterWithInterceptors() {
        HandshakeInterceptor interceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Exception exception) {
            }
        };

        YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .path("/ws")
            .addInterceptor(interceptor)
            .build();

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(any(WebSocketHandler.class), eq("/ws")))
            .thenReturn(registration);
        when(registration.setAllowedOrigins(any(String[].class)))
            .thenReturn(registration);
        when(registration.addInterceptors(any(HandshakeInterceptor[].class)))
            .thenReturn(registration);

        configurer.registerWebSocketHandlers(registry);

        verify(registration).addInterceptors(any(HandshakeInterceptor[].class));
    }

    @Test
    public void testAddNullInterceptorIgnored() {
        YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .addInterceptor(null)
            .build();

        // Should not throw, null interceptor should be ignored
        assertNotNull(configurer);
    }

    @Test
    public void testGetHandler() {
        YHocuspocusWebSocketConfigurer configurer = YHocuspocusWebSocketConfigurer.builder()
            .server(mockServer)
            .build();

        SpringWebSocketHandler handler = configurer.getHandler();
        assertNotNull(handler);
        assertEquals(mockServer, handler.getServer());
    }
}
