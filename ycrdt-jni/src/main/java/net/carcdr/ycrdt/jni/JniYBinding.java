package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YDoc;

/**
 * JNI-based implementation of the YBinding factory.
 * Creates Y-CRDT documents using native Rust code via JNI.
 */
public final class JniYBinding implements YBinding {

    /**
     * Creates a new JniYBinding instance.
     * Ensures the native library is loaded.
     */
    public JniYBinding() {
        NativeLoader.loadLibrary();
    }

    @Override
    public YDoc createDoc() {
        return new JniYDoc();
    }

    @Override
    public YDoc createDoc(long clientId) {
        return new JniYDoc(clientId);
    }

    @Override
    public byte[] mergeUpdates(byte[][] updates) {
        return JniYDoc.mergeUpdates(updates);
    }

    @Override
    public byte[] encodeStateVectorFromUpdate(byte[] update) {
        return JniYDoc.encodeStateVectorFromUpdate(update);
    }
}
