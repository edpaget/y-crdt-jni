package net.carcdr.ycrdt;

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
public class YXmlFragment implements AutoCloseable {

    private final YDoc doc;
    private final long nativeHandle;
    private boolean closed = false;

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
        nativeInsertElement(doc.getNativeHandle(), nativeHandle, index, tag);
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
        nativeInsertText(doc.getNativeHandle(), nativeHandle, index, content);
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
        nativeRemove(doc.getNativeHandle(), nativeHandle, index, length);
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
     * Closes this fragment and releases native resources.
     * After calling this method, the fragment cannot be used.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            nativeDestroy(nativeHandle);
            closed = true;
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

    private static native void nativeInsertElement(long docPtr, long fragmentPtr, int index,
            String tag);

    private static native void nativeInsertText(long docPtr, long fragmentPtr, int index,
            String content);

    private static native void nativeRemove(long docPtr, long fragmentPtr, int index, int length);

    private static native int nativeGetNodeType(long docPtr, long fragmentPtr, int index);

    private static native String nativeToXmlString(long docPtr, long fragmentPtr);
}
