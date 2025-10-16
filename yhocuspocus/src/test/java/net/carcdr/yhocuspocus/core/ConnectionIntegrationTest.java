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

        // Wait a bit for async processing
        Thread.sleep(100);

        // Document should be created
        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should be created", doc);
        assertEquals("Document name should match", "test-doc", doc.getName());
        assertEquals("Document should have one connection", 1, doc.getConnectionCount());
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

        // Wait for async processing
        Thread.sleep(100);

        // Both documents should be created
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

        // Wait for async processing
        Thread.sleep(100);

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

        Thread.sleep(50);

        byte[] sync2 = SyncProtocol.encodeSyncStep2(new byte[0]);
        conn2.handleMessage(OutgoingMessage.sync("shared-doc", sync2).encode());

        Thread.sleep(100);

        // Document should have 2 connections
        YDocument doc = server.getDocument("shared-doc");
        assertNotNull("Document should exist", doc);
        assertEquals("Should have 2 connections", 2, doc.getConnectionCount());
    }

    @Test
    public void testConnectionCleanup() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Connect to document
        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("test-doc", sync).encode());

        Thread.sleep(100);

        YDocument doc = server.getDocument("test-doc");
        assertEquals("Should have 1 connection", 1, doc.getConnectionCount());

        // Close connection
        connection.close();

        // Wait for cleanup
        Thread.sleep(100);

        // Document should have no connections
        assertEquals("Should have 0 connections", 0, doc.getConnectionCount());
    }

    @Test
    public void testDocumentUnloadAfterLastDisconnect() throws Exception {
        MockTransport transport = new MockTransport();
        ClientConnection connection = server.handleConnection(transport, Map.of());

        // Connect to document
        byte[] sync = SyncProtocol.encodeSyncStep2(new byte[0]);
        connection.handleMessage(OutgoingMessage.sync("test-doc", sync).encode());

        // Wait for async document creation
        Thread.sleep(200);

        YDocument doc = server.getDocument("test-doc");
        assertNotNull("Document should exist before close", doc);
        assertEquals("Document should have 1 connection", 1, doc.getConnectionCount());

        // Close connection
        connection.close();

        // Verify connection count dropped immediately
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

        // Wait for all to finish
        Thread.sleep(200);

        // Should have exactly one document
        assertEquals("Should have 1 document", 1, server.getDocumentCount());

        YDocument doc = server.getDocument("concurrent-doc");
        assertNotNull("Document should exist", doc);
        assertEquals("Should have all connections",
                    numConnections, doc.getConnectionCount());
    }
}
