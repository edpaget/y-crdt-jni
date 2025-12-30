package net.carcdr.ycrdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests that verify synchronization works correctly between JNI and Panama implementations.
 *
 * <p>These tests ensure that documents created in one implementation can be synced
 * to the other implementation and vice versa, verifying binary compatibility of
 * updates and state vectors.</p>
 */
public class CrossImplementationSyncTest {

    private YBinding jni;
    private YBinding panama;

    @Before
    public void setUp() {
        jni = YBindingFactory.jni();
        panama = YBindingFactory.panama();
    }

    @Test
    public void testJniToPanamaTextSync() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc();
             YText jniText = jniDoc.getText("content");
             YText panamaText = panamaDoc.getText("content")) {

            // Make changes in JNI
            jniText.insert(0, "Hello from JNI");

            // Sync to Panama
            byte[] update = jniDoc.encodeStateAsUpdate();
            panamaDoc.applyUpdate(update);

            // Verify Panama has the content
            assertEquals("Hello from JNI", panamaText.toString());
        }
    }

    @Test
    public void testPanamaToJniTextSync() {
        try (YDoc panamaDoc = panama.createDoc();
             YDoc jniDoc = jni.createDoc();
             YText panamaText = panamaDoc.getText("content");
             YText jniText = jniDoc.getText("content")) {

            // Make changes in Panama
            panamaText.insert(0, "Hello from Panama");

            // Sync to JNI
            byte[] update = panamaDoc.encodeStateAsUpdate();
            jniDoc.applyUpdate(update);

            // Verify JNI has the content
            assertEquals("Hello from Panama", jniText.toString());
        }
    }

    @Test
    public void testBidirectionalTextSync() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc();
             YText jniText = jniDoc.getText("content");
             YText panamaText = panamaDoc.getText("content")) {

            // Make changes in both
            jniText.push("JNI");
            panamaText.push("Panama");

            // Sync JNI -> Panama
            byte[] jniUpdate = jniDoc.encodeStateAsUpdate();
            panamaDoc.applyUpdate(jniUpdate);

            // Sync Panama -> JNI
            byte[] panamaUpdate = panamaDoc.encodeStateAsUpdate();
            jniDoc.applyUpdate(panamaUpdate);

            // Both should have same content
            assertEquals(jniText.toString(), panamaText.toString());
            String content = jniText.toString();
            assertTrue("Should contain JNI text", content.contains("JNI"));
            assertTrue("Should contain Panama text", content.contains("Panama"));
        }
    }

    @Test
    public void testJniToPanamaArraySync() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc();
             YArray jniArray = jniDoc.getArray("items");
             YArray panamaArray = panamaDoc.getArray("items")) {

            // Add items in JNI
            jniArray.pushString("item1");
            jniArray.pushString("item2");
            jniArray.pushDouble(42.0);

            // Sync to Panama
            byte[] update = jniDoc.encodeStateAsUpdate();
            panamaDoc.applyUpdate(update);

            // Verify Panama has the items
            assertEquals(3, panamaArray.length());
        }
    }

    @Test
    public void testPanamaToJniArraySync() {
        try (YDoc panamaDoc = panama.createDoc();
             YDoc jniDoc = jni.createDoc();
             YArray panamaArray = panamaDoc.getArray("items");
             YArray jniArray = jniDoc.getArray("items")) {

            // Add items in Panama
            panamaArray.pushString("a");
            panamaArray.pushString("b");
            panamaArray.pushDouble(3.14);

            // Sync to JNI
            byte[] update = panamaDoc.encodeStateAsUpdate();
            jniDoc.applyUpdate(update);

            // Verify JNI has the items and can read them
            assertEquals(3, jniArray.length());
            assertEquals("a", jniArray.getString(0));
            assertEquals("b", jniArray.getString(1));
            assertEquals(3.14, jniArray.getDouble(2), 0.001);
        }
    }

    @Test
    public void testJniToPanamaMapSync() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc();
             YMap jniMap = jniDoc.getMap("config");
             YMap panamaMap = panamaDoc.getMap("config")) {

            // Add entries in JNI
            jniMap.setString("name", "test");
            jniMap.setDouble("version", 1.0);

            // Sync to Panama
            byte[] update = jniDoc.encodeStateAsUpdate();
            panamaDoc.applyUpdate(update);

            // Verify Panama has the entries
            assertEquals(2, panamaMap.size());
            assertTrue(panamaMap.containsKey("name"));
            assertTrue(panamaMap.containsKey("version"));
        }
    }

    @Test
    public void testPanamaToJniMapSync() {
        try (YDoc panamaDoc = panama.createDoc();
             YDoc jniDoc = jni.createDoc();
             YMap panamaMap = panamaDoc.getMap("config");
             YMap jniMap = jniDoc.getMap("config")) {

            // Add entries in Panama
            panamaMap.setString("host", "localhost");
            panamaMap.setDouble("port", 8080.0);

            // Sync to JNI
            byte[] update = panamaDoc.encodeStateAsUpdate();
            jniDoc.applyUpdate(update);

            // Verify JNI has the entries and can read them
            assertEquals(2, jniMap.size());
            assertEquals("localhost", jniMap.getString("host"));
            assertEquals(8080.0, jniMap.getDouble("port"), 0.001);
        }
    }

    @Test
    public void testDifferentialSyncJniToPanama() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc();
             YText jniText = jniDoc.getText("content");
             YText panamaText = panamaDoc.getText("content")) {

            // Initial sync
            jniText.insert(0, "Initial");
            byte[] initialUpdate = jniDoc.encodeStateAsUpdate();
            panamaDoc.applyUpdate(initialUpdate);
            assertEquals("Initial", panamaText.toString());

            // Get Panama's state vector
            byte[] panamaStateVector = panamaDoc.encodeStateVector();

            // Add more content in JNI
            jniText.push(" + more");

            // Get differential update
            byte[] diff = jniDoc.encodeDiff(panamaStateVector);
            assertNotNull("Diff should not be null", diff);

            // Apply diff to Panama
            panamaDoc.applyUpdate(diff);

            // Verify synchronization
            assertEquals("Initial + more", panamaText.toString());
        }
    }

    @Test
    public void testDifferentialSyncPanamaToJni() {
        try (YDoc panamaDoc = panama.createDoc();
             YDoc jniDoc = jni.createDoc();
             YText panamaText = panamaDoc.getText("content");
             YText jniText = jniDoc.getText("content")) {

            // Initial sync
            panamaText.insert(0, "Start");
            byte[] initialUpdate = panamaDoc.encodeStateAsUpdate();
            jniDoc.applyUpdate(initialUpdate);
            assertEquals("Start", jniText.toString());

            // Get JNI's state vector
            byte[] jniStateVector = jniDoc.encodeStateVector();

            // Add more content in Panama
            panamaText.push(" + end");

            // Get differential update
            byte[] diff = panamaDoc.encodeDiff(jniStateVector);
            assertNotNull("Diff should not be null", diff);

            // Apply diff to JNI
            jniDoc.applyUpdate(diff);

            // Verify synchronization
            assertEquals("Start + end", jniText.toString());
        }
    }

    @Test
    public void testComplexDocumentSync() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc()) {

            // Create complex document in JNI
            try (YText title = jniDoc.getText("title");
                 YArray items = jniDoc.getArray("items");
                 YMap config = jniDoc.getMap("config")) {

                title.insert(0, "My Document");
                items.pushString("Item 1");
                items.pushString("Item 2");
                config.setString("author", "Test");
                config.setDouble("version", 2.0);
            }

            // Sync to Panama
            byte[] update = jniDoc.encodeStateAsUpdate();
            panamaDoc.applyUpdate(update);

            // Verify all content in Panama
            try (YText title = panamaDoc.getText("title");
                 YArray items = panamaDoc.getArray("items");
                 YMap config = panamaDoc.getMap("config")) {

                assertEquals("My Document", title.toString());
                assertEquals(2, items.length());
                assertEquals(2, config.size());
                assertTrue(config.containsKey("author"));
                assertTrue(config.containsKey("version"));
            }
        }
    }

    @Test
    public void testMultipleRoundTripSync() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc();
             YText jniText = jniDoc.getText("content");
             YText panamaText = panamaDoc.getText("content")) {

            // Round 1: JNI -> Panama
            jniText.push("A");
            panamaDoc.applyUpdate(jniDoc.encodeStateAsUpdate());

            // Round 2: Panama -> JNI
            panamaText.push("B");
            jniDoc.applyUpdate(panamaDoc.encodeStateAsUpdate());

            // Round 3: JNI -> Panama
            jniText.push("C");
            panamaDoc.applyUpdate(jniDoc.encodeStateAsUpdate());

            // Round 4: Panama -> JNI
            panamaText.push("D");
            jniDoc.applyUpdate(panamaDoc.encodeStateAsUpdate());

            // Both should have all content
            assertEquals(jniText.toString(), panamaText.toString());
            String content = jniText.toString();
            assertTrue(content.contains("A"));
            assertTrue(content.contains("B"));
            assertTrue(content.contains("C"));
            assertTrue(content.contains("D"));
        }
    }

    @Test
    public void testStateVectorCompatibility() {
        try (YDoc jniDoc = jni.createDoc();
             YDoc panamaDoc = panama.createDoc()) {

            // Add same content to both
            try (YText jniText = jniDoc.getText("content")) {
                jniText.insert(0, "Hello");
            }

            // Sync JNI to Panama
            panamaDoc.applyUpdate(jniDoc.encodeStateAsUpdate());

            // Both state vectors should work for differential sync
            byte[] jniSv = jniDoc.encodeStateVector();
            byte[] panamaSv = panamaDoc.encodeStateVector();

            assertNotNull("JNI state vector should not be null", jniSv);
            assertNotNull("Panama state vector should not be null", panamaSv);

            // Both should produce valid (possibly empty) diffs
            byte[] diffFromJni = jniDoc.encodeDiff(panamaSv);
            byte[] diffFromPanama = panamaDoc.encodeDiff(jniSv);

            assertNotNull("Diff from JNI should not be null", diffFromJni);
            assertNotNull("Diff from Panama should not be null", diffFromPanama);
        }
    }
}
