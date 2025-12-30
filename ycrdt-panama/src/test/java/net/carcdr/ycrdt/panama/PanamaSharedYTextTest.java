package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.AbstractYTextTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YText tests against the Panama implementation.
 */
public class PanamaSharedYTextTest extends AbstractYTextTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.panama();
    }
}
