package net.carcdr.ycrdt;

/**
 * Functional interface for observing document-level updates.
 */
@FunctionalInterface
public interface UpdateObserver {

    /**
     * Called when the document is updated.
     *
     * @param update the encoded update that can be applied to other documents
     * @param origin the origin identifier for this update, or null if not specified
     */
    void onUpdate(byte[] update, String origin);
}
