package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlElement;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YXmlElement represents a collaborative XML element type in a Y-CRDT document.
 *
 * <p>YXmlElement provides collaborative operations for XML elements including
 * tag names and attributes. Multiple users can modify the same element simultaneously,
 * and changes will be merged automatically using CRDT algorithms.</p>
 *
 * <p>This class implements {@link Closeable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new JniYDoc();
 *      YXmlElement element = doc.getXmlElement("div")) {
 *     element.setAttribute("class", "container");
 *     element.setAttribute("id", "main");
 *     System.out.println(element.getTag()); // div
 *     System.out.println(element.getAttribute("class")); // container
 * }
 * }</pre>
 *
 * @see net.carcdr.ycrdt.YDoc
 */
public class JniYXmlElement implements YXmlElement, JniYObservable {

    private final JniYDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;
    private final ConcurrentHashMap<Long, YObserver> observers = new ConcurrentHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong(0);

    /**
     * Package-private constructor. Use {@link YDoc#getXmlElement(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this XML element object in the document
     */
    JniYXmlElement(JniYDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativePtr = nativeGetXmlElement(doc.getNativePtr(), name);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YXmlElement");
        }
    }

    /**
     * Package-private constructor that accepts a native handle directly.
     * Used for retrieving child elements from fragments.
     *
     * @param doc The parent YDoc instance
     * @param nativeHandle The native pointer to the XmlElementRef
     */
    JniYXmlElement(JniYDoc doc, long nativeHandle) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Invalid native handle");
        }
        this.doc = doc;
        this.nativePtr = nativeHandle;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ELEMENT;
    }

    /**
     * Returns the tag name of this XML element.
     *
     * @return The tag name
     * @throws IllegalStateException if the XML element has been closed
     */
    public String getTag() {
        checkClosed();
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return getTag(txn);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return getTag(autoTxn);
        }
    }

    /**
     * Returns the tag name of this XML element using an existing transaction.
     *
     * @param txn Transaction handle
     * @return The tag name
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public String getTag(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        String result = nativeGetTagWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        return result != null ? result : "";
    }

    /**
     * Gets an attribute value by name.
     *
     * @param name The attribute name
     * @return The attribute value, or null if not found
     * @throws IllegalArgumentException if name is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public String getAttribute(String name) {
        checkClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return getAttribute(txn, name);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return getAttribute(autoTxn, name);
        }
    }

    /**
     * Gets an attribute value by name using an existing transaction.
     *
     * @param txn Transaction handle
     * @param name The attribute name
     * @return The attribute value, or null if not found
     * @throws IllegalArgumentException if txn or name is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public String getAttribute(YTransaction txn, String name) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return nativeGetAttributeWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), name);
    }

    /**
     * Sets an attribute value.
     *
     * @param name The attribute name
     * @param value The attribute value
     * @throws IllegalArgumentException if name or value is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public void setAttribute(String name, String value) {
        checkClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            setAttribute(txn, name, value);
            return;
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            setAttribute(autoTxn, name, value);
        }
    }

    /**
     * Sets an attribute value within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     element.setAttribute(txn, "class", "container");
     *     element.setAttribute(txn, "id", "main");
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param name The attribute name
     * @param value The attribute value
     * @throws IllegalArgumentException if txn, name or value is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public void setAttribute(YTransaction txn, String name, String value) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        nativeSetAttributeWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), name, value);
    }

    /**
     * Removes an attribute.
     *
     * @param name The attribute name to remove
     * @throws IllegalArgumentException if name is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public void removeAttribute(String name) {
        checkClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            removeAttribute(txn, name);
            return;
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            removeAttribute(autoTxn, name);
        }
    }

    /**
     * Removes an attribute within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     element.removeAttribute(txn, "class");
     *     element.removeAttribute(txn, "id");
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param name The attribute name to remove
     * @throws IllegalArgumentException if txn or name is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public void removeAttribute(YTransaction txn, String name) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        nativeRemoveAttributeWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), name);
    }

    /**
     * Gets all attribute names.
     *
     * @return An array of all attribute names
     * @throws IllegalStateException if the XML element has been closed
     */
    public String[] getAttributeNames() {
        checkClosed();
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return getAttributeNames(txn);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return getAttributeNames(autoTxn);
        }
    }

    /**
     * Gets all attribute names using an existing transaction.
     *
     * @param txn Transaction handle
     * @return An array of all attribute names
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public String[] getAttributeNames(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        Object result = nativeGetAttributeNamesWithTxn(doc.getNativePtr(), nativePtr,
            ((JniYTransaction) txn).getNativePtr());
        if (result == null) {
            return new String[0];
        }
        return (String[]) result;
    }

    /**
     * Returns the XML string representation of this element.
     *
     * @return The XML string representation
     * @throws IllegalStateException if the XML element has been closed
     */
    @Override
    public String toString() {
        checkClosed();
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return toString(txn);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return toString(autoTxn);
        }
    }

    /**
     * Returns the XML string representation of this element using an existing transaction.
     *
     * @param txn Transaction handle
     * @return The XML string representation
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public String toString(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        String result = nativeToStringWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        return result != null ? result : "";
    }

    /**
     * Gets the number of child nodes in this element.
     *
     * @return The number of child nodes
     * @throws IllegalStateException if the XML element has been closed
     */
    public int childCount() {
        checkClosed();
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return childCount(txn);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return childCount(autoTxn);
        }
    }

    /**
     * Gets the number of child nodes in this element using an existing transaction.
     *
     * @param txn Transaction handle
     * @return The number of child nodes
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public int childCount(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return nativeChildCountWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
    }

    /**
     * Inserts an XML element child at the specified index.
     *
     * @param index The index at which to insert the child
     * @param tag The tag name for the new element
     * @return The new child element
     * @throws IllegalArgumentException if tag is null
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public JniYXmlElement insertElement(int index, String tag) {
        checkClosed();
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return insertElement(txn, index, tag);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return insertElement(autoTxn, index, tag);
        }
    }

    /**
     * Inserts an XML element child at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     element.insertElement(txn, 0, "div");
     *     element.insertElement(txn, 1, "span");
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index The index at which to insert the child
     * @param tag The tag name for the new element
     * @return The new child element
     * @throws IllegalArgumentException if txn or tag is null
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public JniYXmlElement insertElement(YTransaction txn, int index, String tag) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        long childPtr = nativeInsertElementWithTxn(doc.getNativePtr(), nativePtr,
            ((JniYTransaction) txn).getNativePtr(), index, tag);
        if (childPtr == 0) {
            throw new RuntimeException("Failed to insert element child");
        }
        return new JniYXmlElement(doc, childPtr);
    }

    /**
     * Inserts an XML text child at the specified index.
     *
     * @param index The index at which to insert the child
     * @return The new child text node
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public JniYXmlText insertText(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return insertText(txn, index);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return insertText(autoTxn, index);
        }
    }

    /**
     * Inserts an XML text child at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     element.insertText(txn, 0);
     *     element.insertText(txn, 1);
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index The index at which to insert the child
     * @return The new child text node
     * @throws IllegalArgumentException if txn is null
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public JniYXmlText insertText(YTransaction txn, int index) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        long childPtr = nativeInsertTextWithTxn(doc.getNativePtr(), nativePtr,
            ((JniYTransaction) txn).getNativePtr(), index);
        if (childPtr == 0) {
            throw new RuntimeException("Failed to insert text child");
        }
        return new JniYXmlText(doc, childPtr);
    }

    /**
     * Gets the child node at the specified index.
     * The returned object can be either YXmlElement or YXmlText.
     *
     * @param index The index of the child to retrieve
     * @return The child node, or null if not found
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public Object getChild(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return getChild(txn, index);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return getChild(autoTxn, index);
        }
    }

    /**
     * Gets the child node at the specified index using an existing transaction.
     * The returned object can be either YXmlElement or YXmlText.
     *
     * @param txn Transaction handle
     * @param index The index of the child to retrieve
     * @return The child node, or null if not found
     * @throws IllegalArgumentException if txn is null
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public Object getChild(YTransaction txn, int index) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        Object result = nativeGetChildWithTxn(doc.getNativePtr(), nativePtr,
            ((JniYTransaction) txn).getNativePtr(), index);
        if (result == null) {
            return null;
        }

        // Result is Object[2] where [0] = Integer type, [1] = Long pointer
        Object[] array = (Object[]) result;
        int type = ((Integer) array[0]).intValue();
        long pointer = ((Long) array[1]).longValue();

        if (type == 0) {
            // Element
            return new JniYXmlElement(doc, pointer);
        } else if (type == 1) {
            // Text
            return new JniYXmlText(doc, pointer);
        } else {
            throw new RuntimeException("Unknown child type: " + type);
        }
    }

    /**
     * Removes the child node at the specified index.
     *
     * @param index The index of the child to remove
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public void removeChild(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            removeChild(txn, index);
            return;
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            removeChild(autoTxn, index);
        }
    }

    /**
     * Removes the child node at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     element.removeChild(txn, 0);
     *     element.removeChild(txn, 0);
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index The index of the child to remove
     * @throws IllegalArgumentException if txn is null
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public void removeChild(YTransaction txn, int index) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        nativeRemoveChildWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), index);
    }

    /**
     * Gets the parent of this XML element.
     * The parent can be either a YXmlElement or YXmlFragment.
     *
     * @return The parent node (YXmlElement or YXmlFragment), or null if this element has no parent
     * @throws IllegalStateException if the XML element has been closed
     */
    public Object getParent() {
        checkClosed();
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return getParent(txn);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return getParent(autoTxn);
        }
    }

    /**
     * Gets the parent of this XML element using an existing transaction.
     * The parent can be either a YXmlElement or YXmlFragment.
     *
     * @param txn Transaction handle
     * @return The parent node (YXmlElement or YXmlFragment), or null if this element has no parent
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public Object getParent(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        Object result = nativeGetParentWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        if (result == null) {
            return null;
        }

        // Result is Object[2] where [0] = Integer type, [1] = Long pointer
        Object[] array = (Object[]) result;
        int type = ((Integer) array[0]).intValue();
        long pointer = ((Long) array[1]).longValue();

        if (type == 0) {
            // Element
            return new JniYXmlElement(doc, pointer);
        } else if (type == 1) {
            // Fragment
            return new JniYXmlFragment(doc, pointer);
        } else {
            throw new RuntimeException("Unknown parent type: " + type);
        }
    }

    /**
     * Gets the index of this element within its parent's children.
     *
     * @return The 0-based index within parent, or -1 if this element has no parent
     * @throws IllegalStateException if the XML element has been closed
     */
    public int getIndexInParent() {
        checkClosed();
        YTransaction txn = doc.getActiveTransaction();
        if (txn != null) {
            return getIndexInParent(txn);
        }
        try (YTransaction autoTxn = doc.beginTransaction()) {
            return getIndexInParent(autoTxn);
        }
    }

    /**
     * Gets the index of this element within its parent's children using an existing transaction.
     *
     * @param txn Transaction handle
     * @return The 0-based index within parent, or -1 if this element has no parent
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML element has been closed
     */
    public int getIndexInParent(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return nativeGetIndexInParentWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
    }

    /**
     * Registers an observer to be notified when this XML element changes.
     *
     * <p>The observer will be called whenever children are added/removed or attributes
     * are modified in this element. The observer receives a {@link net.carcdr.ycrdt.YEvent} containing
     * details about the changes.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new JniYDoc();
     *      YXmlElement element = doc.getXmlElement("div");
     *      YSubscription sub = element.observe(event -> {
     *          System.out.println("Element changed!");
     *          for (YChange change : event.getChanges()) {
     *              // Handle change
     *          }
     *      })) {
     *     element.setAttribute("class", "active"); // Observer is called
     * }
     * }</pre>
     *
     * @param observer The observer to register (must not be null)
     * @return A subscription handle that can be used to unregister the observer
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this element has been closed
     */
    public YSubscription observe(YObserver observer) {
        checkClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        long id = nextSubscriptionId.incrementAndGet();
        observers.put(id, observer);
        nativeObserve(doc.getNativePtr(), nativePtr, id, this);
        return new JniYSubscription(id, observer, this);
    }

    /**
     * Unregisters an observer by its subscription ID.
     *
     * <p>This method is typically called automatically when a {@link YSubscription}
     * is closed. Users should prefer using try-with-resources with YSubscription
     * rather than calling this method directly.</p>
     *
     * @param subscriptionId The ID of the subscription to remove
     */
    @Override
    public void unobserveById(long subscriptionId) {
        if (observers.remove(subscriptionId) != null) {
            if (!closed && nativePtr != 0) {
                doc.deferNativeUnsubscribe(subscriptionId);
            }
        }
    }

    /**
     * Dispatches an event to the observer registered with the given subscription ID.
     *
     * <p>This method is called from native code when element changes occur.
     * It should not be called directly by user code.</p>
     *
     * @param subscriptionId The subscription ID
     * @param event The event to dispatch
     */
    void dispatchEvent(long subscriptionId, JniYEvent event) {
        YObserver observer = observers.get(subscriptionId);
        if (observer != null) {
            try {
                observer.onChange(event);
            } catch (Exception e) {
                doc.getObserverErrorHandler().handleError(e, this);
            }
        }
    }

    /**
     * Checks if this YXmlElement has been closed.
     *
     * @return true if this YXmlElement has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this YXmlElement and releases native resources.
     *
     * <p>After calling this method, any operations on this YXmlElement will throw
     * {@link IllegalStateException}.</p>
     *
     * <p>This method is idempotent - calling it multiple times has no effect
     * after the first call.</p>
     */
    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    // Defer unregistration of all observers
                    for (Long subscriptionId : observers.keySet()) {
                        if (nativePtr != 0) {
                            doc.deferNativeUnsubscribe(subscriptionId);
                        }
                    }
                    observers.clear();

                    if (nativePtr != 0) {
                        nativeDestroy(nativePtr);
                        nativePtr = 0;
                    }
                    closed = true;
                }
            }
        }
    }

    /**
     * Checks if this YXmlElement has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this YXmlElement has been closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YXmlElement has been closed");
        }
    }

    /**
     * Gets the native pointer for internal use.
     *
     * @return The native pointer value
     */
    long getNativePtr() {
        return nativePtr;
    }

    // Native methods
    private static native long nativeGetXmlElement(long docPtr, String name);
    private static native void nativeDestroy(long ptr);
    private static native String nativeGetTagWithTxn(long docPtr, long xmlElementPtr, long txnPtr);
    private static native String nativeGetAttributeWithTxn(long docPtr, long xmlElementPtr, long txnPtr, String name);
    private static native void nativeSetAttributeWithTxn(
            long docPtr, long xmlElementPtr, long txnPtr, String name, String value);
    private static native void nativeRemoveAttributeWithTxn(
            long docPtr, long xmlElementPtr, long txnPtr, String name);
    private static native Object nativeGetAttributeNamesWithTxn(long docPtr, long xmlElementPtr, long txnPtr);
    private static native String nativeToStringWithTxn(long docPtr, long xmlElementPtr, long txnPtr);
    private static native int nativeChildCountWithTxn(long docPtr, long xmlElementPtr, long txnPtr);
    private static native long nativeInsertElementWithTxn(
            long docPtr, long xmlElementPtr, long txnPtr, int index, String tag);
    private static native long nativeInsertTextWithTxn(long docPtr, long xmlElementPtr, long txnPtr, int index);
    private static native Object nativeGetChildWithTxn(long docPtr, long xmlElementPtr, long txnPtr, int index);
    private static native void nativeRemoveChildWithTxn(long docPtr, long xmlElementPtr, long txnPtr, int index);
    private static native Object nativeGetParentWithTxn(long docPtr, long xmlElementPtr, long txnPtr);
    private static native int nativeGetIndexInParentWithTxn(long docPtr, long xmlElementPtr, long txnPtr);
    private static native void nativeObserve(long docPtr, long xmlElementPtr, long subscriptionId,
                                              YXmlElement xmlElementObj);
    private static native void nativeUnobserve(long docPtr, long xmlElementPtr, long subscriptionId);
}
