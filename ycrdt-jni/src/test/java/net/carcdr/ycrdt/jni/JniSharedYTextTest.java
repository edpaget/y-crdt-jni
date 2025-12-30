package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.AbstractYTextTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YText tests against the JNI implementation.
 */
public class JniSharedYTextTest extends AbstractYTextTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.jni();
    }
}
