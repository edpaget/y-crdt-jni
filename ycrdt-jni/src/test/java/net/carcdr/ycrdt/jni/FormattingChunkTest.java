package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlText;
import net.carcdr.ycrdt.FormattingChunk;

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
 * Tests for FormattingChunk class and YXmlText.getFormattingChunks() method.
 */
public class FormattingChunkTest {

    @Test
    public void testFormattingChunkCreation() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("bold", true);
        FormattingChunk chunk = new JniFormattingChunk("Hello", attrs);

        assertEquals("Hello", chunk.getText());
        assertTrue(chunk.hasAttributes());
        assertEquals(true, chunk.getAttributes().get("bold"));
    }

    @Test
    public void testFormattingChunkWithoutAttributes() {
        FormattingChunk chunk = new JniFormattingChunk("World", null);

        assertEquals("World", chunk.getText());
        assertFalse(chunk.hasAttributes());
        assertTrue(chunk.getAttributes().isEmpty());
    }

    @Test
    public void testFormattingChunkImmutableAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("italic", true);
        FormattingChunk chunk = new JniFormattingChunk("Test", attrs);

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
        new JniFormattingChunk(null, null);
    }

    @Test
    public void testFormattingChunkEquals() {
        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put("bold", true);
        FormattingChunk chunk1 = new JniFormattingChunk("Hello", attrs1);

        Map<String, Object> attrs2 = new HashMap<>();
        attrs2.put("bold", true);
        FormattingChunk chunk2 = new JniFormattingChunk("Hello", attrs2);

        assertEquals(chunk1, chunk2);
        assertEquals(chunk1.hashCode(), chunk2.hashCode());
    }

    @Test
    public void testFormattingChunkToString() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("bold", true);
        FormattingChunk chunk = new JniFormattingChunk("Hello", attrs);

        String str = chunk.toString();
        assertTrue(str.contains("Hello"));
        assertTrue(str.contains("attributes"));
    }

    @Test
    public void testGetFormattingChunksPlainText() {
        try (YDoc doc = new JniYDoc();
             YXmlText text = doc.getXmlText("mytext")) {

            text.insert(0, "Hello World");

            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);
            assertEquals(1, chunks.size());

            FormattingChunk chunk = chunks.get(0);
            assertEquals("Hello World", chunk.getText());
            assertFalse(chunk.hasAttributes());
        }
    }

    @Test
    public void testGetFormattingChunksWithFormatting() {
        try (YDoc doc = new JniYDoc();
             YXmlText text = doc.getXmlText("mytext")) {

            // Insert plain text first
            text.insert(0, "Hello ");

            // Then insert formatted text
            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text.insertWithAttributes(6, "World", bold);

            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);
            assertEquals(2, chunks.size());

            // First chunk should be plain
            FormattingChunk chunk1 = chunks.get(0);
            assertEquals("Hello ", chunk1.getText());
            assertFalse(chunk1.hasAttributes());

            // Second chunk should be bold
            FormattingChunk chunk2 = chunks.get(1);
            assertEquals("World", chunk2.getText());
            assertTrue(chunk2.hasAttributes());
            assertEquals(true, chunk2.getAttributes().get("bold"));
        }
    }

    @Test
    public void testGetFormattingChunksWithFormat() {
        try (YDoc doc = new JniYDoc();
             YXmlText text = doc.getXmlText("mytext")) {

            // Insert plain text
            text.insert(0, "Hello World");

            // Apply bold formatting to "Hello"
            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text.format(0, 5, bold);

            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);
            assertEquals(2, chunks.size());

            // First chunk should be "Hello" with bold
            FormattingChunk chunk1 = chunks.get(0);
            assertEquals("Hello", chunk1.getText());
            assertTrue(chunk1.hasAttributes());
            assertEquals(true, chunk1.getAttributes().get("bold"));

            // Second chunk should be " World" without formatting
            FormattingChunk chunk2 = chunks.get(1);
            assertEquals(" World", chunk2.getText());
            assertFalse(chunk2.hasAttributes());
        }
    }

    @Test
    public void testGetFormattingChunksMultipleAttributes() {
        try (YDoc doc = new JniYDoc();
             YXmlText text = doc.getXmlText("mytext")) {

            Map<String, Object> attrs = new HashMap<>();
            attrs.put("bold", true);
            attrs.put("italic", true);
            attrs.put("color", "red");
            text.insertWithAttributes(0, "Styled", attrs);

            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertEquals(1, chunks.size());

            FormattingChunk chunk = chunks.get(0);
            assertEquals("Styled", chunk.getText());
            assertTrue(chunk.hasAttributes());
            assertEquals(true, chunk.getAttributes().get("bold"));
            assertEquals(true, chunk.getAttributes().get("italic"));
            assertEquals("red", chunk.getAttributes().get("color"));
        }
    }

    @Test
    public void testGetFormattingChunksWithTransaction() {
        try (YDoc doc = new JniYDoc();
             YXmlText text = doc.getXmlText("mytext")) {

            try (YTransaction txn = doc.beginTransaction()) {
                text.insert(txn, 0, "Hello ");

                Map<String, Object> bold = new HashMap<>();
                bold.put("bold", true);
                text.insertWithAttributes(txn, 6, "World", bold);

                List<FormattingChunk> chunks = text.getFormattingChunks(txn);
                assertNotNull(chunks);
                assertEquals(2, chunks.size());

                FormattingChunk chunk1 = chunks.get(0);
                assertEquals("Hello ", chunk1.getText());
                assertFalse(chunk1.hasAttributes());

                FormattingChunk chunk2 = chunks.get(1);
                assertEquals("World", chunk2.getText());
                assertTrue(chunk2.hasAttributes());
            }
        }
    }

    @Test
    public void testGetFormattingChunksEmpty() {
        try (YDoc doc = new JniYDoc();
             YXmlText text = doc.getXmlText("mytext")) {

            List<FormattingChunk> chunks = text.getFormattingChunks();
            assertNotNull(chunks);
            assertTrue(chunks.isEmpty());
        }
    }

    @Test
    public void testGetFormattingChunksSynchronization() {
        try (YDoc doc1 = new JniYDoc();
             YDoc doc2 = new JniYDoc()) {

            // Create text1 and add content
            YXmlText text1 = doc1.getXmlText("mytext");
            text1.insert(0, "Hello ");

            Map<String, Object> bold = new HashMap<>();
            bold.put("bold", true);
            text1.insertWithAttributes(6, "World", bold);

            // Sync to doc2 BEFORE getting text2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Now get text2 after sync
            YXmlText text2 = doc2.getXmlText("mytext");

            // Both should have the same chunks
            List<FormattingChunk> chunks1 = text1.getFormattingChunks();
            List<FormattingChunk> chunks2 = text2.getFormattingChunks();

            assertEquals(chunks1.size(), chunks2.size());
            assertEquals(2, chunks1.size());

            for (int i = 0; i < chunks1.size(); i++) {
                assertEquals(chunks1.get(i).getText(), chunks2.get(i).getText());
                assertEquals(chunks1.get(i).getAttributes(), chunks2.get(i).getAttributes());
            }

            text1.close();
            text2.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFormattingChunksWithNullTransaction() {
        try (YDoc doc = new JniYDoc();
             YXmlText text = doc.getXmlText("mytext")) {
            text.getFormattingChunks(null);
        }
    }
}
