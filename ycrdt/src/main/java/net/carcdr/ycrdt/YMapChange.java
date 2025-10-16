package net.carcdr.ycrdt;

/**
 * Represents a change to a YMap object.
 *
 * <p>Map changes describe modifications to key-value pairs:
 * <ul>
 *   <li><b>INSERT:</b> A new key was added</li>
 *   <li><b>DELETE:</b> A key was removed</li>
 *   <li><b>ATTRIBUTE:</b> A key's value was modified</li>
 * </ul>
 *
 * @see YEvent
 */
public final class YMapChange extends YChange {

    private final Type type;
    private final String key;
    private final Object newValue;
    private final Object oldValue;

    /**
     * Package-private constructor.
     *
     * @param type the change type
     * @param key the key that changed
     * @param newValue the new value (may be null)
     * @param oldValue the old value (may be null)
     */
    YMapChange(Type type, String key, Object newValue, Object oldValue) {
        this.type = type;
        this.key = key;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    @Override
    public Type getType() {
        return type;
    }

    /**
     * Gets the key that changed.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the new value.
     *
     * @return the new value, or null if this is a DELETE change
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Gets the old value.
     *
     * @return the old value, or null if this is an INSERT change
     */
    public Object getOldValue() {
        return oldValue;
    }

    @Override
    public String toString() {
        switch (type) {
            case INSERT:
                return "INSERT(\"" + key + "\" = " + newValue + ")";
            case DELETE:
                return "DELETE(\"" + key + "\")";
            case ATTRIBUTE:
                return "UPDATE(\"" + key + "\": " + oldValue + " → " + newValue + ")";
            default:
                return type.toString();
        }
    }
}
