package net.carcdr.ycrdt;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YArray represents a collaborative array type in a Y-CRDT document.
 *
 * <p>YArray provides efficient collaborative array operations with automatic conflict resolution.
 * Multiple users can modify the same array simultaneously, and changes will be merged
 * automatically using CRDT algorithms.</p>
 *
 * <p>This class implements {@link Closeable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc();
 *      YArray array = doc.getArray("myarray")) {
 *     array.pushString("Hello");
 *     array.pushDouble(42.0);
 *     array.insertString(1, "World");
 *     System.out.println(array.toJson()); // ["Hello","World",42.0]
 * }
 * }</pre>
 *
 * @see YDoc
 */
public class YArray implements Closeable, YObservable {

    private final YDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;
    private final ConcurrentHashMap<Long, YObserver> observers = new ConcurrentHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong(0);

    /**
     * Package-private constructor. Use {@link YDoc#getArray(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this array object in the document
     */
    YArray(YDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativePtr = nativeGetArray(doc.getNativePtr(), name);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YArray");
        }
    }

    /**
     * Returns the length of the array.
     *
     * @return The number of elements in the array
     * @throws IllegalStateException if the array has been closed
     */
    public int length() {
        checkClosed();
        return nativeLength(doc.getNativePtr(), nativePtr);
    }

    /**
     * Gets a string value at the specified index.
     *
     * @param index The index (0-based)
     * @return The string value, or null if index is out of bounds or value is not a string
     * @throws IllegalStateException if the array has been closed
     */
    public String getString(int index) {
        checkClosed();
        if (index < 0) {
            return null;
        }
        return nativeGetString(doc.getNativePtr(), nativePtr, index);
    }

    /**
     * Gets a double value at the specified index.
     *
     * @param index The index (0-based)
     * @return The double value, or 0.0 if index is out of bounds or value is not a number
     * @throws IllegalStateException if the array has been closed
     */
    public double getDouble(int index) {
        checkClosed();
        if (index < 0) {
            return 0.0;
        }
        return nativeGetDouble(doc.getNativePtr(), nativePtr, index);
    }

    /**
     * Inserts a string value at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     array.insertString(txn, 0, "First");
     *     array.insertString(txn, 1, "Second");
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param index The position at which to insert (0-based)
     * @param value The string value to insert
     * @throws IllegalArgumentException if txn or value is null
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insertString(YTransaction txn, int index, String value) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsertStringWithTxn(doc.getNativePtr(), nativePtr, txn.getNativePtr(), index, value);
    }

    /**
     * Inserts a string value at the specified index (creates implicit transaction).
     *
     * @param index The position at which to insert (0-based)
     * @param value The string value to insert
     * @throws IllegalArgumentException if value is null
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insertString(int index, String value) {
        checkClosed();
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsertString(doc.getNativePtr(), nativePtr, index, value);
    }

    /**
     * Inserts a double value at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     array.insertDouble(txn, 0, 1.0);
     *     array.insertDouble(txn, 1, 2.0);
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param index The position at which to insert (0-based)
     * @param value The double value to insert
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insertDouble(YTransaction txn, int index, double value) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsertDoubleWithTxn(doc.getNativePtr(), nativePtr, txn.getNativePtr(), index, value);
    }

    /**
     * Inserts a double value at the specified index (creates implicit transaction).
     *
     * @param index The position at which to insert (0-based)
     * @param value The double value to insert
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insertDouble(int index, double value) {
        checkClosed();
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsertDouble(doc.getNativePtr(), nativePtr, index, value);
    }

    /**
     * Appends a string value to the end of the array within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     array.pushString(txn, "First");
     *     array.pushString(txn, "Second");
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param value The string value to append
     * @throws IllegalArgumentException if txn or value is null
     * @throws IllegalStateException if the array has been closed
     */
    public void pushString(YTransaction txn, String value) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        nativePushStringWithTxn(doc.getNativePtr(), nativePtr, txn.getNativePtr(), value);
    }

    /**
     * Appends a string value to the end of the array (creates implicit transaction).
     *
     * @param value The string value to append
     * @throws IllegalArgumentException if value is null
     * @throws IllegalStateException if the array has been closed
     */
    public void pushString(String value) {
        checkClosed();
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        nativePushString(doc.getNativePtr(), nativePtr, value);
    }

    /**
     * Appends a double value to the end of the array within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     array.pushDouble(txn, 1.0);
     *     array.pushDouble(txn, 2.0);
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param value The double value to append
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the array has been closed
     */
    public void pushDouble(YTransaction txn, double value) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        nativePushDoubleWithTxn(doc.getNativePtr(), nativePtr, txn.getNativePtr(), value);
    }

    /**
     * Appends a double value to the end of the array (creates implicit transaction).
     *
     * @param value The double value to append
     * @throws IllegalStateException if the array has been closed
     */
    public void pushDouble(double value) {
        checkClosed();
        nativePushDouble(doc.getNativePtr(), nativePtr, value);
    }

    /**
     * Removes a range of elements from the array within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (YTransaction txn = doc.beginTransaction()) {
     *     array.pushString(txn, "A");
     *     array.pushString(txn, "B");
     *     array.remove(txn, 0, 1); // Remove "A"
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param index The starting position (0-based)
     * @param length The number of elements to remove
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void remove(YTransaction txn, int index, int length) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
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
        nativeRemoveWithTxn(doc.getNativePtr(), nativePtr, txn.getNativePtr(), index, length);
    }

    /**
     * Removes a range of elements from the array (creates implicit transaction).
     *
     * @param index The starting position (0-based)
     * @param length The number of elements to remove
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void remove(int index, int length) {
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
        nativeRemove(doc.getNativePtr(), nativePtr, index, length);
    }

    /**
     * Inserts a YDoc subdocument at the specified index within an existing transaction.
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YDoc child = new YDoc();
     *      YArray array = parent.getArray("myarray");
     *      YTransaction txn = parent.beginTransaction()) {
     *     array.insertDoc(txn, 0, child);
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param index The position at which to insert (0-based)
     * @param subdoc The YDoc subdocument to insert
     * @throws IllegalArgumentException if txn or subdoc is null
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insertDoc(YTransaction txn, int index, YDoc subdoc) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (subdoc == null) {
            throw new IllegalArgumentException("Subdocument cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsertDocWithTxn(doc.getNativePtr(), nativePtr, txn.getNativePtr(), index,
            subdoc.getNativePtr());
    }

    /**
     * Inserts a YDoc subdocument at the specified index (creates implicit transaction).
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YDoc child = new YDoc();
     *      YArray array = parent.getArray("myarray")) {
     *     array.insertDoc(0, child);
     * }
     * }</pre>
     *
     * @param index The position at which to insert (0-based)
     * @param subdoc The YDoc subdocument to insert
     * @throws IllegalArgumentException if subdoc is null
     * @throws IllegalStateException if the array has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insertDoc(int index, YDoc subdoc) {
        checkClosed();
        if (subdoc == null) {
            throw new IllegalArgumentException("Subdocument cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsertDoc(doc.getNativePtr(), nativePtr, index, subdoc.getNativePtr());
    }

    /**
     * Appends a YDoc subdocument to the end of the array within an existing transaction.
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YDoc child = new YDoc();
     *      YArray array = parent.getArray("myarray");
     *      YTransaction txn = parent.beginTransaction()) {
     *     array.pushDoc(txn, child);
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param subdoc The YDoc subdocument to append
     * @throws IllegalArgumentException if txn or subdoc is null
     * @throws IllegalStateException if the array has been closed
     */
    public void pushDoc(YTransaction txn, YDoc subdoc) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (subdoc == null) {
            throw new IllegalArgumentException("Subdocument cannot be null");
        }
        nativePushDocWithTxn(doc.getNativePtr(), nativePtr, txn.getNativePtr(),
            subdoc.getNativePtr());
    }

    /**
     * Appends a YDoc subdocument to the end of the array (creates implicit transaction).
     *
     * <p>This allows embedding one YDoc inside another, enabling hierarchical
     * document structures and composition.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YDoc child = new YDoc();
     *      YArray array = parent.getArray("myarray")) {
     *     array.pushDoc(child);
     * }
     * }</pre>
     *
     * @param subdoc The YDoc subdocument to append
     * @throws IllegalArgumentException if subdoc is null
     * @throws IllegalStateException if the array has been closed
     */
    public void pushDoc(YDoc subdoc) {
        checkClosed();
        if (subdoc == null) {
            throw new IllegalArgumentException("Subdocument cannot be null");
        }
        nativePushDoc(doc.getNativePtr(), nativePtr, subdoc.getNativePtr());
    }

    /**
     * Gets a YDoc subdocument at the specified index.
     *
     * <p>The returned YDoc must be closed by the caller when no longer needed.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc parent = new YDoc();
     *      YArray array = parent.getArray("myarray")) {
     *     array.pushDoc(new YDoc());
     *     try (YDoc retrieved = array.getDoc(0)) {
     *         // Use the subdocument
     *     }
     * }
     * }</pre>
     *
     * @param index The index (0-based)
     * @return The YDoc subdocument, or null if index is out of bounds or value is not a Doc
     * @throws IllegalStateException if the array has been closed
     */
    public YDoc getDoc(int index) {
        checkClosed();
        if (index < 0) {
            return null;
        }
        long subdocPtr = nativeGetDoc(doc.getNativePtr(), nativePtr, index);
        if (subdocPtr == 0) {
            return null;
        }
        return new YDoc(subdocPtr, true);
    }

    /**
     * Returns a JSON string representation of the array.
     *
     * @return A JSON string representation
     * @throws IllegalStateException if the array has been closed
     */
    public String toJson() {
        checkClosed();
        return nativeToJson(doc.getNativePtr(), nativePtr);
    }

    /**
     * Registers an observer to be notified when this array changes.
     *
     * <p>The observer will be called whenever elements are added, removed, or modified
     * in this array. The observer receives a {@link YEvent} containing details about
     * the changes.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YDoc doc = new YDoc();
     *      YArray array = doc.getArray("myarray");
     *      YSubscription sub = array.observe(event -> {
     *          System.out.println("Array changed!");
     *          for (YChange change : event.getChanges()) {
     *              // Handle change
     *          }
     *      })) {
     *     array.pushString("Hello"); // Observer is called
     * }
     * }</pre>
     *
     * @param observer The observer to register (must not be null)
     * @return A subscription handle that can be used to unregister the observer
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this array has been closed
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
     * <p>This method is called from native code when array changes occur.
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
     * Checks if this YArray has been closed.
     *
     * @return true if this YArray has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this YArray and releases native resources.
     *
     * <p>After calling this method, any operations on this YArray will throw
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
     * Checks if this YArray has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this YArray has been closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YArray has been closed");
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
    private static native long nativeGetArray(long docPtr, String name);
    private static native void nativeDestroy(long ptr);
    private static native int nativeLength(long docPtr, long arrayPtr);
    private static native String nativeGetString(long docPtr, long arrayPtr, int index);
    private static native double nativeGetDouble(long docPtr, long arrayPtr, int index);
    private static native void nativeInsertString(long docPtr, long arrayPtr, int index,
                                                   String value);
    private static native void nativeInsertStringWithTxn(long docPtr, long arrayPtr, long txnPtr,
                                                          int index, String value);
    private static native void nativeInsertDouble(long docPtr, long arrayPtr, int index,
                                                   double value);
    private static native void nativeInsertDoubleWithTxn(long docPtr, long arrayPtr, long txnPtr,
                                                          int index, double value);
    private static native void nativePushString(long docPtr, long arrayPtr, String value);
    private static native void nativePushStringWithTxn(long docPtr, long arrayPtr, long txnPtr,
                                                        String value);
    private static native void nativePushDouble(long docPtr, long arrayPtr, double value);
    private static native void nativePushDoubleWithTxn(long docPtr, long arrayPtr, long txnPtr,
                                                        double value);
    private static native void nativeRemove(long docPtr, long arrayPtr, int index, int length);
    private static native void nativeRemoveWithTxn(long docPtr, long arrayPtr, long txnPtr,
                                                    int index, int length);
    private static native String nativeToJson(long docPtr, long arrayPtr);
    private static native void nativeInsertDoc(long docPtr, long arrayPtr, int index,
                                                long subdocPtr);
    private static native void nativeInsertDocWithTxn(long docPtr, long arrayPtr, long txnPtr,
                                                       int index, long subdocPtr);
    private static native void nativePushDoc(long docPtr, long arrayPtr, long subdocPtr);
    private static native void nativePushDocWithTxn(long docPtr, long arrayPtr, long txnPtr,
                                                     long subdocPtr);
    private static native long nativeGetDoc(long docPtr, long arrayPtr, int index);
    private static native void nativeObserve(long docPtr, long arrayPtr, long subscriptionId,
                                              YArray yarrayObj);
    private static native void nativeUnobserve(long docPtr, long arrayPtr, long subscriptionId);
}
