package net.carcdr.ycrdt;

import java.io.Closeable;

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
public class YXmlElement implements Closeable {

    private final YDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;

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
}
