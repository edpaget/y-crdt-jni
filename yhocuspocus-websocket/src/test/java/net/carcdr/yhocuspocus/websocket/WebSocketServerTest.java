package net.carcdr.yhocuspocus.websocket;

import net.carcdr.yhocuspocus.core.YHocuspocus;
import net.carcdr.yhocuspocus.extension.InMemoryDatabaseExtension;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for WebSocketServer.
 */
public class WebSocketServerTest {

    private YHocuspocus hocuspocus;
    private WebSocketServer server;
    private WebSocketClient client;

    @Before
    public void setUp() {
        // Create YHocuspocus instance
        hocuspocus = YHocuspocus.builder()
            .extension(new InMemoryDatabaseExtension())
            .debounce(Duration.ofMillis(100))
            .maxDebounce(Duration.ofMillis(500))
            .build();

        // Create WebSocket server on a random available port
        server = WebSocketServer.builder()
            .server(hocuspocus)
            .port(0) // Let the OS choose an available port
            .path("/test")
            .build();

        // Create WebSocket client
        client = new WebSocketClient();
    }

    @After
    public void tearDown() throws Exception {
        if (client != null && client.isStarted()) {
            client.stop();
        }
        if (server != null) {
            server.close();
        }
        if (hocuspocus != null) {
            hocuspocus.close();
        }
    }

    @Test
    public void testServerStartsAndStops() throws Exception {
        // Server should not be started initially
        assertFalse("Server should not be started initially", server.isStarted());

        // Start server
        server.start();
        assertTrue("Server should be started", server.isStarted());

        // Stop server
        server.stop();
        assertFalse("Server should be stopped", server.isStarted());
    }

    @Test
    public void testWebSocketConnectionEstablished() throws Exception {
        // Start server
        server.start();

        // Get the actual port the server is listening on
        int port = server.getPort();
        URI uri = new URI("ws://localhost:" + port + "/test");

        // Start client
        client.start();

        // Connect to server
        TestWebSocketListener listener = new TestWebSocketListener();
        client.connect(listener, uri);

        // Wait for connection
        Session session = listener.getConnectFuture().get(5, TimeUnit.SECONDS);
        assertNotNull("Session should not be null", session);
        assertTrue("Session should be open", session.isOpen());

        // Close connection
        session.close();

        // Wait a bit for cleanup
        Thread.sleep(100);
    }

    @Test
    public void testWebSocketBinaryMessageReceived() throws Exception {
        // Start server
        server.start();

        // Get the actual port
        int port = server.getPort();
        URI uri = new URI("ws://localhost:" + port + "/test");

        // Start client
        client.start();

        // Connect
        TestWebSocketListener listener = new TestWebSocketListener();
        client.connect(listener, uri);
        Session session = listener.getConnectFuture().get(5, TimeUnit.SECONDS);

        // Send a test message (simplified Y.js sync message format)
        // Format: [documentName][messageType][payload]
        byte[] testMessage = createTestSyncMessage("test-doc");
        session.sendBinary(ByteBuffer.wrap(testMessage), null);

        // Give the server time to process the message
        // Note: Full Y.js protocol response testing should be done with a real Yjs client
        // This test just verifies that WebSocket binary messages can be sent without errors
        Thread.sleep(500);

        // Verify connection is still open (no errors occurred)
        assertTrue("Connection should still be open after sending message", session.isOpen());

        // Close
        session.close();
    }

    /**
     * Creates a simple test sync message.
     * Format: [documentName: varString][messageType: 0][syncType: 0][stateVector: empty]
     */
    private byte[] createTestSyncMessage(String documentName) {
        // This is a simplified version - in reality, would use proper lib0 encoding
        byte[] docNameBytes = documentName.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Calculate total size
        int size = 1 + docNameBytes.length + 1 + 1 + 1; // varString + messageType + syncType + empty SV
        byte[] message = new byte[size];
        int offset = 0;

        // Write document name length (simplified - assumes length < 128)
        message[offset++] = (byte) docNameBytes.length;

        // Write document name
        System.arraycopy(docNameBytes, 0, message, offset, docNameBytes.length);
        offset += docNameBytes.length;

        // Write message type (0 = SYNC)
        message[offset++] = 0;

        // Write sync type (0 = SYNC_STEP_1)
        message[offset++] = 0;

        // Write empty state vector (length = 0)
        message[offset++] = 0;

        return message;
    }
}
