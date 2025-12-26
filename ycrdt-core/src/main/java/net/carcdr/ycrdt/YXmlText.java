package net.carcdr.ycrdt;

import java.util.List;
import java.util.Map;

/**
 * A collaborative XML text node that supports concurrent editing with formatting.
 */
public interface YXmlText extends YXmlNode, AutoCloseable {

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
     * Returns the text content within a transaction.
     *
     * @param txn the transaction
     * @return the text as a string
     */
    String toString(YTransaction txn);

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
     * Inserts text with formatting attributes at the specified index.
     *
     * @param index the position to insert at
     * @param chunk the text to insert
     * @param attributes the formatting attributes
     */
    void insertWithAttributes(int index, String chunk, Map<String, Object> attributes);

    /**
     * Inserts text with formatting attributes at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the position to insert at
     * @param chunk the text to insert
     * @param attributes the formatting attributes
     */
    void insertWithAttributes(YTransaction txn, int index, String chunk,
                              Map<String, Object> attributes);

    /**
     * Applies formatting attributes to a range of text.
     *
     * @param index the start position
     * @param length the number of characters to format
     * @param attributes the formatting attributes
     */
    void format(int index, int length, Map<String, Object> attributes);

    /**
     * Applies formatting attributes to a range of text within a transaction.
     *
     * @param txn the transaction
     * @param index the start position
     * @param length the number of characters to format
     * @param attributes the formatting attributes
     */
    void format(YTransaction txn, int index, int length, Map<String, Object> attributes);

    /**
     * Returns the parent element or fragment.
     *
     * @return the parent, or null if none
     */
    Object getParent();

    /**
     * Returns the parent element or fragment within a transaction.
     *
     * @param txn the transaction
     * @return the parent, or null if none
     */
    Object getParent(YTransaction txn);

    /**
     * Returns the index of this node within its parent.
     *
     * @return the index
     */
    int getIndexInParent();

    /**
     * Returns the index of this node within its parent, using a transaction.
     *
     * @param txn the transaction
     * @return the index
     */
    int getIndexInParent(YTransaction txn);

    /**
     * Returns the text content as formatting chunks.
     *
     * @return a list of formatting chunks
     */
    List<FormattingChunk> getFormattingChunks();

    /**
     * Returns the text content as formatting chunks within a transaction.
     *
     * @param txn the transaction
     * @return a list of formatting chunks
     */
    List<FormattingChunk> getFormattingChunks(YTransaction txn);

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
