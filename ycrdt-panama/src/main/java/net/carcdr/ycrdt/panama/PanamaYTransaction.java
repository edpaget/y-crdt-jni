package net.carcdr.ycrdt.panama;

import java.lang.foreign.MemorySegment;

import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YTransaction.
 *
 * <p>Transactions are used to batch operations on a Y-CRDT document.
 * They must be committed (closed) for changes to take effect.</p>
 */
public class PanamaYTransaction implements YTransaction {

    private final PanamaYDoc doc;
    private MemorySegment txnPtr;
    private volatile boolean closed = false;

    /**
     * Creates a new transaction for the given document.
     *
     * @param doc the document
     * @param txnPtr pointer to the native transaction
     */
    PanamaYTransaction(PanamaYDoc doc, MemorySegment txnPtr) {
        this.doc = doc;
        this.txnPtr = txnPtr;
    }

    /**
     * Gets the native transaction pointer.
     *
     * @return the transaction pointer
     */
    MemorySegment getTxnPtr() {
        return txnPtr;
    }

    /**
     * Gets the document this transaction belongs to.
     *
     * @return the document
     */
    PanamaYDoc getDoc() {
        return doc;
    }

    @Override
    public void commit() {
        close();
    }

    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed && !txnPtr.equals(MemorySegment.NULL)) {
                    Yrs.ytransactionCommit(txnPtr);
                    txnPtr = MemorySegment.NULL;
                    doc.clearActiveTransaction();
                    closed = true;
                }
            }
        }
    }

    /**
     * Checks if this transaction has been closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Ensures the transaction is not closed.
     *
     * @throws IllegalStateException if closed
     */
    void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Transaction has been closed");
        }
    }
}
