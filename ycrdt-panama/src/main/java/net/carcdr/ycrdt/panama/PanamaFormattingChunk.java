package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.FormattingChunk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a chunk of text with associated formatting attributes.
 *
 * <p>In Y-CRDT, formatted text is stored as a sequence of chunks (deltas),
 * where each chunk contains text content and optional formatting attributes.
 * This class provides a Java representation of these chunks for retrieving
 * formatted text content.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new PanamaYDoc();
 *      YXmlText text = doc.getXmlText("mytext");
 *      YTransaction txn = doc.beginTransaction()) {
 *
 *     // Insert some formatted text
 *     text.insertWithAttributes(txn, 0, "Hello", Map.of("bold", true));
 *     text.insert(txn, 5, " World");
 *
 *     // Retrieve formatting chunks
 *     List<FormattingChunk> chunks = text.getFormattingChunks(txn);
 *     for (FormattingChunk chunk : chunks) {
 *         System.out.println("Text: " + chunk.getText());
 *         System.out.println("Attributes: " + chunk.getAttributes());
 *     }
 * }
 * }</pre>
 *
 * @see PanamaYXmlText#getFormattingChunks()
 * @since 0.1.0
 */
public final class PanamaFormattingChunk implements FormattingChunk {

    private final String text;
    private final Map<String, Object> attributes;

    /**
     * Creates a new PanamaFormattingChunk with the specified text and attributes.
     *
     * @param text the text content of this chunk (must not be null)
     * @param attributes the formatting attributes for this chunk (may be null or empty)
     * @throws IllegalArgumentException if text is null
     */
    public PanamaFormattingChunk(String text, Map<String, Object> attributes) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        this.text = text;
        // Make a defensive copy and make it immutable
        this.attributes = attributes == null || attributes.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PanamaFormattingChunk that = (PanamaFormattingChunk) o;
        return text.equals(that.text) && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, attributes);
    }

    @Override
    public String toString() {
        if (attributes.isEmpty()) {
            return "PanamaFormattingChunk{text='" + text + "'}";
        }
        return "PanamaFormattingChunk{text='" + text + "', attributes=" + attributes + "}";
    }
}
