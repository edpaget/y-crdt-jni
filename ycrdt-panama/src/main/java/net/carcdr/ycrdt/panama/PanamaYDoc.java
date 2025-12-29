package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

import net.carcdr.ycrdt.UpdateObserver;
import net.carcdr.ycrdt.YArray;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YMap;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YText;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlText;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YDoc.
 *
 * <p>This class provides access to Y-CRDT documents using Project Panama's
 * Foreign Function and Memory API, calling into the yffi native library.</p>
 */
public class PanamaYDoc implements YDoc {

    private MemorySegment docPtr;
    private volatile boolean closed = false;
    private final boolean ownsPointer;
    private final ThreadLocal<PanamaYTransaction> activeTransaction = new ThreadLocal<>();

    /**
     * Creates a new document.
     */
    public PanamaYDoc() {
        this.docPtr = Yrs.ydocNew();
        if (this.docPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to create YDoc: native pointer is null");
        }
        this.ownsPointer = true;
    }

    /**
     * Creates a new document with a specific client ID.
     *
     * @param clientId the client ID
     */
    public PanamaYDoc(long clientId) {
        // yffi doesn't have ydoc_new_with_client_id, only ydoc_new_with_options
        // For now, just create a regular document
        // TODO: Use ydoc_new_with_options when options struct is implemented
        this.docPtr = Yrs.ydocNew();
        if (this.docPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to create YDoc: native pointer is null");
        }
        this.ownsPointer = true;
    }

    /**
     * Wraps an existing native document pointer.
     *
     * @param docPtr the native document pointer
     * @param ownsPointer if true, close() will destroy the native document
     */
    PanamaYDoc(MemorySegment docPtr, boolean ownsPointer) {
        if (docPtr.equals(MemorySegment.NULL)) {
            throw new IllegalArgumentException("Document pointer cannot be null");
        }
        this.docPtr = docPtr;
        this.ownsPointer = ownsPointer;
    }

    /**
     * Gets the native document pointer.
     *
     * @return the document pointer
     */
    MemorySegment getDocPtr() {
        return docPtr;
    }

    @Override
    public long getClientId() {
        ensureNotClosed();
        return Yrs.ydocId(docPtr);
    }

    @Override
    public String getGuid() {
        ensureNotClosed();
        MemorySegment guidPtr = Yrs.ydocGuid(docPtr);
        return Yrs.readAndFreeString(guidPtr);
    }

    @Override
    public byte[] encodeStateAsUpdate() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            return encodeStateAsUpdate(activeTxn);
        }
        try (PanamaYTransaction txn = beginTransactionInternal()) {
            return encodeStateAsUpdate(txn);
        }
    }

    @Override
    public byte[] encodeStateAsUpdate(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            // Pass null state vector to get full update
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment result = Yrs.ytransactionStateDiffV1(
                ptxn.getTxnPtr(), MemorySegment.NULL, 0, lenPtr);
            int len = lenPtr.get(ValueLayout.JAVA_INT, 0);
            return Yrs.readAndFreeBinary(result, len);
        }
    }

    @Override
    public void applyUpdate(byte[] update) {
        ensureNotClosed();
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }
        PanamaYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            applyUpdate(activeTxn, update);
        } else {
            try (PanamaYTransaction txn = beginTransactionInternal()) {
                applyUpdate(txn, update);
            }
        }
    }

    @Override
    public void applyUpdate(YTransaction txn, byte[] update) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment updatePtr = Yrs.createBinary(arena, update);
            byte result = Yrs.ytransactionApply(ptxn.getTxnPtr(), updatePtr, update.length);
            if (result != 0) {
                throw new RuntimeException("Failed to apply update: error code " + result);
            }
        }
    }

    @Override
    public byte[] encodeStateVector() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            return encodeStateVector(activeTxn);
        }
        try (PanamaYTransaction txn = beginTransactionInternal()) {
            return encodeStateVector(txn);
        }
    }

    @Override
    public byte[] encodeStateVector(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment result = Yrs.ytransactionStateVectorV1(ptxn.getTxnPtr(), lenPtr);
            int len = lenPtr.get(ValueLayout.JAVA_INT, 0);
            return Yrs.readAndFreeBinary(result, len);
        }
    }

    @Override
    public byte[] encodeDiff(byte[] stateVector) {
        ensureNotClosed();
        if (stateVector == null) {
            throw new IllegalArgumentException("State vector cannot be null");
        }
        PanamaYTransaction activeTxn = getActiveTransaction();
        if (activeTxn != null) {
            return encodeDiff(activeTxn, stateVector);
        }
        try (PanamaYTransaction txn = beginTransactionInternal()) {
            return encodeDiff(txn, stateVector);
        }
    }

    @Override
    public byte[] encodeDiff(YTransaction txn, byte[] stateVector) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (stateVector == null) {
            throw new IllegalArgumentException("State vector cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment svPtr = Yrs.createBinary(arena, stateVector);
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment result = Yrs.ytransactionStateDiffV1(
                ptxn.getTxnPtr(), svPtr, stateVector.length, lenPtr);
            int len = lenPtr.get(ValueLayout.JAVA_INT, 0);
            return Yrs.readAndFreeBinary(result, len);
        }
    }

    @Override
    public YText getText(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new PanamaYText(this, name);
    }

    @Override
    public YArray getArray(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new PanamaYArray(this, name);
    }

    @Override
    public YMap getMap(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new PanamaYMap(this, name);
    }

    @Override
    public YXmlText getXmlText(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        throw new UnsupportedOperationException("YXmlText not yet implemented");
    }

    @Override
    public YXmlElement getXmlElement(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        throw new UnsupportedOperationException("YXmlElement not yet implemented");
    }

    @Override
    public YXmlFragment getXmlFragment(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return new PanamaYXmlFragment(this, name);
    }

    @Override
    public PanamaYTransaction beginTransaction() {
        return beginTransactionInternal();
    }

    /**
     * Internal method to begin a transaction.
     */
    private PanamaYTransaction beginTransactionInternal() {
        ensureNotClosed();
        MemorySegment txnPtr = Yrs.ydocWriteTransaction(docPtr, 0, MemorySegment.NULL);
        if (txnPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to create transaction");
        }
        PanamaYTransaction txn = new PanamaYTransaction(this, txnPtr);
        activeTransaction.set(txn);
        return txn;
    }

    /**
     * Gets the currently active transaction for this thread.
     *
     * @return the active transaction, or null
     */
    PanamaYTransaction getActiveTransaction() {
        return activeTransaction.get();
    }

    /**
     * Clears the active transaction for this thread.
     */
    void clearActiveTransaction() {
        activeTransaction.remove();
    }

    @Override
    public void transaction(Consumer<YTransaction> fn) {
        if (fn == null) {
            throw new IllegalArgumentException("Transaction function cannot be null");
        }
        try (PanamaYTransaction txn = beginTransactionInternal()) {
            fn.accept(txn);
        }
    }

    @Override
    public YSubscription observeUpdateV1(UpdateObserver observer) {
        ensureNotClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        // TODO: Implement observer support
        throw new UnsupportedOperationException("Observers not yet implemented");
    }

    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed && !docPtr.equals(MemorySegment.NULL)) {
                    if (ownsPointer) {
                        Yrs.ydocDestroy(docPtr);
                    }
                    docPtr = MemorySegment.NULL;
                    closed = true;
                }
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Ensures the document is not closed.
     *
     * @throws IllegalStateException if closed
     */
    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("YDoc has been closed");
        }
    }
}
