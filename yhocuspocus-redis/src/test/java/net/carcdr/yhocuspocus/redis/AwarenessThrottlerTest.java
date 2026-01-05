package net.carcdr.yhocuspocus.redis;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for AwarenessThrottler.
 */
public class AwarenessThrottlerTest {

    @Test
    public void testFirstAcquireSucceeds() {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(100));

        assertTrue("First acquire should succeed", throttler.tryAcquire("doc1"));
    }

    @Test
    public void testImmediateSecondAcquireFails() {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(100));

        assertTrue("First acquire should succeed", throttler.tryAcquire("doc1"));
        assertFalse("Immediate second acquire should fail", throttler.tryAcquire("doc1"));
    }

    @Test
    public void testDifferentDocumentsAreIndependent() {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(100));

        assertTrue("First acquire for doc1", throttler.tryAcquire("doc1"));
        assertTrue("First acquire for doc2", throttler.tryAcquire("doc2"));
        assertFalse("Second acquire for doc1", throttler.tryAcquire("doc1"));
        assertFalse("Second acquire for doc2", throttler.tryAcquire("doc2"));
    }

    @Test
    public void testAcquireSucceedsAfterInterval() throws InterruptedException {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(50));

        assertTrue("First acquire should succeed", throttler.tryAcquire("doc1"));
        assertFalse("Immediate second acquire should fail", throttler.tryAcquire("doc1"));

        // Wait for throttle interval to pass
        Thread.sleep(60);

        assertTrue("Acquire after interval should succeed", throttler.tryAcquire("doc1"));
    }

    @Test
    public void testRemoveAllowsImmediateAcquire() {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(100));

        assertTrue("First acquire should succeed", throttler.tryAcquire("doc1"));
        assertFalse("Immediate second acquire should fail", throttler.tryAcquire("doc1"));

        throttler.remove("doc1");

        assertTrue("Acquire after remove should succeed", throttler.tryAcquire("doc1"));
    }

    @Test
    public void testClearResetsAllTracking() {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(100));

        throttler.tryAcquire("doc1");
        throttler.tryAcquire("doc2");
        throttler.tryAcquire("doc3");

        assertEquals("Should track 3 documents", 3, throttler.getTrackedCount());

        throttler.clear();

        assertEquals("Should track 0 documents after clear", 0, throttler.getTrackedCount());
        assertTrue("Acquire after clear should succeed", throttler.tryAcquire("doc1"));
    }

    @Test
    public void testTrackedCount() {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ofMillis(100));

        assertEquals("Initially 0 tracked", 0, throttler.getTrackedCount());

        throttler.tryAcquire("doc1");
        assertEquals("1 document tracked", 1, throttler.getTrackedCount());

        throttler.tryAcquire("doc2");
        assertEquals("2 documents tracked", 2, throttler.getTrackedCount());

        // Subsequent acquires for same doc don't increase count
        throttler.tryAcquire("doc1");
        assertEquals("Still 2 documents tracked", 2, throttler.getTrackedCount());

        throttler.remove("doc1");
        assertEquals("1 document tracked after remove", 1, throttler.getTrackedCount());
    }

    @Test
    public void testZeroIntervalAlwaysAllows() {
        AwarenessThrottler throttler = new AwarenessThrottler(Duration.ZERO);

        assertTrue("First acquire", throttler.tryAcquire("doc1"));
        assertTrue("Second acquire with zero interval", throttler.tryAcquire("doc1"));
        assertTrue("Third acquire with zero interval", throttler.tryAcquire("doc1"));
    }
}
