package net.carcdr.ycrdt;

import java.io.Closeable;
import java.util.Map;

/**
 * YXmlText represents a collaborative XML text type in a Y-CRDT document.
 *
 * <p>YXmlText provides collaborative text editing operations specifically designed for
 * XML documents. It supports insert, push, delete, and rich text formatting operations
 * with automatic conflict resolution.</p>
 *
 * <p>This class implements {@link Closeable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc();
 *      YXmlText xmlText = doc.getXmlText("myxmltext")) {
 *     xmlText.push("Hello");
 *     xmlText.push(" World");
 *     System.out.println(xmlText.toString()); // Hello World
 * }
 * }</pre>
 *
 * @see YDoc
 */
public class YXmlText implements Closeable {

    private final YDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;

    /**
     * Package-private constructor. Use {@link YDoc#getXmlText(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this XML text object in the document
     */
    YXmlText(YDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativePtr = nativeGetXmlText(doc.getNativePtr(), name);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YXmlText");
        }
    }

    /**
     * Package-private constructor that accepts a native handle directly.
     * Used for retrieving child text nodes from fragments.
     *
     * @param doc The parent YDoc instance
     * @param nativeHandle The native pointer to the XmlTextRef
     */
    YXmlText(YDoc doc, long nativeHandle) {
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
     * Returns the length of the text in characters.
     *
     * @return The text length
     * @throws IllegalStateException if the XML text has been closed
     */
    public int length() {
        checkClosed();
        return nativeLength(doc.getNativePtr(), nativePtr);
    }

    /**
     * Returns the string representation of the XML text content.
     *
     * @return The text content as a string
     * @throws IllegalStateException if the XML text has been closed
     */
    @Override
    public String toString() {
        checkClosed();
        String result = nativeToString(doc.getNativePtr(), nativePtr);
        return result != null ? result : "";
    }

    /**
     * Inserts text at the specified index.
     *
     * @param index The index at which to insert the text (0-based)
     * @param chunk The text to insert
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public void insert(int index, String chunk) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        nativeInsert(doc.getNativePtr(), nativePtr, index, chunk);
    }

    /**
     * Appends text to the end of the XML text.
     *
     * @param chunk The text to append
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the XML text has been closed
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
     * @param index The starting index of the deletion (0-based)
     * @param length The number of characters to delete
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index or length is negative
     */
    public void delete(int index, int length) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
        }
        nativeDelete(doc.getNativePtr(), nativePtr, index, length);
    }

    /**
     * Inserts text with formatting attributes at the specified index.
     *
     * <p>The attributes map can contain formatting information such as:</p>
     * <ul>
     *   <li>Bold: {@code Map.of("b", true)}</li>
     *   <li>Italic: {@code Map.of("i", true)}</li>
     *   <li>Font: {@code Map.of("font", "Arial")}</li>
     *   <li>Color: {@code Map.of("color", "#FF0000")}</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Object> bold = Map.of("b", true);
     * xmlText.insertWithAttributes(0, "Hello", bold);
     * }</pre>
     *
     * @param index The index at which to insert the text (0-based)
     * @param chunk The text to insert
     * @param attributes A map of formatting attributes to apply to the text
     * @throws IllegalArgumentException if chunk or attributes is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public void insertWithAttributes(int index, String chunk, Map<String, Object> attributes) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        nativeInsertWithAttributes(doc.getNativePtr(), nativePtr, index, chunk, attributes);
    }

    /**
     * Formats a range of text with the specified attributes.
     *
     * <p>This method applies formatting to existing text. The attributes map can contain
     * formatting information such as:</p>
     * <ul>
     *   <li>Bold: {@code Map.of("b", true)}</li>
     *   <li>Italic: {@code Map.of("i", true)}</li>
     *   <li>Underline: {@code Map.of("u", true)}</li>
     * </ul>
     *
     * <p>To remove formatting, pass {@code null} as the attribute value:</p>
     * <pre>{@code
     * Map<String, Object> removeBold = Map.of("b", null);
     * xmlText.format(0, 5, removeBold);
     * }</pre>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * xmlText.insert(0, "Hello World");
     * Map<String, Object> bold = Map.of("b", true);
     * xmlText.format(0, 5, bold); // Makes "Hello" bold
     * }</pre>
     *
     * @param index The starting index of the range to format (0-based)
     * @param length The number of characters to format
     * @param attributes A map of formatting attributes to apply
     * @throws IllegalArgumentException if attributes is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index or length is negative
     */
    public void format(int index, int length, Map<String, Object> attributes) {
        checkClosed();
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
        }
        nativeFormat(doc.getNativePtr(), nativePtr, index, length, attributes);
    }

    /**
     * Checks if this YXmlText has been closed.
     *
     * @return true if this YXmlText has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this YXmlText and releases native resources.
     *
     * <p>After calling this method, any operations on this YXmlText will throw
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
     * Checks if this YXmlText has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this YXmlText has been closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YXmlText has been closed");
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
    private static native long nativeGetXmlText(long docPtr, String name);
    private static native void nativeDestroy(long ptr);
    private static native int nativeLength(long docPtr, long xmlTextPtr);
    private static native String nativeToString(long docPtr, long xmlTextPtr);
    private static native void nativeInsert(long docPtr, long xmlTextPtr, int index, String chunk);
    private static native void nativePush(long docPtr, long xmlTextPtr, String chunk);
    private static native void nativeDelete(long docPtr, long xmlTextPtr, int index, int length);
    private static native void nativeInsertWithAttributes(
            long docPtr, long xmlTextPtr, int index, String chunk, Map<String, Object> attributes);
    private static native void nativeFormat(
            long docPtr, long xmlTextPtr, int index, int length, Map<String, Object> attributes);
}
