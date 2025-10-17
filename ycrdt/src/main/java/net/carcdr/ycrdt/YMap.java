package net.carcdr.ycrdt;

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
public class YMap implements Closeable, YObservable {

    private final YDoc doc;
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
        nativeSetString(doc.getNativePtr(), nativePtr, 0, key, value);
    }

    /**
     * Sets a string value in the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
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
        nativeSetString(doc.getNativePtr(), nativePtr, txn.getNativePtr(), key, value);
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
        nativeSetDouble(doc.getNativePtr(), nativePtr, 0, key, value);
    }

    /**
     * Sets a double value in the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
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
        nativeSetDouble(doc.getNativePtr(), nativePtr, txn.getNativePtr(), key, value);
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
        nativeRemove(doc.getNativePtr(), nativePtr, 0, key);
    }

    /**
     * Removes a key from the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
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
        nativeRemove(doc.getNativePtr(), nativePtr, txn.getNativePtr(), key);
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
        nativeClear(doc.getNativePtr(), nativePtr, 0);
    }

    /**
     * Removes all entries from the map within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
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
        nativeClear(doc.getNativePtr(), nativePtr, txn.getNativePtr());
    }

    /**
     * Sets a YDoc subdocument value in the map.
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YDoc child = new YDoc();
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
        nativeSetDoc(doc.getNativePtr(), nativePtr, 0, key, subdoc.getNativePtr());
    }

    /**
     * Sets a YDoc subdocument value in the map within an existing transaction.
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YDoc child = new YDoc();
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
        nativeSetDoc(doc.getNativePtr(), nativePtr, txn.getNativePtr(), key, subdoc.getNativePtr());
    }

    /**
     * Gets a YDoc subdocument from the map by key.
     *
     * <p>The returned YDoc must be closed by the caller when no longer needed.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YMap map = parent.getMap("mymap")) {
     *     map.setDoc("nested", new YDoc());
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
    public YDoc getDoc(String key) {
        checkClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        long subdocPtr = nativeGetDoc(doc.getNativePtr(), nativePtr, key);
        if (subdocPtr == 0) {
            return null;
        }
        return new YDoc(subdocPtr, true);
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
     * Registers an observer to be notified when this map changes.
     *
     * <p>The observer will be called whenever entries are added, removed, or modified
     * in this map. The observer receives a {@link YEvent} containing details about
     * the changes.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
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
     * <p>This method is called from native code when map changes occur.
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
    private static native long nativeSize(long docPtr, long mapPtr);
    private static native String nativeGetString(long docPtr, long mapPtr, String key);
    private static native double nativeGetDouble(long docPtr, long mapPtr, String key);
    private static native void nativeSetString(long docPtr, long mapPtr, long txnPtr,
                                               String key, String value);
    private static native void nativeSetDouble(long docPtr, long mapPtr, long txnPtr,
                                               String key, double value);
    private static native void nativeRemove(long docPtr, long mapPtr, long txnPtr, String key);
    private static native boolean nativeContainsKey(long docPtr, long mapPtr, String key);
    private static native Object nativeKeys(long docPtr, long mapPtr);
    private static native void nativeClear(long docPtr, long mapPtr, long txnPtr);
    private static native String nativeToJson(long docPtr, long mapPtr);
    private static native void nativeSetDoc(long docPtr, long mapPtr, long txnPtr,
                                            String key, long subdocPtr);
    private static native long nativeGetDoc(long docPtr, long mapPtr, String key);
    private static native void nativeObserve(long docPtr, long mapPtr, long subscriptionId,
                                              YMap ymapObj);
    private static native void nativeUnobserve(long docPtr, long mapPtr, long subscriptionId);
}
