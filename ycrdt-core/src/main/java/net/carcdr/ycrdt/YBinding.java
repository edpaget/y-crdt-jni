package net.carcdr.ycrdt;

import java.util.ServiceLoader;

/**
 * Factory for creating Y-CRDT documents.
 * Implementations are discovered via ServiceLoader.
 */
public interface YBinding {

    /**
     * Creates a new document with a random client ID.
     *
     * @return the new document
     */
    YDoc createDoc();

    /**
     * Creates a new document with the specified client ID.
     *
     * @param clientId the client ID
     * @return the new document
     */
    YDoc createDoc(long clientId);

    /**
     * Merges multiple updates into a single update.
     *
     * @param updates the updates to merge
     * @return the merged update
     */
    byte[] mergeUpdates(byte[][] updates);

    /**
     * Extracts a state vector from an encoded update.
     *
     * @param update the encoded update
     * @return the state vector
     */
    byte[] encodeStateVectorFromUpdate(byte[] update);

    /**
     * Returns the default binding discovered via ServiceLoader.
     *
     * @return the binding instance
     * @throws IllegalStateException if no binding is found
     */
    static YBinding getInstance() {
        return ServiceLoader.load(YBinding.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No YBinding implementation found. "
                + "Add ycrdt-jni or ycrdt-panama to your classpath."));
    }
}
