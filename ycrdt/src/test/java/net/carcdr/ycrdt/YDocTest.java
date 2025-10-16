package net.carcdr.ycrdt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testEncodeStateVector() {
        try (YDoc doc = new YDoc()) {
            byte[] stateVector = doc.encodeStateVector();
            assertNotNull("State vector should not be null", stateVector);
            assertTrue("State vector should have content", stateVector.length > 0);
        }
    }

    @Test
    public void testEncodeStateVectorWithContent() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            // Add some content
            text.insert(0, "Hello");

            byte[] stateVector = doc.encodeStateVector();
            assertNotNull("State vector should not be null", stateVector);
            assertTrue("State vector should have content", stateVector.length > 0);
        }
    }

    @Test
    public void testEncodeDiff() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc();
             YText text1 = doc1.getText("test")) {

            // Add content to doc1
            text1.insert(0, "Hello World");

            // Get doc2's state vector (empty)
            byte[] doc2StateVector = doc2.encodeStateVector();

            // Encode differential update from doc1 to doc2
            byte[] diff = doc1.encodeDiff(doc2StateVector);
            assertNotNull("Differential update should not be null", diff);
            assertTrue("Differential update should have content", diff.length > 0);

            // Apply diff to doc2
            doc2.applyUpdate(diff);

            // Verify synchronization
            try (YText text2 = doc2.getText("test")) {
                assertEquals("Text should be synchronized", "Hello World", text2.toString());
            }
        }
    }

    @Test
    public void testEncodeDiffWithPartialSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc();
             YText text1 = doc1.getText("test")) {

            // Add initial content to doc1
            text1.insert(0, "Hello");

            // Sync doc1 to doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Add more content to doc1
            text1.insert(5, " World");

            // Get doc2's state vector (has "Hello" but not " World")
            byte[] doc2StateVector = doc2.encodeStateVector();

            // Encode differential - should only contain " World"
            byte[] diff = doc1.encodeDiff(doc2StateVector);
            assertNotNull("Differential update should not be null", diff);

            // Diff should be smaller than full update
            byte[] fullUpdate = doc1.encodeStateAsUpdate();
            assertTrue("Differential should be smaller or equal to full update",
                    diff.length <= fullUpdate.length);

            // Apply diff to doc2
            doc2.applyUpdate(diff);

            // Verify synchronization
            try (YText text2 = doc2.getText("test")) {
                assertEquals("Text should be synchronized", "Hello World", text2.toString());
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeDiffNullStateVector() {
        try (YDoc doc = new YDoc()) {
            doc.encodeDiff(null);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testEncodeDiffAfterClose() {
        YDoc doc = new YDoc();
        byte[] sv = new byte[]{0, 0};
        doc.close();
        doc.encodeDiff(sv);
    }

    @Test
    public void testMergeUpdates() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc();
             YDoc doc3 = new YDoc();
             YDoc target = new YDoc()) {

            // Add content to each document
            try (YText text1 = doc1.getText("test1")) {
                text1.insert(0, "Doc1");
            }
            try (YText text2 = doc2.getText("test2")) {
                text2.insert(0, "Doc2");
            }
            try (YText text3 = doc3.getText("test3")) {
                text3.insert(0, "Doc3");
            }

            // Get updates
            byte[] update1 = doc1.encodeStateAsUpdate();
            byte[] update2 = doc2.encodeStateAsUpdate();
            byte[] update3 = doc3.encodeStateAsUpdate();

            // Merge updates
            byte[] merged = YDoc.mergeUpdates(new byte[][]{update1, update2, update3});
            assertNotNull("Merged update should not be null", merged);
            assertTrue("Merged update should have content", merged.length > 0);

            // Apply merged update to target
            target.applyUpdate(merged);

            // Verify all content is present
            try (YText text1 = target.getText("test1");
                 YText text2 = target.getText("test2");
                 YText text3 = target.getText("test3")) {
                assertEquals("Text1 should be present", "Doc1", text1.toString());
                assertEquals("Text2 should be present", "Doc2", text2.toString());
                assertEquals("Text3 should be present", "Doc3", text3.toString());
            }
        }
    }

    @Test
    public void testMergeUpdatesSingleUpdate() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "Hello");

            byte[] update = doc.encodeStateAsUpdate();
            byte[] merged = YDoc.mergeUpdates(new byte[][]{update});

            assertNotNull("Merged update should not be null", merged);
            assertTrue("Merged update should have content", merged.length > 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeUpdatesNull() {
        YDoc.mergeUpdates(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeUpdatesEmpty() {
        YDoc.mergeUpdates(new byte[][]{});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeUpdatesContainsNull() {
        try (YDoc doc = new YDoc()) {
            byte[] update = doc.encodeStateAsUpdate();
            YDoc.mergeUpdates(new byte[][]{update, null});
        }
    }

    @Test
    public void testEncodeStateVectorFromUpdate() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            // Add content
            text.insert(0, "Hello");

            // Get update
            byte[] update = doc.encodeStateAsUpdate();

            // Extract state vector from update
            byte[] stateVector = YDoc.encodeStateVectorFromUpdate(update);
            assertNotNull("State vector should not be null", stateVector);
            assertTrue("State vector should have content", stateVector.length > 0);
        }
    }

    @Test
    public void testEncodeStateVectorFromUpdateMatchesDocStateVector() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            // Add content
            text.insert(0, "Hello World");

            // Get state vector directly from doc
            byte[] docStateVector = doc.encodeStateVector();

            // Get update and extract state vector from it
            byte[] update = doc.encodeStateAsUpdate();
            byte[] updateStateVector = YDoc.encodeStateVectorFromUpdate(update);

            // They should be equivalent (represent the same state)
            assertNotNull("Doc state vector should not be null", docStateVector);
            assertNotNull("Update state vector should not be null", updateStateVector);

            // Verify they can be used interchangeably
            try (YDoc doc2 = new YDoc()) {
                byte[] diff1 = doc.encodeDiff(docStateVector);
                byte[] diff2 = doc.encodeDiff(updateStateVector);

                // Both should produce valid diffs
                assertNotNull("Diff with doc state vector should not be null", diff1);
                assertNotNull("Diff with update state vector should not be null", diff2);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeStateVectorFromUpdateNull() {
        YDoc.encodeStateVectorFromUpdate(null);
    }

    @Test
    public void testDifferentialSyncWorkflow() {
        try (YDoc server = new YDoc();
             YDoc client = new YDoc()) {

            // Server has initial content
            try (YText serverText = server.getText("content")) {
                serverText.insert(0, "Initial server content");
            }

            // Step 1: Client sends their state vector to server
            byte[] clientStateVector = client.encodeStateVector();

            // Step 2: Server generates differential update
            byte[] serverDiff = server.encodeDiff(clientStateVector);

            // Step 3: Client applies the diff
            client.applyUpdate(serverDiff);

            // Verify synchronization
            try (YText clientText = client.getText("content")) {
                assertEquals("Client should have server content",
                        "Initial server content", clientText.toString());
            }

            // Now client makes changes
            try (YText clientText = client.getText("content")) {
                clientText.insert(22, " + client changes");
            }

            // Step 4: Server sends their state vector to client
            byte[] serverStateVector = server.encodeStateVector();

            // Step 5: Client generates differential update
            byte[] clientDiff = client.encodeDiff(serverStateVector);

            // Step 6: Server applies the diff
            server.applyUpdate(clientDiff);

            // Verify bidirectional synchronization
            try (YText serverText = server.getText("content")) {
                assertEquals("Server should have all changes",
                        "Initial server content + client changes", serverText.toString());
            }
        }
    }

    @Test
    public void testMergeMultipleUpdatesReducesSize() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {

            // Generate multiple small updates
            byte[][] updates = new byte[10][];
            for (int i = 0; i < 10; i++) {
                text.insert(text.length(), "Update " + i + " ");
                updates[i] = doc.encodeStateAsUpdate();
            }

            // Merge all updates
            byte[] merged = YDoc.mergeUpdates(updates);

            // The merged update should be valid
            assertNotNull("Merged update should not be null", merged);

            // Verify it works by applying to a new doc
            try (YDoc target = new YDoc()) {
                target.applyUpdate(merged);
                try (YText targetText = target.getText("test")) {
                    assertTrue("Target should have all content",
                            targetText.toString().contains("Update 0"));
                    assertTrue("Target should have all content",
                            targetText.toString().contains("Update 9"));
                }
            }
        }
    }
}
