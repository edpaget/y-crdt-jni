package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.FormattingChunk;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YTransaction;
import net.carcdr.ycrdt.YXmlText;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YXmlText represents a collaborative XML text type in a Y-CRDT document.
 *
 * <p>YXmlText provides collaborative text editing operations specifically designed for
 * XML documents. It supports insert, push, delete, and rich text formatting operations
 * with automatic conflict resolution.</p>
 *
 * <p>This class implements {@link Closeable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new JniYDoc();
 *      YXmlText xmlText = doc.getXmlText("myxmltext")) {
 *     xmlText.push("Hello");
 *     xmlText.push(" World");
 *     System.out.println(xmlText.toString()); // Hello World
 * }
 * }</pre>
 *
 * @see net.carcdr.ycrdt.YDoc
 */
public class JniYXmlText implements YXmlText, JniYObservable {

    private final JniYDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;
    private final ConcurrentHashMap<Long, YObserver> observers = new ConcurrentHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong(0);

    /**
     * Package-private constructor. Use {@link YDoc#getXmlText(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this XML text object in the document
     */
    JniYXmlText(JniYDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativePtr = nativeGetXmlText(doc.getNativePtr(), name);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YXmlText");
        }
    }

    /**
     * Package-private constructor that accepts a native handle directly.
     * Used for retrieving child text nodes from fragments.
     *
     * @param doc The parent YDoc instance
     * @param nativeHandle The native pointer to the XmlTextRef
     */
    JniYXmlText(JniYDoc doc, long nativeHandle) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Invalid native handle");
        }
        this.doc = doc;
        this.nativePtr = nativeHandle;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.TEXT;
    }

    /**
     * Returns the length of the text in characters.
     *
     * @return The text length
     * @throws IllegalStateException if the XML text has been closed
     */
    public int length() {
        checkClosed();
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeLengthWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr());
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeLengthWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        }
    }

    /**
     * Returns the length of the text in characters using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @return The text length
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML text has been closed
     */
    public int length(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return nativeLengthWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
    }

    /**
     * Returns the string representation of the XML text content.
     *
     * @return The text content as a string
     * @throws IllegalStateException if the XML text has been closed
     */
    @Override
    public String toString() {
        checkClosed();
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            String result = nativeToStringWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr());
            return result != null ? result : "";
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            String result = nativeToStringWithTxn(doc.getNativePtr(), nativePtr,
                ((JniYTransaction) txn).getNativePtr());
            return result != null ? result : "";
        }
    }

    /**
     * Returns the string representation of the XML text content using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @return The text content as a string
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML text has been closed
     */
    public String toString(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        String result = nativeToStringWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        return result != null ? result : "";
    }

    /**
     * Inserts text at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     xmlText.insert(txn, 0, "Hello");
     *     xmlText.insert(txn, 5, " World");
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index The index at which to insert the text (0-based)
     * @param chunk The text to insert
     * @throws IllegalArgumentException if txn or chunk is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public void insert(YTransaction txn, int index, String chunk) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        nativeInsertWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), index, chunk);
    }

    /**
     * Inserts text at the specified index.
     *
     * @param index The index at which to insert the text (0-based)
     * @param chunk The text to insert
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public void insert(int index, String chunk) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeInsertWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(),
                index, chunk);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeInsertWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(),
                    index, chunk);
            }
        }
    }

    /**
     * Appends text to the end of the XML text within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     xmlText.push(txn, "Hello");
     *     xmlText.push(txn, " World");
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param chunk The text to append
     * @throws IllegalArgumentException if txn or chunk is null
     * @throws IllegalStateException if the XML text has been closed
     */
    public void push(YTransaction txn, String chunk) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        nativePushWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), chunk);
    }

    /**
     * Appends text to the end of the XML text.
     *
     * @param chunk The text to append
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the XML text has been closed
     */
    public void push(String chunk) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativePushWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(), chunk);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativePushWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), chunk);
            }
        }
    }

    /**
     * Deletes a range of text within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     xmlText.delete(txn, 0, 5);
     *     xmlText.delete(txn, 0, 3);
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index The starting index of the deletion (0-based)
     * @param length The number of characters to delete
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index or length is negative
     */
    public void delete(YTransaction txn, int index, int length) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
        }
        nativeDeleteWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), index, length);
    }

    /**
     * Deletes a range of text.
     *
     * @param index The starting index of the deletion (0-based)
     * @param length The number of characters to delete
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index or length is negative
     */
    public void delete(int index, int length) {
        checkClosed();
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
        }
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeDeleteWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(),
                index, length);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeDeleteWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(),
                    index, length);
            }
        }
    }

    /**
     * Inserts text with formatting attributes at the specified index within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     Map<String, Object> bold = Map.of("b", true);
     *     xmlText.insertWithAttributes(txn, 0, "Hello", bold);
     *     xmlText.insertWithAttributes(txn, 5, " World", bold);
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index The index at which to insert the text (0-based)
     * @param chunk The text to insert
     * @param attributes A map of formatting attributes to apply to the text
     * @throws IllegalArgumentException if txn, chunk or attributes is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public void insertWithAttributes(YTransaction txn, int index, String chunk,
                                      Map<String, Object> attributes) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        nativeInsertWithAttributesWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(),
            index, chunk, attributes);
    }

    /**
     * Inserts text with formatting attributes at the specified index.
     *
     * <p>The attributes map can contain formatting information such as:</p>
     * <ul>
     *   <li>Bold: {@code Map.of("b", true)}</li>
     *   <li>Italic: {@code Map.of("i", true)}</li>
     *   <li>Font: {@code Map.of("font", "Arial")}</li>
     *   <li>Color: {@code Map.of("color", "#FF0000")}</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Map<String, Object> bold = Map.of("b", true);
     * xmlText.insertWithAttributes(0, "Hello", bold);
     * }</pre>
     *
     * @param index The index at which to insert the text (0-based)
     * @param chunk The text to insert
     * @param attributes A map of formatting attributes to apply to the text
     * @throws IllegalArgumentException if chunk or attributes is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index is negative
     */
    public void insertWithAttributes(int index, String chunk, Map<String, Object> attributes) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeInsertWithAttributesWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr(), index, chunk, attributes);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeInsertWithAttributesWithTxn(doc.getNativePtr(), nativePtr,
                    ((JniYTransaction) txn).getNativePtr(), index, chunk, attributes);
            }
        }
    }

    /**
     * Formats a range of text with the specified attributes within an existing transaction.
     *
     * <p>Use this method to batch multiple operations:
     * <pre>{@code
     * try (JniYTransaction txn = doc.beginTransaction()) {
     *     Map<String, Object> bold = Map.of("b", true);
     *     xmlText.format(txn, 0, 5, bold);
     *     xmlText.format(txn, 6, 5, bold);
     * }
     * }</pre>
     *
     * @param txn Transaction handle
     * @param index The starting index of the range to format (0-based)
     * @param length The number of characters to format
     * @param attributes A map of formatting attributes to apply
     * @throws IllegalArgumentException if txn or attributes is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index or length is negative
     */
    public void format(YTransaction txn, int index, int length, Map<String, Object> attributes) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
        }
        nativeFormatWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(), index, length,
            attributes);
    }

    /**
     * Formats a range of text with the specified attributes.
     *
     * <p>This method applies formatting to existing text. The attributes map can contain
     * formatting information such as:</p>
     * <ul>
     *   <li>Bold: {@code Map.of("b", true)}</li>
     *   <li>Italic: {@code Map.of("i", true)}</li>
     *   <li>Underline: {@code Map.of("u", true)}</li>
     * </ul>
     *
     * <p>To remove formatting, pass {@code null} as the attribute value:</p>
     * <pre>{@code
     * Map<String, Object> removeBold = Map.of("b", null);
     * xmlText.format(0, 5, removeBold);
     * }</pre>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * xmlText.insert(0, "Hello World");
     * Map<String, Object> bold = Map.of("b", true);
     * xmlText.format(0, 5, bold); // Makes "Hello" bold
     * }</pre>
     *
     * @param index The starting index of the range to format (0-based)
     * @param length The number of characters to format
     * @param attributes A map of formatting attributes to apply
     * @throws IllegalArgumentException if attributes is null
     * @throws IllegalStateException if the XML text has been closed
     * @throws IndexOutOfBoundsException if index or length is negative
     */
    public void format(int index, int length, Map<String, Object> attributes) {
        checkClosed();
        if (attributes == null) {
            throw new IllegalArgumentException("Attributes cannot be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
        }
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            nativeFormatWithTxn(doc.getNativePtr(), nativePtr, activeTxn.getNativePtr(),
                index, length, attributes);
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                nativeFormatWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr(),
                    index, length, attributes);
            }
        }
    }

    /**
     * Gets the parent of this XML text node.
     * The parent can be either a YXmlElement or YXmlFragment.
     *
     * @return The parent node (YXmlElement or YXmlFragment), or null if this text node has no parent
     * @throws IllegalStateException if the XML text has been closed
     */
    public Object getParent() {
        checkClosed();
        JniYTransaction activeTxn = doc.getActiveTransaction();
        Object result;
        if (activeTxn != null) {
            result = nativeGetParentWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr());
        } else {
            try (JniYTransaction txn = doc.beginTransaction()) {
                result = nativeGetParentWithTxn(doc.getNativePtr(), nativePtr,
                    ((JniYTransaction) txn).getNativePtr());
            }
        }
        if (result == null) {
            return null;
        }

        // Result is Object[2] where [0] = Integer type, [1] = Long pointer
        Object[] array = (Object[]) result;
        int type = ((Integer) array[0]).intValue();
        long pointer = ((Long) array[1]).longValue();

        if (type == 0) {
            // Element
            return new JniYXmlElement(doc, pointer);
        } else if (type == 1) {
            // Fragment
            return new JniYXmlFragment(doc, pointer);
        } else {
            throw new RuntimeException("Unknown parent type: " + type);
        }
    }

    /**
     * Gets the parent of this XML text node using an existing transaction.
     * The parent can be either a YXmlElement or YXmlFragment.
     *
     * @param txn The transaction to use for this operation
     * @return The parent node (YXmlElement or YXmlFragment), or null if this text node has no parent
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML text has been closed
     */
    public Object getParent(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        Object result = nativeGetParentWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
        if (result == null) {
            return null;
        }

        // Result is Object[2] where [0] = Integer type, [1] = Long pointer
        Object[] array = (Object[]) result;
        int type = ((Integer) array[0]).intValue();
        long pointer = ((Long) array[1]).longValue();

        if (type == 0) {
            // Element
            return new JniYXmlElement(doc, pointer);
        } else if (type == 1) {
            // Fragment
            return new JniYXmlFragment(doc, pointer);
        } else {
            throw new RuntimeException("Unknown parent type: " + type);
        }
    }

    /**
     * Gets the index of this text node within its parent's children.
     *
     * @return The 0-based index within parent, or -1 if this text node has no parent
     * @throws IllegalStateException if the XML text has been closed
     */
    public int getIndexInParent() {
        checkClosed();
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeGetIndexInParentWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr());
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeGetIndexInParentWithTxn(doc.getNativePtr(), nativePtr,
                ((JniYTransaction) txn).getNativePtr());
        }
    }

    /**
     * Gets the index of this text node within its parent's children using an existing transaction.
     *
     * @param txn The transaction to use for this operation
     * @return The 0-based index within parent, or -1 if this text node has no parent
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML text has been closed
     */
    public int getIndexInParent(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return nativeGetIndexInParentWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
    }

    /**
     * Returns the formatted text as a list of chunks with their formatting attributes.
     *
     * <p>This method retrieves the complete delta representation of the text, where each
     * chunk contains text content and its associated formatting attributes. This is useful
     * for converting Y-CRDT formatted text to other rich text formats like ProseMirror.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * try (YDoc doc = new JniYDoc();
     *      YXmlText text = doc.getXmlText("mytext")) {
     *     text.insertWithAttributes(0, "Hello", Map.of("bold", true));
     *     text.insert(5, " World");
     *
     *     List<FormattingChunk> chunks = text.getFormattingChunks();
     *     for (FormattingChunk chunk : chunks) {
     *         System.out.println("Text: " + chunk.getText());
     *         if (chunk.hasAttributes()) {
     *             System.out.println("Attributes: " + chunk.getAttributes());
     *         }
     *     }
     * }
     * }</pre>
     *
     * @return a list of formatting chunks representing the text and its formatting
     * @throws IllegalStateException if the XML text has been closed
     * @see FormattingChunk
     */
    public List<FormattingChunk> getFormattingChunks() {
        checkClosed();
        JniYTransaction activeTxn = doc.getActiveTransaction();
        if (activeTxn != null) {
            return nativeGetFormattingChunksWithTxn(doc.getNativePtr(), nativePtr,
                activeTxn.getNativePtr());
        }
        try (JniYTransaction txn = doc.beginTransaction()) {
            return nativeGetFormattingChunksWithTxn(doc.getNativePtr(), nativePtr,
                ((JniYTransaction) txn).getNativePtr());
        }
    }

    /**
     * Returns the formatted text as a list of chunks with their formatting attributes
     * using an existing transaction.
     *
     * <p>This method retrieves the complete delta representation of the text, where each
     * chunk contains text content and its associated formatting attributes.</p>
     *
     * @param txn The transaction to use for this operation
     * @return a list of formatting chunks representing the text and its formatting
     * @throws IllegalArgumentException if txn is null
     * @throws IllegalStateException if the XML text has been closed
     * @see FormattingChunk
     */
    public List<FormattingChunk> getFormattingChunks(YTransaction txn) {
        checkClosed();
        if (txn == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return nativeGetFormattingChunksWithTxn(doc.getNativePtr(), nativePtr, ((JniYTransaction) txn).getNativePtr());
    }

    /**
     * Checks if this YXmlText has been closed.
     *
     * @return true if this YXmlText has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Registers an observer to be notified of changes to this XML text.
     *
     * <p>The observer will be called whenever this XML text is modified.
     * Multiple observers can be registered on the same XML text.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YSubscription sub = xmlText.observe(event -> {
     *     System.out.println("XML text changed!");
     *     for (YChange change : event.getChanges()) {
     *         System.out.println("  " + change);
     *     }
     * })) {
     *     xmlText.insert(0, "Hello"); // Triggers observer
     * }
     * }</pre>
     *
     * @param observer the observer to register
     * @return a subscription handle that can be used to unobserve
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this XML text has been closed
     */
    public YSubscription observe(YObserver observer) {
        checkClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        long id = nextSubscriptionId.incrementAndGet();
        observers.put(id, observer);
        nativeObserve(doc.getNativePtr(), nativePtr, id, this);
        return new JniYSubscription(id, observer, this);
    }

    /**
     * Package-private method to unobserve by subscription ID.
     * Called by YSubscription.close().
     *
     * @param subscriptionId the subscription ID to remove
     */
    @Override
    public void unobserveById(long subscriptionId) {
        if (observers.remove(subscriptionId) != null) {
            if (!closed && nativePtr != 0) {
                doc.deferNativeUnsubscribe(subscriptionId);
            }
        }
    }

    /**
     * Package-private method called by JNI to dispatch events.
     *
     * @param subscriptionId the subscription ID
     * @param event the event to dispatch
     */
    void dispatchEvent(long subscriptionId, JniYEvent event) {
        YObserver observer = observers.get(subscriptionId);
        if (observer != null) {
            try {
                observer.onChange(event);
            } catch (Exception e) {
                doc.getObserverErrorHandler().handleError(e, this);
            }
        }
    }

    /**
     * Closes this YXmlText and releases native resources.
     *
     * <p>After calling this method, any operations on this YXmlText will throw
     * {@link IllegalStateException}.</p>
     *
     * <p>This method is idempotent - calling it multiple times has no effect
     * after the first call.</p>
     */
    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    // Defer unregistration of all observers
                    for (Long subscriptionId : observers.keySet()) {
                        if (nativePtr != 0) {
                            doc.deferNativeUnsubscribe(subscriptionId);
                        }
                    }
                    observers.clear();
                    if (nativePtr != 0) {
                        nativeDestroy(nativePtr);
                        nativePtr = 0;
                    }
                    closed = true;
                }
            }
        }
    }

    /**
     * Checks if this YXmlText has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this YXmlText has been closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YXmlText has been closed");
        }
    }

    /**
     * Gets the native pointer for internal use.
     *
     * @return The native pointer value
     */
    long getNativePtr() {
        return nativePtr;
    }

    // Native methods
    private static native long nativeGetXmlText(long docPtr, String name);
    private static native void nativeDestroy(long ptr);
    private static native int nativeLengthWithTxn(long docPtr, long xmlTextPtr, long txnPtr);
    private static native String nativeToStringWithTxn(long docPtr, long xmlTextPtr, long txnPtr);
    private static native void nativeInsertWithTxn(long docPtr, long xmlTextPtr, long txnPtr,
                                                     int index, String chunk);
    private static native void nativePushWithTxn(long docPtr, long xmlTextPtr, long txnPtr,
                                                   String chunk);
    private static native void nativeDeleteWithTxn(long docPtr, long xmlTextPtr, long txnPtr,
                                                     int index, int length);
    private static native void nativeInsertWithAttributesWithTxn(
            long docPtr, long xmlTextPtr, long txnPtr, int index, String chunk,
            Map<String, Object> attributes);
    private static native void nativeFormatWithTxn(
            long docPtr, long xmlTextPtr, long txnPtr, int index, int length,
            Map<String, Object> attributes);
    private static native Object nativeGetParentWithTxn(long docPtr, long xmlTextPtr, long txnPtr);
    private static native int nativeGetIndexInParentWithTxn(long docPtr, long xmlTextPtr,
                                                             long txnPtr);
    private static native void nativeObserve(long docPtr, long xmlTextPtr, long subscriptionId,
                                              YXmlText yxmlTextObj);
    private static native void nativeUnobserve(long docPtr, long xmlTextPtr, long subscriptionId);
    private static native List<FormattingChunk> nativeGetFormattingChunksWithTxn(
            long docPtr, long xmlTextPtr, long txnPtr);
}
