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
}
