package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.AbstractYMapTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YMap tests against the JNI implementation.
 */
public class JniSharedYMapTest extends AbstractYMapTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.jni();
    }
}
