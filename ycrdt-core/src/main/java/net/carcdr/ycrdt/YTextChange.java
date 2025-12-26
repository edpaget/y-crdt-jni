package net.carcdr.ycrdt;

import java.util.Map;

/**
 * Represents a change to a YText or YXmlText instance.
 */
public abstract class YTextChange extends YChange {

    /**
     * Protected constructor for subclasses.
     */
    protected YTextChange() {
    }

    /**
     * Returns the text content for INSERT changes.
     *
     * @return the inserted text, or null for non-INSERT changes
     */
    public abstract String getContent();

    /**
     * Returns the length of the change.
     *
     * @return the number of characters affected
     */
    public abstract int getLength();

    /**
     * Returns the formatting attributes associated with this change.
     *
     * @return a map of attribute names to values, or an empty map if none
     */
    public abstract Map<String, Object> getAttributes();
}
