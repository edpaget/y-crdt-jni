package net.carcdr.ycrdt;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a fragment of XML content in a Y-CRDT document.
 * A fragment is a container for XML nodes (elements and text) and serves as the root
 * of an XML tree structure.
 *
 * <p>XML fragments support hierarchical structures, allowing elements and text nodes
 * to be organized in a tree. Unlike the simpler {@link YXmlElement} and {@link YXmlText}
 * which wrap single nodes, a fragment can contain multiple root-level children.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc();
 *      YXmlFragment fragment = doc.getXmlFragment("document")) {
 *
 *     // Insert an element
 *     fragment.insertElement(0, "div");
 *
 *     // Insert text
 *     fragment.insertText(1, "Hello World");
 *
 *     // Get number of children
 *     System.out.println("Children: " + fragment.length());
 *
 *     // Get XML string representation
 *     System.out.println(fragment.toXmlString());
 * }
 * }</pre>
 *
 * <p>This class implements {@link AutoCloseable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * @since 0.2.0
 */
public class YXmlFragment implements Closeable, YObservable {

    private final YDoc doc;
    private long nativeHandle;
    private volatile boolean closed = false;
    private final ConcurrentHashMap<Long, YObserver> observers = new ConcurrentHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong(0);

    /**
     * Package-private constructor. Use {@link YDoc#getXmlFragment(String)} to create instances.
     *
     * @param doc the parent YDoc
     * @param name the name of the fragment in the document
     */
    YXmlFragment(YDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativeHandle = nativeGetFragment(doc.getNativeHandle(), name);
    }

    /**
     * Package-private constructor that accepts a native handle directly.
     * Used for retrieving fragment references from parent navigation.
     *
     * @param doc The parent YDoc instance
     * @param nativeHandle The native pointer to the XmlFragmentRef
     */
    YXmlFragment(YDoc doc, long nativeHandle) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Invalid native handle");
        }
        this.doc = doc;
        this.nativeHandle = nativeHandle;
    }

    /**
     * Returns the number of children in this fragment.
     *
     * @return the number of child nodes
     * @throws IllegalStateException if this fragment has been closed
     */
    public int length() {
        checkClosed();
        return nativeLength(doc.getNativeHandle(), nativeHandle);
    }

    /**
     * Inserts an XML element as a child at the specified index.
     *
     * @param index the index at which to insert (0-based)
     * @param tag the tag name for the element (e.g., "div", "span")
     * @throws IllegalStateException if this fragment has been closed
     * @throws IllegalArgumentException if tag is null
     * @throws IndexOutOfBoundsException if index is negative or greater than length()
     */
    public void insertElement(int index, String tag) {
        checkClosed();
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length());
        }
        nativeInsertElement(doc.getNativeHandle(), nativeHandle, 0, index, tag);
    }

    /**
     * Inserts an XML element as a child at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     fragment.insertElement(txn, 0, "div");
     *     fragment.insertElement(txn, 1, "span");
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index the index at which to insert (0-based)
     * @param tag the tag name for the element (e.g., "div", "span")
     * @throws IllegalArgumentException if txn or tag is null
     * @throws IllegalStateException if this fragment has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than length()
     */
    public void insertElement(YTransaction txn, int index, String tag) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length());
        }
        nativeInsertElement(doc.getNativeHandle(), nativeHandle, txn.getNativePtr(), index, tag);
    }

    /**
     * Inserts an XML text node as a child at the specified index.
     *
     * @param index the index at which to insert (0-based)
     * @param content the text content
     * @throws IllegalStateException if this fragment has been closed
     * @throws IllegalArgumentException if content is null
     * @throws IndexOutOfBoundsException if index is negative or greater than length()
     */
    public void insertText(int index, String content) {
        checkClosed();
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length());
        }
        nativeInsertText(doc.getNativeHandle(), nativeHandle, 0, index, content);
    }

    /**
     * Inserts an XML text node as a child at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     fragment.insertText(txn, 0, "Hello");
     *     fragment.insertText(txn, 1, "World");
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index the index at which to insert (0-based)
     * @param content the text content
     * @throws IllegalArgumentException if txn or content is null
     * @throws IllegalStateException if this fragment has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than length()
     */
    public void insertText(YTransaction txn, int index, String content) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length());
        }
        nativeInsertText(doc.getNativeHandle(), nativeHandle, txn.getNativePtr(), index, content);
    }

    /**
     * Removes children from this fragment.
     *
     * @param index the starting index (0-based)
     * @param length the number of children to remove
     * @throws IllegalStateException if this fragment has been closed
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void remove(int index, int length) {
        checkClosed();
        if (index < 0 || length < 0) {
            throw new IndexOutOfBoundsException(
                    "Index and length must be non-negative: index=" + index + ", length=" + length);
        }
        int fragmentLength = length();
        if (index + length > fragmentLength) {
            throw new IndexOutOfBoundsException(
                    "Range [" + index + ", " + (index + length)
                            + ") exceeds fragment length " + fragmentLength);
        }
        nativeRemove(doc.getNativeHandle(), nativeHandle, 0, index, length);
    }

    /**
     * Removes children from this fragment within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     fragment.remove(txn, 0, 1);
     *     fragment.remove(txn, 0, 1);
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index the starting index (0-based)
     * @param length the number of children to remove
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if this fragment has been closed
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void remove(YTransaction txn, int index, int length) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0 || length < 0) {
            throw new IndexOutOfBoundsException(
                    "Index and length must be non-negative: index=" + index + ", length=" + length);
        }
        int fragmentLength = length();
        if (index + length > fragmentLength) {
            throw new IndexOutOfBoundsException(
                    "Range [" + index + ", " + (index + length)
                            + ") exceeds fragment length " + fragmentLength);
        }
        nativeRemove(doc.getNativeHandle(), nativeHandle, txn.getNativePtr(), index, length);
    }

    /**
     * Gets the type of the child node at the specified index.
     *
     * @param index the index of the child (0-based)
     * @return the node type, or null if index is out of bounds
     * @throws IllegalStateException if this fragment has been closed
     */
    public YXmlNode.NodeType getNodeType(int index) {
        checkClosed();
        int type = nativeGetNodeType(doc.getNativeHandle(), nativeHandle, index);
        if (type == 0) {
            return YXmlNode.NodeType.ELEMENT;
        } else if (type == 1) {
            return YXmlNode.NodeType.TEXT;
        }
        return null;
    }

    /**
     * Retrieves a child node at the specified index.
     *
     * <p>This generic method automatically returns the correct type (YXmlElement or YXmlText)
     * based on the child node type. This is more convenient than calling {@link #getNodeType}
     * followed by type-specific getters.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YXmlFragment fragment = doc.getXmlFragment("doc")) {
     *     fragment.insertElement(0, "div");
     *     fragment.insertText(1, "Hello");
     *
     *     // Generic access - automatically returns correct type
     *     Object child0 = fragment.getChild(0); // Returns YXmlElement
     *     Object child1 = fragment.getChild(1); // Returns YXmlText
     *
     *     if (child0 instanceof YXmlElement) {
     *         YXmlElement elem = (YXmlElement) child0;
     *         // Use element...
     *         elem.close();
     *     }
     * }
     * }</pre>
     *
     * @param index the index of the child node (0-based)
     * @return a YXmlElement or YXmlText depending on the child type,
     *         or null if the index is out of bounds
     * @throws IllegalStateException if this fragment has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public Object getChild(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }

        YXmlNode.NodeType type = getNodeType(index);
        if (type == null) {
            return null;
        }

        switch (type) {
            case ELEMENT:
                return getElement(index);
            case TEXT:
                return getText(index);
            default:
                return null;
        }
    }

    /**
     * Retrieves a child element at the specified index.
     *
     * <p>This method returns a new {@link YXmlElement} instance that wraps the child element
     * at the given index. The returned element is independent and must be closed separately
     * using try-with-resources or by calling {@link YXmlElement#close()}.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YXmlFragment fragment = doc.getXmlFragment("doc")) {
     *     fragment.insertElement(0, "div");
     *
     *     // Retrieve the element
     *     try (YXmlElement div = fragment.getElement(0)) {
     *         div.setAttribute("class", "container");
     *         System.out.println(div.getTag()); // "div"
     *     }
     * }
     * }</pre>
     *
     * @param index the index of the child element (0-based)
     * @return a YXmlElement wrapping the child element, or null if the child at
     *         the given index is not an element or the index is out of bounds
     * @throws IllegalStateException if this fragment has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public YXmlElement getElement(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        long elementPtr = nativeGetElement(doc.getNativeHandle(), nativeHandle, index);
        if (elementPtr == 0) {
            return null;
        }
        return new YXmlElement(doc, elementPtr);
    }

    /**
     * Retrieves a child text node at the specified index.
     *
     * <p>This method returns a new {@link YXmlText} instance that wraps the child text node
     * at the given index. The returned text node is independent and must be closed separately
     * using try-with-resources or by calling {@link YXmlText#close()}.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YXmlFragment fragment = doc.getXmlFragment("doc")) {
     *     fragment.insertText(0, "Hello");
     *
     *     // Retrieve the text node
     *     try (YXmlText text = fragment.getText(0)) {
     *         text.push(" World");
     *         System.out.println(text.toString()); // "Hello World"
     *     }
     * }
     * }</pre>
     *
     * @param index the index of the child text node (0-based)
     * @return a YXmlText wrapping the child text node, or null if the child at
     *         the given index is not a text node or the index is out of bounds
     * @throws IllegalStateException if this fragment has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public YXmlText getText(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        long textPtr = nativeGetText(doc.getNativeHandle(), nativeHandle, index);
        if (textPtr == 0) {
            return null;
        }
        return new YXmlText(doc, textPtr);
    }

    /**
     * Returns the XML string representation of this fragment.
     * This includes all child nodes serialized as XML.
     *
     * @return the XML string
     * @throws IllegalStateException if this fragment has been closed
     */
    public String toXmlString() {
        checkClosed();
        return nativeToXmlString(doc.getNativeHandle(), nativeHandle);
    }

    /**
     * Returns the XML string representation of this fragment.
     * Equivalent to {@link #toXmlString()}.
     *
     * @return the XML string
     */
    @Override
    public String toString() {
        if (closed) {
            return "YXmlFragment{closed}";
        }
        return toXmlString();
    }

    /**
     * Registers an observer to be notified when this XML fragment changes.
     *
     * <p>The observer will be called whenever child nodes are added, removed, or modified
     * in this fragment. The observer receives a {@link YEvent} containing details about
     * the changes.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YXmlFragment fragment = doc.getXmlFragment("document");
     *      YSubscription sub = fragment.observe(event -> {
     *          System.out.println("Fragment changed!");
     *          for (YChange change : event.getChanges()) {
     *              // Handle change
     *          }
     *      })) {
     *     fragment.insertElement(0, "div"); // Observer is called
     * }
     * }</pre>
     *
     * @param observer The observer to register (must not be null)
     * @return A subscription handle that can be used to unregister the observer
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this fragment has been closed
     */
    public YSubscription observe(YObserver observer) {
        checkClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        long id = nextSubscriptionId.incrementAndGet();
        observers.put(id, observer);
        nativeObserve(doc.getNativeHandle(), nativeHandle, id, this);
        return new YSubscription(id, observer, this);
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
            if (!closed && nativeHandle != 0) {
                nativeUnobserve(doc.getNativeHandle(), nativeHandle, subscriptionId);
            }
        }
    }

    /**
     * Dispatches an event to the observer registered with the given subscription ID.
     *
     * <p>This method is called from native code when fragment changes occur.
     * It should not be called directly by user code.</p>
     *
     * @param subscriptionId The subscription ID
     * @param event The event to dispatch
     */
    void dispatchEvent(long subscriptionId, YEvent event) {
        YObserver observer = observers.get(subscriptionId);
        if (observer != null) {
            try {
                observer.onChange(event);
            } catch (Exception e) {
                System.err.println("Observer threw exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes this fragment and releases native resources.
     * After calling this method, the fragment cannot be used.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    // Unregister all observers
                    for (Long subscriptionId : observers.keySet()) {
                        if (nativeHandle != 0) {
                            nativeUnobserve(doc.getNativeHandle(), nativeHandle, subscriptionId);
                        }
                    }
                    observers.clear();

                    if (nativeHandle != 0) {
                        nativeDestroy(nativeHandle);
                        nativeHandle = 0;
                    }
                    closed = true;
                }
            }
        }
    }

    /**
     * Checks if this fragment has been closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Throws an exception if this fragment has been closed.
     *
     * @throws IllegalStateException if closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YXmlFragment has been closed");
        }
    }

    // Native methods
    private static native long nativeGetFragment(long docPtr, String name);

    private static native void nativeDestroy(long ptr);

    private static native int nativeLength(long docPtr, long fragmentPtr);

    private static native void nativeInsertElement(long docPtr, long fragmentPtr, long txnPtr, int index,
            String tag);

    private static native void nativeInsertText(long docPtr, long fragmentPtr, long txnPtr, int index,
            String content);

    private static native void nativeRemove(long docPtr, long fragmentPtr, long txnPtr, int index, int length);

    private static native int nativeGetNodeType(long docPtr, long fragmentPtr, int index);

    private static native long nativeGetElement(long docPtr, long fragmentPtr, int index);

    private static native long nativeGetText(long docPtr, long fragmentPtr, int index);

    private static native String nativeToXmlString(long docPtr, long fragmentPtr);

    private static native void nativeObserve(long docPtr, long fragmentPtr, long subscriptionId,
                                              YXmlFragment fragmentObj);

    private static native void nativeUnobserve(long docPtr, long fragmentPtr, long subscriptionId);
}
