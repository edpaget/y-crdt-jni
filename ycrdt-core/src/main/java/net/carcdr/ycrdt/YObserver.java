package net.carcdr.ycrdt;

/**
 * Functional interface for observing changes to Y-CRDT types.
 */
@FunctionalInterface
public interface YObserver {

    /**
     * Called when the observed Y-CRDT type changes.
     *
     * @param event the event describing the changes
     */
    void onChange(YEvent event);
}
