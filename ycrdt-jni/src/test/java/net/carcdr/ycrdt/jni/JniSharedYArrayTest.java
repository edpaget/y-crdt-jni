package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.AbstractYArrayTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YArray tests against the JNI implementation.
 */
public class JniSharedYArrayTest extends AbstractYArrayTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.jni();
    }
}
