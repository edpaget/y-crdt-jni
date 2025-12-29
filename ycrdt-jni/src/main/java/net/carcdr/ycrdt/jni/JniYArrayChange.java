package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YArrayChange;

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
 * @see JniYEvent
 */
public final class JniYArrayChange extends YArrayChange {

    private final Type type;
    private final List<Object> items;
    private final int length;

    /**
     * Package-private constructor for INSERT changes.
     *
     * @param items the inserted items
     */
    JniYArrayChange(List<Object> items) {
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
    JniYArrayChange(Type type, int length) {
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

    @Override
    public List<Object> getItems() {
        return items;
    }

    @Override
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
