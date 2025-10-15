package net.carcdr.ycrdt;

import java.io.Closeable;

/**
 * YDoc represents a Y-CRDT document, which is a shared data structure that supports
 * concurrent editing and automatic conflict resolution.
 *
 * <p>This class wraps the native Rust implementation of y-crdt (yrs) and provides
 * a Java API for working with collaborative documents.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc()) {
 *     long clientId = doc.getClientId();
 *     String guid = doc.getGuid();
 *
 *     // Get document state as update
 *     byte[] state = doc.encodeStateAsUpdate();
 *
 *     // Apply update to another document
 *     try (YDoc doc2 = new YDoc()) {
 *         doc2.applyUpdate(state);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> YDoc instances are not thread-safe. If you need to access
 * a document from multiple threads, you must provide external synchronization.</p>
 *
 * <p><b>Memory Management:</b> YDoc implements {@link Closeable} and must be closed
 * when no longer needed to free native resources. Use try-with-resources to ensure
 * proper cleanup.</p>
 */
public class YDoc implements Closeable {

    static {
        // Load the native library
        NativeLoader.loadLibrary();
    }

    /**
     * Pointer to the native YDoc instance
     */
    private long nativePtr;

    /**
     * Flag to track if this instance has been closed
     */
    private volatile boolean closed = false;

    /**
     * Creates a new YDoc instance with a randomly generated client ID.
     *
     * @throws RuntimeException if native initialization fails
     */
    public YDoc() {
        this.nativePtr = nativeCreate();
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YDoc: native pointer is null");
        }
    }

    /**
     * Creates a new YDoc instance with a specific client ID.
     *
     * <p>The client ID is used to identify the source of changes in the CRDT.
     * Each client should have a unique ID to prevent conflicts.</p>
     *
     * @param clientId the client ID to assign to this document
     * @throws RuntimeException if native initialization fails
     * @throws IllegalArgumentException if clientId is negative
     */
    public YDoc(long clientId) {
        if (clientId < 0) {
            throw new IllegalArgumentException("Client ID must be non-negative");
        }
        this.nativePtr = nativeCreateWithClientId(clientId);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YDoc: native pointer is null");
        }
    }

    /**
     * Gets the client ID of this document.
     *
     * <p>The client ID uniquely identifies this document instance in a distributed
     * system. It is used to track the source of changes.</p>
     *
     * @return the client ID
     * @throws IllegalStateException if this document has been closed
     */
    public long getClientId() {
        ensureNotClosed();
        return nativeGetClientId(nativePtr);
    }

    /**
     * Gets the globally unique identifier (GUID) of this document.
     *
     * <p>The GUID is a UUID that uniquely identifies this document across all
     * instances. All replicas of the same document share the same GUID.</p>
     *
     * @return the GUID as a string
     * @throws IllegalStateException if this document has been closed
     */
    public String getGuid() {
        ensureNotClosed();
        return nativeGetGuid(nativePtr);
    }

    /**
     * Encodes the current state of the document as a binary update.
     *
     * <p>This method captures the entire state of the document and returns it
     * as a byte array that can be transmitted to other clients or stored for
     * later use.</p>
     *
     * <p>The returned byte array can be applied to another YDoc instance using
     * {@link #applyUpdate(byte[])} to synchronize their states.</p>
     *
     * @return a byte array containing the encoded document state
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if encoding fails
     */
    public byte[] encodeStateAsUpdate() {
        ensureNotClosed();
        byte[] result = nativeEncodeStateAsUpdate(nativePtr);
        if (result == null) {
            throw new RuntimeException("Failed to encode state as update");
        }
        return result;
    }

    /**
     * Applies a binary update to this document.
     *
     * <p>This method merges changes from another document into this one. The update
     * is typically obtained by calling {@link #encodeStateAsUpdate()} on another
     * YDoc instance.</p>
     *
     * <p>Updates are idempotent - applying the same update multiple times has the
     * same effect as applying it once.</p>
     *
     * @param update the binary update to apply
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if update is null
     * @throws RuntimeException if the update is invalid or cannot be applied
     */
    public void applyUpdate(byte[] update) {
        ensureNotClosed();
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }
        nativeApplyUpdate(nativePtr, update);
    }

    /**
     * Gets or creates a YText instance with the specified name.
     *
     * <p>This method returns a collaborative text type that can be shared between
     * multiple clients. If a text with this name already exists in the document,
     * it will be returned; otherwise, a new one will be created.</p>
     *
     * <p>The returned YText instance must be closed when no longer needed to free
     * native resources. Use try-with-resources for automatic cleanup.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YText text = doc.getText("mytext")) {
     *     text.insert(0, "Hello World");
     *     System.out.println(text.toString());
     * }
     * }</pre>
     *
     * @param name the name of the text object
     * @return a YText instance
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if name is null
     * @throws RuntimeException if text creation fails
     */
    public YText getText(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new YText(this, name);
    }

    /**
     * Gets or creates a YArray instance with the specified name.
     *
     * <p>This method returns a collaborative array type that can be shared between
     * multiple clients. If an array with this name already exists in the document,
     * it will be returned; otherwise, a new one will be created.</p>
     *
     * <p>The returned YArray instance must be closed when no longer needed to free
     * native resources. Use try-with-resources for automatic cleanup.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YArray array = doc.getArray("myarray")) {
     *     array.pushString("Hello");
     *     array.pushDouble(42.0);
     *     System.out.println(array.toJson());
     * }
     * }</pre>
     *
     * @param name the name of the array object
     * @return a YArray instance
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if name is null
     * @throws RuntimeException if array creation fails
     */
    public YArray getArray(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new YArray(this, name);
    }

    /**
     * Gets or creates a YMap instance with the specified name.
     *
     * <p>This method returns a collaborative map type that can be shared between
     * multiple clients. If a map with this name already exists in the document,
     * it will be returned; otherwise, a new one will be created.</p>
     *
     * <p>The returned YMap instance must be closed when no longer needed to free
     * native resources. Use try-with-resources for automatic cleanup.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YMap map = doc.getMap("mymap")) {
     *     map.setString("name", "Alice");
     *     map.setDouble("age", 30.0);
     *     System.out.println(map.toJson());
     * }
     * }</pre>
     *
     * @param name the name of the map object
     * @return a YMap instance
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if name is null
     * @throws RuntimeException if map creation fails
     */
    public YMap getMap(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new YMap(this, name);
    }

    /**
     * Closes this document and frees its native resources.
     *
     * <p>After calling this method, any further operations on this document
     * will throw {@link IllegalStateException}.</p>
     *
     * <p>This method is idempotent - calling it multiple times is safe.</p>
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
     * Checks if this document has been closed.
     *
     * @return true if this document has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Ensures that this document has not been closed.
     *
     * @throws IllegalStateException if this document has been closed
     */
    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("YDoc has been closed");
        }
    }

    /**
     * Gets the native pointer for internal use.
     *
     * @return the native pointer value
     */
    long getNativePtr() {
        return nativePtr;
    }

    /**
     * Ensures proper cleanup of native resources if close() was not called.
     *
     * <p>This is a safety net - you should always call {@link #close()} explicitly
     * or use try-with-resources.</p>
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    // Native method declarations

    private static native long nativeCreate();

    private static native long nativeCreateWithClientId(long clientId);

    private static native void nativeDestroy(long ptr);

    private static native long nativeGetClientId(long ptr);

    private static native String nativeGetGuid(long ptr);

    private static native byte[] nativeEncodeStateAsUpdate(long ptr);

    private static native void nativeApplyUpdate(long ptr, byte[] update);
}
