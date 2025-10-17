package net.carcdr.ycrdt;

/**
 * Functional interface for observing document-level updates.
 *
 * <p>UpdateObservers are notified whenever the document receives an update,
 * regardless of which Y type was modified. This is useful for tracking all
 * changes to a document for persistence, synchronization, or logging.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc()) {
 *     UpdateObserver observer = (update, origin) -> {
 *         System.out.println("Document updated: " + update.length + " bytes");
 *         if (origin != null) {
 *             System.out.println("Origin: " + origin);
 *         }
 *         // Persist update, send to remote peers, etc.
 *     };
 *
 *     try (YSubscription sub = doc.observeUpdateV1(observer)) {
 *         try (YText text = doc.getText("mytext")) {
 *             text.insert(0, "Hello"); // Triggers observer
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Observers are called synchronously on the thread
 * that modifies the document. Implementations should be thread-safe or perform
 * minimal work to avoid blocking document operations.</p>
 *
 * <p><b>Exception Handling:</b> Exceptions thrown by observers are caught
 * and logged but do not prevent other observers from being notified or prevent
 * the update from being applied.</p>
 *
 * <p><b>Reentrancy:</b> Observers should NOT modify the same document that
 * triggered the callback, as this may cause undefined behavior or deadlocks.
 * If you need to modify the document in response to changes, schedule the
 * modification asynchronously.</p>
 *
 * @see YDoc#observeUpdateV1(UpdateObserver)
 * @see YObserver
 */
@FunctionalInterface
public interface UpdateObserver {

    /**
     * Called when the document receives an update.
     *
     * <p>The update parameter contains the binary-encoded changes that were
     * applied to the document. This can be used for persistence, broadcasting
     * to remote peers, or logging.</p>
     *
     * <p>The origin parameter is an optional string identifier that can be
     * used to distinguish between different sources of changes (e.g., "local",
     * "remote", "undo"). This is set by the transaction that created the change.</p>
     *
     * @param update the binary-encoded update that was applied
     * @param origin optional origin identifier, may be null
     */
    void onUpdate(byte[] update, String origin);
}
