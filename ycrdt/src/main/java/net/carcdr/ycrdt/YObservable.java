package net.carcdr.ycrdt;

/**
 * Internal interface for Y types that support observation.
 * Package-private - not exposed in public API.
 */
interface YObservable {
    /**
     * Unobserve by subscription ID.
     *
     * @param subscriptionId the subscription ID to remove
     */
    void unobserveById(long subscriptionId);
}
