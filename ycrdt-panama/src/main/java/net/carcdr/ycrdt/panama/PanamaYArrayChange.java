package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.YArrayChange;

import java.util.Collections;
import java.util.List;

/**
 * Panama implementation of YArrayChange.
 *
 * <p>Represents changes to array-like structures (YArray, YXmlFragment children).</p>
 */
public final class PanamaYArrayChange extends YArrayChange {

    private final Type type;
    private final List<Object> items;
    private final int length;

    /**
     * Creates an INSERT change with the given items.
     *
     * @param items the inserted items
     */
    PanamaYArrayChange(List<Object> items) {
        this.type = Type.INSERT;
        this.items = Collections.unmodifiableList(items);
        this.length = items.size();
    }

    /**
     * Creates a DELETE or RETAIN change with the given length.
     *
     * @param type the change type (DELETE or RETAIN)
     * @param length the number of items affected
     */
    PanamaYArrayChange(Type type, int length) {
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
