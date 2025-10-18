package net.carcdr.ycrdt;

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
 * try (YDoc doc = new YDoc();
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
 * @see YXmlText#getFormattingChunks()
 * @see YXmlText#getFormattingChunks(YTransaction)
 * @since 0.1.0
 */
public final class FormattingChunk {

    private final String text;
    private final Map<String, Object> attributes;

    /**
     * Creates a new FormattingChunk with the specified text and attributes.
     *
     * @param text the text content of this chunk (must not be null)
     * @param attributes the formatting attributes for this chunk (may be null or empty)
     * @throws IllegalArgumentException if text is null
     */
    public FormattingChunk(String text, Map<String, Object> attributes) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        this.text = text;
        // Make a defensive copy and make it immutable
        this.attributes = attributes == null || attributes.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    /**
     * Returns the text content of this chunk.
     *
     * @return the text content (never null)
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the formatting attributes for this chunk.
     *
     * <p>Common attributes include:</p>
     * <ul>
     *   <li>{@code "bold"} - Boolean indicating bold formatting</li>
     *   <li>{@code "italic"} - Boolean indicating italic formatting</li>
     *   <li>{@code "underline"} - Boolean indicating underline formatting</li>
     *   <li>{@code "color"} - String color value (e.g., "#FF0000")</li>
     *   <li>{@code "font"} - String font family name</li>
     * </ul>
     *
     * @return an immutable map of attribute names to values (never null, may be empty)
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Returns whether this chunk has any formatting attributes.
     *
     * @return true if this chunk has formatting attributes, false otherwise
     */
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
        FormattingChunk that = (FormattingChunk) o;
        return text.equals(that.text) && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, attributes);
    }

    @Override
    public String toString() {
        if (attributes.isEmpty()) {
            return "FormattingChunk{text='" + text + "'}";
        }
        return "FormattingChunk{text='" + text + "', attributes=" + attributes + "}";
    }
}
