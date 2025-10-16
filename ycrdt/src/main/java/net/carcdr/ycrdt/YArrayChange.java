package net.carcdr.ycrdt;

import java.util.Collections;
import java.util.List;

/**
 * Represents a change to a YArray or YXmlFragment object.
 *
 * <p>Array changes describe modifications to the array's children:
 * <ul>
 *   <li><b>INSERT:</b> Items were inserted at this position</li>
 *   <li><b>DELETE:</b> Items were removed from this position</li>
 *   <li><b>RETAIN:</b> Position skipped (no change), used for context</li>
 * </ul>
 *
 * @see YEvent
 */
public final class YArrayChange extends YChange {

    private final Type type;
    private final List<Object> items;
    private final int length;

    /**
     * Package-private constructor for INSERT changes.
     *
     * @param items the inserted items
     */
    YArrayChange(List<Object> items) {
        this.type = Type.INSERT;
        this.items = Collections.unmodifiableList(items);
        this.length = items.size();
    }

    /**
     * Package-private constructor for DELETE and RETAIN changes.
     *
     * @param type the change type (DELETE or RETAIN)
     * @param length the number of items deleted or retained
     */
    YArrayChange(Type type, int length) {
        if (type != Type.DELETE && type != Type.RETAIN) {
            throw new IllegalArgumentException("Type must be DELETE or RETAIN");
        }
        this.type = type;
        this.items = Collections.emptyList();
        this.length = length;
    }

    @Override
    public Type getType() {
        return type;
    }

    /**
     * Gets the inserted items.
     *
     * @return the list of inserted items, or empty if not an INSERT change
     */
    public List<Object> getItems() {
        return items;
    }

    /**
     * Gets the length of the change.
     *
     * <p>For INSERT: number of items inserted<br>
     * For DELETE: number of items deleted<br>
     * For RETAIN: number of items skipped</p>
     *
     * @return the length
     */
    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        switch (type) {
            case INSERT:
                return "INSERT(" + items + ")";
            case DELETE:
                return "DELETE(" + length + ")";
            case RETAIN:
                return "RETAIN(" + length + ")";
            default:
                return type.toString();
        }
    }
}
