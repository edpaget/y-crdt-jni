package net.carcdr.yhocuspocus.redis;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Test helper for synchronizing on Redis pub/sub messages.
 *
 * <p>Similar to TestWaiter, provides CountDownLatch-based synchronization
 * for async Redis operations.</p>
 */
public class RedisTestWaiter {

    private CountDownLatch messageLatch = new CountDownLatch(1);
    private CountDownLatch subscriptionLatch = new CountDownLatch(1);
    private final BlockingQueue<ReceivedMessage> receivedMessages = new LinkedBlockingQueue<>();

    /**
     * Records a received pub/sub message.
     *
     * @param channel the channel name
     * @param message the message content
     */
    public void onMessage(String channel, byte[] message) {
        receivedMessages.add(new ReceivedMessage(channel, message));
        messageLatch.countDown();
    }

    /**
     * Records subscription confirmation.
     *
     * @param channel the subscribed channel
     */
    public void onSubscribed(String channel) {
        subscriptionLatch.countDown();
    }

    /**
     * Waits for a message to be received.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if message received within timeout
     * @throws InterruptedException if interrupted
     */
    public boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return messageLatch.await(timeout, unit);
    }

    /**
     * Waits for subscription to be confirmed.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return true if subscribed within timeout
     * @throws InterruptedException if interrupted
     */
    public boolean awaitSubscription(long timeout, TimeUnit unit) throws InterruptedException {
        return subscriptionLatch.await(timeout, unit);
    }

    /**
     * Gets the next received message, waiting if necessary.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return received message, or null if timeout
     * @throws InterruptedException if interrupted
     */
    public ReceivedMessage pollMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return receivedMessages.poll(timeout, unit);
    }

    /**
     * Resets message latch for reuse.
     *
     * @param count number of messages to wait for
     */
    public void resetMessageLatch(int count) {
        messageLatch = new CountDownLatch(count);
    }

    /**
     * Resets subscription latch for reuse.
     *
     * @param count number of subscriptions to wait for
     */
    public void resetSubscriptionLatch(int count) {
        subscriptionLatch = new CountDownLatch(count);
    }

    /**
     * Clears all received messages.
     */
    public void clearMessages() {
        receivedMessages.clear();
    }

    /**
     * Record of a received pub/sub message.
     *
     * @param channel the channel name
     * @param message the message content
     */
    public record ReceivedMessage(String channel, byte[] message) { }
}
