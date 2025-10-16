package net.carcdr.yhocuspocus.protocol;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for message protocol encoding and decoding.
 */
public class MessageProtocolTest {

    @Test
    public void testEncodeDecode() {
        OutgoingMessage outgoing = OutgoingMessage.sync("test-doc",
                                                        new byte[]{1, 2, 3});
        byte[] encoded = outgoing.encode();

        IncomingMessage incoming = MessageDecoder.decode(encoded);

        assertEquals("Document name should match", "test-doc",
                    incoming.getDocumentName());
        assertEquals("Message type should match", MessageType.SYNC, incoming.getType());
        assertArrayEquals("Payload should match", new byte[]{1, 2, 3},
                         incoming.getPayload());
    }

    @Test
    public void testSyncMessage() {
        byte[] payload = {10, 20, 30};
        OutgoingMessage msg = OutgoingMessage.sync("doc1", payload);

        assertEquals("Document name should be correct", "doc1",
                    msg.getDocumentName());
        assertEquals("Type should be SYNC", MessageType.SYNC, msg.getType());
        assertArrayEquals("Payload should be correct", payload, msg.getPayload());
    }

    @Test
    public void testAwarenessMessage() {
        byte[] payload = {5, 10, 15};
        OutgoingMessage msg = OutgoingMessage.awareness("doc2", payload);

        byte[] encoded = msg.encode();
        IncomingMessage decoded = MessageDecoder.decode(encoded);

        assertEquals("Document name should be correct", "doc2",
                    decoded.getDocumentName());
        assertEquals("Type should be AWARENESS", MessageType.AWARENESS,
                    decoded.getType());
        assertArrayEquals("Payload should be correct", payload,
                         decoded.getPayload());
    }

    @Test
    public void testSyncStatusMessage() {
        OutgoingMessage synced = OutgoingMessage.syncStatus("doc3", true);
        OutgoingMessage notSynced = OutgoingMessage.syncStatus("doc3", false);

        byte[] encodedSynced = synced.encode();
        byte[] encodedNotSynced = notSynced.encode();

        IncomingMessage decodedSynced = MessageDecoder.decode(encodedSynced);
        IncomingMessage decodedNotSynced = MessageDecoder.decode(encodedNotSynced);

        assertEquals("Both should have same document name", "doc3",
                    decodedSynced.getDocumentName());
        assertEquals("Type should be SYNC_STATUS", MessageType.SYNC_STATUS,
                    decodedSynced.getType());

        assertEquals("Synced payload should be 1", 1,
                    decodedSynced.getPayload()[0]);
        assertEquals("Not synced payload should be 0", 0,
                    decodedNotSynced.getPayload()[0]);
    }

    @Test
    public void testStatelessMessage() {
        OutgoingMessage msg = OutgoingMessage.stateless("doc4",
                                                        "custom-payload");
        byte[] encoded = msg.encode();

        IncomingMessage decoded = MessageDecoder.decode(encoded);

        assertEquals("Type should be STATELESS", MessageType.STATELESS,
                    decoded.getType());
        assertEquals("Stateless payload should be correct", "custom-payload",
                    decoded.getStatelessPayload());
    }

    @Test
    public void testBroadcastStatelessMessage() {
        OutgoingMessage msg = OutgoingMessage.broadcastStateless("doc5",
                                                                 "broadcast-data");
        byte[] encoded = msg.encode();

        IncomingMessage decoded = MessageDecoder.decode(encoded);

        assertEquals("Type should be BROADCAST_STATELESS",
                    MessageType.BROADCAST_STATELESS, decoded.getType());
        assertEquals("Stateless payload should be correct", "broadcast-data",
                    decoded.getStatelessPayload());
    }

    @Test
    public void testMessageTypeFromValue() {
        assertEquals("0 should be SYNC", MessageType.SYNC,
                    MessageType.fromValue(0));
        assertEquals("1 should be AWARENESS", MessageType.AWARENESS,
                    MessageType.fromValue(1));
        assertEquals("2 should be AUTH", MessageType.AUTH,
                    MessageType.fromValue(2));
        assertEquals("8 should be SYNC_STATUS", MessageType.SYNC_STATUS,
                    MessageType.fromValue(8));
        assertEquals("Unknown value should be UNKNOWN", MessageType.UNKNOWN,
                    MessageType.fromValue(999));
    }

    @Test
    public void testMessageTypeIsValid() {
        assertTrue("SYNC should be valid", MessageType.SYNC.isValid());
        assertTrue("AWARENESS should be valid", MessageType.AWARENESS.isValid());
        assertFalse("UNKNOWN should not be valid", MessageType.UNKNOWN.isValid());
    }

    @Test
    public void testEmptyPayload() {
        OutgoingMessage msg = OutgoingMessage.sync("doc6", new byte[0]);
        byte[] encoded = msg.encode();

        IncomingMessage decoded = MessageDecoder.decode(encoded);

        assertEquals("Document name should be correct", "doc6",
                    decoded.getDocumentName());
        assertEquals("Payload should be empty", 0, decoded.getPayload().length);
    }

    @Test
    public void testUnicodeDocumentName() {
        String docName = "文档-测试-🚀";
        OutgoingMessage msg = OutgoingMessage.sync(docName, new byte[]{1});
        byte[] encoded = msg.encode();

        IncomingMessage decoded = MessageDecoder.decode(encoded);

        assertEquals("Unicode document name should be preserved", docName,
                    decoded.getDocumentName());
    }

    @Test
    public void testLargePayload() {
        byte[] largePayload = new byte[10000];
        for (int i = 0; i < largePayload.length; i++) {
            largePayload[i] = (byte) (i % 256);
        }

        OutgoingMessage msg = OutgoingMessage.sync("large-doc", largePayload);
        byte[] encoded = msg.encode();

        IncomingMessage decoded = MessageDecoder.decode(encoded);

        assertArrayEquals("Large payload should be preserved", largePayload,
                         decoded.getPayload());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeNull() {
        MessageDecoder.decode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeEmpty() {
        MessageDecoder.decode(new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateMessageWithNullDocumentName() {
        OutgoingMessage.sync(null, new byte[]{1});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateMessageWithNullPayload() {
        OutgoingMessage.sync("doc", null);
    }

    @Test
    public void testRawDataPreserved() {
        byte[] originalData = OutgoingMessage.sync("doc7",
                                                   new byte[]{1, 2, 3}).encode();
        IncomingMessage msg = MessageDecoder.decode(originalData);

        assertArrayEquals("Raw data should be preserved", originalData,
                         msg.getRawData());
    }

    @Test
    public void testToString() {
        OutgoingMessage outgoing = OutgoingMessage.sync("test-doc",
                                                        new byte[]{1, 2, 3});
        String str = outgoing.toString();

        assertTrue("toString should contain document name",
                  str.contains("test-doc"));
        assertTrue("toString should contain type", str.contains("SYNC"));
        assertTrue("toString should contain payload size",
                  str.contains("payloadSize"));
    }

    @Test
    public void testMultipleMessageTypes() {
        MessageType[] types = {
            MessageType.SYNC,
            MessageType.AWARENESS,
            MessageType.AUTH,
            MessageType.QUERY_AWARENESS,
            MessageType.SYNC_STATUS
        };

        for (MessageType type : types) {
            // Can't easily create messages of all types, but test the enum
            assertTrue("Type should be valid", type.isValid());
            assertEquals("FromValue should roundtrip",
                        type, MessageType.fromValue(type.getValue()));
        }
    }
}
