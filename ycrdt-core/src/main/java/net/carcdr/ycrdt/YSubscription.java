package net.carcdr.ycrdt;

/**
 * Handle to a registered observer subscription.
 * Closing the subscription unregisters the observer.
 */
public interface YSubscription extends AutoCloseable {

    /**
     * Returns the internal subscription ID.
     *
     * @return the subscription ID
     */
    long getSubscriptionId();

    /**
     * Returns the registered observer, if available.
     *
     * @return the observer, or null for update observers
     */
    YObserver getObserver();

    /**
     * Returns the observed target.
     *
     * @return the Y-CRDT type being observed
     */
    Object getTarget();

    /**
     * Checks if this subscription has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();

    /**
     * Unregisters the observer and releases resources.
     */
    @Override
    void close();
}
