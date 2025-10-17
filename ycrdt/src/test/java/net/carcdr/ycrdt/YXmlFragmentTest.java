package net.carcdr.ycrdt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for YXmlFragment.
 */
public class YXmlFragmentTest {

    @Test
    public void testFragmentCreation() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            assertNotNull(fragment);
            assertEquals(0, fragment.length());
            assertFalse(fragment.isClosed());
        }
    }

    @Test
    public void testInsertElement() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            assertEquals(1, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0));
        }
    }

    @Test
    public void testInsertText() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertText(0, "Hello World");
            assertEquals(1, fragment.length());
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(0));
        }
    }

    @Test
    public void testInsertMultipleNodes() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");
            fragment.insertElement(2, "span");

            assertEquals(3, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0));
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(1));
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(2));
        }
    }

    @Test
    public void testRemove() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");
            fragment.insertElement(2, "span");
            assertEquals(3, fragment.length());

            fragment.remove(1, 1);
            assertEquals(2, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0));
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(1));
        }
    }

    @Test
    public void testRemoveMultiple() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");
            fragment.insertElement(2, "span");
            fragment.insertText(3, "World");
            assertEquals(4, fragment.length());

            fragment.remove(1, 2);
            assertEquals(2, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0));
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(1));
        }
    }

    @Test
    public void testToXmlString() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");

            String xml = fragment.toXmlString();
            assertNotNull(xml);
            // The exact format may vary, but it should contain our elements
            assertTrue(xml.length() > 0);
        }
    }

    @Test
    public void testSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Make changes in doc1
            try (YXmlFragment fragment1 = doc1.getXmlFragment("test")) {
                fragment1.insertElement(0, "div");
                fragment1.insertText(1, "Hello");
            }

            // Sync from doc1 to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Verify doc2 has the changes
            try (YXmlFragment fragment2 = doc2.getXmlFragment("test")) {
                assertEquals(2, fragment2.length());
                assertEquals(YXmlNode.NodeType.ELEMENT, fragment2.getNodeType(0));
                assertEquals(YXmlNode.NodeType.TEXT, fragment2.getNodeType(1));
            }
        }
    }

    @Test
    public void testBidirectionalSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Create fragment in doc1
            try (YXmlFragment fragment1 = doc1.getXmlFragment("test")) {
                fragment1.insertElement(0, "div");
            }

            // Sync doc1 to doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Make changes in doc2
            try (YXmlFragment fragment2 = doc2.getXmlFragment("test")) {
                fragment2.insertText(1, "Hello");
            }

            // Sync doc2 back to doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have both changes
            try (YXmlFragment fragment1 = doc1.getXmlFragment("test");
                 YXmlFragment fragment2 = doc2.getXmlFragment("test")) {
                assertEquals(2, fragment1.length());
                assertEquals(2, fragment2.length());
                assertEquals(YXmlNode.NodeType.ELEMENT, fragment1.getNodeType(0));
                assertEquals(YXmlNode.NodeType.TEXT, fragment1.getNodeType(1));
            }
        }
    }

    @Test
    public void testFragmentClosed() {
        YDoc doc = new YDoc();
        YXmlFragment fragment = doc.getXmlFragment("test");
        fragment.insertElement(0, "div");

        fragment.close();
        assertTrue(fragment.isClosed());

        try {
            fragment.insertElement(1, "span");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        doc.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertElementNullTag() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertTextNullContent() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertText(0, null);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertElementInvalidIndex() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(5, "div");
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveInvalidRange() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.remove(0, 5);
        }
    }

    @Test
    public void testGetSameFragmentTwice() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment1 = doc.getXmlFragment("test")) {
            fragment1.insertElement(0, "div");

            // Get the same fragment again
            try (YXmlFragment fragment2 = doc.getXmlFragment("test")) {
                // Both should show the same content
                assertEquals(1, fragment1.length());
                assertEquals(1, fragment2.length());

                fragment2.insertText(1, "Hello");

                // Both should reflect the change
                assertEquals(2, fragment1.length());
                assertEquals(2, fragment2.length());
            }
        }
    }

    @Test
    public void testGetElement() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");

            // Get the element
            try (YXmlElement element = fragment.getElement(0)) {
                assertNotNull(element);
                assertEquals("div", element.getTag());

                // Modify it
                element.setAttribute("class", "container");
                assertEquals("container", element.getAttribute("class"));
            }

            // Verify changes persist
            try (YXmlElement element = fragment.getElement(0)) {
                assertEquals("container", element.getAttribute("class"));
            }
        }
    }

    @Test
    public void testGetText() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertText(0, "Hello");
            fragment.insertElement(1, "span");

            // Get the text node
            try (YXmlText text = fragment.getText(0)) {
                assertNotNull(text);
                assertEquals("Hello", text.toString());

                // Modify it
                text.push(" World");
                assertEquals("Hello World", text.toString());
            }

            // Verify changes persist
            try (YXmlText text = fragment.getText(0)) {
                assertEquals("Hello World", text.toString());
            }
        }
    }

    @Test
    public void testGetElementReturnsNull() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertText(0, "Hello");

            // Try to get element at text node index
            YXmlElement element = fragment.getElement(0);
            assertEquals(null, element);

            // Try to get element at invalid index
            YXmlElement element2 = fragment.getElement(10);
            assertEquals(null, element2);
        }
    }

    @Test
    public void testGetTextReturnsNull() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");

            // Try to get text at element node index
            YXmlText text = fragment.getText(0);
            assertEquals(null, text);

            // Try to get text at invalid index
            YXmlText text2 = fragment.getText(10);
            assertEquals(null, text2);
        }
    }

    @Test
    public void testMultipleChildRetrieval() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");
            fragment.insertElement(2, "span");

            // Get multiple children
            try (YXmlElement div = fragment.getElement(0);
                 YXmlText text = fragment.getText(1);
                 YXmlElement span = fragment.getElement(2)) {

                assertNotNull(div);
                assertNotNull(text);
                assertNotNull(span);

                assertEquals("div", div.getTag());
                assertEquals("Hello", text.toString());
                assertEquals("span", span.getTag());

                // Modify all of them
                div.setAttribute("id", "main");
                text.push("!");
                span.setAttribute("class", "highlight");

                assertEquals("main", div.getAttribute("id"));
                assertEquals("Hello!", text.toString());
                assertEquals("highlight", span.getAttribute("class"));
            }
        }
    }

    @Test
    public void testChildRetrievalSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Create structure in doc1
            try (YXmlFragment fragment1 = doc1.getXmlFragment("test")) {
                fragment1.insertElement(0, "div");

                try (YXmlElement div = fragment1.getElement(0)) {
                    div.setAttribute("class", "container");
                }
            }

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Retrieve and verify in doc2
            try (YXmlFragment fragment2 = doc2.getXmlFragment("test");
                 YXmlElement div = fragment2.getElement(0)) {

                assertNotNull(div);
                assertEquals("div", div.getTag());
                assertEquals("container", div.getAttribute("class"));
            }
        }
    }

    // Transaction-based tests

    @Test
    public void testInsertElementWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                fragment.insertElement(txn, 0, "div");
                fragment.insertElement(txn, 1, "span");
                fragment.insertElement(txn, 2, "p");
            }
            assertEquals(3, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0));
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(1));
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(2));
        }
    }

    @Test
    public void testInsertTextWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                fragment.insertText(txn, 0, "Hello");
                fragment.insertText(txn, 1, "World");
            }
            assertEquals(2, fragment.length());
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(0));
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(1));
        }
    }

    @Test
    public void testRemoveWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");
            fragment.insertElement(2, "span");
            fragment.insertText(3, "World");
            assertEquals(4, fragment.length());

            try (YTransaction txn = doc.beginTransaction()) {
                fragment.remove(txn, 1, 1);
                fragment.remove(txn, 1, 1);
            }

            assertEquals(2, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0));
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(1));
        }
    }

    @Test
    public void testMixedInsertionsWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                fragment.insertElement(txn, 0, "div");
                fragment.insertText(txn, 1, "Hello");
                fragment.insertElement(txn, 2, "span");
                fragment.insertText(txn, 3, "World");
            }
            assertEquals(4, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0));
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(1));
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(2));
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(3));
        }
    }

    @Test
    public void testTransactionBatchingPerformance() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            // Using transaction should batch all operations
            try (YTransaction txn = doc.beginTransaction()) {
                for (int i = 0; i < 100; i++) {
                    if (i % 2 == 0) {
                        fragment.insertElement(txn, i, "div");
                    } else {
                        fragment.insertText(txn, i, "text" + i);
                    }
                }
            }

            assertEquals(100, fragment.length());
        }
    }

    @Test
    public void testComplexTransactionOperations() {
        try (YDoc doc = new YDoc();
             YXmlFragment fragment = doc.getXmlFragment("test")) {
            // First add some initial content
            fragment.insertElement(0, "div");
            fragment.insertText(1, "Hello");
            fragment.insertElement(2, "span");

            // Use transaction to modify
            try (YTransaction txn = doc.beginTransaction()) {
                fragment.insertText(txn, 1, "Start");
                fragment.insertElement(txn, 4, "p");
                fragment.remove(txn, 2, 1); // Remove "Hello"
            }

            assertEquals(4, fragment.length());
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(0)); // div
            assertEquals(YXmlNode.NodeType.TEXT, fragment.getNodeType(1));    // Start
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(2)); // span
            assertEquals(YXmlNode.NodeType.ELEMENT, fragment.getNodeType(3)); // p
        }
    }
}
