package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlNode;
import net.carcdr.ycrdt.YXmlText;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YXmlFragment.
 */
public class PanamaYXmlFragment implements YXmlFragment {

    private final PanamaYDoc doc;
    private final MemorySegment branchPtr;
    private volatile boolean closed = false;

    /**
     * Creates a new YXmlFragment instance.
     *
     * @param doc the document
     * @param name the name of the fragment
     */
    PanamaYXmlFragment(PanamaYDoc doc, String name) {
        this.doc = doc;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            this.branchPtr = Yrs.yxmlfragment(doc.getDocPtr(), namePtr);
        }
        if (this.branchPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to create YXmlFragment");
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
        return Yrs.yxmlelemChildLen(branchPtr, ptxn.getTxnPtr());
    }

    @Override
    public void insertElement(int index, String tag) {
        ensureNotClosed();
        throw new UnsupportedOperationException("insertElement not yet implemented");
    }

    @Override
    public void insertElement(YTransaction txn, int index, String tag) {
        ensureNotClosed();
        throw new UnsupportedOperationException("insertElement not yet implemented");
    }

    @Override
    public void insertText(int index, String content) {
        ensureNotClosed();
        throw new UnsupportedOperationException("insertText not yet implemented");
    }

    @Override
    public void insertText(YTransaction txn, int index, String content) {
        ensureNotClosed();
        throw new UnsupportedOperationException("insertText not yet implemented");
    }

    @Override
    public void remove(int index, int length) {
        ensureNotClosed();
        throw new UnsupportedOperationException("remove not yet implemented");
    }

    @Override
    public void remove(YTransaction txn, int index, int length) {
        ensureNotClosed();
        throw new UnsupportedOperationException("remove not yet implemented");
    }

    @Override
    public YXmlNode.NodeType getNodeType(int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getNodeType not yet implemented");
    }

    @Override
    public YXmlNode.NodeType getNodeType(YTransaction txn, int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getNodeType not yet implemented");
    }

    @Override
    public Object getChild(int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getChild not yet implemented");
    }

    @Override
    public YXmlElement getElement(int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getElement not yet implemented");
    }

    @Override
    public YXmlElement getElement(YTransaction txn, int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getElement not yet implemented");
    }

    @Override
    public YXmlText getText(int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getText not yet implemented");
    }

    @Override
    public YXmlText getText(YTransaction txn, int index) {
        ensureNotClosed();
        throw new UnsupportedOperationException("getText not yet implemented");
    }

    @Override
    public String toXmlString() {
        ensureNotClosed();
        throw new UnsupportedOperationException("toXmlString not yet implemented");
    }

    @Override
    public String toXmlString(YTransaction txn) {
        ensureNotClosed();
        throw new UnsupportedOperationException("toXmlString not yet implemented");
    }

    @Override
    public String toString() {
        if (closed) {
            return "YXmlFragment[closed]";
        }
        try {
            return toXmlString();
        } catch (UnsupportedOperationException e) {
            return "YXmlFragment[length=" + length() + "]";
        }
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
            throw new IllegalStateException("YXmlFragment has been closed");
        }
    }
}
