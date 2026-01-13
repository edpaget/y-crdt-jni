package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.FormattingChunk;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Tests for text formatting in PanamaYXmlText.
 *
 * <p>Note: Panama doesn't support top-level YXmlText via getXmlText().
 * Tests use getXmlFragment() and insertText() instead.</p>
 */
public class PanamaYXmlTextFormattingTest {

    /**
     * Helper to create YXmlText within a fragment.
     */
    private YXmlText createXmlText(YDoc doc, String name) {
        YXmlFragment fragment = doc.getXmlFragment(name);
        fragment.insertText(0, "");  // Insert empty text node
        return fragment.getText(0);
    }

    @Test
    public void testFormattingChunkCreation() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("bold", true);
        FormattingChunk chunk = new PanamaFormattingChunk("Hello", attrs);

        assertEquals("Hello", chunk.getText());
        assertTrue(chunk.hasAttributes());
        assertEquals(true, chunk.getAttributes().get("bold"));
    }

    @Test
    public void testFormattingChunkWithoutAttributes() {
        FormattingChunk chunk = new PanamaFormattingChunk("World", null);

        assertEquals("World", chunk.getText());
        assertFalse(chunk.hasAttributes());
        assertTrue(chunk.getAttributes().isEmpty());
    }

    @Test
    public void testFormattingChunkImmutableAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("italic", true);
        FormattingChunk chunk = new PanamaFormattingChunk("Test", attrs);

        // Modifying original map shouldn't affect chunk
        attrs.put("underline", true);
        assertFalse(chunk.getAttributes().containsKey("underline"));

        // Returned map should be immutable
        try {
            chunk.getAttributes().put("color", "red");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormattingChunkNullText() {
        new PanamaFormattingChunk(null, null);
    }

    @Test
    public void testFormattingChunkEquals() {
        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put("bold", true);
        FormattingChunk chunk1 = new PanamaFormattingChunk("Hello", attrs1);

        Map<String, Object> attrs2 = new HashMap<>();
        attrs2.put("bold", true);
        FormattingChunk chunk2 = new PanamaFormattingChunk("Hello", attrs2);

        assertEquals(chunk1, chunk2);
        assertEquals(chunk1.hashCode(), chunk2.hashCode());
    }

    @Test
    public void testFormattingChunkToString() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("bold", true);
        FormattingChunk chunk = new PanamaFormattingChunk("Hello", attrs);

        String str = chunk.toString();
        assertTrue(str.contains("Hello"));
        assertTrue(str.contains("attributes"));
    }

    /**
     * YXmlText with attributes creates XML elements.
     * insertWithAttributes({bold: true}) creates &lt;bold&gt;text&lt;/bold&gt;
     */
    @Test
    public void testInsertWithBooleanAttribute() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text.insertWithAttributes(0, "Bold text", bold);

            // YXmlText creates XML element wrappers for formatting
            String result = text.toString();
            assertTrue("Should contain bold tags", result.contains("<bold>"));
            assertTrue("Should contain text content", result.contains("Bold text"));
        }
    }

    /**
     * YXmlText with string attribute creates XML element with that name.
     */
    @Test
    public void testInsertWithStringAttribute() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            Map<String, Object> color = new HashMap<>();
            color.put("color", "red");
            text.insertWithAttributes(0, "Red text", color);

            // YXmlText creates XML element wrappers
            String result = text.toString();
            assertTrue("Should contain color tags", result.contains("<color>"));
            assertTrue("Should contain text content", result.contains("Red text"));
        }
    }

    /**
     * YXmlText with numeric attribute creates XML element.
     */
    @Test
    public void testInsertWithNumericAttribute() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            Map<String, Object> size = new HashMap<>();
            size.put("size", 14.0);
            text.insertWithAttributes(0, "Sized text", size);

            // YXmlText creates XML element wrappers
            String result = text.toString();
            assertTrue("Should contain size tags", result.contains("<size>"));
            assertTrue("Should contain text content", result.contains("Sized text"));
        }
    }

    /**
     * YXmlText with multiple attributes creates nested XML elements.
     */
    @Test
    public void testInsertWithMultipleAttributes() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            Map<String, Object> attrs = new HashMap<>();
            attrs.put("bold", true);
            attrs.put("italic", true);
            attrs.put("color", "blue");
            text.insertWithAttributes(0, "Styled text", attrs);

            // YXmlText creates nested XML element wrappers
            String result = text.toString();
            assertTrue("Should contain bold tags", result.contains("<bold>"));
            assertTrue("Should contain italic tags", result.contains("<italic>"));
            assertTrue("Should contain color tags", result.contains("<color>"));
            assertTrue("Should contain text content", result.contains("Styled text"));
        }
    }

    @Test
    public void testInsertWithNullAttributes() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            // Should work just like regular insert
            text.insertWithAttributes(0, "Plain text", null);
            assertEquals("Plain text", text.toString());

            // Empty map should also work
            text.insertWithAttributes(10, " more", new HashMap<>());
            assertEquals("Plain text more", text.toString());
        }
    }

    /**
     * Format on YXmlText wraps text in XML elements.
     */
    @Test
    public void testFormatAppliesBold() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            text.insert(0, "Hello World");

            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text.format(0, 5, bold);

            // YXmlText wraps formatted text in XML elements
            String result = text.toString();
            assertTrue("Should contain bold tags", result.contains("<bold>"));
            assertTrue("Should contain Hello", result.contains("Hello"));
            assertTrue("Should contain World", result.contains("World"));
        }
    }

    @Test
    public void testFormatWithEmptyAttributesIsNoOp() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            text.insert(0, "Hello World");

            // Empty attributes should be a no-op
            text.format(0, 5, new HashMap<>());
            text.format(0, 5, null);

            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testGetFormattingChunksPlainText() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            text.insert(0, "Hello World");

            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);
            assertEquals(1, chunks.size());

            FormattingChunk chunk = chunks.get(0);
            assertEquals("Hello World", chunk.getText());
            assertFalse(chunk.hasAttributes());
        }
    }

    /**
     * YXmlText with insertWithAttributes creates XML element children.
     * getFormattingChunks returns direct text content, not content inside child elements.
     *
     * <p>Note: ytext_chunks returns direct content of the YXmlText. Text inside
     * child XML elements (like &lt;bold&gt;) is not included - it's part of the
     * child element's content.</p>
     */
    @Test
    public void testGetFormattingChunksWithFormatting() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            // Insert plain text first
            text.insert(0, "Hello ");

            // Then insert formatted text - creates <bold>World</bold> as child element
            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text.insertWithAttributes(6, "World", bold);

            // getFormattingChunks returns direct text content
            // "Hello " is direct content, "World" is inside a child element
            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);
            assertTrue("Should have at least one chunk", chunks.size() >= 1);

            // Verify direct text content is accessible
            StringBuilder allText = new StringBuilder();
            for (FormattingChunk chunk : chunks) {
                allText.append(chunk.getText());
            }
            assertTrue("Should contain Hello", allText.toString().contains("Hello"));

            // Verify full content via toString() includes both parts
            String fullContent = text.toString();
            assertTrue("Full content should contain World", fullContent.contains("World"));
        }
    }

    /**
     * YXmlText format() wraps text in XML elements.
     * The formatted text structure is accessible via toString().
     */
    @Test
    public void testGetFormattingChunksWithFormat() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            // Insert plain text
            text.insert(0, "Hello World");

            // Apply bold formatting - wraps "Hello" in <bold> element
            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text.format(0, 5, bold);

            // getFormattingChunks returns what's accessible
            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);

            // Verify full content via toString() includes all parts
            String fullContent = text.toString();
            assertTrue("Full content should contain Hello", fullContent.contains("Hello"));
            assertTrue("Full content should contain World", fullContent.contains("World"));
            assertTrue("Full content should contain bold element", fullContent.contains("<bold>"));
        }
    }

    /**
     * YXmlText with multiple attributes creates nested XML elements.
     * The text inside is returned as a plain text chunk.
     */
    @Test
    public void testGetFormattingChunksMultipleAttributes() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            Map<String, Object> attrs = new HashMap<>();
            attrs.put("bold", true);
            attrs.put("italic", true);
            attrs.put("color", "red");
            text.insertWithAttributes(0, "Styled", attrs);

            // YXmlText creates nested elements, text is returned as chunk
            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertTrue("Should have at least one chunk", chunks.size() >= 1);

            // Verify text content is accessible
            StringBuilder allText = new StringBuilder();
            for (FormattingChunk chunk : chunks) {
                allText.append(chunk.getText());
            }
            assertTrue("Should contain Styled", allText.toString().contains("Styled"));
        }
    }

    /**
     * Test getFormattingChunks with explicit transaction.
     * Direct text content is returned; content inside child elements is not.
     */
    @Test
    public void testGetFormattingChunksWithTransaction() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            try (YTransaction txn = doc.beginTransaction()) {
                text.insert(txn, 0, "Hello ");

                Map<String, Object> bold = new HashMap<>();
                bold.put("bold", true);
                text.insertWithAttributes(txn, 6, "World", bold);

                List<FormattingChunk> chunks = text.getFormattingChunks(txn);
                assertNotNull(chunks);
                assertTrue("Should have at least one chunk", chunks.size() >= 1);

                // Verify direct text content is accessible
                StringBuilder allText = new StringBuilder();
                for (FormattingChunk chunk : chunks) {
                    allText.append(chunk.getText());
                }
                assertTrue("Should contain Hello", allText.toString().contains("Hello"));

                // World is inside a child element, verify via toString
                String fullContent = text.toString(txn);
                assertTrue("Full content should contain World", fullContent.contains("World"));
            }
        }
    }

    @Test
    public void testGetFormattingChunksEmpty() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);
            assertTrue(chunks.isEmpty());
        }
    }

    /**
     * Test that formatted text is preserved via XML element structure.
     * The text content should be retrievable via getFormattingChunks.
     */
    @Test
    public void testFormattingRoundTrip() {
        try (YDoc doc = new PanamaYDoc();
             YXmlText text = createXmlText(doc, "mytext")) {

            // Insert formatted text - creates nested XML elements
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("bold", true);
            attrs.put("color", "blue");
            text.insertWithAttributes(0, "Test", attrs);

            // Read chunks - text is inside XML elements
            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertTrue("Should have at least one chunk", chunks.size() >= 1);

            // Verify text content is accessible
            StringBuilder allText = new StringBuilder();
            for (FormattingChunk chunk : chunks) {
                allText.append(chunk.getText());
            }
            assertTrue("Should contain Test", allText.toString().contains("Test"));

            // Verify toString() contains XML structure
            String result = text.toString();
            assertTrue("Should contain bold element", result.contains("<bold>"));
            assertTrue("Should contain color element", result.contains("<color>"));
        }
    }

    /**
     * Test that YXmlText content syncs correctly between documents.
     * getFormattingChunks returns direct text content; child element content
     * is accessible via toString().
     */
    @Test
    public void testFormattingSynchronization() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc()) {

            // Create text1 and add content
            YXmlText text1 = createXmlText(doc1, "mytext");
            text1.insert(0, "Hello ");

            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text1.insertWithAttributes(6, "World", bold);

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Get text2 after sync - fragment and text are synced
            YXmlFragment fragment2 = doc2.getXmlFragment("mytext");
            YXmlText text2 = fragment2.getText(0);
            List<FormattingChunk> chunks = text2.getFormattingChunks();

            // Verify direct text content synced correctly
            assertTrue("Should have at least one chunk", chunks.size() >= 1);

            StringBuilder allText = new StringBuilder();
            for (FormattingChunk chunk : chunks) {
                allText.append(chunk.getText());
            }
            assertTrue("Should contain Hello", allText.toString().contains("Hello"));

            // Verify full content via toString() includes all parts
            String result = text2.toString();
            assertTrue("Should contain World", result.contains("World"));
            assertTrue("Should contain bold element", result.contains("<bold>"));
        }
    }
}
