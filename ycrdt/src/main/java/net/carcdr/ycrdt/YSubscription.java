package net.carcdr.ycrdt;

/**
 * Represents an active observer subscription.
 *
 * <p>Subscriptions should be closed when no longer needed to prevent
 * memory leaks. Use try-with-resources for automatic cleanup:</p>
 *
 * <pre>{@code
 * try (YSubscription sub = text.observe(event -> { ... })) {
 *     // Observer is active
 *     text.insert(0, "Hello");
 * } // Observer automatically unregistered
 * }</pre>
 *
 * @see YObserver
 */
public final class YSubscription implements AutoCloseable {

    private final long subscriptionId;
    private final YObserver observer;
    private final Object target;
    private volatile boolean closed = false;

    /**
     * Package-private constructor.
     *
     * @param subscriptionId the native subscription ID
     * @param observer the observer
     * @param target the observed object
     */
    YSubscription(long subscriptionId, YObserver observer, Object target) {
        this.subscriptionId = subscriptionId;
        this.observer = observer;
        this.target = target;
    }

    /**
     * Gets the subscription ID.
     *
     * @return the native subscription ID
     */
    public long getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Gets the observer associated with this subscription.
     *
     * @return the observer
     */
    public YObserver getObserver() {
        return observer;
    }

    /**
     * Gets the target object being observed.
     *
     * @return the target object
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Checks if this subscription is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Unregisters the observer and closes this subscription.
     *
     * <p>This method is idempotent - calling it multiple times has no
     * effect after the first call.</p>
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            // Notify the target to remove this subscription
            if (target instanceof YObservable) {
                ((YObservable) target).unobserveById(subscriptionId);
            }
            closed = true;
        }
    }
}
