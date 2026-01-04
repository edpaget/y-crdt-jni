package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlText;
import net.carcdr.ycrdt.panama.ffi.Yrs;

/**
 * Panama FFM implementation of YXmlElement.
 *
 * <p>Represents an XML element with a tag name, attributes, and children.</p>
 */
public class PanamaYXmlElement implements YXmlElement {

    private final PanamaYDoc doc;
    private final MemorySegment branchPtr;
    private volatile boolean closed = false;

    /**
     * Creates a new YXmlElement as a top-level element from a document.
     *
     * @param doc the document
     * @param name the name of the element in the document
     */
    PanamaYXmlElement(PanamaYDoc doc, String name) {
        this.doc = doc;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            // Use yxmlfragment to create a top-level XML container
            // Note: yffi doesn't have a direct yxmlelement function for top-level
            // We create it via fragment and then insert an element
            MemorySegment fragmentPtr = Yrs.yxmlfragment(doc.getDocPtr(), namePtr);
            if (fragmentPtr.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Failed to create XML container");
            }
            // For named elements, we treat the fragment as the element container
            this.branchPtr = fragmentPtr;
        }
    }

    /**
     * Creates a YXmlElement from an existing branch pointer.
     *
     * @param doc the document
     * @param branchPtr pointer to the xml element branch
     */
    PanamaYXmlElement(PanamaYDoc doc, MemorySegment branchPtr) {
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
    public NodeType getNodeType() {
        return NodeType.ELEMENT;
    }

    @Override
    public String getTag() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getTag(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getTag(txn);
        }
    }

    @Override
    public String getTag(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        MemorySegment tagPtr = Yrs.yxmlelemTag(branchPtr);
        if (tagPtr.equals(MemorySegment.NULL)) {
            return "";
        }
        return Yrs.readAndFreeString(tagPtr);
    }

    @Override
    public String getAttribute(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getAttribute(activeTxn, name);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getAttribute(txn, name);
        }
    }

    @Override
    public String getAttribute(YTransaction txn, String name) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            MemorySegment valuePtr = Yrs.yxmlelemGetAttr(branchPtr, ptxn.getTxnPtr(), namePtr);
            if (valuePtr.equals(MemorySegment.NULL)) {
                return null;
            }
            return Yrs.readAndFreeString(valuePtr);
        }
    }

    @Override
    public void setAttribute(String name, String value) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            setAttribute(activeTxn, name, value);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                setAttribute(txn, name, value);
            }
        }
    }

    @Override
    public void setAttribute(YTransaction txn, String name, String value) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            MemorySegment valuePtr = Yrs.createString(arena, value);
            Yrs.yxmlelemInsertAttr(branchPtr, ptxn.getTxnPtr(), namePtr, valuePtr);
        }
    }

    @Override
    public void removeAttribute(String name) {
        ensureNotClosed();
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            removeAttribute(activeTxn, name);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                removeAttribute(txn, name);
            }
        }
    }

    @Override
    public void removeAttribute(YTransaction txn, String name) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = Yrs.createString(arena, name);
            Yrs.yxmlelemRemoveAttr(branchPtr, ptxn.getTxnPtr(), namePtr);
        }
    }

    @Override
    public String[] getAttributeNames() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getAttributeNames(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getAttributeNames(txn);
        }
    }

    @Override
    public String[] getAttributeNames(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        List<String> names = new ArrayList<>();
        MemorySegment iter = Yrs.yxmlelemAttrIter(branchPtr, ptxn.getTxnPtr());
        if (iter.equals(MemorySegment.NULL)) {
            return new String[0];
        }
        try {
            while (true) {
                MemorySegment attr = Yrs.yxmlattrIterNext(iter);
                if (attr.equals(MemorySegment.NULL)) {
                    break;
                }
                try {
                    String name = Yrs.yxmlattrReadName(attr);
                    if (name != null) {
                        names.add(name);
                    }
                } finally {
                    Yrs.yxmlattrDestroy(attr);
                }
            }
        } finally {
            Yrs.yxmlattrIterDestroy(iter);
        }
        return names.toArray(new String[0]);
    }

    @Override
    public int childCount() {
        ensureNotClosed();
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return childCount(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return childCount(txn);
        }
    }

    @Override
    public int childCount(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        return Yrs.yxmlelemChildLen(branchPtr, ptxn.getTxnPtr());
    }

    @Override
    public YXmlElement insertElement(int index, String tag) {
        ensureNotClosed();
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return insertElement(activeTxn, index, tag);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return insertElement(txn, index, tag);
        }
    }

    @Override
    public YXmlElement insertElement(YTransaction txn, int index, String tag) {
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
            return new PanamaYXmlElement(doc, childPtr);
        }
    }

    @Override
    public YXmlText insertText(int index) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return insertText(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return insertText(txn, index);
        }
    }

    @Override
    public YXmlText insertText(YTransaction txn, int index) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        MemorySegment childPtr = Yrs.yxmlelemInsertText(branchPtr, ptxn.getTxnPtr(), index);
        if (childPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to insert text child");
        }
        return new PanamaYXmlText(doc, childPtr);
    }

    @Override
    public Object getChild(int index) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getChild(activeTxn, index);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getChild(txn, index);
        }
    }

    @Override
    public Object getChild(YTransaction txn, int index) {
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
    public void removeChild(int index) {
        ensureNotClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            removeChild(activeTxn, index);
        } else {
            try (PanamaYTransaction txn = doc.beginTransaction()) {
                removeChild(txn, index);
            }
        }
    }

    @Override
    public void removeChild(YTransaction txn, int index) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        Yrs.yxmlelemRemoveRange(branchPtr, ptxn.getTxnPtr(), index, 1);
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
            return null;  // Root-level element has no parent
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
        MemorySegment strPtr = Yrs.yxmlelemString(branchPtr, ptxn.getTxnPtr());
        if (strPtr.equals(MemorySegment.NULL)) {
            return "";
        }
        return Yrs.readAndFreeString(strPtr);
    }

    @Override
    public YSubscription observe(YObserver observer) {
        ensureNotClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        // Observers for XML elements require complex upcall stubs - deferred
        throw new UnsupportedOperationException("XML element observers not yet implemented");
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
            throw new IllegalStateException("YXmlElement has been closed");
        }
    }
}
