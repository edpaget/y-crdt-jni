package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YText;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YText.
 */
public class PanamaYText implements YText {

    private final PanamaYDoc doc;
    private final MemorySegment branchPtr;
    private volatile boolean closed = false;

    /**
     * Creates a new YText instance.
     *
     * @param doc the document
     * @param name the name of the text
     */
    PanamaYText(PanamaYDoc doc, String name) {
        this.doc = doc;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            this.branchPtr = Yrs.ytext(doc.getDocPtr(), namePtr);
        }
        if (this.branchPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to create YText");
        }
    }

    @Override
    public int length() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return length(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return length(txn);
        }
    }

    @Override
    public int length(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        return Yrs.ytextLen(branchPtr, ptxn.getTxnPtr());
    }

    @Override
    public String toString() {
        if (closed) {
            return "YText[closed]";
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return toStringWithTxn(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return toStringWithTxn(txn);
        }
    }

    private String toStringWithTxn(PanamaYTransaction txn) {
        MemorySegment strPtr = Yrs.ytextString(branchPtr, txn.getTxnPtr());
        return Yrs.readAndFreeString(strPtr);
    }

    @Override
    public void insert(int index, String chunk) {
        ensureNotClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            insert(activeTxn, index, chunk);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                insert(txn, index, chunk);
            }
        }
    }

    @Override
    public void insert(YTransaction txn, int index, String chunk) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment chunkPtr = Yrs.createString(arena, chunk);
            Yrs.ytextInsert(branchPtr, ptxn.getTxnPtr(), index, chunkPtr, MemorySegment.NULL);
        }
    }

    @Override
    public void push(String chunk) {
        ensureNotClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            push(activeTxn, chunk);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                push(txn, chunk);
            }
        }
    }

    @Override
    public void push(YTransaction txn, String chunk) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        int len = length(txn);
        insert(txn, len, chunk);
    }

    @Override
    public void delete(int index, int length) {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            delete(activeTxn, index, length);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                delete(txn, index, length);
            }
        }
    }

    @Override
    public void delete(YTransaction txn, int index, int length) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        Yrs.ytextRemoveRange(branchPtr, ptxn.getTxnPtr(), index, length);
    }

    @Override
    public YSubscription observe(YObserver observer) {
        ensureNotClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        // TODO: Implement observer support
        throw new UnsupportedOperationException("Observers not yet implemented");
    }

    @Override
    public void close() {
        // Branch pointers are owned by the document, not freed individually
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("YText has been closed");
        }
    }
}
