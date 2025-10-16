package net.carcdr.ycrdt;

/**
 * Abstract base class for Y-CRDT change descriptions.
 *
 * <p>Changes come in different types depending on the Y data structure:
 * <ul>
 *   <li>{@link YTextChange} - for YText and YXmlText</li>
 *   <li>{@link YArrayChange} - for YArray and YXmlFragment</li>
 *   <li>{@link YMapChange} - for YMap</li>
 *   <li>{@link YXmlElementChange} - for YXmlElement</li>
 * </ul>
 *
 * @see YEvent
 */
public abstract class YChange {

    /**
     * The type of change.
     */
    public enum Type {
        /** Content was inserted */
        INSERT,

        /** Content was deleted */
        DELETE,

        /** Position was retained (no change, used for context) */
        RETAIN,

        /** Attribute/property was modified */
        ATTRIBUTE
    }

    /**
     * Gets the type of this change.
     *
     * @return the change type
     */
    public abstract Type getType();
}
