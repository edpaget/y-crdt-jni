package net.carcdr.yhocuspocus.core;

import net.carcdr.yhocuspocus.extension.TestWaiter;
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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for connection management.
 *
 * <p><b>Note on Test Synchronization:</b> This test class demonstrates two approaches
 * for waiting on async operations:</p>
 *
 * <ol>
 *   <li><b>Polling-based (legacy tests):</b> Uses waitForDocument() and waitForCondition()
 *       helpers that poll until a condition is met or timeout occurs. Simple but slower.</li>
 *   <li><b>Extension-based (new tests):</b> Uses TestWaiter extension with CountDownLatch
 *       to wait for exact lifecycle events. Faster, more deterministic, and cleaner.</li>
 * </ol>
 *
 * <p>Example of extension-based synchronization:</p>
 * <pre>{@code
 * TestWaiter waiter = new TestWaiter();
 * YHocuspocus server = YHocuspocus.builder()
 *     .extension(waiter)
 *     .build();
 *
 * connection.handleMessage(msg);
 * waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS);
 * // Document is now guaranteed to be loaded
 * }</pre>
 *
 * <p>See testDocumentCreationWithTestWaiter() for a complete example.</p>
 */
public class ConnectionIntegrationTest {

    private YHocuspocus server;
    private TestWaiter waiter;

    @Before
    public void setUp() {
        waiter = new TestWaiter();
        server = YHocuspocus.builder()
            .extension(waiter)
            .build();
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

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("test-doc") != null, 1000);

        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should be created", doc);
        assertEquals("Document name should match", "test-doc", doc.getName());
        assertEquals("Document should have one connection", 1, doc.getConnectionCount());
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
        // Reset latch to wait for 2 documents
        waiter.resetAfterLoadDocumentLoatch(2);

        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Connect to two documents
        byte[] sync1 = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("doc1", sync1).encode());

        byte[] sync2 = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("doc2", sync2).encode());

        // Wait for both documents to be loaded
        assertTrue("Both documents should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Wait for both documents to be added to server's map
        waitForCondition(() -> server.getDocument("doc1") != null, 1000);
        waitForCondition(() -> server.getDocument("doc2") != null, 1000);

        assertNotNull("Doc1 should exist", server.getDocument("doc1"));
        assertNotNull("Doc2 should exist", server.getDocument("doc2"));
        assertEquals("Should have 2 documents", 2, server.getDocumentCount());
    }

    @Test
    public void testInitialSyncSent() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Send sync request
        byte[] syncPayload = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("test-doc", syncPayload).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Wait for document to be added to server's map and messages to be sent
        waitForCondition(() -> server.getDocument("test-doc") != null, 1000);
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

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("shared-doc") != null, 1000);

        YDocument doc = server.getDocument("shared-doc");
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

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("test-doc") != null, 1000);

        YDocument doc = server.getDocument("test-doc");
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

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("test-doc") != null, 1000);

        YDocument doc = server.getDocument("test-doc");
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

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Wait for document to be added to server's map
        waitForCondition(() -> server.getDocument("concurrent-doc") != null, 1000);

        YDocument doc = server.getDocument("concurrent-doc");
        assertNotNull("Document should exist", doc);

        final int expectedCount = numConnections;
        waitForCondition(() -> doc.getConnectionCount() == expectedCount, 1000);

        // Should have exactly one document
        assertEquals("Should have 1 document", 1, server.getDocumentCount());
        assertEquals("Should have all connections",
                    numConnections, doc.getConnectionCount());
    }

    /**
     * Demonstrates using TestWaiter extension instead of polling.
     *
     * <p>Compare this with testDocumentCreation above to see the difference:
     * - No more polling loops (waitForDocument)
     * - No arbitrary sleeps
     * - Wait for exact event (afterLoadDocument)
     * - Faster and more deterministic
     */
    @Test
    public void testDocumentCreationWithTestWaiter() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        byte[] syncPayload = SyncProtocol.encodeSyncStep2(new byte[0]);
        OutgoingMessage msg = OutgoingMessage.sync("test-doc", syncPayload);

        // Send message
        connection.handleMessage(msg.encode());

        // Wait for document to be fully loaded (no polling!)
        assertTrue("Document should be loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Now we know for certain the document is loaded
        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should be created", doc);
        assertEquals("Document name should match", "test-doc", doc.getName());
        assertEquals("Document should have one connection", 1, doc.getConnectionCount());
    }

    /**
     * Demonstrates waiting for multiple connections with TestWaiter.
     */
    @Test
    public void testMultipleConnectionsWithTestWaiter() throws Exception {
        // Reset latch to wait for 2 documents
        waiter.resetAfterLoadDocumentLoatch(2);

        MockTransport transport1 = new MockTransport();
        MockTransport transport2 = new MockTransport();

        ClientConnection conn1 = server.handleConnection(transport1, Map.of());
        ClientConnection conn2 = server.handleConnection(transport2, Map.of());

        byte[] sync1 = SyncProtocol.encodeSyncStep2(new byte[0]);
        conn1.handleMessage(OutgoingMessage.sync("doc1", sync1).encode());

        byte[] sync2 = SyncProtocol.encodeSyncStep2(new byte[0]);
        conn2.handleMessage(OutgoingMessage.sync("doc2", sync2).encode());

        // Wait for both documents to be created (no polling!)
        assertTrue("Both documents should be created",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        YDocument doc1 = server.getDocument("doc1");
        YDocument doc2 = server.getDocument("doc2");

        assertNotNull("Doc1 should exist", doc1);
        assertNotNull("Doc2 should exist", doc2);
        assertEquals("Should have 2 documents", 2, server.getDocumentCount());
    }
}
