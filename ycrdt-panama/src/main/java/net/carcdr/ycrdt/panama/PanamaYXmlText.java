package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;

import net.carcdr.ycrdt.FormattingChunk;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlText;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YXmlText.
 *
 * <p>Represents an XML text node that can contain formatted text content.</p>
 */
public class PanamaYXmlText implements YXmlText {

    private final PanamaYDoc doc;
    private final MemorySegment branchPtr;
    private volatile boolean closed = false;

    /**
     * Creates a YXmlText from an existing branch pointer.
     *
     * @param doc the document
     * @param branchPtr pointer to the xml text branch
     */
    PanamaYXmlText(PanamaYDoc doc, MemorySegment branchPtr) {
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

    /**
     * Gets the document for internal use.
     *
     * @return the document
     */
    PanamaYDoc getDoc() {
        return doc;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.TEXT;
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
        return Yrs.yxmltextLen(branchPtr, ptxn.getTxnPtr());
    }

    @Override
    public String toString() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return toString(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return toString(txn);
        }
    }

    @Override
    public String toString(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment strPtr = Yrs.yxmltextString(branchPtr, ptxn.getTxnPtr());
        if (strPtr.equals(MemorySegment.NULL)) {
            return "";
        }
        return Yrs.readAndFreeString(strPtr);
    }

    @Override
    public void insert(int index, String chunk) {
        ensureNotClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
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
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment strPtr = Yrs.createString(arena, chunk);
            Yrs.yxmltextInsert(branchPtr, ptxn.getTxnPtr(), index, strPtr, MemorySegment.NULL);
        }
    }

    @Override
    public void push(String chunk) {
        ensureNotClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        int len = length();
        insert(len, chunk);
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
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
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
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        Yrs.yxmltextRemoveRange(branchPtr, ptxn.getTxnPtr(), index, length);
    }

    @Override
    public void insertWithAttributes(int index, String chunk, Map<String, Object> attributes) {
        ensureNotClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            insertWithAttributes(activeTxn, index, chunk, attributes);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                insertWithAttributes(txn, index, chunk, attributes);
            }
        }
    }

    @Override
    public void insertWithAttributes(YTransaction txn, int index, String chunk,
                                     Map<String, Object> attributes) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        // For now, ignore attributes and just insert text
        // Full attribute support requires constructing YInput map which is complex
        if (attributes != null && !attributes.isEmpty()) {
            throw new UnsupportedOperationException("Attributes not yet supported");
        }
        insert(txn, index, chunk);
    }

    @Override
    public void format(int index, int length, Map<String, Object> attributes) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            format(activeTxn, index, length, attributes);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                format(txn, index, length, attributes);
            }
        }
    }

    @Override
    public void format(YTransaction txn, int index, int length, Map<String, Object> attributes) {
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
        // Full formatting support requires constructing YInput map
        throw new UnsupportedOperationException("Formatting not yet implemented");
    }

    @Override
    public Object getParent() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getParent(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getParent(txn);
        }
    }

    @Override
    public Object getParent(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        // Get parent branch pointer
        MemorySegment parentPtr = Yrs.yxmlelemParent(branchPtr);
        if (parentPtr.equals(MemorySegment.NULL)) {
            return null;  // Root-level text has no parent
        }

        // Determine parent type using ytype_kind
        int kind = Yrs.ytypeKind(parentPtr);
        if (kind == Yrs.Y_XML_ELEM) {
            return new PanamaYXmlElement(doc, parentPtr);
        } else if (kind == Yrs.Y_XML_FRAG) {
            return new PanamaYXmlFragment(doc, parentPtr);
        }
        return null;  // Unknown type
    }

    @Override
    public int getIndexInParent() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getIndexInParent(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getIndexInParent(txn);
        }
    }

    @Override
    public int getIndexInParent(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;

        // Get parent
        MemorySegment parentPtr = Yrs.yxmlelemParent(branchPtr);
        if (parentPtr.equals(MemorySegment.NULL)) {
            return -1;  // No parent
        }

        // Get child count from parent
        int childCount = Yrs.yxmlelemChildLen(parentPtr, ptxn.getTxnPtr());

        // Iterate through children to find matching index
        for (int i = 0; i < childCount; i++) {
            MemorySegment childOutput = Yrs.yxmlelemGet(parentPtr, ptxn.getTxnPtr(), i);
            if (!childOutput.equals(MemorySegment.NULL)) {
                // Read the branch pointer from YOutput
                MemorySegment childBranch = Yrs.youtputReadYxmlelem(childOutput);
                if (childBranch.equals(MemorySegment.NULL)) {
                    childBranch = Yrs.youtputReadYxmltext(childOutput);
                }
                Yrs.youtputDestroy(childOutput);

                // Compare branch pointers
                if (childBranch.equals(branchPtr)) {
                    return i;
                }
            }
        }
        return -1;  // Not found (shouldn't happen)
    }

    @Override
    public List<FormattingChunk> getFormattingChunks() {
        ensureNotClosed();
        // Formatting chunks require parsing the internal delta format
        throw new UnsupportedOperationException("Formatting chunks not yet implemented");
    }

    @Override
    public List<FormattingChunk> getFormattingChunks(YTransaction txn) {
        ensureNotClosed();
        throw new UnsupportedOperationException("Formatting chunks not yet implemented");
    }

    @Override
    public YSubscription observe(YObserver observer) {
        ensureNotClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        return new PanamaYXmlSubscription(this, observer, PanamaYXmlSubscription.Type.TEXT);
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
            throw new IllegalStateException("YXmlText has been closed");
        }
    }
}
