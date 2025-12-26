package net.carcdr.ycrdt;

/**
 * A collaborative map type that supports concurrent editing.
 */
public interface YMap extends AutoCloseable {

    /**
     * Returns the number of entries in the map.
     *
     * @return the size
     */
    int size();

    /**
     * Returns the number of entries in the map within a transaction.
     *
     * @param txn the transaction
     * @return the size
     */
    int size(YTransaction txn);

    /**
     * Checks if the map is empty.
     *
     * @return true if empty, false otherwise
     */
    boolean isEmpty();

    // String operations

    /**
     * Gets a string value for the specified key.
     *
     * @param key the key
     * @return the string value, or null if not present
     */
    String getString(String key);

    /**
     * Gets a string value for the specified key within a transaction.
     *
     * @param txn the transaction
     * @param key the key
     * @return the string value, or null if not present
     */
    String getString(YTransaction txn, String key);

    /**
     * Sets a string value for the specified key.
     *
     * @param key the key
     * @param value the value to set
     */
    void setString(String key, String value);

    /**
     * Sets a string value for the specified key within a transaction.
     *
     * @param txn the transaction
     * @param key the key
     * @param value the value to set
     */
    void setString(YTransaction txn, String key, String value);

    // Double operations

    /**
     * Gets a double value for the specified key.
     *
     * @param key the key
     * @return the double value
     */
    double getDouble(String key);

    /**
     * Gets a double value for the specified key within a transaction.
     *
     * @param txn the transaction
     * @param key the key
     * @return the double value
     */
    double getDouble(YTransaction txn, String key);

    /**
     * Sets a double value for the specified key.
     *
     * @param key the key
     * @param value the value to set
     */
    void setDouble(String key, double value);

    /**
     * Sets a double value for the specified key within a transaction.
     *
     * @param txn the transaction
     * @param key the key
     * @param value the value to set
     */
    void setDouble(YTransaction txn, String key, double value);

    // Subdocument operations

    /**
     * Gets a subdocument for the specified key.
     *
     * @param key the key
     * @return the subdocument, or null if not present
     */
    YDoc getDoc(String key);

    /**
     * Gets a subdocument for the specified key within a transaction.
     *
     * @param txn the transaction
     * @param key the key
     * @return the subdocument, or null if not present
     */
    YDoc getDoc(YTransaction txn, String key);

    /**
     * Sets a subdocument for the specified key.
     *
     * @param key the key
     * @param subdoc the subdocument to set
     */
    void setDoc(String key, YDoc subdoc);

    /**
     * Sets a subdocument for the specified key within a transaction.
     *
     * @param txn the transaction
     * @param key the key
     * @param subdoc the subdocument to set
     */
    void setDoc(YTransaction txn, String key, YDoc subdoc);

    // Key operations

    /**
     * Checks if the map contains the specified key.
     *
     * @param key the key
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(String key);

    /**
     * Checks if the map contains the specified key within a transaction.
     *
     * @param txn the transaction
     * @param key the key
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(YTransaction txn, String key);

    /**
     * Returns all keys in the map.
     *
     * @return an array of keys
     */
    String[] keys();

    /**
     * Returns all keys in the map within a transaction.
     *
     * @param txn the transaction
     * @return an array of keys
     */
    String[] keys(YTransaction txn);

    /**
     * Removes the specified key from the map.
     *
     * @param key the key to remove
     */
    void remove(String key);

    /**
     * Removes the specified key from the map within a transaction.
     *
     * @param txn the transaction
     * @param key the key to remove
     */
    void remove(YTransaction txn, String key);

    /**
     * Removes all entries from the map.
     */
    void clear();

    /**
     * Removes all entries from the map within a transaction.
     *
     * @param txn the transaction
     */
    void clear(YTransaction txn);

    // Serialization

    /**
     * Returns a JSON representation of this map.
     *
     * @return the JSON string
     */
    String toJson();

    /**
     * Returns a JSON representation of this map within a transaction.
     *
     * @param txn the transaction
     * @return the JSON string
     */
    String toJson(YTransaction txn);

    /**
     * Registers an observer for changes to this map.
     *
     * @param observer the observer to register
     * @return a subscription handle for unregistering
     */
    YSubscription observe(YObserver observer);

    /**
     * Closes this map and releases resources.
     */
    @Override
    void close();

    /**
     * Checks if this map has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}
