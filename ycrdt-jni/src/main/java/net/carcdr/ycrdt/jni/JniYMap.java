package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YMap;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
 * try (YDoc doc = new JniYDoc();
 *      YMap map = doc.getMap("mymap")) {
 *     map.setString("name", "Alice");
 *     map.setDouble("age", 30.0);
 *     System.out.println(map.toJson()); // {"name":"Alice","age":30.0}
 * }
 * }</pre>
 *
 * @see net.carcdr.ycrdt.YDoc
 */
public class JniYMap implements YMap, JniYObservable {

    private final JniYDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;
    private final ConcurrentHashMap<Long, YObserver> observers = new ConcurrentHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong(0);

    /**
     * Package-private constructor. Use {@link YDoc#getMap(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this map object in the document
     */
    JniYMap(JniYDoc doc, String name) {
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return (int) nativeSizeWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr());
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return (int) nativeSizeWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        }
    }

    /**
     * Returns the number of entries in the map using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @return The size of the map
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the map has been closed
     */
    public int size(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return (int) nativeSizeWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeGetStringWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr(), key);
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeGetStringWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
        }
    }

    /**
     * Gets a string value by key using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @param key The key to look up
     * @return The string value, or null if key not found or value is not a string
     * @throws IllegalArgumentException if txn or key is null
     * @throws IllegalStateException if the map has been closed
     */
    public String getString(YTransaction txn, String key) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return nativeGetStringWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeGetDoubleWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr(), key);
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeGetDoubleWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
        }
    }

    /**
     * Gets a double value by key using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @param key The key to look up
     * @return The double value, or 0.0 if key not found or value is not a number
     * @throws IllegalArgumentException if txn or key is null
     * @throws IllegalStateException if the map has been closed
     */
    public double getDouble(YTransaction txn, String key) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return nativeGetDoubleWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeSetStringWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(),
                key, value);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeSetStringWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(),
                    key, value);
            }
        }
    }

    /**
     * Sets a string value in the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     map.setString(txn, "name", "Alice");
     *     map.setString(txn, "city", "NYC");
     * }
     * }</pre>
     *
     * @param txn The transaction to use
     * @param key The key to set
     * @param value The string value to set
     * @throws IllegalArgumentException if txn, key, or value is null
     * @throws IllegalStateException if the map or transaction has been closed
     */
    public void setString(YTransaction txn, String key, String value) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        nativeSetStringWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key, value);
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeSetDoubleWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(),
                key, value);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeSetDoubleWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(),
                    key, value);
            }
        }
    }

    /**
     * Sets a double value in the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     map.setDouble(txn, "age", 30.0);
     *     map.setDouble(txn, "height", 165.5);
     * }
     * }</pre>
     *
     * @param txn The transaction to use
     * @param key The key to set
     * @param value The double value to set
     * @throws IllegalArgumentException if txn or key is null
     * @throws IllegalStateException if the map or transaction has been closed
     */
    public void setDouble(YTransaction txn, String key, double value) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        nativeSetDoubleWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key, value);
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeRemoveWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(), key);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeRemoveWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
            }
        }
    }

    /**
     * Removes a key from the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     map.remove(txn, "key1");
     *     map.remove(txn, "key2");
     * }
     * }</pre>
     *
     * @param txn The transaction to use
     * @param key The key to remove
     * @throws IllegalArgumentException if txn or key is null
     * @throws IllegalStateException if the map or transaction has been closed
     */
    public void remove(YTransaction txn, String key) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        nativeRemoveWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeContainsKeyWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr(), key);
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeContainsKeyWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
        }
    }

    /**
     * Checks if a key exists in the map using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @param key The key to check
     * @return true if the key exists, false otherwise
     * @throws IllegalArgumentException if txn or key is null
     * @throws IllegalStateException if the map has been closed
     */
    public boolean containsKey(YTransaction txn, String key) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return nativeContainsKeyWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key);
    }

    /**
     * Gets all keys from the map.
     *
     * @return An array of all keys in the map
     * @throws IllegalStateException if the map has been closed
     */
    public String[] keys() {
        checkClosed();
        JniYTransaction activeTxn = doc.getActiveTransaction();
        Object result;
        if (activeTxn != null) {
            result = nativeKeysWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr());
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                result = nativeKeysWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
            }
        }
        if (result == null) {
            return new String[0];
        }
        return (String[]) result;
    }

    /**
     * Gets all keys from the map using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @return An array of all keys in the map
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the map has been closed
     */
    public String[] keys(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        Object result = nativeKeysWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeClearWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr());
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeClearWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
            }
        }
    }

    /**
     * Removes all entries from the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     map.clear(txn);
     *     map.setString(txn, "reset", "true");
     * }
     * }</pre>
     *
     * @param txn The transaction to use
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the map or transaction has been closed
     */
    public void clear(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        nativeClearWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
    }

    /**
     * Sets a YDoc subdocument value in the map.
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new JniYDoc();
     *      YDoc child = new JniYDoc();
     *      YMap map = parent.getMap("mymap")) {
     *     map.setDoc("nested", child);
     * }
     * }</pre>
     *
     * @param key The key to set
     * @param subdoc The YDoc subdocument to set
     * @throws IllegalArgumentException if key or subdoc is null
     * @throws IllegalStateException if the map has been closed
     */
    public void setDoc(String key, YDoc subdoc) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (subdoc == null) {
            throw new IllegalArgumentException("Subdocument cannot be null");
        }
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeSetDocWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(),
                key, ((JniYDoc) subdoc).getNativePtr());
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeSetDocWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(),
                    key, ((JniYDoc) subdoc).getNativePtr());
            }
        }
    }

    /**
     * Sets a YDoc subdocument value in the map within an existing transaction.
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YDoc parent = new JniYDoc();
     *      YDoc child = new JniYDoc();
     *      YMap map = parent.getMap("mymap");
     *      YTransaction txn = parent.beginTransaction()) {
     *     map.setDoc(txn, "nested", child);
     *     map.setString(txn, "type", "subdocument");
     * }
     * }</pre>
     *
     * @param txn The transaction to use
     * @param key The key to set
     * @param subdoc The YDoc subdocument to set
     * @throws IllegalArgumentException if txn, key, or subdoc is null
     * @throws IllegalStateException if the map or transaction has been closed
     */
    public void setDoc(YTransaction txn, String key, YDoc subdoc) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (subdoc == null) {
            throw new IllegalArgumentException("Subdocument cannot be null");
        }
        nativeSetDocWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), key,
            ((JniYDoc) subdoc).getNativePtr());
    }

    /**
     * Gets a YDoc subdocument from the map by key.
     *
     * <p>The returned YDoc must be closed by the caller when no longer needed.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new JniYDoc();
     *      YMap map = parent.getMap("mymap")) {
     *     map.setDoc("nested", new JniYDoc());
     *     try (YDoc retrieved = map.getDoc("nested")) {
     *         // Use the subdocument
     *     }
     * }
     * }</pre>
     *
     * @param key The key to look up
     * @return The YDoc subdocument, or null if key not found or value is not a Doc
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException if the map has been closed
     */
    public JniYDoc getDoc(String key) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        JniYTransaction activeTxn = doc.getActiveTransaction();
        long subdocPtr;
        if (activeTxn != null) {
            subdocPtr = nativeGetDocWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr(), key);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                subdocPtr = nativeGetDocWithTxn(doc.getNativePtr(), nativePtr,
                    ((JniYTransaction) txn).getNativePtr(), key);
            }
        }
        if (subdocPtr == 0) {
            return null;
        }
        return new JniYDoc(subdocPtr, true);
    }

    /**
     * Gets a YDoc subdocument from the map by key using an existing transaction.
     *
     * <p>The returned YDoc must be closed by the caller when no longer needed.</p>
     *
     * @param txn The transaction to use for this operation
     * @param key The key to look up
     * @return The YDoc subdocument, or null if key not found or value is not a Doc
     * @throws IllegalArgumentException if txn or key is null
     * @throws IllegalStateException if the map has been closed
     */
    public JniYDoc getDoc(YTransaction txn, String key) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        long subdocPtr = nativeGetDocWithTxn(doc.getNativePtr(), nativePtr,
            ((JniYTransaction) txn).getNativePtr(), key);
        if (subdocPtr == 0) {
            return null;
        }
        return new JniYDoc(subdocPtr, true);
    }

    /**
     * Returns a JSON string representation of the map.
     *
     * @return A JSON string representation
     * @throws IllegalStateException if the map has been closed
     */
    public String toJson() {
        checkClosed();
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeToJsonWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr());
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeToJsonWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        }
    }

    /**
     * Returns a JSON string representation of the map using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @return A JSON string representation
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the map has been closed
     */
    public String toJson(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return nativeToJsonWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
    }

    /**
     * Registers an observer to be notified when this map changes.
     *
     * <p>The observer will be called whenever entries are added, removed, or modified
     * in this map. The observer receives a {@link net.carcdr.ycrdt.YEvent} containing details about
     * the changes.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new JniYDoc();
     *      YMap map = doc.getMap("mymap");
     *      YSubscription sub = map.observe(event -> {
     *          System.out.println("Map changed!");
     *          for (YChange change : event.getChanges()) {
     *              // Handle change
     *          }
     *      })) {
     *     map.setString("key", "value"); // Observer is called
     * }
     * }</pre>
     *
     * @param observer The observer to register (must not be null)
     * @return A subscription handle that can be used to unregister the observer
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this map has been closed
     */
    public YSubscription observe(YObserver observer) {
        checkClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        long id = nextSubscriptionId.incrementAndGet();
        observers.put(id, observer);
        nativeObserve(doc.getNativePtr(), nativePtr, id, this);
        return new JniYSubscription(id, observer, this);
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
     * <p>This method is called from native code when map changes occur.
     * It should not be called directly by user code.</p>
     *
     * @param subscriptionId The subscription ID
     * @param event The event to dispatch
     */
    void dispatchEvent(long subscriptionId, JniYEvent event) {
        YObserver observer = observers.get(subscriptionId);
        if (observer != null) {
            try {
                observer.onChange(event);
            } catch (Exception e) {
                doc.getObserverErrorHandler().handleError(e, this);
            }
        }
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
    private static native long nativeSizeWithTxn(long docPtr, long mapPtr, long txnPtr);
    private static native String nativeGetStringWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                         String key);
    private static native double nativeGetDoubleWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                         String key);
    private static native void nativeSetStringWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                       String key, String value);
    private static native void nativeSetDoubleWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                       String key, double value);
    private static native void nativeRemoveWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                    String key);
    private static native boolean nativeContainsKeyWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                            String key);
    private static native Object nativeKeysWithTxn(long docPtr, long mapPtr, long txnPtr);
    private static native void nativeClearWithTxn(long docPtr, long mapPtr, long txnPtr);
    private static native String nativeToJsonWithTxn(long docPtr, long mapPtr, long txnPtr);
    private static native void nativeSetDocWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                    String key, long subdocPtr);
    private static native long nativeGetDocWithTxn(long docPtr, long mapPtr, long txnPtr,
                                                    String key);
    private static native void nativeObserve(long docPtr, long mapPtr, long subscriptionId,
                                              YMap ymapObj);
    private static native void nativeUnobserve(long docPtr, long mapPtr, long subscriptionId);
}
