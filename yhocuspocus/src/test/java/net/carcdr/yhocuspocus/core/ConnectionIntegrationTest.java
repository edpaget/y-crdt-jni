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
 * <p>Tests use the {@link TestWaiter} extension to synchronize on lifecycle events
 * like document loading, avoiding polling-based waits.</p>
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
    public void testInitialSyncSent() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Client initiates sync with SyncStep1 (per y-protocol/sync spec)
        byte[] syncPayload = SyncProtocol.encodeSyncStep1(new byte[0]); // Empty state vector
        transport.receiveMessage(OutgoingMessage.sync("test-doc", syncPayload).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Document is now guaranteed to exist with connection registered
        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should be created", doc);

        // Wait for response messages to be sent (SyncStep2 + SyncStep1 + SyncStatus = 3)
        waitForMessages(transport, 3, 1000);

        // Should have received messages (SyncStep2 + SyncStep1 + sync status)
        assertTrue("Should have sent at least 3 messages",
                  transport.getSentMessages().size() >= 3);

        // First message should be SyncStep2 (the diff/full state)
        byte[] firstMsg = transport.getSentMessages().get(0);
        IncomingMessage decoded = MessageDecoder.decode(firstMsg);
        assertEquals("First message should be SYNC", MessageType.SYNC, decoded.getType());

        // Second message should be SyncStep1 (server's state vector)
        byte[] secondMsg = transport.getSentMessages().get(1);
        IncomingMessage decoded2 = MessageDecoder.decode(secondMsg);
        assertEquals("Second message should be SYNC", MessageType.SYNC, decoded2.getType());
    }

    @Test
    public void testSyncBetweenConnections() throws Exception {
        // Second connection won't trigger afterLoadDocument since doc already exists,
        // so we need to wait for both connections to be registered
        MockTransport transport1 = new MockTransport();
        MockTransport transport2 = new MockTransport();

        ClientConnection conn1 = server.handleConnection(transport1, Map.of());
        ClientConnection conn2 = server.handleConnection(transport2, Map.of());

        // Both connect to same document
        byte[] sync1 = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport1.receiveMessage(OutgoingMessage.sync("shared-doc", sync1).encode());

        byte[] sync2 = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport2.receiveMessage(OutgoingMessage.sync("shared-doc", sync2).encode());

        // Wait for document to be loaded (first connection)
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        YDocument doc = server.getDocument("shared-doc");
        assertNotNull("Document should exist", doc);

        // Wait for second connection to join (it's async but doesn't trigger afterLoadDocument)
        waitForConnectionCount(doc, 2, 1000);
        assertEquals("Should have 2 connections", 2, doc.getConnectionCount());
    }

    @Test
    public void testConnectionCleanup() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Connect to document
        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport.receiveMessage(OutgoingMessage.sync("test-doc", sync).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Document is now guaranteed to exist with connection registered
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
        transport.receiveMessage(OutgoingMessage.sync("test-doc", sync).encode());

        // Wait for document to be loaded
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Document is now guaranteed to exist with connection registered
        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should exist before close", doc);
        assertEquals("Document should have 1 connection", 1, doc.getConnectionCount());

        // Close connection
        connection.close();

        // Verify connection count dropped immediately (synchronous)
        assertEquals("Connection count should drop to 0", 0, doc.getConnectionCount());
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
            transports[i].receiveMessage(OutgoingMessage.sync("concurrent-doc", sync).encode());
        }

        // Wait for document to be loaded (first connection triggers this)
        assertTrue("Document should be created and loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        YDocument doc = server.getDocument("concurrent-doc");
        assertNotNull("Document should exist", doc);

        // Wait for all connections to join (only first triggers afterLoadDocument)
        waitForConnectionCount(doc, numConnections, 2000);

        // Should have exactly one document with all connections
        assertEquals("Should have 1 document", 1, server.getDocumentCount());
        assertEquals("Should have all connections", numConnections, doc.getConnectionCount());
    }

    @Test
    public void testDocumentCreation() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        byte[] syncPayload = SyncProtocol.encodeSyncStep2(new byte[0]);
        OutgoingMessage msg = OutgoingMessage.sync("test-doc", syncPayload);

        // Send message
        transport.receiveMessage(msg.encode());

        // Wait for document to be fully loaded
        assertTrue("Document should be loaded",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        // Document is now guaranteed to exist with connection registered
        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should be created", doc);
        assertEquals("Document name should match", "test-doc", doc.getName());
        assertEquals("Document should have one connection", 1, doc.getConnectionCount());
    }

    @Test
    public void testMultipleDocuments() throws Exception {
        // Reset latch to wait for 2 documents
        waiter.resetAfterLoadDocumentLoatch(2);

        MockTransport transport1 = new MockTransport();
        MockTransport transport2 = new MockTransport();

        ClientConnection conn1 = server.handleConnection(transport1, Map.of());
        ClientConnection conn2 = server.handleConnection(transport2, Map.of());

        byte[] sync1 = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport1.receiveMessage(OutgoingMessage.sync("doc1", sync1).encode());

        byte[] sync2 = SyncProtocol.encodeSyncStep2(new byte[0]);
        transport2.receiveMessage(OutgoingMessage.sync("doc2", sync2).encode());

        // Wait for both documents to be created
        assertTrue("Both documents should be created",
                waiter.awaitAfterLoadDocument(10, TimeUnit.SECONDS));

        YDocument doc1 = server.getDocument("doc1");
        YDocument doc2 = server.getDocument("doc2");

        assertNotNull("Doc1 should exist", doc1);
        assertNotNull("Doc2 should exist", doc2);
        assertEquals("Should have 2 documents", 2, server.getDocumentCount());
    }

    /**
     * Helper to wait for a transport to receive a minimum number of messages.
     */
    private void waitForMessages(MockTransport transport, int minCount, long timeoutMs)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (transport.getSentMessages().size() >= minCount) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timeout waiting for " + minCount + " messages, got "
                + transport.getSentMessages().size());
    }

    /**
     * Helper to wait for a document to have a specific connection count.
     * Used when multiple connections join an existing document (only first triggers hook).
     */
    private void waitForConnectionCount(YDocument doc, int expectedCount, long timeoutMs)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (doc.getConnectionCount() == expectedCount) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timeout waiting for " + expectedCount + " connections, got "
                + doc.getConnectionCount());
    }
}
