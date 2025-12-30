package net.carcdr.ycrdt.panama;

import net.carcdr.ycrdt.AbstractYDocTest;
import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YBindingFactory;

/**
 * Runs the shared YDoc tests against the Panama implementation.
 */
public class PanamaSharedYDocTest extends AbstractYDocTest {

    @Override
    protected YBinding getBinding() {
        return YBindingFactory.panama();
    }

    @Override
    protected boolean supportsCustomClientId() {
        // Panama implementation doesn't yet support ydoc_new_with_options
        return false;
    }
}
