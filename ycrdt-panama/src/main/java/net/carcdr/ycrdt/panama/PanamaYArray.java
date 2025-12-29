package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import net.carcdr.ycrdt.YArray;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YArray.
 */
public class PanamaYArray implements YArray {

    private final PanamaYDoc doc;
    private final MemorySegment branchPtr;
    private volatile boolean closed = false;

    /**
     * Creates a new YArray instance.
     *
     * @param doc the document
     * @param name the name of the array
     */
    PanamaYArray(PanamaYDoc doc, String name) {
        this.doc = doc;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            this.branchPtr = Yrs.yarray(doc.getDocPtr(), namePtr);
        }
        if (this.branchPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to create YArray");
        }
    }

    @Override
    public int length() {
        ensureNotClosed();
        return Yrs.yarrayLen(branchPtr);
    }

    @Override
    public int length(YTransaction txn) {
        ensureNotClosed();
        // yarray_len doesn't take a transaction parameter in yffi
        return Yrs.yarrayLen(branchPtr);
    }

    @Override
    public String getString(int index) {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getString(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getString(txn, index);
        }
    }

    @Override
    public String getString(YTransaction txn, int index) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment output = Yrs.yarrayGet(branchPtr, ptxn.getTxnPtr(), index);
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

    @Override
    public void insertString(int index, String value) {
        ensureNotClosed();
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            insertString(activeTxn, index, value);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                insertString(txn, index, value);
            }
        }
    }

    @Override
    public void insertString(YTransaction txn, int index, String value) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment strPtr = Yrs.createString(arena, value);
            // yinputString returns struct by value, allocated by the arena
            MemorySegment yinput = Yrs.yinputString(arena, strPtr);
            Yrs.yarrayInsertRange(branchPtr, ptxn.getTxnPtr(), index, yinput, 1);
        }
    }

    @Override
    public void pushString(String value) {
        ensureNotClosed();
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        int len = length();
        insertString(len, value);
    }

    @Override
    public void pushString(YTransaction txn, String value) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        int len = length(txn);
        insertString(txn, len, value);
    }

    @Override
    public double getDouble(int index) {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getDouble(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getDouble(txn, index);
        }
    }

    @Override
    public double getDouble(YTransaction txn, int index) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment output = Yrs.yarrayGet(branchPtr, ptxn.getTxnPtr(), index);
        if (output.equals(MemorySegment.NULL)) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
        }
        try {
            // Try reading as double first
            MemorySegment doublePtr = Yrs.youtputReadFloat(output);
            if (!doublePtr.equals(MemorySegment.NULL)) {
                MemorySegment reinterpreted = doublePtr.reinterpret(Double.BYTES);
                return reinterpreted.get(ValueLayout.JAVA_DOUBLE, 0);
            }
            // Fall back to reading as long and converting
            MemorySegment longPtr = Yrs.youtputReadLong(output);
            if (!longPtr.equals(MemorySegment.NULL)) {
                MemorySegment reinterpreted = longPtr.reinterpret(Long.BYTES);
                return (double) reinterpreted.get(ValueLayout.JAVA_LONG, 0);
            }
            throw new ClassCastException("Value at index " + index + " is not a number");
        } finally {
            Yrs.youtputDestroy(output);
        }
    }

    @Override
    public void insertDouble(int index, double value) {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            insertDouble(activeTxn, index, value);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                insertDouble(txn, index, value);
            }
        }
    }

    @Override
    public void insertDouble(YTransaction txn, int index, double value) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            // yinputFloat returns struct by value, allocated by the arena
            MemorySegment yinput = Yrs.yinputFloat(arena, value);
            Yrs.yarrayInsertRange(branchPtr, ptxn.getTxnPtr(), index, yinput, 1);
        }
    }

    @Override
    public void pushDouble(double value) {
        ensureNotClosed();
        int len = length();
        insertDouble(len, value);
    }

    @Override
    public void pushDouble(YTransaction txn, double value) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        int len = length(txn);
        insertDouble(txn, len, value);
    }

    @Override
    public YDoc getDoc(int index) {
        ensureNotClosed();
        // Subdocument support requires complex lifecycle management
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public YDoc getDoc(YTransaction txn, int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public void insertDoc(int index, YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public void insertDoc(YTransaction txn, int index, YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public void pushDoc(YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public void pushDoc(YTransaction txn, YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public void remove(int index, int length) {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            remove(activeTxn, index, length);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                remove(txn, index, length);
            }
        }
    }

    @Override
    public void remove(YTransaction txn, int index, int length) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        Yrs.yarrayRemoveRange(branchPtr, ptxn.getTxnPtr(), index, length);
    }

    @Override
    public String toJson() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return toJson(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return toJson(txn);
        }
    }

    @Override
    public String toJson(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment jsonPtr = Yrs.ybranchJson(branchPtr, ptxn.getTxnPtr());
        return Yrs.readAndFreeString(jsonPtr);
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
            throw new IllegalStateException("YArray has been closed");
        }
    }
}
