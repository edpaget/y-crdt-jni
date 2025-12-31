package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YText;
import net.carcdr.ycrdt.YTransaction;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
 * @see net.carcdr.ycrdt.YDoc
 */
public class JniYText implements YText, JniYObservable {

    private final JniYDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;
    private final ConcurrentHashMap<Long, YObserver> observers = new ConcurrentHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong(0);

    /**
     * Package-private constructor. Use {@link YDoc#getText(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this text object in the document
     */
    JniYText(JniYDoc doc, String name) {
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeLengthWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr());
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeLengthWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        }
    }

    /**
     * Returns the length of the text.
     *
     * @param txn The transaction to use for this operation
     * @return The number of characters in the text
     * @throws IllegalStateException if the text has been closed
     */
    @Override
    public int length(YTransaction txn) {
        checkClosed();
        return nativeLengthWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeToStringWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr());
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeToStringWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        }
    }

    /**
     * Inserts text at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     text.insert(txn, 0, "Hello");
     *     text.insert(txn, 5, " World");
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param index The position at which to insert the text (0-based)
     * @param chunk The text to insert
     * @throws IllegalArgumentException if txn or chunk is null
     * @throws IllegalStateException if the text has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    @Override
    public void insert(YTransaction txn, int index, String chunk) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0 || index > length(txn)) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length(txn));
        }
        nativeInsertWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), index, chunk);
    }

    /**
     * Inserts text at the specified index (creates implicit transaction).
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            if (index < 0 || index > length(activeTxn)) {
                throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for length " + length(activeTxn));
            }
            nativeInsertWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(), index, chunk);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                if (index < 0 || index > length(txn)) {
                    throw new IndexOutOfBoundsException(
                        "Index " + index + " out of bounds for length " + length(txn));
                }
                nativeInsertWithTxn(doc.getNativePtr(), nativePtr,
                    ((JniYTransaction) txn).getNativePtr(), index, chunk);
            }
        }
    }

    /**
     * Appends text to the end within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     text.push(txn, "Hello");
     *     text.push(txn, " World");
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param chunk The text to append
     * @throws IllegalArgumentException if txn or chunk is null
     * @throws IllegalStateException if the text has been closed
     */
    @Override
    public void push(YTransaction txn, String chunk) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        nativePushWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), chunk);
    }

    /**
     * Appends text to the end (creates implicit transaction).
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativePushWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(), chunk);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativePushWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), chunk);
            }
        }
    }

    /**
     * Deletes a range of text within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     text.insert(txn, 0, "Hello World");
     *     text.delete(txn, 5, 6); // Delete " World"
     * }
     * }</pre>
     *
     * @param txn The transaction to use for this operation
     * @param index The starting position (0-based)
     * @param length The number of characters to delete
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the text has been closed
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    @Override
    public void delete(YTransaction txn, int index, int length) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0 || length < 0) {
            throw new IndexOutOfBoundsException(
                "Index and length must be non-negative");
        }
        int currentLength = length(txn);
        if (index + length > currentLength) {
            throw new IndexOutOfBoundsException(
                "Range [" + index + ", " + (index + length) + ") out of bounds for length "
                + currentLength);
        }
        nativeDeleteWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), index, length);
    }

    /**
     * Deletes a range of text (creates implicit transaction).
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
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            int currentLength = length(activeTxn);
            if (index + length > currentLength) {
                throw new IndexOutOfBoundsException(
                    "Range [" + index + ", " + (index + length) + ") out of bounds for length "
                    + currentLength);
            }
            nativeDeleteWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(), index, length);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                int currentLength = length(txn);
                if (index + length > currentLength) {
                    throw new IndexOutOfBoundsException(
                        "Range [" + index + ", " + (index + length) + ") out of bounds for length "
                        + currentLength);
                }
                nativeDeleteWithTxn(doc.getNativePtr(), nativePtr,
                    ((JniYTransaction) txn).getNativePtr(), index, length);
            }
        }
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
     * Registers an observer to be notified of changes to this text.
     *
     * <p>The observer will be called whenever this text is modified.
     * Multiple observers can be registered on the same text.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YSubscription sub = text.observe(event -> {
     *     System.out.println("Text changed!");
     *     for (YChange change : event.getChanges()) {
     *         System.out.println("  " + change);
     *     }
     * })) {
     *     text.insert(0, "Hello"); // Triggers observer
     * }
     * }</pre>
     *
     * @param observer the observer to register
     * @return a subscription handle that can be used to unobserve
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this text has been closed
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
     * Package-private method to unobserve by subscription ID.
     * Called by YSubscription.close().
     *
     * @param subscriptionId the subscription ID to remove
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
     * Package-private method called by JNI to dispatch events.
     *
     * @param subscriptionId the subscription ID
     * @param event the event to dispatch
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
                    // Clear all observers
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
    private static native int nativeLengthWithTxn(long docPtr, long textPtr, long txnPtr);
    private static native String nativeToStringWithTxn(long docPtr, long textPtr, long txnPtr);
    private static native void nativeInsertWithTxn(long docPtr, long textPtr, long txnPtr, int index, String chunk);
    private static native void nativePushWithTxn(long docPtr, long textPtr, long txnPtr, String chunk);
    private static native void nativeDeleteWithTxn(long docPtr, long textPtr, long txnPtr, int index, int length);
    private static native void nativeObserve(long docPtr, long textPtr, long subscriptionId, YText ytextObj);
    private static native void nativeUnobserve(long docPtr, long textPtr, long subscriptionId);
}
