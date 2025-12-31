package net.carcdr.ycrdt.jni;

import java.lang.ref.Cleaner;

/**
 * Shared Cleaner instance for cleaning up native resources in JNI classes.
 *
 * <p>This class provides a single shared {@link Cleaner} instance that all JNI
 * classes can use to register cleanup actions for their native pointers.</p>
 *
 * <p>Using a shared Cleaner is more efficient than creating individual Cleaners
 * per class, as it reduces thread overhead.</p>
 */
final class NativeCleaner {

    /**
     * The shared Cleaner instance.
     */
    static final Cleaner CLEANER = Cleaner.create();

    /**
     * Private constructor to prevent instantiation.
     */
    private NativeCleaner() {
    }
}
