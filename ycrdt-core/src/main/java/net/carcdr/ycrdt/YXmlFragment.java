package net.carcdr.ycrdt;

/**
 * A collaborative XML fragment container that supports concurrent editing.
 */
public interface YXmlFragment extends AutoCloseable {

    /**
     * Returns the number of child nodes.
     *
     * @return the child count
     */
    int length();

    /**
     * Returns the number of child nodes within a transaction.
     *
     * @param txn the transaction
     * @return the child count
     */
    int length(YTransaction txn);

    /**
     * Inserts a child element at the specified index.
     *
     * @param index the index to insert at
     * @param tag the tag name of the new element
     */
    void insertElement(int index, String tag);

    /**
     * Inserts a child element at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index to insert at
     * @param tag the tag name of the new element
     */
    void insertElement(YTransaction txn, int index, String tag);

    /**
     * Inserts a child text node at the specified index.
     *
     * @param index the index to insert at
     * @param content the initial text content
     */
    void insertText(int index, String content);

    /**
     * Inserts a child text node at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index to insert at
     * @param content the initial text content
     */
    void insertText(YTransaction txn, int index, String content);

    /**
     * Removes child nodes at the specified range.
     *
     * @param index the start index
     * @param length the number of nodes to remove
     */
    void remove(int index, int length);

    /**
     * Removes child nodes at the specified range within a transaction.
     *
     * @param txn the transaction
     * @param index the start index
     * @param length the number of nodes to remove
     */
    void remove(YTransaction txn, int index, int length);

    /**
     * Returns the type of the child node at the specified index.
     *
     * @param index the index
     * @return the node type
     */
    YXmlNode.NodeType getNodeType(int index);

    /**
     * Returns the type of the child node at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @return the node type
     */
    YXmlNode.NodeType getNodeType(YTransaction txn, int index);

    /**
     * Gets a child node at the specified index.
     *
     * @param index the index
     * @return the child node (YXmlElement or YXmlText)
     */
    Object getChild(int index);

    /**
     * Gets a child element at the specified index.
     *
     * @param index the index
     * @return the element
     */
    YXmlElement getElement(int index);

    /**
     * Gets a child element at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @return the element
     */
    YXmlElement getElement(YTransaction txn, int index);

    /**
     * Gets a child text node at the specified index.
     *
     * @param index the index
     * @return the text node
     */
    YXmlText getText(int index);

    /**
     * Gets a child text node at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @return the text node
     */
    YXmlText getText(YTransaction txn, int index);

    /**
     * Returns the XML representation of this fragment.
     *
     * @return the XML string
     */
    String toXmlString();

    /**
     * Returns the XML representation of this fragment within a transaction.
     *
     * @param txn the transaction
     * @return the XML string
     */
    String toXmlString(YTransaction txn);

    /**
     * Returns the XML representation of this fragment.
     *
     * @return the XML string
     */
    @Override
    String toString();

    /**
     * Registers an observer for changes to this fragment.
     *
     * @param observer the observer to register
     * @return a subscription handle for unregistering
     */
    YSubscription observe(YObserver observer);

    /**
     * Closes this fragment and releases resources.
     */
    @Override
    void close();

    /**
     * Checks if this fragment has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}
