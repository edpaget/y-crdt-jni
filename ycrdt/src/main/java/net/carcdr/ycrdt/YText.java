package net.carcdr.ycrdt;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YText represents a collaborative text type in a Y-CRDT document.
 *
 * <p>YText provides efficient collaborative text editing with automatic conflict resolution.
 * Multiple users can edit the same text simultaneously, and changes will be merged
 * automatically using CRDT algorithms.</p>
 *
 * <p>This class implements {@link Closeable} and should be used with try-with-resources
 * to ensure proper cleanup of native resources.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc();
 *      YText text = doc.getText("mytext")) {
 *     text.insert(0, "Hello");
 *     text.push(" World");
 *     System.out.println(text.toString()); // "Hello World"
 * }
 * }</pre>
 *
 * @see YDoc
 */
public class YText implements Closeable, YObservable {

    private final YDoc doc;
    private long nativePtr;
    private volatile boolean closed = false;
    private final ConcurrentHashMap<Long, YObserver> observers = new ConcurrentHashMap<>();
    private final AtomicLong nextSubscriptionId = new AtomicLong(0);

    /**
     * Package-private constructor. Use {@link YDoc#getText(String)} to create instances.
     *
     * @param doc The parent YDoc instance
     * @param name The name of this text object in the document
     */
    YText(YDoc doc, String name) {
        if (doc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.doc = doc;
        this.nativePtr = nativeGetText(doc.getNativePtr(), name);
        if (this.nativePtr == 0) {
            throw new RuntimeException("Failed to create YText");
        }
    }

    /**
     * Returns the length of the text.
     *
     * @return The number of characters in the text
     * @throws IllegalStateException if the text has been closed
     */
    public int length() {
        checkClosed();
        return nativeLength(doc.getNativePtr(), nativePtr);
    }

    /**
     * Returns the text content as a string.
     *
     * @return The current text content
     * @throws IllegalStateException if the text has been closed
     */
    @Override
    public String toString() {
        checkClosed();
        return nativeToString(doc.getNativePtr(), nativePtr);
    }

    /**
     * Inserts text at the specified index.
     *
     * @param index The position at which to insert the text (0-based)
     * @param chunk The text to insert
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the text has been closed
     * @throws IndexOutOfBoundsException if index is negative or greater than the current length
     */
    public void insert(int index, String chunk) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for length " + length());
        }
        nativeInsert(doc.getNativePtr(), nativePtr, index, chunk);
    }

    /**
     * Appends text to the end.
     *
     * @param chunk The text to append
     * @throws IllegalArgumentException if chunk is null
     * @throws IllegalStateException if the text has been closed
     */
    public void push(String chunk) {
        checkClosed();
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        nativePush(doc.getNativePtr(), nativePtr, chunk);
    }

    /**
     * Deletes a range of text.
     *
     * @param index The starting position (0-based)
     * @param length The number of characters to delete
     * @throws IllegalStateException if the text has been closed
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void delete(int index, int length) {
        checkClosed();
        if (index < 0 || length < 0) {
            throw new IndexOutOfBoundsException(
                "Index and length must be non-negative");
        }
        int currentLength = length();
        if (index + length > currentLength) {
            throw new IndexOutOfBoundsException(
                "Range [" + index + ", " + (index + length) + ") out of bounds for length "
                + currentLength);
        }
        nativeDelete(doc.getNativePtr(), nativePtr, index, length);
    }

    /**
     * Checks if this YText has been closed.
     *
     * @return true if this YText has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Registers an observer to be notified of changes to this text.
     *
     * <p>The observer will be called whenever this text is modified.
     * Multiple observers can be registered on the same text.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (YSubscription sub = text.observe(event -> {
     *     System.out.println("Text changed!");
     *     for (YChange change : event.getChanges()) {
     *         System.out.println("  " + change);
     *     }
     * })) {
     *     text.insert(0, "Hello"); // Triggers observer
     * }
     * }</pre>
     *
     * @param observer the observer to register
     * @return a subscription handle that can be used to unobserve
     * @throws IllegalArgumentException if observer is null
     * @throws IllegalStateException if this text has been closed
     */
    public YSubscription observe(YObserver observer) {
        checkClosed();
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        long id = nextSubscriptionId.incrementAndGet();
        observers.put(id, observer);
        nativeObserve(doc.getNativePtr(), nativePtr, id, this);
        return new YSubscription(id, observer, this);
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
                nativeUnobserve(doc.getNativePtr(), nativePtr, subscriptionId);
            }
        }
    }

    /**
     * Package-private method called by JNI to dispatch events.
     *
     * @param subscriptionId the subscription ID
     * @param event the event to dispatch
     */
    void dispatchEvent(long subscriptionId, YEvent event) {
        YObserver observer = observers.get(subscriptionId);
        if (observer != null) {
            try {
                observer.onChange(event);
            } catch (Exception e) {
                System.err.println("Observer threw exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes this YText and releases native resources.
     *
     * <p>After calling this method, any operations on this YText will throw
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
                    // Clear all observers
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
     * Checks if this YText has been closed and throws an exception if it has.
     *
     * @throws IllegalStateException if this YText has been closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YText has been closed");
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
    private static native long nativeGetText(long docPtr, String name);
    private static native void nativeDestroy(long ptr);
    private static native int nativeLength(long docPtr, long textPtr);
    private static native String nativeToString(long docPtr, long textPtr);
    private static native void nativeInsert(long docPtr, long textPtr, int index, String chunk);
    private static native void nativePush(long docPtr, long textPtr, String chunk);
    private static native void nativeDelete(long docPtr, long textPtr, int index, int length);
    private static native void nativeObserve(long docPtr, long textPtr, long subscriptionId, YText ytextObj);
    private static native void nativeUnobserve(long docPtr, long textPtr, long subscriptionId);
}
