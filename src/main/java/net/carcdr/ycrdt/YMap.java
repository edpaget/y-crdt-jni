package net.carcdr.ycrdt;

import java.io.Closeable;

/**
 * YMap represents a collaborative map type in a Y-CRDT document.
 *
 * <p>YMap provides efficient collaborative map operations with automatic conflict resolution.
 * Multiple users can modify the same map simultaneously, and changes will be merged
 * automatically using CRDT algorithms.</p>
 *
 * <p>This class implements {@link Closeable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc();
 *      YMap map = doc.getMap("mymap")) {
 *     map.setString("name", "Alice");
 *     map.setDouble("age", 30.0);
 *     System.out.println(map.toJson()); // {"name":"Alice","age":30.0}
 * }
 * }</pre>
 *
 * @see YDoc
 */
public class YMap implements Closeable {

    private final YDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;

    /**
     * Package-private constructor. Use {@link YDoc#getMap(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this map object in the document
     */
    YMap(YDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativePtr = nativeGetMap(doc.getNativePtr(), name);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YMap");
        }
    }

    /**
     * Returns the number of entries in the map.
     *
     * @return The size of the map
     * @throws IllegalStateException if the map has been closed
     */
    public int size() {
        checkClosed();
        return (int) nativeSize(doc.getNativePtr(), nativePtr);
    }

    /**
     * Checks if the map is empty.
     *
     * @return true if the map contains no entries, false otherwise
     * @throws IllegalStateException if the map has been closed
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Gets a string value by key.
     *
     * @param key The key to look up
     * @return The string value, or null if key not found or value is not a string
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException if the map has been closed
     */
    public String getString(String key) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return nativeGetString(doc.getNativePtr(), nativePtr, key);
    }

    /**
     * Gets a double value by key.
     *
     * @param key The key to look up
     * @return The double value, or 0.0 if key not found or value is not a number
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException if the map has been closed
     */
    public double getDouble(String key) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return nativeGetDouble(doc.getNativePtr(), nativePtr, key);
    }

    /**
     * Sets a string value in the map.
     *
     * @param key The key to set
     * @param value The string value to set
     * @throws IllegalArgumentException if key or value is null
     * @throws IllegalStateException if the map has been closed
     */
    public void setString(String key, String value) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        nativeSetString(doc.getNativePtr(), nativePtr, key, value);
    }

    /**
     * Sets a double value in the map.
     *
     * @param key The key to set
     * @param value The double value to set
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException if the map has been closed
     */
    public void setDouble(String key, double value) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        nativeSetDouble(doc.getNativePtr(), nativePtr, key, value);
    }

    /**
     * Removes a key from the map.
     *
     * @param key The key to remove
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException if the map has been closed
     */
    public void remove(String key) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        nativeRemove(doc.getNativePtr(), nativePtr, key);
    }

    /**
     * Checks if a key exists in the map.
     *
     * @param key The key to check
     * @return true if the key exists, false otherwise
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException if the map has been closed
     */
    public boolean containsKey(String key) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return nativeContainsKey(doc.getNativePtr(), nativePtr, key);
    }

    /**
     * Gets all keys from the map.
     *
     * @return An array of all keys in the map
     * @throws IllegalStateException if the map has been closed
     */
    public String[] keys() {
        checkClosed();
        Object result = nativeKeys(doc.getNativePtr(), nativePtr);
        if (result == null) {
            return new String[0];
        }
        return (String[]) result;
    }

    /**
     * Removes all entries from the map.
     *
     * @throws IllegalStateException if the map has been closed
     */
    public void clear() {
        checkClosed();
        nativeClear(doc.getNativePtr(), nativePtr);
    }

    /**
     * Returns a JSON string representation of the map.
     *
     * @return A JSON string representation
     * @throws IllegalStateException if the map has been closed
     */
    public String toJson() {
        checkClosed();
        return nativeToJson(doc.getNativePtr(), nativePtr);
    }

    /**
     * Checks if this YMap has been closed.
     *
     * @return true if this YMap has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this YMap and releases native resources.
     *
     * <p>After calling this method, any operations on this YMap will throw
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
     * Checks if this YMap has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this YMap has been closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YMap has been closed");
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
    private static native long nativeGetMap(long docPtr, String name);
    private static native void nativeDestroy(long ptr);
    private static native long nativeSize(long docPtr, long mapPtr);
    private static native String nativeGetString(long docPtr, long mapPtr, String key);
    private static native double nativeGetDouble(long docPtr, long mapPtr, String key);
    private static native void nativeSetString(long docPtr, long mapPtr, String key, String value);
    private static native void nativeSetDouble(long docPtr, long mapPtr, String key, double value);
    private static native void nativeRemove(long docPtr, long mapPtr, String key);
    private static native boolean nativeContainsKey(long docPtr, long mapPtr, String key);
    private static native Object nativeKeys(long docPtr, long mapPtr);
    private static native void nativeClear(long docPtr, long mapPtr);
    private static native String nativeToJson(long docPtr, long mapPtr);
}
