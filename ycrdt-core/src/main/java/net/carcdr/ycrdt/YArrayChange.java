package net.carcdr.ycrdt;

import java.util.List;

/**
 * Represents a change to a YArray or YXmlFragment instance.
 */
public abstract class YArrayChange extends YChange {

    /**
     * Protected constructor for subclasses.
     */
    protected YArrayChange() {
    }

    /**
     * Returns the items for INSERT changes.
     *
     * @return the list of inserted items, or an empty list for non-INSERT changes
     */
    public abstract List<Object> getItems();

    /**
     * Returns the length of the change.
     *
     * @return the number of items affected
     */
    public abstract int getLength();
}
