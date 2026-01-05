package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.YTextChange;

import java.util.Collections;
import java.util.Map;

/**
 * Panama implementation of YTextChange.
 *
 * <p>Represents text changes as deltas (INSERT, DELETE, RETAIN).</p>
 */
public final class PanamaYTextChange extends YTextChange {

    private final Type type;
    private final String content;
    private final int length;
    private final Map<String, Object> attributes;

    /**
     * Creates an INSERT change with the given content.
     *
     * @param content the inserted text
     * @param attributes formatting attributes (may be null)
     */
    PanamaYTextChange(String content, Map<String, Object> attributes) {
        this.type = Type.INSERT;
        this.content = content;
        this.length = content.length();
        this.attributes = attributes != null
            ? Collections.unmodifiableMap(attributes)
            : Collections.emptyMap();
    }

    /**
     * Creates a DELETE or RETAIN change with the given length.
     *
     * @param type the change type (DELETE or RETAIN)
     * @param length the number of characters affected
     */
    PanamaYTextChange(Type type, int length) {
        this(type, length, null);
    }

    /**
     * Creates a DELETE or RETAIN change with the given length and attributes.
     *
     * @param type the change type (DELETE or RETAIN)
     * @param length the number of characters affected
     * @param attributes formatting attributes (may be null)
     */
    PanamaYTextChange(Type type, int length, Map<String, Object> attributes) {
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
