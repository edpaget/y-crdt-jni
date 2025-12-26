package net.carcdr.ycrdt;

/**
 * A collaborative array type that supports concurrent editing.
 */
public interface YArray extends AutoCloseable {

    /**
     * Returns the length of the array.
     *
     * @return the number of elements
     */
    int length();

    /**
     * Returns the length of the array within a transaction.
     *
     * @param txn the transaction
     * @return the number of elements
     */
    int length(YTransaction txn);

    // String operations

    /**
     * Gets a string value at the specified index.
     *
     * @param index the index
     * @return the string value
     */
    String getString(int index);

    /**
     * Gets a string value at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @return the string value
     */
    String getString(YTransaction txn, int index);

    /**
     * Inserts a string at the specified index.
     *
     * @param index the index
     * @param value the value to insert
     */
    void insertString(int index, String value);

    /**
     * Inserts a string at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @param value the value to insert
     */
    void insertString(YTransaction txn, int index, String value);

    /**
     * Appends a string to the end.
     *
     * @param value the value to append
     */
    void pushString(String value);

    /**
     * Appends a string to the end within a transaction.
     *
     * @param txn the transaction
     * @param value the value to append
     */
    void pushString(YTransaction txn, String value);

    // Double operations

    /**
     * Gets a double value at the specified index.
     *
     * @param index the index
     * @return the double value
     */
    double getDouble(int index);

    /**
     * Gets a double value at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @return the double value
     */
    double getDouble(YTransaction txn, int index);

    /**
     * Inserts a double at the specified index.
     *
     * @param index the index
     * @param value the value to insert
     */
    void insertDouble(int index, double value);

    /**
     * Inserts a double at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @param value the value to insert
     */
    void insertDouble(YTransaction txn, int index, double value);

    /**
     * Appends a double to the end.
     *
     * @param value the value to append
     */
    void pushDouble(double value);

    /**
     * Appends a double to the end within a transaction.
     *
     * @param txn the transaction
     * @param value the value to append
     */
    void pushDouble(YTransaction txn, double value);

    // Subdocument operations

    /**
     * Gets a subdocument at the specified index.
     *
     * @param index the index
     * @return the subdocument
     */
    YDoc getDoc(int index);

    /**
     * Gets a subdocument at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @return the subdocument
     */
    YDoc getDoc(YTransaction txn, int index);

    /**
     * Inserts a subdocument at the specified index.
     *
     * @param index the index
     * @param subdoc the subdocument to insert
     */
    void insertDoc(int index, YDoc subdoc);

    /**
     * Inserts a subdocument at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @param subdoc the subdocument to insert
     */
    void insertDoc(YTransaction txn, int index, YDoc subdoc);

    /**
     * Appends a subdocument to the end.
     *
     * @param subdoc the subdocument to append
     */
    void pushDoc(YDoc subdoc);

    /**
     * Appends a subdocument to the end within a transaction.
     *
     * @param txn the transaction
     * @param subdoc the subdocument to append
     */
    void pushDoc(YTransaction txn, YDoc subdoc);

    // Removal

    /**
     * Removes elements at the specified range.
     *
     * @param index the start index
     * @param length the number of elements to remove
     */
    void remove(int index, int length);

    /**
     * Removes elements at the specified range within a transaction.
     *
     * @param txn the transaction
     * @param index the start index
     * @param length the number of elements to remove
     */
    void remove(YTransaction txn, int index, int length);

    // Serialization

    /**
     * Returns a JSON representation of this array.
     *
     * @return the JSON string
     */
    String toJson();

    /**
     * Returns a JSON representation of this array within a transaction.
     *
     * @param txn the transaction
     * @return the JSON string
     */
    String toJson(YTransaction txn);

    /**
     * Registers an observer for changes to this array.
     *
     * @param observer the observer to register
     * @return a subscription handle for unregistering
     */
    YSubscription observe(YObserver observer);

    /**
     * Closes this array and releases resources.
     */
    @Override
    void close();

    /**
     * Checks if this array has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}
