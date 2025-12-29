package net.carcdr.ycrdt.jni;

/**
 * Internal interface for Y types that support observation.
 * Package-private - not exposed in public API.
 */
interface JniYObservable {
    /**
     * Unobserve by subscription ID.
     *
     * @param subscriptionId the subscription ID to remove
     */
    void unobserveById(long subscriptionId);
}
