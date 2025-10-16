package net.carcdr.ycrdt;

/**
 * Represents a change to a YXmlElement's attributes.
 *
 * <p>Element changes describe modifications to attributes:
 * <ul>
 *   <li><b>INSERT:</b> A new attribute was added</li>
 *   <li><b>DELETE:</b> An attribute was removed</li>
 *   <li><b>ATTRIBUTE:</b> An attribute's value was modified</li>
 * </ul>
 *
 * <p>Note: Changes to children are reported as YArrayChange events.</p>
 *
 * @see YEvent
 */
public final class YXmlElementChange extends YChange {

    private final Type type;
    private final String attributeName;
    private final String newValue;
    private final String oldValue;

    /**
     * Package-private constructor.
     *
     * @param type the change type
     * @param attributeName the attribute name that changed
     * @param newValue the new value (may be null)
     * @param oldValue the old value (may be null)
     */
    YXmlElementChange(Type type, String attributeName, String newValue, String oldValue) {
        this.type = type;
        this.attributeName = attributeName;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    @Override
    public Type getType() {
        return type;
    }

    /**
     * Gets the attribute name that changed.
     *
     * @return the attribute name
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Gets the new attribute value.
     *
     * @return the new value, or null if this is a DELETE change
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Gets the old attribute value.
     *
     * @return the old value, or null if this is an INSERT change
     */
    public String getOldValue() {
        return oldValue;
    }

    @Override
    public String toString() {
        switch (type) {
            case INSERT:
                return "INSERT(" + attributeName + " = \"" + newValue + "\")";
            case DELETE:
                return "DELETE(" + attributeName + ")";
            case ATTRIBUTE:
                return "UPDATE(" + attributeName + ": \"" + oldValue + "\" → \"" + newValue + "\")";
            default:
                return type.toString();
        }
    }
}
