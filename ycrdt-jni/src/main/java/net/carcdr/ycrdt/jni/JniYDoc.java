package net.carcdr.ycrdt.jni;

import java.lang.ref.Cleaner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import net.carcdr.ycrdt.DefaultObserverErrorHandler;
import net.carcdr.ycrdt.ObserverErrorHandler;
import net.carcdr.ycrdt.UpdateObserver;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;

/**
 * JniYDoc represents a Y-CRDT document, which is a shared data structure that supports
 * concurrent editing and automatic conflict resolution.
 *
 * <p>This class wraps the native Rust implementation of y-crdt (yrs) and provides
 * a Java API for working with collaborative documents.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * try (YDoc doc = new JniYDoc()) {
 *     long clientId = doc.getClientId();
 *     String guid = doc.getGuid();
 *
 *     // Get document state as update
 *     byte[] state = doc.encodeStateAsUpdate();
 *
 *     // Apply update to another document
 *     try (YDoc doc2 = new JniYDoc()) {
 *         doc2.applyUpdate(state);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> JniYDoc instances are not thread-safe. If you need to access
 * a document from multiple threads, you must provide external synchronization.</p>
 *
 * <p><b>Memory Management:</b> JniYDoc implements {@link java.io.Closeable} and must be closed
 * when no longer needed to free native resources. Use try-with-resources to ensure
 * proper cleanup.</p>
 */
public class JniYDoc implements YDoc, JniYObservable {

    static {
        // Load the native library
        NativeLoader.loadLibrary();
    }

    /**
     * Pointer to the native YDoc instance.
     */
    private long nativePtr;

    /**
     * Flag to track if this instance has been closed.
     */
    private volatile boolean closed = false;

    /**
     * Cleaner registration for cleaning up native resources when GC collects this object.
     */
    private final Cleaner.Cleanable cleanable;

    /**
     * Cleanup action that releases native resources.
     * This is a static class to avoid preventing the JniYDoc from being garbage collected.
     */
    private static class CleanupAction implements Runnable {
        private final AtomicLong ptr;

        CleanupAction(long ptr) {
            this.ptr = new AtomicLong(ptr);
        }

        @Override
        public void run() {
            long p = ptr.getAndSet(0);
            if (p != 0) {
                nativeDestroy(p);
            }
        }
    }

    /**
     * Thread-local storage for the currently active transaction.
     * This allows implicit transaction methods to reuse the active transaction
     * if one exists, preventing deadlocks from nested transaction attempts.
     */
    private final ThreadLocal<JniYTransaction> activeTransaction = new ThreadLocal<>();

    /**
     * Map of active update observers by subscription ID.
     */
    private final ConcurrentHashMap<Long, UpdateObserver> updateObservers = new ConcurrentHashMap<>();

    /**
     * Counter for generating unique subscription IDs.
     */
    private final AtomicLong nextSubscriptionId = new AtomicLong(1);

    /**
     * Handler for observer exceptions.
     */
    private ObserverErrorHandler observerErrorHandler = DefaultObserverErrorHandler.INSTANCE;

    /**
     * Queue of native subscription IDs whose Rust-side Subscription objects
     * need to be dropped. The Java observer map is updated immediately so
     * callbacks become no-ops, but the actual native unsubscribe is deferred
     * to the next safe synchronization point (transaction begin, observer
     * registration, or doc close) to avoid racing with yrs EventHandler
     * dispatch on another thread.
     */
    private final ConcurrentLinkedQueue<Long> pendingNativeUnsubscribes =
        new ConcurrentLinkedQueue<>();

    /**
     * Creates a new JniYDoc instance with a randomly generated client ID.
     *
     * @throws RuntimeException if native initialization fails
     */
    public JniYDoc() {
        this.nativePtr = nativeCreate();
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create JniYDoc: native pointer is null");
        }
        this.cleanable = NativeCleaner.CLEANER.register(this, new CleanupAction(nativePtr));
    }

    /**
     * Creates a new JniYDoc instance with a specific client ID.
     *
     * <p>The client ID is used to identify the source of changes in the CRDT.
     * Each client should have a unique ID to prevent conflicts.</p>
     *
     * @param clientId the client ID to assign to this document
     * @throws RuntimeException if native initialization fails
     * @throws IllegalArgumentException if clientId is negative
     */
    public JniYDoc(long clientId) {
        if (clientId < 0) {
            throw new IllegalArgumentException("Client ID must be non-negative");
        }
        this.nativePtr = nativeCreateWithClientId(clientId);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create JniYDoc: native pointer is null");
        }
        this.cleanable = NativeCleaner.CLEANER.register(this, new CleanupAction(nativePtr));
    }

    /**
     * Package-private constructor for wrapping an existing native pointer.
     * Used when retrieving subdocuments from collections.
     *
     * @param nativePtr the native pointer to wrap
     * @param dummy unused parameter to distinguish from public constructor
     */
    JniYDoc(long nativePtr, boolean dummy) {
        this.nativePtr = nativePtr;
        if (this.nativePtr == 0) {
            throw new RuntimeException("Invalid native pointer");
        }
        this.cleanable = NativeCleaner.CLEANER.register(this, new CleanupAction(nativePtr));
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
    @Override
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
    @Override
    public String getGuid() {
        ensureNotClosed();
        return nativeGetGuid(nativePtr);
    }

    /**
     * Encodes the current state of the document as a binary update within an existing transaction.
     *
     * <p>This method captures the entire state of the document and returns it
     * as a byte array that can be transmitted to other clients or stored for
     * later use.</p>
     *
     * <p>The returned byte array can be applied to another YDoc instance using
     * {@link #applyUpdate(byte[])} to synchronize their states.</p>
     *
     * @param txn The transaction to use for this operation
     * @return a byte array containing the encoded document state
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if encoding fails
     */
    @Override
    public byte[] encodeStateAsUpdate(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        byte[] result = nativeEncodeStateAsUpdateWithTxn(nativePtr,
            ((JniYTransaction) txn).getNativePtr());
        if (result == null) {
            throw new RuntimeException("Failed to encode state as update");
        }
        return result;
    }

    /**
     * Encodes the current state of the document as a binary update (creates implicit transaction).
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
    @Override
    public byte[] encodeStateAsUpdate() {
        ensureNotClosed();
        JniYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            byte[] result = nativeEncodeStateAsUpdateWithTxn(nativePtr, activeTxn.getNativePtr());
            if (result == null) {
                throw new RuntimeException("Failed to encode state as update");
            }
            return result;
        }
        try (JniYTransaction txn = beginTransactionInternal()) {
            byte[] result = nativeEncodeStateAsUpdateWithTxn(nativePtr, txn.getNativePtr());
            if (result == null) {
                throw new RuntimeException("Failed to encode state as update");
            }
            return result;
        }
    }

    /**
     * Applies a binary update to this document within an existing transaction.
     *
     * <p>This method merges changes from another document into this one. The update
     * is typically obtained by calling {@link #encodeStateAsUpdate()} on another
     * YDoc instance.</p>
     *
     * <p>Updates are idempotent - applying the same update multiple times has the
     * same effect as applying it once.</p>
     *
     * @param txn The transaction to use for this operation
     * @param update the binary update to apply
     * @throws IllegalArgumentException if txn or update is null
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if the update is invalid or cannot be applied
     */
    @Override
    public void applyUpdate(YTransaction txn, byte[] update) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }
        nativeApplyUpdateWithTxn(nativePtr, ((JniYTransaction) txn).getNativePtr(), update);
    }

    /**
     * Applies a binary update to this document (creates implicit transaction).
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
    @Override
    public void applyUpdate(byte[] update) {
        ensureNotClosed();
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }
        JniYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            nativeApplyUpdateWithTxn(nativePtr, activeTxn.getNativePtr(), update);
        } else {
            try (JniYTransaction txn = beginTransactionInternal()) {
                nativeApplyUpdateWithTxn(nativePtr, txn.getNativePtr(), update);
            }
        }
    }

    /**
     * Encodes the current state vector of this document within an existing transaction.
     *
     * <p>A state vector is a compact representation of all known blocks inserted and
     * integrated into this document. It serves as a logical timestamp describing which
     * updates this document has observed.</p>
     *
     * <p>State vectors are used to generate differential updates - by sending a state
     * vector to a remote peer, they can determine which changes this document has not
     * yet seen and send only those changes.</p>
     *
     * @param txn The transaction to use for this operation
     * @return a byte array containing the encoded state vector
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if encoding fails
     */
    @Override
    public byte[] encodeStateVector(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        byte[] result = nativeEncodeStateVectorWithTxn(nativePtr,
            ((JniYTransaction) txn).getNativePtr());
        if (result == null) {
            throw new RuntimeException("Failed to encode state vector");
        }
        return result;
    }

    /**
     * Encodes the current state vector of this document (creates implicit transaction).
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
    @Override
    public byte[] encodeStateVector() {
        ensureNotClosed();
        JniYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            byte[] result = nativeEncodeStateVectorWithTxn(nativePtr, activeTxn.getNativePtr());
            if (result == null) {
                throw new RuntimeException("Failed to encode state vector");
            }
            return result;
        }
        try (JniYTransaction txn = beginTransactionInternal()) {
            byte[] result = nativeEncodeStateVectorWithTxn(nativePtr, txn.getNativePtr());
            if (result == null) {
                throw new RuntimeException("Failed to encode state vector");
            }
            return result;
        }
    }

    /**
     * Encodes a differential update containing only changes not yet observed by the
     * remote peer within an existing transaction.
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
     * try (YTransaction txn = doc.beginTransaction()) {
     *     // Generate differential update
     *     byte[] diff = doc.encodeDiff(txn, remoteStateVector);
     *     // Send diff to remote peer
     *     // Remote peer applies it with applyUpdate(diff)
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param stateVector the state vector from the remote peer
     * @return a byte array containing the differential update
     * @throws IllegalArgumentException if txn or stateVector is null
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if encoding fails
     */
    @Override
    public byte[] encodeDiff(YTransaction txn, byte[] stateVector) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (stateVector == null) {
            throw new IllegalArgumentException("State vector cannot be null");
        }
        byte[] result = nativeEncodeDiffWithTxn(nativePtr,
            ((JniYTransaction) txn).getNativePtr(), stateVector);
        if (result == null) {
            throw new RuntimeException("Failed to encode differential update");
        }
        return result;
    }

    /**
     * Encodes a differential update containing only changes not yet observed by the
     * remote peer (creates implicit transaction).
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
    @Override
    public byte[] encodeDiff(byte[] stateVector) {
        ensureNotClosed();
        if (stateVector == null) {
            throw new IllegalArgumentException("State vector cannot be null");
        }
        JniYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            byte[] result = nativeEncodeDiffWithTxn(nativePtr, activeTxn.getNativePtr(), stateVector);
            if (result == null) {
                throw new RuntimeException("Failed to encode differential update");
            }
            return result;
        }
        try (JniYTransaction txn = beginTransactionInternal()) {
            byte[] result = nativeEncodeDiffWithTxn(nativePtr, txn.getNativePtr(), stateVector);
            if (result == null) {
                throw new RuntimeException("Failed to encode differential update");
            }
            return result;
        }
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
     * byte[] merged = JniYDoc.mergeUpdates(new byte[][]{update1, update2, update3});
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
     * byte[] stateVector = JniYDoc.encodeStateVectorFromUpdate(update);
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
     * try (YDoc doc = new JniYDoc();
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
    @Override
    public JniYText getText(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new JniYText(this, name);
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
     * try (YDoc doc = new JniYDoc();
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
    @Override
    public JniYArray getArray(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new JniYArray(this, name);
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
     * try (YDoc doc = new JniYDoc();
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
    @Override
    public JniYMap getMap(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new JniYMap(this, name);
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
     * try (YDoc doc = new JniYDoc();
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
    @Override
    public JniYXmlText getXmlText(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new JniYXmlText(this, name);
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
     * try (YDoc doc = new JniYDoc();
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
    @Override
    public JniYXmlElement getXmlElement(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new JniYXmlElement(this, name);
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
     * try (YDoc doc = new JniYDoc();
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
    @Override
    public JniYXmlFragment getXmlFragment(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new JniYXmlFragment(this, name);
    }

    /**
     * Enqueues a native subscription ID for deferred unsubscription.
     * The Java-side observer map should already be updated so callbacks
     * become no-ops. The actual native Subscription drop happens at the
     * next safe point.
     *
     * @param subscriptionId the native subscription ID to drop later
     */
    void deferNativeUnsubscribe(long subscriptionId) {
        pendingNativeUnsubscribes.add(subscriptionId);
    }

    /**
     * Drains the pending native unsubscribe queue, calling
     * nativeUnobserveUpdateV1 for each queued ID. Must be called
     * at a point where no concurrent yrs EventHandler dispatch is
     * occurring (e.g. before beginning a transaction, before registering
     * an observer, or during close).
     */
    private void drainPendingUnsubscribes() {
        Long id;
        while ((id = pendingNativeUnsubscribes.poll()) != null) {
            if (!closed && nativePtr != 0) {
                nativeUnobserveUpdateV1(nativePtr, id);
            }
        }
    }

    /**
     * Begin a new transaction for batching operations.
     *
     * <p>Transactions allow multiple CRDT operations to be batched together,
     * resulting in better performance, single observer notifications, and more
     * efficient update encoding for synchronization.</p>
     *
     * <p>Use with try-with-resources for automatic cleanup:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     text.insert(txn, 0, "Hello");
     *     array.pushString(txn, "World");
     * } // Auto-commits
     * }</pre>
     *
     * <p>The transaction must be closed (either explicitly or via try-with-resources)
     * to commit the changes. Uncommitted transactions will be automatically committed
     * when the transaction object is garbage collected, but this is not recommended.</p>
     *
     * @return transaction handle (use with try-with-resources)
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if transaction creation fails
     * @see JniYTransaction
     * @see #transaction(Consumer)
     */
    @Override
    public JniYTransaction beginTransaction() {
        return beginTransactionInternal();
    }

    /**
     * Internal method to begin a transaction, returning concrete type.
     */
    private JniYTransaction beginTransactionInternal() {
        ensureNotClosed();
        drainPendingUnsubscribes();
        long txnPtr = nativeBeginTransaction(nativePtr);
        if (txnPtr == 0) {
            throw new RuntimeException("Failed to create transaction: native pointer is null");
        }
        JniYTransaction txn = new JniYTransaction(this, txnPtr);
        activeTransaction.set(txn);
        return txn;
    }

    /**
     * Gets the currently active transaction for this thread, or null if none.
     * Package-private for internal use.
     *
     * @return the active transaction, or null
     */
    JniYTransaction getActiveTransaction() {
        return activeTransaction.get();
    }

    /**
     * Clears the currently active transaction for this thread.
     * Package-private for internal use by JniYTransaction.
     */
    void clearActiveTransaction() {
        activeTransaction.remove();
    }

    /**
     * Execute operations within a transaction using a callback.
     *
     * <p>This is a convenience method that automatically manages transaction lifecycle.
     * The transaction is automatically committed when the callback completes successfully,
     * or rolled back if an exception is thrown.</p>
     *
     * <p>Convenience method for simple transaction usage:
     * <pre>{@code
     * doc.transaction(txn -> {
     *     text.insert(txn, 0, "Hello");
     *     array.pushString(txn, "World");
     * });
     * }</pre>
     *
     * @param fn function receiving transaction handle
     * @throws IllegalStateException if this document has been closed
     * @throws RuntimeException if transaction creation fails or fn throws an exception
     * @see JniYTransaction
     * @see #beginTransaction()
     */
    @Override
    public void transaction(Consumer<YTransaction> fn) {
        if (fn == null) {
            throw new IllegalArgumentException("Transaction function cannot be null");
        }
        try (JniYTransaction txn = beginTransactionInternal()) {
            fn.accept(txn);
        }
    }

    /**
     * Observes all updates to this document.
     *
     * <p>The observer will be called whenever any Y type within this document
     * is modified, providing the binary-encoded update. This is useful for:
     * <ul>
     *   <li>Persisting document changes</li>
     *   <li>Broadcasting updates to remote peers</li>
     *   <li>Logging or auditing changes</li>
     *   <li>Triggering side effects on document changes</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * try (YDoc doc = new JniYDoc()) {
     *     UpdateObserver observer = (update, origin) -> {
     *         System.out.println("Document updated: " + update.length + " bytes");
     *         // Persist to database, broadcast to peers, etc.
     *     };
     *
     *     try (YSubscription sub = doc.observeUpdateV1(observer)) {
     *         try (YText text = doc.getText("mytext")) {
     *             text.insert(0, "Hello"); // Triggers observer
     *         }
     *     }
     * }
     * }</pre>
     *
     * <p><b>Important:</b> The observer is called synchronously on the thread
     * that modifies the document. Observers should perform minimal work to avoid
     * blocking document operations. For expensive operations, schedule them
     * asynchronously.</p>
     *
     * <p><b>Reentrancy Warning:</b> Observers should NOT modify the same document
     * that triggered the callback, as this may cause undefined behavior or deadlocks.</p>
     *
     * @param observer the observer to register
     * @return a subscription that can be closed to unregister the observer
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this document has been closed
     * @see UpdateObserver
     */
    @Override
    public YSubscription observeUpdateV1(UpdateObserver observer) {
        ensureNotClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }

        long subscriptionId = nextSubscriptionId.getAndIncrement();
        updateObservers.put(subscriptionId, observer);

        // Drain any pending unsubscribes before registering with native layer
        drainPendingUnsubscribes();
        nativeObserveUpdateV1(nativePtr, subscriptionId, this);

        return new JniYSubscription(subscriptionId, null, this);
    }

    /**
     * Unregisters an update observer by subscription ID.
     *
     * <p>This is called automatically when a YSubscription is closed.
     * You typically don't need to call this directly.</p>
     *
     * @param subscriptionId the subscription ID to remove
     */
    @Override
    public void unobserveById(long subscriptionId) {
        if (updateObservers.remove(subscriptionId) != null) {
            if (!closed && nativePtr != 0) {
                deferNativeUnsubscribe(subscriptionId);
            }
        }
    }

    /**
     * Sets the error handler for observer exceptions.
     *
     * <p>When an observer throws an exception, this handler will be called
     * instead of letting the exception propagate. The default handler prints
     * to stderr for backwards compatibility.</p>
     *
     * @param handler the error handler to use, or null to use the default handler
     * @see ObserverErrorHandler
     * @see DefaultObserverErrorHandler
     */
    @Override
    public void setObserverErrorHandler(ObserverErrorHandler handler) {
        if (handler == null) {
            this.observerErrorHandler = DefaultObserverErrorHandler.INSTANCE;
        } else {
            this.observerErrorHandler = handler;
        }
    }

    /**
     * Gets the current error handler for observer exceptions.
     *
     * @return the current error handler (never null)
     */
    @Override
    public ObserverErrorHandler getObserverErrorHandler() {
        return observerErrorHandler;
    }

    /**
     * Called from native code when an update occurs.
     *
     * <p>This method is invoked by the native layer and dispatches the update
     * to all registered observers.</p>
     *
     * @param subscriptionId the subscription ID (currently unused, may be used for filtering)
     * @param update the binary-encoded update
     * @param origin optional origin string, may be null
     */
    @SuppressWarnings("unused") // Called from native code
    private void onUpdateCallback(long subscriptionId, byte[] update, String origin) {
        // Call all registered observers
        for (UpdateObserver observer : updateObservers.values()) {
            try {
                observer.onUpdate(update, origin);
            } catch (Exception e) {
                // Use configured error handler - observers should not break each other
                observerErrorHandler.handleError(e, this);
            }
        }
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
        drainPendingUnsubscribes();
        cleanable.clean();
        closed = true;
    }

    /**
     * Checks if this document has been closed.
     *
     * @return true if this document has been closed, false otherwise
     */
    @Override
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
            throw new IllegalStateException("JniYDoc has been closed");
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

    // Native method declarations

    private static native long nativeCreate();

    private static native long nativeCreateWithClientId(long clientId);

    private static native void nativeDestroy(long ptr);

    private static native long nativeGetClientId(long ptr);

    private static native String nativeGetGuid(long ptr);

    private static native byte[] nativeEncodeStateAsUpdateWithTxn(long ptr, long txnPtr);

    private static native void nativeApplyUpdateWithTxn(long ptr, long txnPtr, byte[] update);

    private static native byte[] nativeEncodeStateVectorWithTxn(long ptr, long txnPtr);

    private static native byte[] nativeEncodeDiffWithTxn(long ptr, long txnPtr, byte[] stateVector);

    private static native byte[] nativeMergeUpdates(byte[][] updates);

    private static native byte[] nativeEncodeStateVectorFromUpdate(byte[] update);

    private static native long nativeBeginTransaction(long ptr);

    private static native void nativeObserveUpdateV1(long ptr, long subscriptionId, JniYDoc ydocObj);

    private static native void nativeUnobserveUpdateV1(long ptr, long subscriptionId);
}
