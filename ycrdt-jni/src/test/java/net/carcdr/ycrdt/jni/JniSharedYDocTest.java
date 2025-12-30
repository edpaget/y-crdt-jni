package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.AbstractYDocTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YDoc tests against the JNI implementation.
 */
public class JniSharedYDocTest extends AbstractYDocTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.jni();
    }
}
