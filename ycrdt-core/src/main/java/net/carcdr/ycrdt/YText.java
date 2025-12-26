package net.carcdr.ycrdt;

/**
 * A collaborative text type that supports concurrent editing.
 */
public interface YText extends AutoCloseable {

    /**
     * Returns the length of the text.
     *
     * @return the number of characters
     */
    int length();

    /**
     * Returns the length of the text within a transaction.
     *
     * @param txn the transaction
     * @return the number of characters
     */
    int length(YTransaction txn);

    /**
     * Returns the text content.
     *
     * @return the text as a string
     */
    @Override
    String toString();

    /**
     * Inserts text at the specified index.
     *
     * @param index the position to insert at
     * @param chunk the text to insert
     */
    void insert(int index, String chunk);

    /**
     * Inserts text at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the position to insert at
     * @param chunk the text to insert
     */
    void insert(YTransaction txn, int index, String chunk);

    /**
     * Appends text to the end.
     *
     * @param chunk the text to append
     */
    void push(String chunk);

    /**
     * Appends text to the end within a transaction.
     *
     * @param txn the transaction
     * @param chunk the text to append
     */
    void push(YTransaction txn, String chunk);

    /**
     * Deletes text at the specified range.
     *
     * @param index the start position
     * @param length the number of characters to delete
     */
    void delete(int index, int length);

    /**
     * Deletes text at the specified range within a transaction.
     *
     * @param txn the transaction
     * @param index the start position
     * @param length the number of characters to delete
     */
    void delete(YTransaction txn, int index, int length);

    /**
     * Registers an observer for changes to this text.
     *
     * @param observer the observer to register
     * @return a subscription handle for unregistering
     */
    YSubscription observe(YObserver observer);

    /**
     * Closes this text and releases resources.
     */
    @Override
    void close();

    /**
     * Checks if this text has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}
