package net.carcdr.yhocuspocus.spring.websocket;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SpringWebSocketTransport.
 */
public class SpringWebSocketTransportTest {

    private WebSocketSession mockSession;
    private SpringWebSocketTransport transport;

    @Before
    public void setUp() {
        mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-123");
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession.getRemoteAddress()).thenReturn(
            new InetSocketAddress("127.0.0.1", 12345));

        transport = new SpringWebSocketTransport(mockSession);
    }

    @Test
    public void testConnectionIdGenerated() {
        String connectionId = transport.getConnectionId();
        assertNotNull(connectionId);
        assertTrue(connectionId.startsWith("spring-ws-test-session-123-"));
    }

    @Test
    public void testRemoteAddressExtracted() {
        String remoteAddress = transport.getRemoteAddress();
        assertNotNull(remoteAddress);
        assertTrue(remoteAddress.contains("127.0.0.1"));
    }

    @Test
    public void testRemoteAddressUnknownOnError() {
        WebSocketSession errorSession = mock(WebSocketSession.class);
        when(errorSession.getId()).thenReturn("error-session");
        when(errorSession.getRemoteAddress()).thenThrow(new RuntimeException("Error"));

        SpringWebSocketTransport errorTransport = new SpringWebSocketTransport(errorSession);
        assertEquals("unknown", errorTransport.getRemoteAddress());
    }

    @Test
    public void testIsOpenWhenSessionOpen() {
        assertTrue(transport.isOpen());
    }

    @Test
    public void testIsOpenWhenSessionClosed() {
        when(mockSession.isOpen()).thenReturn(false);
        assertFalse(transport.isOpen());
    }

    @Test
    public void testIsOpenAfterClose() throws IOException {
        transport.close(1000, "Normal");
        assertFalse(transport.isOpen());
    }

    @Test
    public void testSendMessage() throws Exception {
        byte[] message = "test message".getBytes();

        CompletableFuture<Void> future = transport.send(message);
        future.get(); // Wait for completion

        verify(mockSession).sendMessage(any(BinaryMessage.class));
    }

    @Test
    public void testSendFailsWhenClosed() {
        transport.close(1000, "Normal");

        CompletableFuture<Void> future = transport.send("test".getBytes());

        assertTrue(future.isCompletedExceptionally());
        try {
            future.get();
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    public void testSendFailsWhenSessionNotOpen() {
        when(mockSession.isOpen()).thenReturn(false);

        CompletableFuture<Void> future = transport.send("test".getBytes());

        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    public void testCloseWithCode() throws IOException {
        transport.close(1001, "Going away");

        verify(mockSession).close(any(CloseStatus.class));
        assertFalse(transport.isOpen());
    }

    @Test
    public void testCloseOnlyOnce() throws IOException {
        transport.close(1000, "First");
        transport.close(1001, "Second");

        // Should only be called once
        verify(mockSession).close(any(CloseStatus.class));
    }

    @Test
    public void testCloseWithApplicationCode() throws IOException {
        transport.close(4001, "Custom error");

        verify(mockSession).close(any(CloseStatus.class));
    }

    @Test
    public void testReceiveMessageNotifiesListener() {
        AtomicReference<byte[]> received = new AtomicReference<>();
        transport.setReceiveListener(received::set);

        byte[] message = "incoming".getBytes();
        transport.receiveMessage(message);

        assertNotNull(received.get());
        assertEquals("incoming", new String(received.get()));
    }

    @Test
    public void testReceiveMessageWithNoListener() {
        // Should not throw
        transport.receiveMessage("test".getBytes());
    }

    @Test
    public void testSetReceiveListenerReplacesExisting() {
        AtomicReference<byte[]> first = new AtomicReference<>();
        AtomicReference<byte[]> second = new AtomicReference<>();

        transport.setReceiveListener(first::set);
        transport.setReceiveListener(second::set);

        transport.receiveMessage("test".getBytes());

        // Only second listener should receive
        assertTrue(first.get() == null);
        assertNotNull(second.get());
    }

    @Test
    public void testGetSession() {
        assertEquals(mockSession, transport.getSession());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSessionThrows() {
        new SpringWebSocketTransport(null);
    }

    @Test
    public void testCloseStatusMapping() throws IOException {
        // Test normal close
        transport.close(1000, "Normal");
        verify(mockSession).close(any(CloseStatus.class));
    }

    @Test
    public void testCloseHandlesIOException() throws IOException {
        doThrow(new IOException("Close error")).when(mockSession).close(any(CloseStatus.class));

        // Should not throw
        transport.close(1000, "Normal");

        // Transport should still be marked as closed
        assertFalse(transport.isOpen());
    }
}
