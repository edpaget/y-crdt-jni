package net.carcdr.ycrdt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for YXmlText.
 */
public class YXmlTextTest {

    @Test
    public void testXmlTextCreation() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            assertNotNull(xmlText);
            assertEquals(0, xmlText.length());
            assertFalse(xmlText.isClosed());
        }
    }

    @Test
    public void testPushText() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.push("Hello");
            assertEquals("Hello", xmlText.toString());
            assertEquals(5, xmlText.length());

            xmlText.push(" World");
            assertEquals("Hello World", xmlText.toString());
            assertEquals(11, xmlText.length());
        }
    }

    @Test
    public void testInsertText() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "World");
            assertEquals("World", xmlText.toString());

            xmlText.insert(0, "Hello ");
            assertEquals("Hello World", xmlText.toString());

            xmlText.insert(6, "Beautiful ");
            assertEquals("Hello Beautiful World", xmlText.toString());
        }
    }

    @Test
    public void testDeleteText() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.push("Hello Beautiful World");
            assertEquals(21, xmlText.length());

            xmlText.delete(6, 10); // Remove "Beautiful "
            assertEquals("Hello World", xmlText.toString());
            assertEquals(11, xmlText.length());

            xmlText.delete(5, 6); // Remove " World"
            assertEquals("Hello", xmlText.toString());
            assertEquals(5, xmlText.length());
        }
    }

    @Test
    public void testComplexEditingSequence() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.push("The quick brown fox");
            xmlText.insert(4, "very ");
            assertEquals("The very quick brown fox", xmlText.toString());

            xmlText.delete(4, 5); // Remove "very "
            assertEquals("The quick brown fox", xmlText.toString());

            xmlText.push(" jumps");
            assertEquals("The quick brown fox jumps", xmlText.toString());
        }
    }

    @Test
    public void testSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Make changes in doc1
            try (YXmlText xmlText1 = doc1.getXmlText("shared")) {
                xmlText1.push("Hello from Doc1");
            }

            // Sync from doc1 to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Verify doc2 has the changes
            try (YXmlText xmlText2 = doc2.getXmlText("shared")) {
                assertEquals("Hello from Doc1", xmlText2.toString());
                assertEquals(15, xmlText2.length());
            }
        }
    }

    @Test
    public void testBidirectionalSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Changes in doc1
            try (YXmlText xmlText1 = doc1.getXmlText("shared")) {
                xmlText1.push("Doc1");
            }

            // Changes in doc2
            try (YXmlText xmlText2 = doc2.getXmlText("shared")) {
                xmlText2.push("Doc2");
            }

            // Sync doc1 to doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Sync doc2 to doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have both texts (order may vary based on CRDT resolution)
            try (YXmlText xmlText1 = doc1.getXmlText("shared");
                 YXmlText xmlText2 = doc2.getXmlText("shared")) {
                String text1 = xmlText1.toString();
                String text2 = xmlText2.toString();

                // They should be the same
                assertEquals(text1, text2);

                // Both contents should be present
                assertTrue(text1.contains("Doc1") || text1.contains("Doc2"));
            }
        }
    }

    @Test
    public void testXmlTextClosed() {
        YDoc doc = new YDoc();
        YXmlText xmlText = doc.getXmlText("test");
        xmlText.push("Hello");

        xmlText.close();
        assertTrue(xmlText.isClosed());

        try {
            xmlText.push("World");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        doc.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushNullChunk() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.push(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertNullChunk() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, null);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertNegativeIndex() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(-1, "Hello");
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testDeleteNegativeIndex() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.delete(-1, 5);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testDeleteNegativeLength() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.delete(0, -1);
        }
    }

    @Test
    public void testEmptyXmlText() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            assertEquals(0, xmlText.length());
            assertEquals("", xmlText.toString());
            assertFalse(xmlText.isClosed());
        }
    }

    @Test
    public void testGetSameXmlTextTwice() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText1 = doc.getXmlText("shared")) {
            xmlText1.push("Hello");

            // Get the same XML text again
            try (YXmlText xmlText2 = doc.getXmlText("shared")) {
                // Both should show the same content
                assertEquals(5, xmlText1.length());
                assertEquals(5, xmlText2.length());
                assertEquals("Hello", xmlText1.toString());
                assertEquals("Hello", xmlText2.toString());

                xmlText2.push(" World");

                // Both should reflect the change
                assertEquals(11, xmlText1.length());
                assertEquals(11, xmlText2.length());
                assertEquals("Hello World", xmlText1.toString());
                assertEquals("Hello World", xmlText2.toString());
            }
        }
    }

    @Test
    public void testMultipleXmlTextsInDocument() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText1 = doc.getXmlText("text1");
             YXmlText xmlText2 = doc.getXmlText("text2")) {

            xmlText1.push("First");
            xmlText2.push("Second");

            assertEquals("First", xmlText1.toString());
            assertEquals("Second", xmlText2.toString());
        }
    }

    @Test
    public void testUnicodeSupport() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.push("Hello 世界");
            assertEquals("Hello 世界", xmlText.toString());

            xmlText.insert(6, "美麗的");
            assertEquals("Hello 美麗的世界", xmlText.toString());
        }
    }

    @Test
    public void testEmojiSupport() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.push("Hello 👋");
            assertTrue(xmlText.toString().contains("👋"));

            xmlText.push(" 🌍");
            assertTrue(xmlText.toString().contains("🌍"));
        }
    }
}
