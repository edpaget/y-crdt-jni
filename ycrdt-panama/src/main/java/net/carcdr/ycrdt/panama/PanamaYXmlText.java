package net.carcdr.ycrdt.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashMap;
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
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment strPtr = Yrs.createString(arena, chunk);
            MemorySegment attrsPtr = (attributes == null || attributes.isEmpty())
                    ? MemorySegment.NULL
                    : Yrs.yinputMap(arena, attributes);
            Yrs.yxmltextInsert(branchPtr, ptxn.getTxnPtr(), index, strPtr, attrsPtr);
        }
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
        if (attributes == null || attributes.isEmpty()) {
            return; // No-op if no attributes to apply
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment attrsPtr = Yrs.yinputMap(arena, attributes);
            Yrs.yxmltextFormat(branchPtr, ptxn.getTxnPtr(), index, length, attrsPtr);
        }
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
        PanamaYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return getFormattingChunks(activeTxn);
        }
        try (PanamaYTransaction txn = doc.beginTransaction()) {
            return getFormattingChunks(txn);
        }
    }

    @Override
    public List<FormattingChunk> getFormattingChunks(YTransaction txn) {
        ensureNotClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        PanamaYTransaction ptxn = (PanamaYTransaction) txn;
        List<FormattingChunk> result = new ArrayList<>();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lenPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment chunksPtr = Yrs.ytextChunks(branchPtr, ptxn.getTxnPtr(), lenPtr);
            int chunksLen = lenPtr.get(ValueLayout.JAVA_INT, 0);

            if (chunksPtr.address() == 0 || chunksLen == 0) {
                return result;
            }

            try {
                long chunkSize = Yrs.YCHUNK_LAYOUT.byteSize();
                MemorySegment chunksArray = chunksPtr.reinterpret(chunkSize * chunksLen);

                for (int i = 0; i < chunksLen; i++) {
                    MemorySegment chunk = chunksArray.asSlice(i * chunkSize, chunkSize);
                    MemorySegment dataOutput = Yrs.ychunkGetData(chunk);
                    byte tag = Yrs.youtputGetTag(dataOutput);

                    // Only process string chunks (tag == -5)
                    // Non-string chunks (XML elements, etc.) are skipped
                    if (tag == Yrs.YOUTPUT_TAG_STRING) {
                        String text = readChunkText(chunk, dataOutput);
                        Map<String, Object> attributes = readChunkAttributes(chunk);
                        result.add(new PanamaFormattingChunk(text, attributes));
                    }
                }
            } finally {
                Yrs.ychunksDestroy(chunksPtr, chunksLen);
            }
        }
        return result;
    }

    /**
     * Reads the text content from a YChunk.
     *
     * @param chunk the YChunk memory segment
     * @param dataOutput the pre-extracted YOutput data segment
     */
    private String readChunkText(MemorySegment chunk, MemorySegment dataOutput) {
        MemorySegment strPtr = Yrs.youtputReadString(dataOutput);
        if (strPtr.address() == 0) {
            return "";
        }
        // Read the string (owned by chunk, will be freed by ychunks_destroy)
        return strPtr.reinterpret(Long.MAX_VALUE).getString(0);
    }

    /**
     * Reads formatting attributes from a YChunk.
     *
     * <p>Note: For YXmlText, formatting is typically expressed via XML elements
     * rather than inline attributes. String chunks within YXmlText may not have
     * formatting attributes.</p>
     */
    private Map<String, Object> readChunkAttributes(MemorySegment chunk) {
        // Use accessor methods to get formatting info
        int fmtLen = Yrs.ychunkGetFmtLen(chunk);
        if (fmtLen <= 0 || fmtLen > 1000) {
            // Safety check: no attributes or invalid count
            return null;
        }

        MemorySegment fmtPtr = Yrs.ychunkGetFmt(chunk);
        if (fmtPtr.address() == 0) {
            return null;
        }

        try {
            Map<String, Object> attrs = new HashMap<>();
            long entrySize = Yrs.YCHUNK_FMT_ENTRY_LAYOUT.byteSize();
            MemorySegment fmtArray = fmtPtr.reinterpret(entrySize * fmtLen);

            for (int j = 0; j < fmtLen; j++) {
                MemorySegment entry = fmtArray.asSlice(j * entrySize, entrySize);
                String key = Yrs.ychunkFmtEntryReadKey(entry);
                if (key != null) {
                    Object value = readYOutputValue(Yrs.ychunkFmtEntryGetValue(entry));
                    attrs.put(key, value);
                }
            }
            return attrs.isEmpty() ? null : attrs;
        } catch (Exception e) {
            // Safety: if reading fails, return null attributes
            return null;
        }
    }

    /**
     * Reads a value from a YOutput struct, trying different types.
     */
    private Object readYOutputValue(MemorySegment output) {
        // Try boolean first
        MemorySegment boolPtr = Yrs.youtputReadBool(output);
        if (boolPtr.address() != 0) {
            return boolPtr.reinterpret(1).get(ValueLayout.JAVA_BYTE, 0) != 0;
        }

        // Try string
        MemorySegment strPtr = Yrs.youtputReadString(output);
        if (strPtr.address() != 0) {
            return strPtr.reinterpret(Long.MAX_VALUE).getString(0);
        }

        // Try double
        MemorySegment doublePtr = Yrs.youtputReadFloat(output);
        if (doublePtr.address() != 0) {
            return doublePtr.reinterpret(8).get(ValueLayout.JAVA_DOUBLE, 0);
        }

        // Try long
        MemorySegment longPtr = Yrs.youtputReadLong(output);
        if (longPtr.address() != 0) {
            return longPtr.reinterpret(8).get(ValueLayout.JAVA_LONG, 0);
        }

        return null;
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
