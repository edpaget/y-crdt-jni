package net.carcdr.ycrdt;

/**
 * A transaction for batching multiple operations on a Y-CRDT document.
 * All operations within a transaction are applied atomically.
 */
public interface YTransaction extends AutoCloseable {

    /**
     * Explicitly commits the transaction.
     * After calling commit, the transaction is closed.
     */
    void commit();

    /**
     * Closes the transaction, committing any pending changes.
     */
    @Override
    void close();

    /**
     * Checks if this transaction has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}
