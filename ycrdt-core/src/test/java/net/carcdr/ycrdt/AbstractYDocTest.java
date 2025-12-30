package net.carcdr.ycrdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

/**
 * Abstract test class for YDoc implementations.
 *
 * <p>Subclasses must implement {@link #getBinding()} to provide the
 * implementation-specific binding. All tests in this class will then
 * run against that implementation.
 */
public abstract class AbstractYDocTest {

    /**
     * Returns the YBinding implementation to test.
     *
     * @return the binding to use for creating documents
     */
    protected abstract YBinding getBinding();

    /**
     * Returns whether the implementation supports creating documents with custom client IDs.
     * Subclasses can override this to return false if the feature is not supported.
     *
     * @return true if createDoc(clientId) is supported, false otherwise
     */
    protected boolean supportsCustomClientId() {
        return true;
    }

    @Test
    public void testCreateDoc() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc()) {
            assertNotNull("YDoc should be created", doc);
            assertFalse("YDoc should not be closed", doc.isClosed());
        }
    }

    @Test
    public void testCreateDocWithClientId() {
        assumeTrue("Skipping: implementation does not support custom client IDs",
                supportsCustomClientId());
        YBinding binding = getBinding();
        long clientId = 12345L;
        try (YDoc doc = binding.createDoc(clientId)) {
            assertEquals("Client ID should match", clientId, doc.getClientId());
        }
    }

    @Test
    public void testGetClientId() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc()) {
            long clientId = doc.getClientId();
            assertTrue("Client ID should be non-negative", clientId >= 0);
        }
    }

    @Test
    public void testGetGuid() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc()) {
            String guid = doc.getGuid();
            assertNotNull("GUID should not be null", guid);
            assertFalse("GUID should not be empty", guid.isEmpty());
        }
    }

    @Test
    public void testEncodeStateAsUpdate() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc()) {
            byte[] state = doc.encodeStateAsUpdate();
            assertNotNull("Encoded state should not be null", state);
            assertTrue("Encoded state should have content", state.length > 0);
        }
    }

    @Test
    public void testApplyUpdate() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc()) {

            byte[] state = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(state);
            // Should not throw
        }
    }

    @Test
    public void testSynchronization() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc()) {

            byte[] state1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(state1);

            byte[] state2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(state2);

            assertNotNull("State should not be null", state1);
            assertNotNull("State should not be null", state2);
        }
    }

    @Test
    public void testClose() {
        YBinding binding = getBinding();
        YDoc doc = binding.createDoc();
        assertFalse("Doc should not be closed", doc.isClosed());

        doc.close();
        assertTrue("Doc should be closed", doc.isClosed());

        // Close again should be safe (idempotent)
        doc.close();
        assertTrue("Doc should still be closed", doc.isClosed());
    }

    @Test
    public void testTryWithResources() {
        YBinding binding = getBinding();
        YDoc doc;
        try (YDoc d = binding.createDoc()) {
            doc = d;
            assertFalse("Doc should not be closed inside try block", doc.isClosed());
        }
        assertTrue("Doc should be closed after try block", doc.isClosed());
    }

    @Test
    public void testMultipleDocuments() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YDoc doc3 = binding.createDoc()) {

            long id1 = doc1.getClientId();
            long id2 = doc2.getClientId();
            long id3 = doc3.getClientId();

            assertTrue("Client ID should be non-negative", id1 >= 0);
            assertTrue("Client ID should be non-negative", id2 >= 0);
            assertTrue("Client ID should be non-negative", id3 >= 0);
        }
    }

    @Test
    public void testEncodeStateVector() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc()) {
            byte[] stateVector = doc.encodeStateVector();
            assertNotNull("State vector should not be null", stateVector);
            assertTrue("State vector should have content", stateVector.length > 0);
        }
    }

    @Test
    public void testEncodeStateVectorWithContent() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "Hello");

            byte[] stateVector = doc.encodeStateVector();
            assertNotNull("State vector should not be null", stateVector);
            assertTrue("State vector should have content", stateVector.length > 0);
        }
    }

    @Test
    public void testEncodeDiff() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YText text1 = doc1.getText("test")) {

            text1.insert(0, "Hello World");

            byte[] doc2StateVector = doc2.encodeStateVector();
            byte[] diff = doc1.encodeDiff(doc2StateVector);
            assertNotNull("Differential update should not be null", diff);
            assertTrue("Differential update should have content", diff.length > 0);

            doc2.applyUpdate(diff);

            try (YText text2 = doc2.getText("test")) {
                assertEquals("Text should be synchronized", "Hello World", text2.toString());
            }
        }
    }

    @Test
    public void testEncodeDiffWithPartialSync() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YText text1 = doc1.getText("test")) {

            text1.insert(0, "Hello");

            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            text1.insert(5, " World");

            byte[] doc2StateVector = doc2.encodeStateVector();
            byte[] diff = doc1.encodeDiff(doc2StateVector);
            assertNotNull("Differential update should not be null", diff);

            byte[] fullUpdate = doc1.encodeStateAsUpdate();
            assertTrue("Differential should be smaller or equal to full update",
                    diff.length <= fullUpdate.length);

            doc2.applyUpdate(diff);

            try (YText text2 = doc2.getText("test")) {
                assertEquals("Text should be synchronized", "Hello World", text2.toString());
            }
        }
    }

    @Test
    public void testBidirectionalSync() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YText text1 = doc1.getText("mytext");
             YText text2 = doc2.getText("mytext")) {

            text1.push("Hello");
            text2.push("World");

            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have the same content (order depends on client IDs)
            assertEquals(text1.toString(), text2.toString());
            assertTrue(text1.toString().contains("Hello"));
            assertTrue(text1.toString().contains("World"));
        }
    }

    @Test
    public void testDifferentialSyncWorkflow() {
        YBinding binding = getBinding();
        try (YDoc server = binding.createDoc();
             YDoc client = binding.createDoc()) {

            try (YText serverText = server.getText("content")) {
                serverText.insert(0, "Initial server content");
            }

            byte[] clientStateVector = client.encodeStateVector();
            byte[] serverDiff = server.encodeDiff(clientStateVector);
            client.applyUpdate(serverDiff);

            try (YText clientText = client.getText("content")) {
                assertEquals("Client should have server content",
                        "Initial server content", clientText.toString());
            }

            try (YText clientText = client.getText("content")) {
                clientText.insert(22, " + client changes");
            }

            byte[] serverStateVector = server.encodeStateVector();
            byte[] clientDiff = client.encodeDiff(serverStateVector);
            server.applyUpdate(clientDiff);

            try (YText serverText = server.getText("content")) {
                assertEquals("Server should have all changes",
                        "Initial server content + client changes", serverText.toString());
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testOperationAfterClose() {
        YBinding binding = getBinding();
        YDoc doc = binding.createDoc();
        doc.close();
        doc.getClientId(); // Should throw IllegalStateException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplyNullUpdate() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc()) {
            doc.applyUpdate(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeDiffNullStateVector() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc()) {
            doc.encodeDiff(null);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testEncodeDiffAfterClose() {
        YBinding binding = getBinding();
        YDoc doc = binding.createDoc();
        byte[] sv = new byte[]{0, 0};
        doc.close();
        doc.encodeDiff(sv);
    }
}
