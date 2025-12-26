package net.carcdr.ycrdt;

/**
 * Represents an attribute change to a YXmlElement instance.
 */
public abstract class YXmlElementChange extends YChange {

    /**
     * Protected constructor for subclasses.
     */
    protected YXmlElementChange() {
    }

    /**
     * Returns the name of the changed attribute.
     *
     * @return the attribute name
     */
    public abstract String getAttributeName();

    /**
     * Returns the new value of the attribute.
     *
     * @return the new value, or null if the attribute was removed
     */
    public abstract String getNewValue();

    /**
     * Returns the previous value of the attribute.
     *
     * @return the old value, or null if the attribute was newly added
     */
    public abstract String getOldValue();
}
