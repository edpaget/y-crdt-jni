package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.YXmlElementChange;

/**
 * Panama implementation of YXmlElementChange.
 *
 * <p>Represents attribute changes to YXmlElement.</p>
 */
public final class PanamaYXmlElementChange extends YXmlElementChange {

    private final Type type;
    private final String attributeName;
    private final String newValue;
    private final String oldValue;

    /**
     * Creates an attribute change.
     *
     * @param type the change type (INSERT, DELETE, or ATTRIBUTE)
     * @param attributeName the name of the attribute
     * @param newValue the new value (null for DELETE)
     * @param oldValue the old value (null for INSERT)
     */
    PanamaYXmlElementChange(Type type, String attributeName, String newValue, String oldValue) {
        this.type = type;
        this.attributeName = attributeName;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public String getNewValue() {
        return newValue;
    }

    @Override
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
                return "UPDATE(" + attributeName + ": \"" + oldValue + "\" -> \"" + newValue + "\")";
            default:
                return type.toString();
        }
    }
}
