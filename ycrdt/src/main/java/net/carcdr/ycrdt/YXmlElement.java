package net.carcdr.ycrdt;

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
 * try (YDoc doc = new YDoc();
 *      YXmlElement element = doc.getXmlElement("div")) {
 *     element.setAttribute("class", "container");
 *     element.setAttribute("id", "main");
 *     System.out.println(element.getTag()); // div
 *     System.out.println(element.getAttribute("class")); // container
 * }
 * }</pre>
 *
 * @see YDoc
 */
public class YXmlElement implements Closeable, YObservable {

    private final YDoc doc;
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
    YXmlElement(YDoc doc, String name) {
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
    YXmlElement(YDoc doc, long nativeHandle) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Invalid native handle");
        }
        this.doc = doc;
        this.nativePtr = nativeHandle;
    }

    /**
     * Returns the tag name of this XML element.
     *
     * @return The tag name
     * @throws IllegalStateException if the XML element has been closed
     */
    public String getTag() {
        checkClosed();
        String result = nativeGetTag(doc.getNativePtr(), nativePtr);
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
        return nativeGetAttribute(doc.getNativePtr(), nativePtr, name);
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
        nativeSetAttribute(doc.getNativePtr(), nativePtr, name, value);
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
        nativeRemoveAttribute(doc.getNativePtr(), nativePtr, name);
    }

    /**
     * Gets all attribute names.
     *
     * @return An array of all attribute names
     * @throws IllegalStateException if the XML element has been closed
     */
    public String[] getAttributeNames() {
        checkClosed();
        Object result = nativeGetAttributeNames(doc.getNativePtr(), nativePtr);
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
        String result = nativeToString(doc.getNativePtr(), nativePtr);
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
        return nativeChildCount(doc.getNativePtr(), nativePtr);
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
    public YXmlElement insertElement(int index, String tag) {
        checkClosed();
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        long childPtr = nativeInsertElement(doc.getNativePtr(), nativePtr, index, tag);
        if (childPtr == 0) {
            throw new RuntimeException("Failed to insert element child");
        }
        return new YXmlElement(doc, childPtr);
    }

    /**
     * Inserts an XML text child at the specified index.
     *
     * @param index The index at which to insert the child
     * @return The new child text node
     * @throws IndexOutOfBoundsException if index is negative
     * @throws IllegalStateException if the XML element has been closed
     */
    public YXmlText insertText(int index) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        long childPtr = nativeInsertText(doc.getNativePtr(), nativePtr, index);
        if (childPtr == 0) {
            throw new RuntimeException("Failed to insert text child");
        }
        return new YXmlText(doc, childPtr);
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
        Object result = nativeGetChild(doc.getNativePtr(), nativePtr, index);
        if (result == null) {
            return null;
        }

        // Result is Object[2] where [0] = Integer type, [1] = Long pointer
        Object[] array = (Object[]) result;
        int type = ((Integer) array[0]).intValue();
        long pointer = ((Long) array[1]).longValue();

        if (type == 0) {
            // Element
            return new YXmlElement(doc, pointer);
        } else if (type == 1) {
            // Text
            return new YXmlText(doc, pointer);
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
        nativeRemoveChild(doc.getNativePtr(), nativePtr, index);
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
        Object result = nativeGetParent(doc.getNativePtr(), nativePtr);
        if (result == null) {
            return null;
        }

        // Result is Object[2] where [0] = Integer type, [1] = Long pointer
        Object[] array = (Object[]) result;
        int type = ((Integer) array[0]).intValue();
        long pointer = ((Long) array[1]).longValue();

        if (type == 0) {
            // Element
            return new YXmlElement(doc, pointer);
        } else if (type == 1) {
            // Fragment
            return new YXmlFragment(doc, pointer);
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
        return nativeGetIndexInParent(doc.getNativePtr(), nativePtr);
    }

    /**
     * Registers an observer to be notified when this XML element changes.
     *
     * <p>The observer will be called whenever children are added/removed or attributes
     * are modified in this element. The observer receives a {@link YEvent} containing
     * details about the changes.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
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
            if (!closed && nativePtr != 0) {
                nativeUnobserve(doc.getNativePtr(), nativePtr, subscriptionId);
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
                    // Unregister all observers
                    for (Long subscriptionId : observers.keySet()) {
                        if (nativePtr != 0) {
                            nativeUnobserve(doc.getNativePtr(), nativePtr, subscriptionId);
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
    private static native String nativeGetTag(long docPtr, long xmlElementPtr);
    private static native String nativeGetAttribute(long docPtr, long xmlElementPtr, String name);
    private static native void nativeSetAttribute(
            long docPtr, long xmlElementPtr, String name, String value);
    private static native void nativeRemoveAttribute(
            long docPtr, long xmlElementPtr, String name);
    private static native Object nativeGetAttributeNames(long docPtr, long xmlElementPtr);
    private static native String nativeToString(long docPtr, long xmlElementPtr);
    private static native int nativeChildCount(long docPtr, long xmlElementPtr);
    private static native long nativeInsertElement(long docPtr, long xmlElementPtr, int index, String tag);
    private static native long nativeInsertText(long docPtr, long xmlElementPtr, int index);
    private static native Object nativeGetChild(long docPtr, long xmlElementPtr, int index);
    private static native void nativeRemoveChild(long docPtr, long xmlElementPtr, int index);
    private static native Object nativeGetParent(long docPtr, long xmlElementPtr);
    private static native int nativeGetIndexInParent(long docPtr, long xmlElementPtr);
    private static native void nativeObserve(long docPtr, long xmlElementPtr, long subscriptionId,
                                              YXmlElement xmlElementObj);
    private static native void nativeUnobserve(long docPtr, long xmlElementPtr, long subscriptionId);
}
