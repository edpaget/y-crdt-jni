package net.carcdr.ycrdt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for YText.
 */
public class YTextTest {

    @Test
    public void testTextCreation() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            assertNotNull(text);
            assertEquals(0, text.length());
            assertEquals("", text.toString());
        }
    }

    @Test
    public void testTextInsert() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "Hello");
            assertEquals(5, text.length());
            assertEquals("Hello", text.toString());

            text.insert(5, " World");
            assertEquals(11, text.length());
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testTextInsertAtMiddle() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "HelloWorld");
            text.insert(5, " ");
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testTextPush() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.push(" ");
            text.push("World");
            assertEquals("Hello World", text.toString());
            assertEquals(11, text.length());
        }
    }

    @Test
    public void testTextDelete() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello World");
            text.delete(5, 6); // Delete " World"
            assertEquals("Hello", text.toString());
            assertEquals(5, text.length());
        }
    }

    @Test
    public void testTextDeleteFromBeginning() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello World");
            text.delete(0, 6); // Delete "Hello "
            assertEquals("World", text.toString());
        }
    }

    @Test
    public void testTextDeleteEntire() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.delete(0, 5); // Delete entire text
            assertEquals("", text.toString());
            assertEquals(0, text.length());
        }
    }

    @Test
    public void testTextSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Make changes in doc1
            try (YText text1 = doc1.getText("shared")) {
                text1.push("Hello World");
            }

            // Sync from doc1 to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Get text from doc2 after sync
            try (YText text2 = doc2.getText("shared")) {
                assertEquals("Hello World", text2.toString());
            }
        }
    }

    @Test
    public void testTextBidirectionalSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Changes in doc1
            try (YText text1 = doc1.getText("shared")) {
                text1.push("Hello");
            }

            // Changes in doc2
            try (YText text2 = doc2.getText("shared")) {
                text2.push("World");
            }

            // Sync doc1 to doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Sync doc2 to doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have both changes (order depends on client IDs)
            try (YText text1 = doc1.getText("shared");
                 YText text2 = doc2.getText("shared")) {
                String result1 = text1.toString();
                String result2 = text2.toString();
                assertEquals(result1, result2);
                assertTrue(result1.contains("Hello"));
                assertTrue(result1.contains("World"));
            }
        }
    }

    @Test
    public void testMultipleTextsInDocument() {
        try (YDoc doc = new YDoc();
             YText text1 = doc.getText("text1");
             YText text2 = doc.getText("text2")) {

            text1.push("First");
            text2.push("Second");

            assertEquals("First", text1.toString());
            assertEquals("Second", text2.toString());
        }
    }

    @Test
    public void testTextClosed() {
        YDoc doc = new YDoc();
        YText text = doc.getText("test");
        text.push("Hello");

        text.close();
        assertTrue(text.isClosed());

        try {
            text.push("World");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        doc.close();
    }

    @Test
    public void testTextWithClosedDoc() {
        YDoc doc = new YDoc();
        YText text = doc.getText("test");

        doc.close();

        // YText should still work with closed doc reference until explicitly closed
        // This tests the behavior when doc is closed first
        text.close();
        assertTrue(text.isClosed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertNullChunk() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.insert(0, null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushNullChunk() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push(null);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertNegativeIndex() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.insert(-1, "Hello");
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertIndexTooLarge() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.insert(10, "World");
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testDeleteNegativeIndex() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.delete(-1, 1);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testDeleteNegativeLength() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.delete(0, -1);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testDeleteRangeTooLarge() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.delete(0, 10);
        }
    }

    @Test
    public void testEmptyText() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            assertEquals("", text.toString());
            assertEquals(0, text.length());
            assertFalse(text.isClosed());
        }
    }

    @Test
    public void testUnicodeText() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello 世界");
            text.push(" 🌍");
            assertTrue(text.toString().contains("世界"));
            assertTrue(text.toString().contains("🌍"));
        }
    }

    @Test
    public void testComplexEditingSequence() {
        try (YDoc doc = new YDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.push(" ");
            text.push("World");
            assertEquals("Hello World", text.toString());

            text.insert(6, "Beautiful ");
            assertEquals("Hello Beautiful World", text.toString());

            text.delete(6, 10); // Delete "Beautiful "
            assertEquals("Hello World", text.toString());

            text.delete(5, 1); // Delete space
            assertEquals("HelloWorld", text.toString());

            text.insert(5, " ");
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testGetSameTextTwice() {
        try (YDoc doc = new YDoc();
             YText text1 = doc.getText("shared")) {
            text1.push("Hello");

            // Get the same text again
            try (YText text2 = doc.getText("shared")) {
                // Both should show the same content
                assertEquals("Hello", text1.toString());
                assertEquals("Hello", text2.toString());

                text2.push(" World");

                // Both should reflect the change
                assertEquals("Hello World", text1.toString());
                assertEquals("Hello World", text2.toString());
            }
        }
    }
}
