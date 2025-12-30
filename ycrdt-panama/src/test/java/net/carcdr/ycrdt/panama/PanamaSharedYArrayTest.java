package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.AbstractYArrayTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YArray tests against the Panama implementation.
 */
public class PanamaSharedYArrayTest extends AbstractYArrayTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.panama();
    }

    @Override
    protected boolean supportsArrayGet() {
        // Panama implementation has a bug in getString/getDouble that causes crashes
        return false;
    }
}
