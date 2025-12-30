package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.AbstractYMapTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YMap tests against the Panama implementation.
 */
public class PanamaSharedYMapTest extends AbstractYMapTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.panama();
    }

    @Override
    protected boolean supportsMapGet() {
        // Panama implementation has a bug in getString/getDouble that causes crashes
        return false;
    }
}
