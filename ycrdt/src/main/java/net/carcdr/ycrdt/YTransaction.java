package net.carcdr.ycrdt;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Transaction handle for batching multiple CRDT operations.
 *
 * <p>Transactions allow multiple operations to be combined into a single
 * atomic unit, resulting in:
 * <ul>
 *   <li>Better performance (fewer JNI calls, single commit)</li>
 *   <li>Single observer notification with combined changes</li>
 *   <li>More efficient update encoding for synchronization</li>
 * </ul>
 *
 * <p>Usage with try-with-resources (recommended):
 * <pre>{@code
 * try (YTransaction txn = doc.beginTransaction()) {
 *     text.insert(txn, 0, "Hello");
 *     text.insert(txn, 5, " World");
 * } // Auto-commits here
 * }</pre>
 *
 * <p><b>Thread Safety:</b> YTransaction instances are not thread-safe and must
 * only be used from the thread that created them.</p>
 *
 * <p><b>Memory Management:</b> YTransaction implements {@link Closeable} and must
 * be closed when no longer needed to commit changes and free native resources.
 * Use try-with-resources to ensure proper cleanup.</p>
 *
 * @see YDoc#beginTransaction()
 * @see YDoc#transaction(Consumer)
 */
public class YTransaction implements AutoCloseable {

    /**
     * The document this transaction belongs to
     */
    private final YDoc doc;

    /**
     * Pointer to the native transaction instance
     */
    private final long nativePtr;

    /**
     * Flag to track if this transaction has been closed
     */
    private volatile boolean closed = false;

    /**
     * Package-private constructor (created by YDoc only).
     *
     * @param doc the document this transaction belongs to
     * @param nativePtr the native transaction pointer
     */
    YTransaction(YDoc doc, long nativePtr) {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (nativePtr == 0) {
            throw new IllegalArgumentException("Invalid native pointer");
        }
        this.doc = doc;
        this.nativePtr = nativePtr;
    }

    /**
     * Commits the transaction explicitly.
     *
     * <p>After commit, all batched operations become visible to observers
     * and are encoded as a single update for synchronization.</p>
     *
     * <p>This method is called automatically by {@link #close()}, so explicit
     * calls are typically not necessary when using try-with-resources.</p>
     *
     * <p>This method is idempotent - calling it multiple times is safe.</p>
     *
     * @throws IllegalStateException if transaction already closed
     * @throws RuntimeException if commit fails
     */
    public void commit() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    nativeCommit(doc.getNativePtr(), nativePtr);
                    closed = true;
                }
            }
        }
    }

    /**
     * Rolls back the transaction, discarding all changes.
     *
     * <p><b>Note:</b> The underlying yrs library may not support true rollback.
     * This method is reserved for future use and currently behaves the same as
     * commit. Check the implementation before relying on rollback semantics.</p>
     *
     * <p>This method is idempotent - calling it multiple times is safe.</p>
     *
     * @throws IllegalStateException if transaction already closed
     */
    public void rollback() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    nativeRollback(doc.getNativePtr(), nativePtr);
                    closed = true;
                }
            }
        }
    }

    /**
     * Closes the transaction, committing all batched operations.
     *
     * <p>This method is called automatically when using try-with-resources.</p>
     *
     * <p>This method is idempotent - calling it multiple times is safe.</p>
     */
    @Override
    public void close() {
        commit(); // Auto-commit on close
    }

    /**
     * Checks if this transaction has been closed.
     *
     * @return true if this transaction has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Gets the native pointer for internal use by operation methods.
     *
     * @return the native pointer value
     * @throws IllegalStateException if transaction already closed
     */
    long getNativePtr() {
        if (closed) {
            throw new IllegalStateException("Transaction has been closed");
        }
        return nativePtr;
    }

    /**
     * Gets the document this transaction belongs to.
     *
     * @return the YDoc instance
     */
    YDoc getDoc() {
        return doc;
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

    /**
     * Commits the transaction on the native side.
     *
     * @param docPtr pointer to the YDoc
     * @param txnPtr pointer to the transaction
     */
    private static native void nativeCommit(long docPtr, long txnPtr);

    /**
     * Rolls back the transaction on the native side.
     *
     * @param docPtr pointer to the YDoc
     * @param txnPtr pointer to the transaction
     */
    private static native void nativeRollback(long docPtr, long txnPtr);
}
