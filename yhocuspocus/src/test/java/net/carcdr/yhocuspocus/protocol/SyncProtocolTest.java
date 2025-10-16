package net.carcdr.yhocuspocus.protocol;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YText;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for sync protocol.
 */
public class SyncProtocolTest {

    private YDoc doc1;
    private YDoc doc2;

    @Before
    public void setUp() {
        doc1 = new YDoc();
        doc2 = new YDoc();
    }

    @After
    public void tearDown() {
        if (doc1 != null) {
            doc1.close();
        }
        if (doc2 != null) {
            doc2.close();
        }
    }

    @Test
    public void testEncodeSyncStep2() {
        byte[] update = new byte[]{1, 2, 3};
        byte[] encoded = SyncProtocol.encodeSyncStep2(update);

        assertNotNull("Encoded should not be null", encoded);
        assertTrue("Encoded should have content", encoded.length > 0);

        // Verify structure
        VarIntReader reader = new VarIntReader(encoded);
        int syncType = reader.readVarInt();
        assertTrue("Should be SYNC_STEP_2 (1)", syncType == 1);
    }

    @Test
    public void testEncodeUpdate() {
        byte[] update = new byte[]{1, 2, 3};
        byte[] encoded = SyncProtocol.encodeUpdate(update);

        assertNotNull("Encoded should not be null", encoded);
        assertTrue("Encoded should have content", encoded.length > 0);

        // Verify structure
        VarIntReader reader = new VarIntReader(encoded);
        int syncType = reader.readVarInt();
        assertTrue("Should be UPDATE (2)", syncType == 2);
    }

    @Test
    public void testApplySyncMessageStep2() {
        // Create document with some content
        YText text1 = doc1.getText("content");
        text1.insert(0, "Hello");

        // Get update
        byte[] update = doc1.encodeStateAsUpdate();
        byte[] syncPayload = SyncProtocol.encodeSyncStep2(update);

        // Apply to second document
        byte[] response = SyncProtocol.applySyncMessage(doc2, syncPayload);

        // Response should be null (no response needed for step 2)
        assertTrue("Response should be null or empty",
                  response == null || response.length == 0);

        // Verify content synced
        YText text2 = doc2.getText("content");
        assertTrue("Content should match", text2.toString().equals("Hello"));
    }

    @Test
    public void testApplySyncMessageUpdate() {
        // Create initial state in both docs
        YText text1 = doc1.getText("content");
        text1.insert(0, "Hello");

        byte[] initialUpdate = doc1.encodeStateAsUpdate();
        doc2.applyUpdate(initialUpdate);

        // Make a change in doc1
        text1.insert(5, " World");

        // Get differential update
        byte[] changeUpdate = doc1.encodeStateAsUpdate();
        byte[] updatePayload = SyncProtocol.encodeUpdate(changeUpdate);

        // Apply to doc2
        byte[] response = SyncProtocol.applySyncMessage(doc2, updatePayload);

        // Should have no response
        assertTrue("Response should be null", response == null);

        // Verify content synced
        YText text2 = doc2.getText("content");
        assertTrue("Content should include World",
                  text2.toString().contains("World"));
    }

    @Test
    public void testHasChangesWithUpdate() {
        // Create update payload
        byte[] update = new byte[]{1, 2, 3};
        byte[] payload = SyncProtocol.encodeUpdate(update);

        assertTrue("Should detect changes", SyncProtocol.hasChanges(payload));
    }

    @Test
    public void testHasChangesWithEmptyPayload() {
        byte[] payload = new byte[0];

        assertFalse("Empty payload should have no changes",
                   SyncProtocol.hasChanges(payload));
    }

    @Test
    public void testHasChangesWithNullPayload() {
        assertFalse("Null payload should have no changes",
                   SyncProtocol.hasChanges(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplySyncMessageWithNullDoc() {
        byte[] payload = SyncProtocol.encodeSyncStep2(new byte[0]);
        SyncProtocol.applySyncMessage(null, payload);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplySyncMessageWithNullPayload() {
        SyncProtocol.applySyncMessage(doc1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplySyncMessageWithEmptyPayload() {
        SyncProtocol.applySyncMessage(doc1, new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeSyncStep2WithNull() {
        SyncProtocol.encodeSyncStep2(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeUpdateWithNull() {
        SyncProtocol.encodeUpdate(null);
    }
}
