package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

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
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment keyPtr = Yrs.createString(arena, key);
            MemorySegment valuePtr = value != null ? Yrs.createString(arena, value) : MemorySegment.NULL;
            // yinputString returns struct by value, allocated by the arena
            MemorySegment yinput = Yrs.yinputString(arena, valuePtr);
            Yrs.ymapInsert(branchPtr, ptxn.getTxnPtr(), keyPtr, yinput);
        }
    }

    @Override
    public double getDouble(String key) {
        ensureNotClosed();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getDouble(activeTxn, key);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getDouble(txn, key);
        }
    }

    @Override
    public double getDouble(YTransaction txn, String key) {
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
                throw new IllegalArgumentException("Key not found: " + key);
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
                throw new ClassCastException("Value for key '" + key + "' is not a number");
            } finally {
                Yrs.youtputDestroy(output);
            }
        }
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
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment keyPtr = Yrs.createString(arena, key);
            // yinputFloat returns struct by value, allocated by the arena
            MemorySegment yinput = Yrs.yinputFloat(arena, value);
            Yrs.ymapInsert(branchPtr, ptxn.getTxnPtr(), keyPtr, yinput);
        }
    }

    @Override
    public YDoc getDoc(String key) {
        ensureNotClosed();
        // Subdocument support requires complex lifecycle management
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public YDoc getDoc(YTransaction txn, String key) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public void setDoc(String key, YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
    }

    @Override
    public void setDoc(YTransaction txn, String key, YDoc subdoc) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Subdocuments not yet implemented");
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
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return keys(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return keys(txn);
        }
    }

    @Override
    public String[] keys(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        List<String> keyList = new ArrayList<>();
        MemorySegment iter = Yrs.ymapIter(branchPtr, ptxn.getTxnPtr());
        if (iter.equals(MemorySegment.NULL)) {
            return new String[0];
        }
        try {
            while (true) {
                MemorySegment entry = Yrs.ymapIterNext(iter);
                if (entry.equals(MemorySegment.NULL)) {
                    break;
                }
                try {
                    String key = Yrs.ymapEntryReadKey(entry);
                    if (key != null) {
                        keyList.add(key);
                    }
                } finally {
                    Yrs.ymapEntryDestroy(entry);
                }
            }
        } finally {
            Yrs.ymapIterDestroy(iter);
        }
        return keyList.toArray(new String[0]);
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
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            clear(activeTxn);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                clear(txn);
            }
        }
    }

    @Override
    public void clear(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        // Get all keys and remove them one by one
        String[] allKeys = keys(txn);
        for (String key : allKeys) {
            remove(txn, key);
        }
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
            throw new IllegalStateException("YMap has been closed");
        }
    }
}
