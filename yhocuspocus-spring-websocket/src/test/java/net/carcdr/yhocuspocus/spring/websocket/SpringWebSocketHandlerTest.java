package net.carcdr.yhocuspocus.spring.websocket;

import net.carcdr.yhocuspocus.core.ClientConnection;
import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.transport.Transport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SpringWebSocketHandler.
 */
public class SpringWebSocketHandlerTest {

    private YHocuspocus mockServer;
    private ClientConnection mockConnection;
    private WebSocketSession mockSession;
    private SpringWebSocketHandler handler;

    @Before
    public void setUp() {
        mockServer = mock(YHocuspocus.class);
        mockConnection = mock(ClientConnection.class);
        mockSession = mock(WebSocketSession.class);

        when(mockSession.getId()).thenReturn("session-123");
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession.getRemoteAddress()).thenReturn(
            new InetSocketAddress("127.0.0.1", 12345));
        when(mockSession.getAttributes()).thenReturn(new HashMap<>());
        when(mockServer.handleConnection(any(Transport.class), anyMap()))
            .thenReturn(mockConnection);

        handler = new SpringWebSocketHandler(mockServer);
    }

    @Test
    public void testAfterConnectionEstablished() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        // Verify server.handleConnection was called
        ArgumentCaptor<Transport> transportCaptor = ArgumentCaptor.forClass(Transport.class);
        ArgumentCaptor<Map> contextCaptor = ArgumentCaptor.forClass(Map.class);

        verify(mockServer).handleConnection(transportCaptor.capture(), contextCaptor.capture());

        Transport transport = transportCaptor.getValue();
        assertNotNull(transport);
        assertTrue(transport instanceof SpringWebSocketTransport);

        Map<String, Object> context = contextCaptor.getValue();
        assertNotNull(context);
        assertEquals("session-123", context.get("sessionId"));
        assertNotNull(context.get("remoteAddress"));
    }

    @Test
    public void testAfterConnectionEstablishedCopiesSessionAttributes() throws Exception {
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", "user123");
        sessionAttrs.put("authenticated", true);
        when(mockSession.getAttributes()).thenReturn(sessionAttrs);

        handler.afterConnectionEstablished(mockSession);

        ArgumentCaptor<Map> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockServer).handleConnection(any(Transport.class), contextCaptor.capture());

        Map<String, Object> context = contextCaptor.getValue();
        assertEquals("user123", context.get("userId"));
        assertEquals(true, context.get("authenticated"));
    }

    @Test
    public void testHandleBinaryMessage() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        byte[] payload = "test message".getBytes();
        BinaryMessage message = new BinaryMessage(ByteBuffer.wrap(payload));

        handler.handleBinaryMessage(mockSession, message);

        // Message should be forwarded to transport/connection
        // The transport's receiveMessage should be called
        assertEquals(1, handler.getConnectionCount());
    }

    @Test
    public void testHandleBinaryMessageUnknownSession() throws Exception {
        // Don't establish connection first
        byte[] payload = "test".getBytes();
        BinaryMessage message = new BinaryMessage(ByteBuffer.wrap(payload));

        // Should not throw
        handler.handleBinaryMessage(mockSession, message);
    }

    @Test
    public void testAfterConnectionClosed() throws Exception {
        handler.afterConnectionEstablished(mockSession);
        assertEquals(1, handler.getConnectionCount());

        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        verify(mockConnection).close(1000, "Connection closed");
        assertEquals(0, handler.getConnectionCount());
    }

    @Test
    public void testAfterConnectionClosedWithReason() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        CloseStatus status = CloseStatus.GOING_AWAY.withReason("Browser closed");
        handler.afterConnectionClosed(mockSession, status);

        verify(mockConnection).close(1001, "Browser closed");
    }

    @Test
    public void testAfterConnectionClosedNoConnection() {
        // Don't establish connection first
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Should not throw, connection.close should not be called
        verify(mockConnection, never()).close(any(Integer.class), any(String.class));
    }

    @Test
    public void testHandleTransportError() throws Exception {
        handler.afterConnectionEstablished(mockSession);

        Throwable error = new RuntimeException("Connection lost");
        handler.handleTransportError(mockSession, error);

        verify(mockConnection).close(1011, "Transport error: Connection lost");
        assertEquals(0, handler.getConnectionCount());
    }

    @Test
    public void testHandleTransportErrorNoConnection() {
        // Don't establish connection first
        handler.handleTransportError(mockSession, new RuntimeException("Error"));

        // Should not throw
        verify(mockConnection, never()).close(any(Integer.class), any(String.class));
    }

    @Test
    public void testGetConnectionCount() throws Exception {
        assertEquals(0, handler.getConnectionCount());

        handler.afterConnectionEstablished(mockSession);
        assertEquals(1, handler.getConnectionCount());

        // Add another session
        WebSocketSession mockSession2 = mock(WebSocketSession.class);
        when(mockSession2.getId()).thenReturn("session-456");
        when(mockSession2.isOpen()).thenReturn(true);
        when(mockSession2.getRemoteAddress()).thenReturn(
            new InetSocketAddress("127.0.0.1", 12346));
        when(mockSession2.getAttributes()).thenReturn(new HashMap<>());

        handler.afterConnectionEstablished(mockSession2);
        assertEquals(2, handler.getConnectionCount());
    }

    @Test
    public void testGetServer() {
        assertEquals(mockServer, handler.getServer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullServerThrows() {
        new SpringWebSocketHandler(null);
    }

    @Test
    public void testConnectionErrorOnEstablish() throws Exception {
        when(mockServer.handleConnection(any(Transport.class), anyMap()))
            .thenThrow(new RuntimeException("Server error"));

        handler.afterConnectionEstablished(mockSession);

        // Session should be closed
        verify(mockSession).close(any(CloseStatus.class));
        assertEquals(0, handler.getConnectionCount());
    }

    // Helper to assert instance type
    private void assertTrue(boolean condition) {
        org.junit.Assert.assertTrue(condition);
    }
}
