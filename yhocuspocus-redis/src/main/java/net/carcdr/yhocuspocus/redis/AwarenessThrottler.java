package net.carcdr.yhocuspocus.redis;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles awareness updates to prevent excessive Redis traffic.
 *
 * <p>Awareness updates (cursor positions, user presence) are high-frequency
 * and can overwhelm Redis with pub/sub messages. This throttler ensures that
 * updates are not published more frequently than the configured interval.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(100));
 *
 * // In awareness update handler:
 * if (throttler.tryAcquire(documentName)) {
 *     // Publish awareness update to Redis
 * }
 * // else: skip this update, it's too soon
 * }</pre>
 *
 * <p>Thread-safe implementation using ConcurrentHashMap.</p>
 */
public final class AwarenessThrottler {

    private final long minIntervalMs;
    private final Map<String, Long> lastUpdate;

    /**
     * Creates a new throttler with the specified minimum interval.
     *
     * @param minInterval minimum interval between updates per document
     */
    public AwarenessThrottler(Duration minInterval) {
        this.minIntervalMs = minInterval.toMillis();
        this.lastUpdate = new ConcurrentHashMap<>();
    }

    /**
     * Attempts to acquire permission to send an awareness update.
     *
     * <p>Returns true if enough time has passed since the last update
     * for the given document. If true is returned, the update timestamp
     * is recorded and future calls will be throttled.</p>
     *
     * @param documentName the document name
     * @return true if the update should be sent, false if throttled
     */
    public boolean tryAcquire(String documentName) {
        long now = System.currentTimeMillis();
        long[] result = new long[1]; // Use array to capture result from lambda

        lastUpdate.compute(documentName, (key, lastTime) -> {
            if (lastTime == null || now - lastTime >= minIntervalMs) {
                result[0] = 1; // Allowed
                return now; // Record new timestamp
            }
            result[0] = 0; // Denied
            return lastTime; // Keep old timestamp
        });

        return result[0] == 1;
    }

    /**
     * Removes tracking for a document.
     *
     * <p>Call this when a document is unloaded to free memory.</p>
     *
     * @param documentName the document name
     */
    public void remove(String documentName) {
        lastUpdate.remove(documentName);
    }

    /**
     * Clears all tracking state.
     */
    public void clear() {
        lastUpdate.clear();
    }

    /**
     * Gets the number of documents being tracked.
     *
     * @return number of tracked documents
     */
    public int getTrackedCount() {
        return lastUpdate.size();
    }
}
