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
 *
 * <p>Represents a container for XML nodes that can hold elements and text nodes.</p>
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

    /**
     * Creates a YXmlFragment from an existing branch pointer.
     *
     * @param doc the document
     * @param branchPtr pointer to the xml fragment branch
     */
    PanamaYXmlFragment(PanamaYDoc doc, MemorySegment branchPtr) {
        this.doc = doc;
        this.branchPtr = branchPtr;
    }

    /**
     * Gets the branch pointer for internal use.
     *
     * @return the branch pointer
     */
    MemorySegment getBranchPtr() {
        return branchPtr;
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
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            insertElement(activeTxn, index, tag);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                insertElement(txn, index, tag);
            }
        }
    }

    @Override
    public void insertElement(YTransaction txn, int index, String tag) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment tagPtr = Yrs.createString(arena, tag);
            MemorySegment childPtr = Yrs.yxmlelemInsertElem(branchPtr, ptxn.getTxnPtr(), index, tagPtr);
            if (childPtr.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Failed to insert element child");
            }
            // We don't return the element, just insert it
        }
    }

    @Override
    public void insertText(int index, String content) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            insertText(activeTxn, index, content);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                insertText(txn, index, content);
            }
        }
    }

    @Override
    public void insertText(YTransaction txn, int index, String content) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment textPtr = Yrs.yxmlelemInsertText(branchPtr, ptxn.getTxnPtr(), index);
        if (textPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to insert text child");
        }
        // If content is provided, insert it into the new text node
        if (content != null && !content.isEmpty()) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment strPtr = Yrs.createString(arena, content);
                Yrs.yxmltextInsert(textPtr, ptxn.getTxnPtr(), 0, strPtr, MemorySegment.NULL);
            }
        }
    }

    @Override
    public void remove(int index, int length) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
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
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        Yrs.yxmlelemRemoveRange(branchPtr, ptxn.getTxnPtr(), index, length);
    }

    @Override
    public YXmlNode.NodeType getNodeType(int index) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getNodeType(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getNodeType(txn, index);
        }
    }

    @Override
    public YXmlNode.NodeType getNodeType(YTransaction txn, int index) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment output = Yrs.yxmlelemGet(branchPtr, ptxn.getTxnPtr(), index);
        if (output.equals(MemorySegment.NULL)) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
        }
        try {
            // Check if it's an element
            MemorySegment elemPtr = Yrs.youtputReadYxmlelem(output);
            if (!elemPtr.equals(MemorySegment.NULL)) {
                return YXmlNode.NodeType.ELEMENT;
            }
            // Check if it's text
            MemorySegment textPtr = Yrs.youtputReadYxmltext(output);
            if (!textPtr.equals(MemorySegment.NULL)) {
                return YXmlNode.NodeType.TEXT;
            }
            throw new RuntimeException("Unknown node type at index " + index);
        } finally {
            Yrs.youtputDestroy(output);
        }
    }

    @Override
    public Object getChild(int index) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getChildInternal(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getChildInternal(txn, index);
        }
    }

    private Object getChildInternal(PanamaYTransaction txn, int index) {
        MemorySegment output = Yrs.yxmlelemGet(branchPtr, txn.getTxnPtr(), index);
        if (output.equals(MemorySegment.NULL)) {
            return null;
        }
        try {
            // Try reading as element first
            MemorySegment elemPtr = Yrs.youtputReadYxmlelem(output);
            if (!elemPtr.equals(MemorySegment.NULL)) {
                return new PanamaYXmlElement(doc, elemPtr);
            }
            // Try reading as text
            MemorySegment textPtr = Yrs.youtputReadYxmltext(output);
            if (!textPtr.equals(MemorySegment.NULL)) {
                return new PanamaYXmlText(doc, textPtr);
            }
            return null;
        } finally {
            Yrs.youtputDestroy(output);
        }
    }

    @Override
    public YXmlElement getElement(int index) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getElement(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getElement(txn, index);
        }
    }

    @Override
    public YXmlElement getElement(YTransaction txn, int index) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment output = Yrs.yxmlelemGet(branchPtr, ptxn.getTxnPtr(), index);
        if (output.equals(MemorySegment.NULL)) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
        }
        try {
            MemorySegment elemPtr = Yrs.youtputReadYxmlelem(output);
            if (elemPtr.equals(MemorySegment.NULL)) {
                throw new ClassCastException("Child at index " + index + " is not an element");
            }
            return new PanamaYXmlElement(doc, elemPtr);
        } finally {
            Yrs.youtputDestroy(output);
        }
    }

    @Override
    public YXmlText getText(int index) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getText(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getText(txn, index);
        }
    }

    @Override
    public YXmlText getText(YTransaction txn, int index) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment output = Yrs.yxmlelemGet(branchPtr, ptxn.getTxnPtr(), index);
        if (output.equals(MemorySegment.NULL)) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds");
        }
        try {
            MemorySegment textPtr = Yrs.youtputReadYxmltext(output);
            if (textPtr.equals(MemorySegment.NULL)) {
                throw new ClassCastException("Child at index " + index + " is not a text node");
            }
            return new PanamaYXmlText(doc, textPtr);
        } finally {
            Yrs.youtputDestroy(output);
        }
    }

    @Override
    public String toXmlString() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return toXmlString(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return toXmlString(txn);
        }
    }

    @Override
    public String toXmlString(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        // yffi's yxmlelem_string doesn't work for fragments, only elements
        // Build the string by iterating over children
        int len = Yrs.yxmlelemChildLen(branchPtr, ptxn.getTxnPtr());
        if (len == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            MemorySegment output = Yrs.yxmlelemGet(branchPtr, ptxn.getTxnPtr(), i);
            if (output.equals(MemorySegment.NULL)) {
                continue;
            }
            try {
                // Try reading as element
                MemorySegment elemPtr = Yrs.youtputReadYxmlelem(output);
                if (!elemPtr.equals(MemorySegment.NULL)) {
                    MemorySegment strPtr = Yrs.yxmlelemString(elemPtr, ptxn.getTxnPtr());
                    if (!strPtr.equals(MemorySegment.NULL)) {
                        sb.append(Yrs.readAndFreeString(strPtr));
                    }
                    continue;
                }
                // Try reading as text
                MemorySegment textPtr = Yrs.youtputReadYxmltext(output);
                if (!textPtr.equals(MemorySegment.NULL)) {
                    MemorySegment strPtr = Yrs.yxmltextString(textPtr, ptxn.getTxnPtr());
                    if (!strPtr.equals(MemorySegment.NULL)) {
                        sb.append(Yrs.readAndFreeString(strPtr));
                    }
                }
            } finally {
                Yrs.youtputDestroy(output);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (closed) {
            return "YXmlFragment[closed]";
        }
        try {
            return toXmlString();
        } catch (Exception e) {
            return "YXmlFragment[length=" + length() + "]";
        }
    }

    @Override
    public YSubscription observe(YObserver observer) {
        ensureNotClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        // Observers for XML fragments require complex upcall stubs - deferred
        throw new UnsupportedOperationException("XML fragment observers not yet implemented");
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
