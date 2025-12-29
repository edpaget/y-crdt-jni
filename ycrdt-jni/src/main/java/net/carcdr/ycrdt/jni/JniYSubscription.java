package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;

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
public final class JniYSubscription implements YSubscription {

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
    JniYSubscription(long subscriptionId, YObserver observer, Object target) {
        this.subscriptionId = subscriptionId;
        this.observer = observer;
        this.target = target;
    }

    @Override
    public long getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public YObserver getObserver() {
        return observer;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            // Notify the target to remove this subscription
            if (target instanceof JniYObservable) {
                ((JniYObservable) target).unobserveById(subscriptionId);
            }
            closed = true;
        }
    }
}
