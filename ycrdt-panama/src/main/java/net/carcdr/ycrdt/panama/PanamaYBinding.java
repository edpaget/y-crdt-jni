package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YDoc;

/**
 * Panama FFM implementation of the YBinding factory.
 *
 * <p>Creates Y-CRDT documents using Project Panama's Foreign Function and Memory API
 * to call the yffi native library.</p>
 */
public final class PanamaYBinding implements YBinding {

    /**
     * Creates a new PanamaYBinding instance.
     */
    public PanamaYBinding() {
        // Library loading happens lazily when first native call is made
    }

    @Override
    public YDoc createDoc() {
        return new PanamaYDoc();
    }

    @Override
    public YDoc createDoc(long clientId) {
        return new PanamaYDoc(clientId);
    }

    @Override
    public byte[] mergeUpdates(byte[][] updates) {
        if (updates == null || updates.length == 0) {
            throw new IllegalArgumentException("Updates array cannot be null or empty");
        }
        // TODO: Implement using ymerge_updates_v1
        throw new UnsupportedOperationException("mergeUpdates not yet implemented");
    }

    @Override
    public byte[] encodeStateVectorFromUpdate(byte[] update) {
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }
        // TODO: Implement using yencode_state_vector_from_update_v1
        throw new UnsupportedOperationException("encodeStateVectorFromUpdate not yet implemented");
    }
}
