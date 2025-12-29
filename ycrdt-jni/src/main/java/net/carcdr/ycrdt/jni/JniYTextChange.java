package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YTextChange;

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
 * @see JniYEvent
 */
public final class JniYTextChange extends YTextChange {

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
    JniYTextChange(String content, Map<String, Object> attributes) {
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
    JniYTextChange(Type type, int length) {
        this(type, length, null);
    }

    /**
     * Package-private constructor for DELETE and RETAIN changes with attributes.
     *
     * @param type the change type (DELETE or RETAIN)
     * @param length the number of characters deleted or retained
     * @param attributes the formatting attributes (may be null, only for RETAIN)
     */
    JniYTextChange(Type type, int length, Map<String, Object> attributes) {
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

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
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
                return "RETAIN(" + length
                       + (attributes.isEmpty() ? "" : ", " + attributes) + ")";
            default:
                return type.toString();
        }
    }
}
