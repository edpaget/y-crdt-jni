package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YText;
import net.carcdr.ycrdt.YArray;
import net.carcdr.ycrdt.YMap;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Stress tests to verify memory management and performance under heavy load.
 * These tests create many objects and perform many operations to detect memory leaks.
 */
public class StressTest {

    private static final int ITERATIONS = 1000;
    private static final int LARGE_ITERATIONS = 10000;

    // ==================== YDoc Stress Tests ====================

    @Test
    public void testYDocCreateCloseCycles() {
        // Create and close many YDoc instances to test for memory leaks
        for (int i = 0; i < ITERATIONS; i++) {
            try (YDoc doc = new JniYDoc()) {
                assertNotNull(doc);
                assertEquals(i, i); // Keep loop active
            }
        }
    }

    @Test
    public void testYDocWithClientIdCycles() {
        // Create docs with specific client IDs
        for (int i = 0; i < ITERATIONS; i++) {
            try (YDoc doc = new JniYDoc(i)) {
                assertEquals(i, doc.getClientId());
            }
        }
    }

    @Test
    public void testYDocStateEncodingCycles() {
        // Repeatedly encode and decode state
        for (int i = 0; i < ITERATIONS; i++) {
            try (YDoc doc1 = new JniYDoc(); YDoc doc2 = new JniYDoc()) {
                byte[] update = doc1.encodeStateAsUpdate();
                assertNotNull(update);
                doc2.applyUpdate(update);
            }
        }
    }

    // ==================== YText Stress Tests ====================

    @Test
    public void testYTextCreateCloseCycles() {
        // Create and close many YText instances
        try (YDoc doc = new JniYDoc()) {
            for (int i = 0; i < ITERATIONS; i++) {
                try (YText text = doc.getText("text" + i)) {
                    assertNotNull(text);
                }
            }
        }
    }

    @Test
    public void testYTextLargeDocument() {
        // Build a large text document
        try (YDoc doc = new JniYDoc(); YText text = doc.getText("text")) {
            for (int i = 0; i < LARGE_ITERATIONS; i++) {
                text.push("x");
            }
            assertEquals(LARGE_ITERATIONS, text.length());
        }
    }

    @Test
    public void testYTextManyEdits() {
        // Perform many insert/delete operations
        try (YDoc doc = new JniYDoc(); YText text = doc.getText("text")) {
            text.push("Initial text");
            for (int i = 0; i < ITERATIONS; i++) {
                text.insert(0, "a");
                text.delete(0, 1);
            }
            assertEquals(12, text.length()); // "Initial text"
        }
    }

    @Test
    public void testYTextSynchronizationStress() {
        // Stress test synchronization between many documents
        try (YDoc doc1 = new JniYDoc(); YText text1 = doc1.getText("text")) {
            // Build up state in doc1
            for (int i = 0; i < ITERATIONS; i++) {
                text1.push(String.valueOf(i));
            }

            // Synchronize to many other documents
            for (int i = 0; i < 100; i++) {
                try (YDoc doc2 = new JniYDoc(); YText text2 = doc2.getText("text")) {
                    byte[] update = doc1.encodeStateAsUpdate();
                    doc2.applyUpdate(update);
                    assertEquals(text1.toString(), text2.toString());
                }
            }
        }
    }

    // ==================== YArray Stress Tests ====================

    @Test
    public void testYArrayCreateCloseCycles() {
        try (YDoc doc = new JniYDoc()) {
            for (int i = 0; i < ITERATIONS; i++) {
                try (YArray array = doc.getArray("array" + i)) {
                    assertNotNull(array);
                }
            }
        }
    }

    @Test
    public void testYArrayLargeCollection() {
        // Build a large array
        try (YDoc doc = new JniYDoc(); YArray array = doc.getArray("array")) {
            for (int i = 0; i < LARGE_ITERATIONS; i++) {
                array.pushString("item" + i);
            }
            assertEquals(LARGE_ITERATIONS, array.length());
        }
    }

    @Test
    public void testYArrayManyOperations() {
        // Many insert/remove operations
        try (YDoc doc = new JniYDoc(); YArray array = doc.getArray("array")) {
            for (int i = 0; i < ITERATIONS; i++) {
                array.pushString("x");
                array.pushDouble(i);
            }
            assertEquals(ITERATIONS * 2, array.length());

            for (int i = 0; i < ITERATIONS; i++) {
                array.remove(0, 1);
            }
            assertEquals(ITERATIONS, array.length());
        }
    }

    // ==================== YMap Stress Tests ====================

    @Test
    public void testYMapCreateCloseCycles() {
        try (YDoc doc = new JniYDoc()) {
            for (int i = 0; i < ITERATIONS; i++) {
                try (YMap map = doc.getMap("map" + i)) {
                    assertNotNull(map);
                }
            }
        }
    }

    @Test
    public void testYMapLargeCollection() {
        // Build a large map
        try (YDoc doc = new JniYDoc(); YMap map = doc.getMap("map")) {
            for (int i = 0; i < LARGE_ITERATIONS; i++) {
                map.setString("key" + i, "value" + i);
            }
            assertEquals(LARGE_ITERATIONS, map.size());
        }
    }

    @Test
    public void testYMapManyOperations() {
        // Many set/remove operations
        try (YDoc doc = new JniYDoc(); YMap map = doc.getMap("map")) {
            for (int i = 0; i < ITERATIONS; i++) {
                map.setString("key" + i, "value");
                map.setDouble("num" + i, i);
            }
            assertEquals(ITERATIONS * 2, map.size());

            for (int i = 0; i < ITERATIONS; i++) {
                map.remove("key" + i);
            }
            assertEquals(ITERATIONS, map.size());
        }
    }

    // ==================== XML Stress Tests ====================

    @Test
    public void testYXmlTextCreateCloseCycles() {
        try (YDoc doc = new JniYDoc()) {
            for (int i = 0; i < ITERATIONS; i++) {
                try (YXmlText text = doc.getXmlText("text" + i)) {
                    assertNotNull(text);
                }
            }
        }
    }

    @Test
    public void testYXmlElementCreateCloseCycles() {
        try (YDoc doc = new JniYDoc()) {
            for (int i = 0; i < ITERATIONS; i++) {
                try (YXmlElement element = doc.getXmlElement("elem" + i)) {
                    assertNotNull(element);
                }
            }
        }
    }

    @Test
    public void testYXmlFragmentCreateCloseCycles() {
        try (YDoc doc = new JniYDoc()) {
            for (int i = 0; i < ITERATIONS; i++) {
                try (YXmlFragment fragment = doc.getXmlFragment("frag" + i)) {
                    assertNotNull(fragment);
                }
            }
        }
    }

    @Test
    public void testYXmlDeeplyNestedStructure() {
        // Create a deeply nested XML structure
        final int depth = 100;
        try (YDoc doc = new JniYDoc(); YXmlFragment fragment = doc.getXmlFragment("root")) {
            fragment.insertElement(0, "root");
            try (YXmlElement root = fragment.getElement(0)) {
                YXmlElement current = root;
                // Build deep nesting
                for (int i = 0; i < depth - 1; i++) {
                    current.insertElement(0, "level" + i);
                    YXmlElement child = (YXmlElement) current.getChild(0);
                    current = child;
                }
                // Verify depth by traversing back up
                int countUp = 0;
                YXmlElement node = current;
                while (node != null) {
                    countUp++;
                    Object parent = node.getParent();
                    if (parent instanceof YXmlElement) {
                        node = (YXmlElement) parent;
                    } else {
                        node = null;
                    }
                }
                assertTrue(countUp >= depth);
            }
        }
    }

    @Test
    public void testYXmlLargeTree() {
        // Create a wide tree with many children
        final int childCount = 1000;
        try (YDoc doc = new JniYDoc(); YXmlFragment fragment = doc.getXmlFragment("root")) {
            fragment.insertElement(0, "root");
            try (YXmlElement root = fragment.getElement(0)) {
                // Add many children
                for (int i = 0; i < childCount; i++) {
                    root.insertElement(i, "child" + i);
                }
                assertEquals(childCount, root.childCount());

                // Access all children and close them
                for (int i = 0; i < childCount; i++) {
                    try (YXmlElement child = (YXmlElement) root.getChild(i)) {
                        assertNotNull(child);
                        assertEquals("child" + i, child.getTag());
                    }
                }
            }
        }
    }

    @Test
    public void testYXmlManyAttributes() {
        // Element with many attributes
        final int attrCount = 1000;
        try (YDoc doc = new JniYDoc(); YXmlElement element = doc.getXmlElement("elem")) {
            for (int i = 0; i < attrCount; i++) {
                element.setAttribute("attr" + i, "value" + i);
            }

            String[] names = element.getAttributeNames();
            assertEquals(attrCount, names.length);

            for (int i = 0; i < attrCount; i++) {
                assertEquals("value" + i, element.getAttribute("attr" + i));
            }
        }
    }

    @Test
    public void testYXmlTextFormatting() {
        // Many formatting operations
        try (YDoc doc = new JniYDoc(); YXmlText text = doc.getXmlText("text")) {
            text.push("x".repeat(LARGE_ITERATIONS));
            assertEquals(LARGE_ITERATIONS, text.length());

            // Apply formatting to chunks
            for (int i = 0; i < ITERATIONS; i++) {
                java.util.Map<String, Object> attrs = new java.util.HashMap<>();
                attrs.put("bold", true);
                text.format(i, 1, attrs);
            }

            assertTrue(text.toString().contains("bold"));
        }
    }

    @Test
    public void testYXmlMixedContent() {
        // Mix elements and text nodes
        try (YDoc doc = new JniYDoc(); YXmlFragment fragment = doc.getXmlFragment("root")) {
            fragment.insertElement(0, "root");
            try (YXmlElement root = fragment.getElement(0)) {
                // Add alternating elements and text
                for (int i = 0; i < 500; i++) {
                    root.insertElement(i * 2, "elem" + i);
                    root.insertText(i * 2 + 1);
                }
                assertEquals(1000, root.childCount());

                // Verify mixed content
                for (int i = 0; i < 500; i++) {
                    Object elemChild = root.getChild(i * 2);
                    assertTrue(elemChild instanceof YXmlElement);
                    if (elemChild instanceof YXmlElement) {
                        ((YXmlElement) elemChild).close();
                    }

                    Object textChild = root.getChild(i * 2 + 1);
                    assertTrue(textChild instanceof YXmlText);
                    if (textChild instanceof YXmlText) {
                        ((YXmlText) textChild).close();
                    }
                }
            }
        }
    }

    @Test
    public void testYXmlParentNavigationStress() {
        // Test parent navigation with many nodes
        final int depth = 50;
        try (YDoc doc = new JniYDoc(); YXmlFragment fragment = doc.getXmlFragment("root")) {
            fragment.insertElement(0, "root");
            try (YXmlElement root = fragment.getElement(0)) {
                // Build nested structure
                YXmlElement current = root;
                for (int i = 0; i < depth - 1; i++) {
                    current.insertElement(0, "level" + i);
                    YXmlElement child = (YXmlElement) current.getChild(0);
                    current = child;
                }

                // Navigate up repeatedly
                for (int iteration = 0; iteration < 100; iteration++) {
                    YXmlElement node = current;
                    int count = 0;
                    while (node != null && count < depth) {
                        Object parent = node.getParent();
                        if (parent instanceof YXmlElement) {
                            node = (YXmlElement) parent;
                        } else {
                            node = null;
                        }
                        count++;
                    }
                    assertTrue(count > 0);
                }
            }
        }
    }

    // ==================== Combined Stress Tests ====================

    @Test
    public void testMultipleTypesInOneDocument() {
        // Use all types in a single document
        try (YDoc doc = new JniYDoc()) {
            for (int i = 0; i < 100; i++) {
                try (YText text = doc.getText("text" + i);
                        YArray array = doc.getArray("array" + i);
                        YMap map = doc.getMap("map" + i);
                        YXmlText xmlText = doc.getXmlText("xmltext" + i);
                        YXmlElement xmlElement = doc.getXmlElement("xmlelem" + i);
                        YXmlFragment xmlFragment = doc.getXmlFragment("xmlfrag" + i)) {

                    text.push("content");
                    array.pushString("item");
                    map.setString("key", "value");
                    xmlText.push("xml");
                    xmlElement.setAttribute("attr", "val");
                    xmlFragment.insertElement(0, "tag");
                }
            }
        }
    }

    @Test
    public void testSynchronizationStress() {
        // Synchronize complex state between many documents
        try (YDoc doc1 = new JniYDoc()) {
            // Build complex state
            try (YText text = doc1.getText("text");
                    YArray array = doc1.getArray("array");
                    YMap map = doc1.getMap("map");
                    YXmlElement element = doc1.getXmlElement("elem")) {

                for (int i = 0; i < 100; i++) {
                    text.push("x");
                    array.pushString("item" + i);
                    map.setString("key" + i, "value" + i);
                    element.setAttribute("attr" + i, "val" + i);
                }
            }

            byte[] update = doc1.encodeStateAsUpdate();

            // Apply to many documents
            for (int i = 0; i < 100; i++) {
                try (YDoc doc2 = new JniYDoc()) {
                    doc2.applyUpdate(update);

                    try (YText text = doc2.getText("text")) {
                        assertEquals(100, text.length());
                    }
                    try (YArray array = doc2.getArray("array")) {
                        assertEquals(100, array.length());
                    }
                    try (YMap map = doc2.getMap("map")) {
                        assertEquals(100, map.size());
                    }
                    try (YXmlElement element = doc2.getXmlElement("elem")) {
                        assertEquals(100, element.getAttributeNames().length);
                    }
                }
            }
        }
    }

    @Test
    public void testMemoryIntensiveOperations() {
        // Combine multiple memory-intensive operations
        try (YDoc doc = new JniYDoc()) {
            // Large text document
            try (YText text = doc.getText("text")) {
                for (int i = 0; i < 5000; i++) {
                    text.push("Line " + i + "\n");
                }
            }

            // Large array
            try (YArray array = doc.getArray("array")) {
                for (int i = 0; i < 5000; i++) {
                    array.pushString("Item " + i);
                }
            }

            // Large map
            try (YMap map = doc.getMap("map")) {
                for (int i = 0; i < 5000; i++) {
                    map.setString("key" + i, "value" + i);
                }
            }

            // Deep XML tree
            try (YXmlFragment fragment = doc.getXmlFragment("xml")) {
                fragment.insertElement(0, "root");
                try (YXmlElement root = fragment.getElement(0)) {
                    for (int i = 0; i < 100; i++) {
                        root.insertElement(i, "child" + i);
                    }
                }
            }

            // Verify state can be encoded
            byte[] update = doc.encodeStateAsUpdate();
            assertTrue(update.length > 0);
        }
    }
}
