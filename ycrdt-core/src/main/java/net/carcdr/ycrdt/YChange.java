package net.carcdr.ycrdt;

/**
 * Base class for all Y-CRDT change types.
 * Changes describe modifications to collaborative data structures.
 */
public abstract class YChange {

    /**
     * Protected constructor for subclasses.
     */
    protected YChange() {
    }

    /**
     * Types of changes that can occur in Y-CRDT data structures.
     */
    public enum Type {
        /** Content was inserted. */
        INSERT,
        /** Content was deleted. */
        DELETE,
        /** Content was retained (unchanged). */
        RETAIN,
        /** An attribute was modified. */
        ATTRIBUTE
    }

    /**
     * Returns the type of this change.
     *
     * @return the change type
     */
    public abstract Type getType();
}
