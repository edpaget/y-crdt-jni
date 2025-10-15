package net.carcdr.ycrdt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the YDoc class.
 */
public class YDocTest {

    @Test
    public void testCreateYDoc() {
        try (YDoc doc = new YDoc()) {
            assertNotNull("YDoc should be created", doc);
            assertFalse("YDoc should not be closed", doc.isClosed());
        }
    }

    @Test
    public void testYDocWithClientId() {
        long clientId = 12345L;
        try (YDoc doc = new YDoc(clientId)) {
            assertEquals("Client ID should match", clientId, doc.getClientId());
        }
    }

    @Test
    public void testGetClientId() {
        try (YDoc doc = new YDoc()) {
            long clientId = doc.getClientId();
            assertTrue("Client ID should be non-negative", clientId >= 0);
        }
    }

    @Test
    public void testGetGuid() {
        try (YDoc doc = new YDoc()) {
            String guid = doc.getGuid();
            assertNotNull("GUID should not be null", guid);
            assertFalse("GUID should not be empty", guid.isEmpty());
        }
    }

    @Test
    public void testEncodeStateAsUpdate() {
        try (YDoc doc = new YDoc()) {
            byte[] state = doc.encodeStateAsUpdate();
            assertNotNull("Encoded state should not be null", state);
            assertTrue("Encoded state should have content", state.length > 0);
        }
    }

    @Test
    public void testApplyUpdate() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Get state from doc1
            byte[] state = doc1.encodeStateAsUpdate();

            // Apply to doc2 - should not throw
            doc2.applyUpdate(state);
        }
    }

    @Test
    public void testSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Simulate changes in doc1 by getting its state
            byte[] state1 = doc1.encodeStateAsUpdate();

            // Apply doc1's state to doc2
            doc2.applyUpdate(state1);

            // Get doc2's state
            byte[] state2 = doc2.encodeStateAsUpdate();

            // Apply doc2's state back to doc1
            doc1.applyUpdate(state2);

            // Both documents should now be synchronized
            assertNotNull("State should not be null", state1);
            assertNotNull("State should not be null", state2);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testOperationAfterClose() {
        YDoc doc = new YDoc();
        doc.close();
        doc.getClientId(); // Should throw IllegalStateException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeClientId() {
        new YDoc(-1); // Should throw IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplyNullUpdate() {
        try (YDoc doc = new YDoc()) {
            doc.applyUpdate(null); // Should throw IllegalArgumentException
        }
    }

    @Test
    public void testClose() {
        YDoc doc = new YDoc();
        assertFalse("Doc should not be closed", doc.isClosed());

        doc.close();
        assertTrue("Doc should be closed", doc.isClosed());

        // Close again should be safe (idempotent)
        doc.close();
        assertTrue("Doc should still be closed", doc.isClosed());
    }

    @Test
    public void testTryWithResources() {
        YDoc doc;
        try (YDoc d = new YDoc()) {
            doc = d;
            assertFalse("Doc should not be closed inside try block", doc.isClosed());
        }
        assertTrue("Doc should be closed after try block", doc.isClosed());
    }

    @Test
    public void testMultipleDocuments() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc();
             YDoc doc3 = new YDoc()) {

            // All should have different client IDs (most likely)
            long id1 = doc1.getClientId();
            long id2 = doc2.getClientId();
            long id3 = doc3.getClientId();

            // At least verify they're all valid
            assertTrue("Client ID should be non-negative", id1 >= 0);
            assertTrue("Client ID should be non-negative", id2 >= 0);
            assertTrue("Client ID should be non-negative", id3 >= 0);
        }
    }
}
