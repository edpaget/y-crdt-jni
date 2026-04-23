package net.carcdr.ycrdt;

/**
 * A collaborative XML element that supports concurrent editing.
 */
public interface YXmlElement extends YXmlNode, AutoCloseable {

    /**
     * Returns the tag name of this element.
     *
     * @return the tag name
     */
    String getTag();

    /**
     * Returns the tag name of this element within a transaction.
     *
     * @param txn the transaction
     * @return the tag name
     */
    String getTag(YTransaction txn);

    // Attributes

    /**
     * Gets an attribute value.
     *
     * <p>Returns the attribute value with its original type. Supported return
     * types are {@link String}, {@link Long}, {@link Double}, {@link Boolean},
     * or {@code null} if the attribute is not present or was stored as null.
     *
     * @param name the attribute name
     * @return the attribute value, or {@code null} if not present
     */
    Object getAttribute(String name);

    /**
     * Gets an attribute value within a transaction.
     *
     * @param txn the transaction
     * @param name the attribute name
     * @return the attribute value, or {@code null} if not present
     * @see #getAttribute(String)
     */
    Object getAttribute(YTransaction txn, String name);

    /**
     * Sets an attribute value.
     *
     * <p>Supported value types are {@link String}, {@link Long},
     * {@link Integer}, {@link Double}, {@link Float}, {@link Boolean},
     * or {@code null}. {@code Integer} and {@code Float} are widened to
     * {@code Long} and {@code Double} respectively when stored, so they
     * round-trip through {@link #getAttribute(String)} as the wider type.
     *
     * @param name the attribute name
     * @param value the attribute value (may be {@code null})
     * @throws IllegalArgumentException if {@code value} is not a supported type
     */
    void setAttribute(String name, Object value);

    /**
     * Sets an attribute value within a transaction.
     *
     * @param txn the transaction
     * @param name the attribute name
     * @param value the attribute value (may be {@code null})
     * @throws IllegalArgumentException if {@code value} is not a supported type
     * @see #setAttribute(String, Object)
     */
    void setAttribute(YTransaction txn, String name, Object value);

    /**
     * Removes an attribute.
     *
     * @param name the attribute name
     */
    void removeAttribute(String name);

    /**
     * Removes an attribute within a transaction.
     *
     * @param txn the transaction
     * @param name the attribute name
     */
    void removeAttribute(YTransaction txn, String name);

    /**
     * Returns all attribute names.
     *
     * @return an array of attribute names
     */
    String[] getAttributeNames();

    /**
     * Returns all attribute names within a transaction.
     *
     * @param txn the transaction
     * @return an array of attribute names
     */
    String[] getAttributeNames(YTransaction txn);

    // Children

    /**
     * Returns the number of child nodes.
     *
     * @return the child count
     */
    int childCount();

    /**
     * Returns the number of child nodes within a transaction.
     *
     * @param txn the transaction
     * @return the child count
     */
    int childCount(YTransaction txn);

    /**
     * Inserts a child element at the specified index.
     *
     * @param index the index to insert at
     * @param tag the tag name of the new element
     * @return the new element
     */
    YXmlElement insertElement(int index, String tag);

    /**
     * Inserts a child element at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index to insert at
     * @param tag the tag name of the new element
     * @return the new element
     */
    YXmlElement insertElement(YTransaction txn, int index, String tag);

    /**
     * Inserts a child text node at the specified index.
     *
     * @param index the index to insert at
     * @return the new text node
     */
    YXmlText insertText(int index);

    /**
     * Inserts a child text node at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index to insert at
     * @return the new text node
     */
    YXmlText insertText(YTransaction txn, int index);

    /**
     * Gets a child node at the specified index.
     *
     * @param index the index
     * @return the child node (YXmlElement or YXmlText)
     */
    Object getChild(int index);

    /**
     * Gets a child node at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     * @return the child node (YXmlElement or YXmlText)
     */
    Object getChild(YTransaction txn, int index);

    /**
     * Removes a child node at the specified index.
     *
     * @param index the index
     */
    void removeChild(int index);

    /**
     * Removes a child node at the specified index within a transaction.
     *
     * @param txn the transaction
     * @param index the index
     */
    void removeChild(YTransaction txn, int index);

    // Parent

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
     * Returns the index of this element within its parent.
     *
     * @return the index
     */
    int getIndexInParent();

    /**
     * Returns the index of this element within its parent, using a transaction.
     *
     * @param txn the transaction
     * @return the index
     */
    int getIndexInParent(YTransaction txn);

    /**
     * Returns the XML representation of this element.
     *
     * @return the XML string
     */
    @Override
    String toString();

    /**
     * Returns the XML representation of this element within a transaction.
     *
     * @param txn the transaction
     * @return the XML string
     */
    String toString(YTransaction txn);

    /**
     * Registers an observer for changes to this element.
     *
     * @param observer the observer to register
     * @return a subscription handle for unregistering
     */
    YSubscription observe(YObserver observer);

    /**
     * Closes this element and releases resources.
     */
    @Override
    void close();

    /**
     * Checks if this element has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}
