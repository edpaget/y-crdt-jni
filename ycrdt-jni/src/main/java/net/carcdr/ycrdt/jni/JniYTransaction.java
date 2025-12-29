package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YTransaction;

/**
 * JNI-based implementation of YTransaction.
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
 */
public class JniYTransaction implements YTransaction {

    /**
     * The document this transaction belongs to.
     */
    private final JniYDoc doc;

    /**
     * Pointer to the native transaction instance.
     */
    private final long nativePtr;

    /**
     * Flag to track if this transaction has been closed.
     */
    private volatile boolean closed = false;

    /**
     * Package-private constructor (created by JniYDoc only).
     *
     * @param doc the document this transaction belongs to
     * @param nativePtr the native transaction pointer
     */
    JniYTransaction(JniYDoc doc, long nativePtr) {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (nativePtr == 0) {
            throw new IllegalArgumentException("Invalid native pointer");
        }
        this.doc = doc;
        this.nativePtr = nativePtr;
    }

    @Override
    public void commit() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    nativeCommit(doc.getNativePtr(), nativePtr);
                    doc.clearActiveTransaction();
                    closed = true;
                }
            }
        }
    }

    @Override
    public void close() {
        commit();
    }

    @Override
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
     * @return the JniYDoc instance
     */
    JniYDoc getDoc() {
        return doc;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    // Native method declarations
    private static native void nativeCommit(long docPtr, long txnPtr);
    private static native void nativeRollback(long docPtr, long txnPtr);
}
