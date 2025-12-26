package net.carcdr.ycrdt;

import java.util.ServiceLoader;

/**
 * Factory for explicitly selecting a Y-CRDT binding implementation.
 */
public final class YBindingFactory {

    private YBindingFactory() {
    }

    /**
     * Returns the default binding discovered via ServiceLoader.
     *
     * @return the binding instance
     * @throws IllegalStateException if no binding is found
     */
    public static YBinding auto() {
        return YBinding.getInstance();
    }

    /**
     * Returns the JNI binding implementation.
     *
     * @return the JNI binding
     * @throws IllegalStateException if the JNI binding is not available
     */
    public static YBinding jni() {
        return ServiceLoader.load(YBinding.class).stream()
            .filter(p -> p.type().getName().contains(".jni."))
            .findFirst()
            .map(ServiceLoader.Provider::get)
            .orElseThrow(() -> new IllegalStateException(
                "JNI binding not found. Add ycrdt-jni to your classpath."));
    }

    /**
     * Returns the Panama FFM binding implementation.
     *
     * @return the Panama binding
     * @throws IllegalStateException if the Panama binding is not available
     */
    public static YBinding panama() {
        return ServiceLoader.load(YBinding.class).stream()
            .filter(p -> p.type().getName().contains(".panama."))
            .findFirst()
            .map(ServiceLoader.Provider::get)
            .orElseThrow(() -> new IllegalStateException(
                "Panama binding not found. Add ycrdt-panama to your classpath."));
    }
}
