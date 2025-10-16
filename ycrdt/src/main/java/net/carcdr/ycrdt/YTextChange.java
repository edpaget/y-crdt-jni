package net.carcdr.ycrdt;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a change to a YText or YXmlText object.
 *
 * <p>Text changes are expressed as deltas with three operations:
 * <ul>
 *   <li><b>INSERT:</b> Text was inserted at this position</li>
 *   <li><b>DELETE:</b> Text was deleted from this position</li>
 *   <li><b>RETAIN:</b> Position skipped (no change), used for context</li>
 * </ul>
 *
 * <p>Example delta sequence for changing "Hello" to "Hello World":
 * <pre>
 * RETAIN(5)           // Skip first 5 chars ("Hello")
 * INSERT(" World")    // Insert " World"
 * </pre>
 *
 * @see YEvent
 */
public final class YTextChange extends YChange {

    private final Type type;
    private final String content;
    private final int length;
    private final Map<String, Object> attributes;

    /**
     * Package-private constructor for INSERT changes.
     *
     * @param content the inserted text
     * @param attributes the formatting attributes (may be null)
     */
    YTextChange(String content, Map<String, Object> attributes) {
        this.type = Type.INSERT;
        this.content = content;
        this.length = content.length();
        this.attributes = attributes != null
            ? Collections.unmodifiableMap(attributes)
            : Collections.emptyMap();
    }

    /**
     * Package-private constructor for DELETE and RETAIN changes.
     *
     * @param type the change type (DELETE or RETAIN)
     * @param length the number of characters deleted or retained
     */
    YTextChange(Type type, int length) {
        this(type, length, null);
    }

    /**
     * Package-private constructor for DELETE and RETAIN changes with attributes.
     *
     * @param type the change type (DELETE or RETAIN)
     * @param length the number of characters deleted or retained
     * @param attributes the formatting attributes (may be null, only for RETAIN)
     */
    YTextChange(Type type, int length, Map<String, Object> attributes) {
        if (type != Type.DELETE && type != Type.RETAIN) {
            throw new IllegalArgumentException("Type must be DELETE or RETAIN");
        }
        this.type = type;
        this.content = null;
        this.length = length;
        this.attributes = attributes != null
            ? Collections.unmodifiableMap(attributes)
            : Collections.emptyMap();
    }

    @Override
    public Type getType() {
        return type;
    }

    /**
     * Gets the inserted text content.
     *
     * @return the inserted text, or null if this is not an INSERT change
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the length of the change.
     *
     * <p>For INSERT: length of inserted text<br>
     * For DELETE: number of characters deleted<br>
     * For RETAIN: number of characters skipped</p>
     *
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * Gets the formatting attributes associated with this change.
     *
     * <p>For INSERT: attributes applied to inserted text<br>
     * For RETAIN: attribute changes (if any)<br>
     * For DELETE: always empty</p>
     *
     * @return an immutable map of attributes (may be empty)
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        switch (type) {
            case INSERT:
                return "INSERT(\"" + content + "\", " + attributes + ")";
            case DELETE:
                return "DELETE(" + length + ")";
            case RETAIN:
                return "RETAIN(" + length +
                       (attributes.isEmpty() ? "" : ", " + attributes) + ")";
            default:
                return type.toString();
        }
    }
}
