package net.carcdr.ycrdt;

/**
 * Functional interface for observing changes to Y-CRDT data structures.
 *
 * <p>Observers are notified whenever the observed Y type is modified.
 * Changes are described by {@link YEvent} objects containing detailed
 * delta information.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (YDoc doc = new YDoc();
 *      YText text = doc.getText("mytext")) {
 *
 *     YObserver observer = event -> {
 *         System.out.println("Text changed!");
 *         for (YChange change : event.getChanges()) {
 *             System.out.println("  " + change);
 *         }
 *     };
 *
 *     try (YSubscription sub = text.observe(observer)) {
 *         text.insert(0, "Hello"); // Triggers observer
 *     }
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Observers may be called from different threads.
 * Implementations should be thread-safe or perform synchronization.</p>
 *
 * <p><b>Exception Handling:</b> Exceptions thrown by observers are caught
 * and logged but do not prevent other observers from being notified.</p>
 *
 * @see YEvent
 * @see YChange
 */
@FunctionalInterface
public interface YObserver {

    /**
     * Called when the observed Y type is modified.
     *
     * @param event the event describing the change
     */
    void onChange(YEvent event);
}
