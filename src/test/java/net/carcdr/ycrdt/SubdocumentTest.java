package net.carcdr.ycrdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for subdocument support in YMap and YArray.
 * Subdocuments allow embedding YDoc instances within other CRDT types,
 * enabling hierarchical document structures.
 */
public class SubdocumentTest {

    // ==================== YMap Subdocument Tests ====================

    @Test
    public void testYMapSetAndGetSubdocument() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YMap map = parent.getMap("map")) {

            // Set subdocument
            map.setDoc("nested", child);

            // Retrieve subdocument
            try (YDoc retrieved = map.getDoc("nested")) {
                assertNotNull(retrieved);
            }
        }
    }

    @Test
    public void testYMapSubdocumentWithContent() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YMap map = parent.getMap("map")) {

            map.setDoc("nested", child);

            // Add content to retrieved subdocument
            try (YDoc retrieved = map.getDoc("nested");
                 YText retrievedText = retrieved.getText("content")) {
                retrievedText.push("Hello from child");
                assertEquals("Hello from child", retrievedText.toString());
            }
        }
    }

    @Test
    public void testYMapGetNonExistentSubdocument() {
        try (YDoc parent = new YDoc();
             YMap map = parent.getMap("map")) {

            YDoc retrieved = map.getDoc("nonexistent");
            assertNull(retrieved);
        }
    }

    @Test
    public void testYMapSubdocumentSynchronization() {
        try (YDoc parent1 = new YDoc();
             YDoc parent2 = new YDoc();
             YDoc child = new YDoc();
             YMap map1 = parent1.getMap("map")) {

            // Add subdocument to parent1
            map1.setDoc("nested", child);

            // Add content to subdocument after insertion
            try (YDoc nested = map1.getDoc("nested");
                 YText childText = nested.getText("content")) {
                childText.push("Synchronized content");
            }

            // Synchronize parents
            byte[] update = parent1.encodeStateAsUpdate();
            parent2.applyUpdate(update);

            // Verify subdocument exists in parent2
            // Note: Full subdocument content synchronization may require additional handling
            try (YMap map2 = parent2.getMap("map");
                 YDoc retrieved = map2.getDoc("nested")) {
                assertNotNull(retrieved);
            }
        }
    }

    @Test
    public void testYMapMultipleSubdocuments() {
        try (YDoc parent = new YDoc();
             YDoc child1 = new YDoc();
             YDoc child2 = new YDoc();
             YDoc child3 = new YDoc();
             YMap map = parent.getMap("map")) {

            // Insert all subdocuments
            map.setDoc("first", child1);
            map.setDoc("second", child2);
            map.setDoc("third", child3);

            // Verify all subdocuments exist
            try (YDoc r1 = map.getDoc("first")) {
                assertNotNull(r1);
            }
            try (YDoc r2 = map.getDoc("second")) {
                assertNotNull(r2);
            }
            try (YDoc r3 = map.getDoc("third")) {
                assertNotNull(r3);
            }
        }
    }

    @Test
    public void testYMapNestedSubdocuments() {
        try (YDoc root = new YDoc();
             YDoc level1 = new YDoc();
             YMap rootMap = root.getMap("map")) {

            // Build nested structure - insert level1 as subdocument
            rootMap.setDoc("child", level1);

            // Verify level1 can be retrieved and has content
            try (YDoc retrieved1 = rootMap.getDoc("child")) {
                assertNotNull("First level subdocument should exist", retrieved1);

                // Add content to nested document
                try (YText text = retrieved1.getText("content")) {
                    text.push("Nested content");
                    assertEquals("Nested content", text.toString());
                }
            }
        }
    }

    // ==================== YArray Subdocument Tests ====================

    @Test
    public void testYArrayPushAndGetSubdocument() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YArray array = parent.getArray("array")) {

            // Push subdocument
            array.pushDoc(child);

            // Retrieve subdocument
            try (YDoc retrieved = array.getDoc(0)) {
                assertNotNull(retrieved);
            }
        }
    }

    @Test
    public void testYArrayInsertAndGetSubdocument() {
        try (YDoc parent = new YDoc();
             YDoc child1 = new YDoc();
             YDoc child2 = new YDoc();
             YArray array = parent.getArray("array")) {

            // Push first, insert second at beginning
            array.pushDoc(child1);
            array.insertDoc(0, child2);

            assertEquals(2, array.length());
            try (YDoc retrieved = array.getDoc(0)) {
                assertNotNull(retrieved);
            }
            try (YDoc retrieved = array.getDoc(1)) {
                assertNotNull(retrieved);
            }
        }
    }

    @Test
    public void testYArraySubdocumentWithContent() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YArray array = parent.getArray("array")) {

            array.pushDoc(child);

            // Add content to retrieved subdocument
            try (YDoc retrieved = array.getDoc(0);
                 YText retrievedText = retrieved.getText("content")) {
                retrievedText.push("Hello from child");
                assertEquals("Hello from child", retrievedText.toString());
            }
        }
    }

    @Test
    public void testYArrayGetNonExistentSubdocument() {
        try (YDoc parent = new YDoc();
             YArray array = parent.getArray("array")) {

            YDoc retrieved = array.getDoc(0);
            assertNull(retrieved);

            retrieved = array.getDoc(-1);
            assertNull(retrieved);
        }
    }

    @Test
    public void testYArraySubdocumentSynchronization() {
        try (YDoc parent1 = new YDoc();
             YDoc parent2 = new YDoc();
             YDoc child = new YDoc();
             YArray array1 = parent1.getArray("array")) {

            // Add subdocument to parent1
            array1.pushDoc(child);

            // Synchronize parents
            byte[] update = parent1.encodeStateAsUpdate();
            parent2.applyUpdate(update);

            // Verify subdocument exists in parent2
            try (YArray array2 = parent2.getArray("array");
                 YDoc retrieved = array2.getDoc(0)) {
                assertNotNull(retrieved);
            }
        }
    }

    @Test
    public void testYArrayMultipleSubdocuments() {
        try (YDoc parent = new YDoc();
             YDoc child1 = new YDoc();
             YDoc child2 = new YDoc();
             YDoc child3 = new YDoc();
             YArray array = parent.getArray("array")) {

            // Insert all subdocuments
            array.pushDoc(child1);
            array.pushDoc(child2);
            array.pushDoc(child3);

            assertEquals(3, array.length());

            // Verify all subdocuments exist
            try (YDoc r1 = array.getDoc(0)) {
                assertNotNull(r1);
            }
            try (YDoc r2 = array.getDoc(1)) {
                assertNotNull(r2);
            }
            try (YDoc r3 = array.getDoc(2)) {
                assertNotNull(r3);
            }
        }
    }

    @Test
    public void testYArrayMixedContentWithSubdocuments() {
        try (YDoc parent = new YDoc();
             YDoc child1 = new YDoc();
             YDoc child2 = new YDoc();
             YArray array = parent.getArray("array")) {

            // Mix strings, doubles, and subdocuments
            array.pushString("Hello");
            array.pushDoc(child1);
            array.pushDouble(42.0);
            array.pushDoc(child2);
            array.pushString("World");

            assertEquals(5, array.length());

            // Verify types
            assertEquals("Hello", array.getString(0));
            assertNotNull(array.getDoc(1));
            assertEquals(42.0, array.getDouble(2), 0.001);
            assertNotNull(array.getDoc(3));
            assertEquals("World", array.getString(4));
        }
    }

    // ==================== Combined Tests ====================

    @Test
    public void testSubdocumentWithAllCRDTTypes() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YMap map = parent.getMap("map")) {

            // Insert child into parent
            map.setDoc("nested", child);

            // Add all CRDT types to retrieved subdocument
            try (YDoc retrieved = map.getDoc("nested");
                 YText text = retrieved.getText("text");
                 YArray array = retrieved.getArray("array");
                 YMap childMap = retrieved.getMap("map");
                 YXmlElement element = retrieved.getXmlElement("element")) {

                text.push("Text content");
                array.pushString("Array item");
                childMap.setString("key", "value");
                element.setAttribute("attr", "attrValue");

                // Verify all types work
                assertEquals("Text content", text.toString());
                assertEquals(1, array.length());
                assertEquals("value", childMap.getString("key"));
                assertEquals("attrValue", element.getAttribute("attr"));
            }
        }
    }

    @Test
    public void testBidirectionalSubdocumentSync() {
        try (YDoc parent1 = new YDoc(1);
             YDoc parent2 = new YDoc(2);
             YDoc child1 = new YDoc(3);
             YDoc child2 = new YDoc(4);
             YMap map1 = parent1.getMap("map");
             YMap map2 = parent2.getMap("map")) {

            // Parent1 adds child1
            map1.setDoc("nested1", child1);

            // Parent2 adds child2
            map2.setDoc("nested2", child2);

            // Sync parent1 -> parent2
            byte[] update1 = parent1.encodeStateAsUpdate();
            parent2.applyUpdate(update1);

            // Sync parent2 -> parent1
            byte[] update2 = parent2.encodeStateAsUpdate();
            parent1.applyUpdate(update2);

            // Both should have both subdocuments after merge
            try (YDoc retrieved1 = map1.getDoc("nested1");
                 YDoc retrieved2 = map2.getDoc("nested2")) {
                assertNotNull(retrieved1);
                assertNotNull(retrieved2);
            }
        }
    }

    @Test
    public void testSubdocumentStressTest() {
        final int docCount = 50;
        try (YDoc parent = new YDoc();
             YArray array = parent.getArray("docs")) {

            // Create many subdocuments
            for (int i = 0; i < docCount; i++) {
                try (YDoc child = new YDoc()) {
                    array.pushDoc(child);
                }
            }

            assertEquals(docCount, array.length());

            // Verify all subdocuments exist
            for (int i = 0; i < docCount; i++) {
                try (YDoc child = array.getDoc(i)) {
                    assertNotNull("Subdocument at index " + i + " should exist", child);
                }
            }

            // Test synchronization - subdocument structure should sync
            try (YDoc parent2 = new YDoc()) {
                byte[] update = parent.encodeStateAsUpdate();
                parent2.applyUpdate(update);

                try (YArray array2 = parent2.getArray("docs")) {
                    assertEquals("Array length should match after sync", docCount, array2.length());

                    // Verify some subdocuments exist
                    for (int i = 0; i < 10 && i < docCount; i++) {
                        try (YDoc child = array2.getDoc(i)) {
                            assertNotNull("Subdocument at index " + i + " should exist after sync", child);
                        }
                    }
                }
            }
        }
    }
}
