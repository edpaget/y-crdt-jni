package net.carcdr.ycrdt;

import java.io.Closeable;

/**
 * YText represents a collaborative text type in a Y-CRDT document.
 *
 * <p>YText provides efficient collaborative text editing with automatic conflict resolution.
 * Multiple users can edit the same text simultaneously, and changes will be merged
 * automatically using CRDT algorithms.</p>
 *
 * <p>This class implements {@link Closeable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc();
 *      YText text = doc.getText("mytext")) {
 *     text.insert(0, "Hello");
 *     text.push(" World");
 *     System.out.println(text.toString()); // "Hello World"
 * }
 * }</pre>
 *
 * @see YDoc
 */
public class YText implements Closeable {

    private final YDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;

    /**
     * Package-private constructor. Use {@link YDoc#getText(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this text object in the document
     */
    YText(YDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativePtr = nativeGetText(doc.getNativePtr(), name);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YText");
        }
    }

    /**
     * Returns the length of the text.
     *
     * @return The number of characters in the text
     * @throws IllegalStateException if the text has been closed
     */
    public int length() {
        checkClosed();
        return nativeLength(doc.getNativePtr(), nativePtr);
    }

    /**
     * Returns the text content as a string.
     *
     * @return The current text content
     * @throws IllegalStateException if the text has been closed
     */
    @Override
    public String toString() {
        checkClosed();
        return nativeToString(doc.getNativePtr(), nativePtr);
    }

    /**
     * Inserts text at the specified index.
     *
     * @param index The position at which to insert the text (0-based)
     * @param chunk The text to insert
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the text has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insert(int index, String chunk) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsert(doc.getNativePtr(), nativePtr, index, chunk);
    }

    /**
     * Appends text to the end.
     *
     * @param chunk The text to append
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the text has been closed
     */
    public void push(String chunk) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        nativePush(doc.getNativePtr(), nativePtr, chunk);
    }

    /**
     * Deletes a range of text.
     *
     * @param index The starting position (0-based)
     * @param length The number of characters to delete
     * @throws IllegalStateException if the text has been closed
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void delete(int index, int length) {
        checkClosed();
        if (index < 0 || length < 0) {
            throw new IndexOutOfBoundsException(
                "Index and length must be non-negative");
        }
        int currentLength = length();
        if (index + length > currentLength) {
            throw new IndexOutOfBoundsException(
                "Range [" + index + ", " + (index + length) + ") out of bounds for length "
                + currentLength);
        }
        nativeDelete(doc.getNativePtr(), nativePtr, index, length);
    }

    /**
     * Checks if this YText has been closed.
     *
     * @return true if this YText has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this YText and releases native resources.
     *
     * <p>After calling this method, any operations on this YText will throw
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
     * Checks if this YText has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this YText has been closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YText has been closed");
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
    private static native long nativeGetText(long docPtr, String name);
    private static native void nativeDestroy(long ptr);
    private static native int nativeLength(long docPtr, long textPtr);
    private static native String nativeToString(long docPtr, long textPtr);
    private static native void nativeInsert(long docPtr, long textPtr, int index, String chunk);
    private static native void nativePush(long docPtr, long textPtr, String chunk);
    private static native void nativeDelete(long docPtr, long textPtr, int index, int length);
}
