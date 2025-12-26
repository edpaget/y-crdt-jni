package net.carcdr.ycrdt;

/**
 * Represents a change to a YMap instance.
 */
public abstract class YMapChange extends YChange {

    /**
     * Protected constructor for subclasses.
     */
    protected YMapChange() {
    }

    /**
     * Returns the key that was changed.
     *
     * @return the changed key
     */
    public abstract String getKey();

    /**
     * Returns the new value for the key.
     *
     * @return the new value, or null if the key was removed
     */
    public abstract Object getNewValue();

    /**
     * Returns the previous value for the key.
     *
     * @return the old value, or null if the key was newly added
     */
    public abstract Object getOldValue();
}
