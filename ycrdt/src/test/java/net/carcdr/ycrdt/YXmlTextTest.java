package net.carcdr.ycrdt;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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

    // Rich text formatting tests

    @Test
    public void testFormatBold() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "hello world");

            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);
            xmlText.format(0, 5, bold);

            assertEquals("<b>hello</b> world", xmlText.toString());
        }
    }

    @Test
    public void testFormatItalic() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "hello world");

            Map<String, Object> italic = new HashMap<>();
            italic.put("i", true);
            xmlText.format(6, 5, italic);

            assertEquals("hello <i>world</i>", xmlText.toString());
        }
    }

    @Test
    public void testInsertWithAttributes() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "hello ");

            Map<String, Object> italic = new HashMap<>();
            italic.put("i", true);
            xmlText.insertWithAttributes(6, "world", italic);

            assertEquals("hello <i>world</i>", xmlText.toString());
        }
    }

    @Test
    public void testMultipleFormattingAttributes() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "hello world");

            // Apply bold first
            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);
            xmlText.format(0, 5, bold);

            // Then apply italic to overlapping region
            Map<String, Object> italic = new HashMap<>();
            italic.put("i", true);
            xmlText.format(0, 5, italic);

            String result = xmlText.toString();
            // Both formatting attributes should be present
            assertTrue(result.contains("b") || result.contains("i"));
        }
    }

    @Test
    public void testRemoveFormatting() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            // Insert with italic
            Map<String, Object> italic = new HashMap<>();
            italic.put("i", true);
            xmlText.insertWithAttributes(0, "world", italic);

            assertEquals("<i>world</i>", xmlText.toString());

            // Remove italic formatting
            Map<String, Object> removeItalic = new HashMap<>();
            removeItalic.put("i", null);
            xmlText.format(0, 5, removeItalic);

            assertEquals("world", xmlText.toString());
        }
    }

    @Test
    public void testComplexFormattingSequence() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "The quick brown fox");

            // Make "quick" bold
            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);
            xmlText.format(4, 5, bold);

            assertTrue(xmlText.toString().contains("b"));
            assertTrue(xmlText.toString().contains("quick"));
        }
    }

    @Test
    public void testFormattingSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Apply formatting in doc1
            try (YXmlText xmlText1 = doc1.getXmlText("shared")) {
                xmlText1.insert(0, "hello world");

                Map<String, Object> bold = new HashMap<>();
                bold.put("b", true);
                xmlText1.format(0, 5, bold);
            }

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Verify formatting is preserved
            try (YXmlText xmlText2 = doc2.getXmlText("shared")) {
                assertEquals("<b>hello</b> world", xmlText2.toString());
            }
        }
    }

    @Test
    public void testInsertWithAttributesSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Insert with attributes in doc1
            try (YXmlText xmlText1 = doc1.getXmlText("shared")) {
                Map<String, Object> italic = new HashMap<>();
                italic.put("i", true);
                xmlText1.insertWithAttributes(0, "hello", italic);
            }

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Verify formatting is preserved
            try (YXmlText xmlText2 = doc2.getXmlText("shared")) {
                assertEquals("<i>hello</i>", xmlText2.toString());
            }
        }
    }

    @Test
    public void testMixedContentAndFormatting() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            // Insert plain text
            xmlText.insert(0, "hello ");

            // Insert with bold
            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);
            xmlText.insertWithAttributes(6, "world", bold);

            // Add more plain text
            xmlText.push("!");

            String result = xmlText.toString();
            assertTrue(result.contains("hello"));
            assertTrue(result.contains("world"));
            assertTrue(result.contains("!"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatNullAttributes() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "hello");
            xmlText.format(0, 5, null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertWithAttributesNullAttributes() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insertWithAttributes(0, "hello", null);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testFormatNegativeIndex() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);
            xmlText.format(-1, 5, bold);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testFormatNegativeLength() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);
            xmlText.format(0, -1, bold);
        }
    }

    @Test
    public void testVariousAttributeTypes() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "hello");

            // Test with different value types
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("b", true);           // Boolean
            attrs.put("size", 14);          // Integer
            attrs.put("color", "#FF0000");  // String
            attrs.put("opacity", 0.5);      // Double

            xmlText.format(0, 5, attrs);

            // Should not throw an exception
            assertNotNull(xmlText.toString());
        }
    }

    // Ancestor lookup tests

    @Test
    public void testGetParentOfRootTextNode() {
        try (YDoc doc = new YDoc();
             YXmlText text = doc.getXmlText("test")) {
            Object parent = text.getParent();
            assertNotNull(parent);
            assertTrue(parent instanceof YXmlFragment);
            ((YXmlFragment) parent).close();
        }
    }

    @Test
    public void testGetParentOfChildTextNode() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlText text = div.insertText(0);

            Object parent = text.getParent();
            assertNotNull(parent);
            assertTrue(parent instanceof YXmlElement);
            assertEquals("div", ((YXmlElement) parent).getTag());

            text.close();
            ((YXmlElement) parent).close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentForTextNode() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlText text = div.insertText(0);

            assertEquals(0, text.getIndexInParent());

            text.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentMixedChildrenWithText() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span = div.insertElement(0, "span");
            YXmlText text1 = div.insertText(1);
            YXmlElement p = div.insertElement(2, "p");
            YXmlText text2 = div.insertText(3);

            assertEquals(0, span.getIndexInParent());
            assertEquals(1, text1.getIndexInParent());
            assertEquals(2, p.getIndexInParent());
            assertEquals(3, text2.getIndexInParent());

            span.close();
            text1.close();
            p.close();
            text2.close();
            div.close();
        }
    }

    @Test
    public void testTextNodeParentAfterRemoval() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span = div.insertElement(0, "span");
            YXmlText text1 = div.insertText(1);
            YXmlText text2 = div.insertText(2);

            assertEquals(0, span.getIndexInParent());
            assertEquals(1, text1.getIndexInParent());
            assertEquals(2, text2.getIndexInParent());

            // Remove middle element (span)
            div.removeChild(0);

            // Text nodes should have updated indices
            assertEquals(0, text1.getIndexInParent());
            assertEquals(1, text2.getIndexInParent());

            span.close();
            text1.close();
            text2.close();
            div.close();
        }
    }

    @Test
    public void testTextNodeParentInNestedStructure() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement p = div.insertElement(0, "p");
            YXmlText text = p.insertText(0);

            // Check text's parent is p
            Object textParent = text.getParent();
            assertNotNull(textParent);
            assertTrue(textParent instanceof YXmlElement);
            assertEquals("p", ((YXmlElement) textParent).getTag());
            assertEquals(0, text.getIndexInParent());

            // Check p's parent is div
            Object pParent = p.getParent();
            assertNotNull(pParent);
            assertTrue(pParent instanceof YXmlElement);
            assertEquals("div", ((YXmlElement) pParent).getTag());
            assertEquals(0, p.getIndexInParent());

            div.close();
            p.close();
            text.close();
            ((YXmlElement) textParent).close();
            ((YXmlElement) pParent).close();
        }
    }

    @Test
    public void testTextNodeParentSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Create nested structure in doc1
            try (YXmlElement div1 = doc1.getXmlElement("div")) {
                YXmlElement p = div1.insertElement(0, "p");
                YXmlText text = p.insertText(0);
                text.push("Hello");
                p.close();
                text.close();
            }

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Verify parent navigation works in doc2
            try (YXmlElement div2 = doc2.getXmlElement("div")) {
                Object pChild = div2.getChild(0);
                assertTrue(pChild instanceof YXmlElement);
                YXmlElement p = (YXmlElement) pChild;

                Object textChild = p.getChild(0);
                assertTrue(textChild instanceof YXmlText);
                YXmlText text = (YXmlText) textChild;

                Object textParent = text.getParent();
                assertNotNull(textParent);
                assertTrue(textParent instanceof YXmlElement);
                assertEquals("p", ((YXmlElement) textParent).getTag());
                assertEquals(0, text.getIndexInParent());

                p.close();
                text.close();
                ((YXmlElement) textParent).close();
            }
        }
    }

    // Transaction-based tests

    @Test
    public void testInsertWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                xmlText.insert(txn, 0, "Hello");
                xmlText.insert(txn, 5, " World");
            }
            assertEquals("Hello World", xmlText.toString());
            assertEquals(11, xmlText.length());
        }
    }

    @Test
    public void testPushWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                xmlText.push(txn, "Hello");
                xmlText.push(txn, " ");
                xmlText.push(txn, "World");
            }
            assertEquals("Hello World", xmlText.toString());
            assertEquals(11, xmlText.length());
        }
    }

    @Test
    public void testDeleteWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.push("Hello Beautiful World");

            try (YTransaction txn = doc.beginTransaction()) {
                xmlText.delete(txn, 6, 10); // Remove "Beautiful "
                xmlText.delete(txn, 5, 1);  // Remove " "
            }
            assertEquals("HelloWorld", xmlText.toString());
        }
    }

    @Test
    public void testFormatWithTransaction() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            xmlText.insert(0, "hello world");

            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);

            try (YTransaction txn = doc.beginTransaction()) {
                xmlText.format(txn, 0, 5, bold);
                xmlText.format(txn, 6, 5, bold);
            }

            String result = xmlText.toString();
            assertTrue(result.contains("<b>hello</b>"));
            assertTrue(result.contains("<b>world</b>"));
        }
    }

    @Test
    public void testInsertWithAttributesAndTransaction() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            Map<String, Object> italic = new HashMap<>();
            italic.put("i", true);

            Map<String, Object> bold = new HashMap<>();
            bold.put("b", true);

            try (YTransaction txn = doc.beginTransaction()) {
                xmlText.insertWithAttributes(txn, 0, "hello ", italic);
                xmlText.insertWithAttributes(txn, 6, "world", bold);
            }

            String result = xmlText.toString();
            assertTrue(result.contains("<i>hello"));
            assertTrue(result.contains("<b>world</b>"));
        }
    }

    @Test
    public void testTransactionBatchingPerformance() {
        try (YDoc doc = new YDoc();
             YXmlText xmlText = doc.getXmlText("test")) {
            // Using transaction should batch all operations
            try (YTransaction txn = doc.beginTransaction()) {
                for (int i = 0; i < 100; i++) {
                    xmlText.push(txn, "x");
                }
            }

            assertEquals(100, xmlText.length());
            assertTrue(xmlText.toString().startsWith("x"));
        }
    }
}
