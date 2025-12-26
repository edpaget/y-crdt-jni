package net.carcdr.ycrdt;

import java.util.Map;

/**
 * Represents a chunk of text with associated formatting attributes.
 * Used by YXmlText to represent formatted text content.
 */
public interface FormattingChunk {

    /**
     * Returns the text content of this chunk.
     *
     * @return the text content
     */
    String getText();

    /**
     * Returns the formatting attributes for this chunk.
     *
     * @return an immutable map of attribute names to values
     */
    Map<String, Object> getAttributes();

    /**
     * Checks if this chunk has any formatting attributes.
     *
     * @return true if attributes exist, false otherwise
     */
    boolean hasAttributes();
}
