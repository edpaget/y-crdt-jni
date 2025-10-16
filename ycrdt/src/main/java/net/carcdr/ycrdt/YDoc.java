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
     * Package-private constructor for wrapping an existing native pointer.
     * Used when retrieving subdocuments from collections.
     *
     * @param nativePtr the native pointer to wrap
     * @param dummy unused parameter to distinguish from public constructor
     */
    YDoc(long nativePtr, boolean dummy) {
        this.nativePtr = nativePtr;
        if (this.nativePtr == 0) {
            throw new RuntimeException("Invalid native pointer");
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
     * Encodes the current state vector of this document.
     *
     * <p>A state vector is a compact representation of all known blocks inserted and
     * integrated into this document. It serves as a logical timestamp describing which
     * updates this document has observed.</p>
     *
     * <p>State vectors are used to generate differential updates - by sending a state
     * vector to a remote peer, they can determine which changes this document has not
     * yet seen and send only those changes.</p>
     *
     * @return a byte array containing the encoded state vector
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if encoding fails
     */
    public byte[] encodeStateVector() {
        ensureNotClosed();
        byte[] result = nativeEncodeStateVector(nativePtr);
        if (result == null) {
            throw new RuntimeException("Failed to encode state vector");
        }
        return result;
    }

    /**
     * Encodes a differential update containing only changes not yet observed by the
     * remote peer.
     *
     * <p>This method generates an update that includes only the changes that are present
     * in this document but not reflected in the provided state vector. This is more
     * efficient than sending the entire document state when synchronizing with a peer
     * that already has some of the data.</p>
     *
     * <p>Typical usage:</p>
     * <pre>{@code
     * // Remote peer sends their state vector
     * byte[] remoteStateVector = ...;
     *
     * // Generate differential update
     * byte[] diff = doc.encodeDiff(remoteStateVector);
     *
     * // Send diff to remote peer
     * // Remote peer applies it with applyUpdate(diff)
     * }</pre>
     *
     * @param stateVector the state vector from the remote peer
     * @return a byte array containing the differential update
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if stateVector is null
     * @throws RuntimeException if encoding fails
     */
    public byte[] encodeDiff(byte[] stateVector) {
        ensureNotClosed();
        if (stateVector == null) {
            throw new IllegalArgumentException("State vector cannot be null");
        }
        byte[] result = nativeEncodeDiff(nativePtr, stateVector);
        if (result == null) {
            throw new RuntimeException("Failed to encode differential update");
        }
        return result;
    }

    /**
     * Merges multiple updates into a single compact update.
     *
     * <p>This static method takes an array of updates and combines them into a single
     * update that has the same effect as applying all the individual updates in sequence.
     * This is useful for reducing network overhead and storage requirements.</p>
     *
     * <p>The merged update is often smaller than the sum of the individual updates
     * because redundant operations are eliminated during the merge process.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * byte[] update1 = doc1.encodeStateAsUpdate();
     * byte[] update2 = doc2.encodeStateAsUpdate();
     * byte[] update3 = doc3.encodeStateAsUpdate();
     *
     * // Merge into single update
     * byte[] merged = YDoc.mergeUpdates(new byte[][]{update1, update2, update3});
     *
     * // Apply merged update to target document
     * targetDoc.applyUpdate(merged);
     * }</pre>
     *
     * @param updates array of updates to merge
     * @return a byte array containing the merged update
     * @throws IllegalArgumentException if updates is null or empty, or contains null elements
     * @throws RuntimeException if merging fails
     */
    public static byte[] mergeUpdates(byte[][] updates) {
        if (updates == null || updates.length == 0) {
            throw new IllegalArgumentException("Updates array cannot be null or empty");
        }
        for (int i = 0; i < updates.length; i++) {
            if (updates[i] == null) {
                throw new IllegalArgumentException("Update at index " + i + " cannot be null");
            }
        }
        byte[] result = nativeMergeUpdates(updates);
        if (result == null) {
            throw new RuntimeException("Failed to merge updates");
        }
        return result;
    }

    /**
     * Extracts the state vector from an encoded update without applying it.
     *
     * <p>This method decodes an update and returns its state vector, which represents
     * the logical timestamp of the document state after applying the update. This is
     * useful for understanding what changes an update contains without actually applying
     * it to a document.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * byte[] update = remoteDoc.encodeStateAsUpdate();
     *
     * // Check what state this update represents
     * byte[] stateVector = YDoc.encodeStateVectorFromUpdate(update);
     *
     * // Can be used to determine if we need this update
     * }</pre>
     *
     * @param update the update to extract the state vector from
     * @return a byte array containing the encoded state vector
     * @throws IllegalArgumentException if update is null
     * @throws RuntimeException if extraction fails or update is invalid
     */
    public static byte[] encodeStateVectorFromUpdate(byte[] update) {
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }
        byte[] result = nativeEncodeStateVectorFromUpdate(update);
        if (result == null) {
            throw new RuntimeException("Failed to extract state vector from update");
        }
        return result;
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
     * Gets or creates a YXmlText instance with the specified name.
     *
     * <p>This method returns a collaborative XML text type that can be shared between
     * multiple clients. If an XML text with this name already exists in the document,
     * it will be returned; otherwise, a new one will be created.</p>
     *
     * <p>The returned YXmlText instance must be closed when no longer needed to free
     * native resources. Use try-with-resources for automatic cleanup.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YXmlText xmlText = doc.getXmlText("myxmltext")) {
     *     xmlText.push("Hello");
     *     System.out.println(xmlText.toString());
     * }
     * }</pre>
     *
     * @param name the name of the XML text object
     * @return a YXmlText instance
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if name is null
     * @throws RuntimeException if XML text creation fails
     */
    public YXmlText getXmlText(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new YXmlText(this, name);
    }

    /**
     * Gets or creates a YXmlElement instance with the specified name.
     *
     * <p>This method returns a collaborative XML element type that can be shared between
     * multiple clients. If an XML element with this name already exists in the document,
     * it will be returned; otherwise, a new one will be created.</p>
     *
     * <p>The returned YXmlElement instance must be closed when no longer needed to free
     * native resources. Use try-with-resources for automatic cleanup.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YXmlElement element = doc.getXmlElement("div")) {
     *     element.setAttribute("class", "container");
     *     System.out.println(element.getTag());
     * }
     * }</pre>
     *
     * @param name the name of the XML element object
     * @return a YXmlElement instance
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if name is null
     * @throws RuntimeException if XML element creation fails
     */
    public YXmlElement getXmlElement(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new YXmlElement(this, name);
    }

    /**
     * Gets or creates a YXmlFragment instance with the specified name.
     *
     * <p>This method returns a collaborative XML fragment that can contain multiple
     * XML nodes (elements and text) in a hierarchical structure. Fragments are the
     * foundation for building XML trees and support full parent-child relationships.</p>
     *
     * <p>The returned YXmlFragment instance must be closed when no longer needed to free
     * native resources. Use try-with-resources for automatic cleanup.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YXmlFragment fragment = doc.getXmlFragment("document")) {
     *     fragment.insertElement(0, "div");
     *     fragment.insertText(1, "Hello World");
     *     System.out.println(fragment.toXmlString());
     * }
     * }</pre>
     *
     * @param name the name of the XML fragment
     * @return a YXmlFragment instance
     * @throws IllegalStateException if this document has been closed
     * @throws IllegalArgumentException if name is null
     * @throws RuntimeException if XML fragment creation fails
     * @since 0.2.0
     */
    public YXmlFragment getXmlFragment(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new YXmlFragment(this, name);
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
     * Gets the native handle for internal use.
     * Alias for getNativePtr() for consistency.
     *
     * @return the native pointer value
     */
    long getNativeHandle() {
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

    private static native byte[] nativeEncodeStateVector(long ptr);

    private static native byte[] nativeEncodeDiff(long ptr, byte[] stateVector);

    private static native byte[] nativeMergeUpdates(byte[][] updates);

    private static native byte[] nativeEncodeStateVectorFromUpdate(byte[] update);
}
