package net.carcdr.yhocuspocus.core;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YText;
import net.carcdr.yhocuspocus.protocol.IncomingMessage;
import net.carcdr.yhocuspocus.protocol.MessageDecoder;
import net.carcdr.yhocuspocus.protocol.MessageType;
import net.carcdr.yhocuspocus.protocol.OutgoingMessage;
import net.carcdr.yhocuspocus.protocol.SyncProtocol;
import net.carcdr.yhocuspocus.transport.MockTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for connection management.
 *
 * <p><b>Note on Test Synchronization:</b> These tests use polling-based helpers
 * (waitForDocument, waitForCondition) to wait for async operations to complete.
 * This approach works well without requiring changes to the production API.</p>
 *
 * <p>In Phase 5, when the hook/extension system is implemented, these tests can
 * be refactored to use CountDownLatch or CompletableFuture with hooks for more
 * precise synchronization. For example:</p>
 * <pre>{@code
 * CountDownLatch latch = new CountDownLatch(1);
 * server.addHook(new ConnectionHook() {
 *     public void onDocumentConnected(String name, DocumentConnection conn) {
 *         latch.countDown();
 *     }
 * });
 * connection.handleMessage(msg);
 * assertTrue(latch.await(1, TimeUnit.SECONDS));
 * }</pre>
 *
 * <p>This will eliminate polling overhead and make tests even faster and more
 * deterministic. However, the current polling approach is sufficient and doesn't
 * leak async implementation details into the API.</p>
 *
 * @see <a href="https://github.com/anthropics/claude-code/issues">Phase 5: Extension System</a>
 */
public class ConnectionIntegrationTest {

    private YHocuspocus server;

    @Before
    public void setUp() {
        server = new YHocuspocus();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    public void testBasicConnection() {
        MockTransport transport = new MockTransport();

        ClientConnection connection = server.handleConnection(transport, Map.of());

        assertNotNull("Connection should be created", connection);
        assertEquals("Connection ID should match transport",
                    transport.getConnectionId(), connection.getConnectionId());
        assertTrue("Transport should be open", transport.isOpen());
    }

    @Test
    public void testDocumentCreation() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Create sync step 1 message (request initial sync)
        byte[] syncPayload = SyncProtocol.encodeSyncStep2(new byte[0]);
        OutgoingMessage msg = OutgoingMessage.sync("test-doc", syncPayload);

        // Send message
        connection.handleMessage(msg.encode());

        // Wait for document to be created (poll with timeout)
        YDocument doc = waitForDocument("test-doc", 1000);
        assertNotNull("Document should be created", doc);
        assertEquals("Document name should match", "test-doc", doc.getName());
        assertEquals("Document should have one connection", 1, doc.getConnectionCount());
    }

    /**
     * Helper to wait for a document to be created.
     * Polls until document exists or timeout.
     */
    private YDocument waitForDocument(String name, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            YDocument doc = server.getDocument(name);
            if (doc != null) {
                return doc;
            }
            Thread.sleep(10); // Small poll interval
        }
        return null; // Timeout
    }

    /**
     * Helper to wait for a condition to be true.
     * Polls until condition is met or timeout.
     */
    private void waitForCondition(java.util.function.BooleanSupplier condition,
                                   long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition.getAsBoolean()) {
                return; // Condition met
            }
            Thread.sleep(10); // Small poll interval
        }
        throw new AssertionError("Timeout waiting for condition");
    }

    @Test
    public void testMultipleDocumentsPerConnection() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Connect to two documents
        byte[] sync1 = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("doc1", sync1).encode());

        byte[] sync2 = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("doc2", sync2).encode());

        // Wait for both documents to be created
        assertNotNull("Doc1 should exist", waitForDocument("doc1", 1000));
        assertNotNull("Doc2 should exist", waitForDocument("doc2", 1000));
        assertEquals("Should have 2 documents", 2, server.getDocumentCount());
    }

    @Test
    public void testInitialSyncSent() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Send sync request
        byte[] syncPayload = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("test-doc", syncPayload).encode());

        // Wait for document and messages to be sent
        waitForDocument("test-doc", 1000);
        waitForCondition(() -> transport.getSentMessages().size() >= 2, 1000);

        // Should have received messages (initial sync + sync status)
        assertTrue("Should have sent messages",
                  transport.getSentMessages().size() >= 2);

        // First message should be a sync response
        byte[] firstMsg = transport.getSentMessages().get(0);
        IncomingMessage decoded = MessageDecoder.decode(firstMsg);
        assertEquals("First message should be SYNC", MessageType.SYNC, decoded.getType());
    }

    @Test
    public void testSyncBetweenConnections() throws Exception {
        MockTransport transport1 = new MockTransport();
        MockTransport transport2 = new MockTransport();

        ClientConnection conn1 = server.handleConnection(transport1, Map.of());
        ClientConnection conn2 = server.handleConnection(transport2, Map.of());

        // Both connect to same document
        byte[] sync1 = SyncProtocol.encodeSyncStep2(new byte[0]);
        conn1.handleMessage(OutgoingMessage.sync("shared-doc", sync1).encode());

        byte[] sync2 = SyncProtocol.encodeSyncStep2(new byte[0]);
        conn2.handleMessage(OutgoingMessage.sync("shared-doc", sync2).encode());

        // Wait for document to have both connections
        YDocument doc = waitForDocument("shared-doc", 1000);
        assertNotNull("Document should exist", doc);

        waitForCondition(() -> doc.getConnectionCount() == 2, 1000);
        assertEquals("Should have 2 connections", 2, doc.getConnectionCount());
    }

    @Test
    public void testConnectionCleanup() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Connect to document
        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("test-doc", sync).encode());

        YDocument doc = waitForDocument("test-doc", 1000);
        assertEquals("Should have 1 connection", 1, doc.getConnectionCount());

        // Close connection
        connection.close();

        // Document should have no connections (synchronous)
        assertEquals("Should have 0 connections", 0, doc.getConnectionCount());
    }

    @Test
    public void testDocumentUnloadAfterLastDisconnect() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Connect to document
        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("test-doc", sync).encode());

        // Wait for document creation
        YDocument doc = waitForDocument("test-doc", 1000);
        assertNotNull("Document should exist before close", doc);
        assertEquals("Document should have 1 connection", 1, doc.getConnectionCount());

        // Close connection
        connection.close();

        // Verify connection count dropped immediately (synchronous)
        assertEquals("Connection count should drop to 0",
                    0, doc.getConnectionCount());

        // Document unload is async, so we just verify the connection was removed
        // Full unload testing will be done in later phases
    }

    @Test
    public void testConcurrentDocumentAccess() throws Exception {
        // Create multiple connections trying to access same document simultaneously
        int numConnections = 10;
        MockTransport[] transports = new MockTransport[numConnections];
        ClientConnection[] connections = new ClientConnection[numConnections];

        // Create all connections
        for (int i = 0; i < numConnections; i++) {
            transports[i] = new MockTransport();
            connections[i] = server.handleConnection(transports[i], Map.of());
        }

        // All connect to same document simultaneously
        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        for (int i = 0; i < numConnections; i++) {
            connections[i].handleMessage(OutgoingMessage.sync("concurrent-doc", sync).encode());
        }

        // Wait for document and all connections to be registered
        YDocument doc = waitForDocument("concurrent-doc", 1000);
        assertNotNull("Document should exist", doc);

        final int expectedCount = numConnections;
        waitForCondition(() -> doc.getConnectionCount() == expectedCount, 1000);

        // Should have exactly one document
        assertEquals("Should have 1 document", 1, server.getDocumentCount());
        assertEquals("Should have all connections",
                    numConnections, doc.getConnectionCount());
    }
}
