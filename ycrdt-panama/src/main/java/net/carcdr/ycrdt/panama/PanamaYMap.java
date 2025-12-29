package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YMap;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YMap.
 */
public class PanamaYMap implements YMap {

    private final PanamaYDoc doc;
    private final MemorySegment branchPtr;
    private volatile boolean closed = false;

    /**
     * Creates a new YMap instance.
     *
     * @param doc the document
     * @param name the name of the map
     */
    PanamaYMap(PanamaYDoc doc, String name) {
        this.doc = doc;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            this.branchPtr = Yrs.ymap(doc.getDocPtr(), namePtr);
        }
        if (this.branchPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to create YMap");
        }
    }

    @Override
    public int size() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return size(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return size(txn);
        }
    }

    @Override
    public int size(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        return Yrs.ymapLen(branchPtr, ptxn.getTxnPtr());
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public String getString(String key) {
        ensureNotClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getString(activeTxn, key);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getString(txn, key);
        }
    }

    @Override
    public String getString(YTransaction txn, String key) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment keyPtr = Yrs.createString(arena, key);
            MemorySegment output = Yrs.ymapGet(branchPtr, ptxn.getTxnPtr(), keyPtr);
            if (output.equals(MemorySegment.NULL)) {
                return null;
            }
            try {
                MemorySegment strPtr = Yrs.youtputReadString(output);
                if (strPtr.equals(MemorySegment.NULL)) {
                    return null;
                }
                return Yrs.readAndFreeString(strPtr);
            } finally {
                Yrs.youtputDestroy(output);
            }
        }
    }

    @Override
    public void setString(String key, String value) {
        ensureNotClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            setString(activeTxn, key, value);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                setString(txn, key, value);
            }
        }
    }

    @Override
    public void setString(YTransaction txn, String key, String value) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        // TODO: Fix YInput struct-by-value return handling in Panama FFM
        throw new UnsupportedOperationException("setString not yet implemented");
    }

    @Override
    public double getDouble(String key) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getDouble not yet implemented");
    }

    @Override
    public double getDouble(YTransaction txn, String key) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getDouble not yet implemented");
    }

    @Override
    public void setDouble(String key, double value) {
        ensureNotClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            setDouble(activeTxn, key, value);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                setDouble(txn, key, value);
            }
        }
    }

    @Override
    public void setDouble(YTransaction txn, String key, double value) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        // TODO: Fix YInput struct-by-value return handling in Panama FFM
        throw new UnsupportedOperationException("setDouble not yet implemented");
    }

    @Override
    public YDoc getDoc(String key) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getDoc not yet implemented");
    }

    @Override
    public YDoc getDoc(YTransaction txn, String key) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getDoc not yet implemented");
    }

    @Override
    public void setDoc(String key, YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("setDoc not yet implemented");
    }

    @Override
    public void setDoc(YTransaction txn, String key, YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("setDoc not yet implemented");
    }

    @Override
    public boolean containsKey(String key) {
        ensureNotClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return containsKey(activeTxn, key);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return containsKey(txn, key);
        }
    }

    @Override
    public boolean containsKey(YTransaction txn, String key) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment keyPtr = Yrs.createString(arena, key);
            MemorySegment output = Yrs.ymapGet(branchPtr, ptxn.getTxnPtr(), keyPtr);
            if (output.equals(MemorySegment.NULL)) {
                return false;
            }
            Yrs.youtputDestroy(output);
            return true;
        }
    }

    @Override
    public String[] keys() {
        ensureNotClosed();
        // TODO: Implement using ymap_iter
        throw new UnsupportedOperationException("keys not yet implemented");
    }

    @Override
    public String[] keys(YTransaction txn) {
        ensureNotClosed();
        throw new UnsupportedOperationException("keys not yet implemented");
    }

    @Override
    public void remove(String key) {
        ensureNotClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            remove(activeTxn, key);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                remove(txn, key);
            }
        }
    }

    @Override
    public void remove(YTransaction txn, String key) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment keyPtr = Yrs.createString(arena, key);
            Yrs.ymapRemove(branchPtr, ptxn.getTxnPtr(), keyPtr);
        }
    }

    @Override
    public void clear() {
        ensureNotClosed();
        // TODO: Implement using ymap_remove_all
        throw new UnsupportedOperationException("clear not yet implemented");
    }

    @Override
    public void clear(YTransaction txn) {
        ensureNotClosed();
        throw new UnsupportedOperationException("clear not yet implemented");
    }

    @Override
    public String toJson() {
        ensureNotClosed();
        throw new UnsupportedOperationException("toJson not yet implemented");
    }

    @Override
    public String toJson(YTransaction txn) {
        ensureNotClosed();
        throw new UnsupportedOperationException("toJson not yet implemented");
    }

    @Override
    public YSubscription observe(YObserver observer) {
        ensureNotClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        throw new UnsupportedOperationException("Observers not yet implemented");
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("YMap has been closed");
        }
    }
}
