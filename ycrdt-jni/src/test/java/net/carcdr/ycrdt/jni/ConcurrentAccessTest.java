package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YArray;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YMap;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YText;
import net.carcdr.ycrdt.YTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Concurrent access tests to validate threading model and behavior under multi-threaded load.
 *
 * <p>These tests verify:</p>
 * <ul>
 *   <li>Synchronized access patterns work correctly</li>
 *   <li>Concurrent document creation/destruction is safe</li>
 *   <li>Concurrent sync operations between documents work</li>
 *   <li>Observer callbacks function under concurrent load</li>
 *   <li>Thread-per-document isolation pattern</li>
 * </ul>
 *
 * <p>Note: YDoc instances are NOT thread-safe by design. External synchronization
 * is required when accessing a single document from multiple threads.</p>
 */
public class ConcurrentAccessTest {

    private static final int THREAD_COUNT = 8;
    private static final int OPERATIONS_PER_THREAD = 100;
    private static final int TIMEOUT_SECONDS = 30;

    // ==================== Synchronized Access Pattern Tests ====================

    /**
     * Test that synchronized access to a single YDoc from multiple threads works correctly.
     * This is the expected usage pattern when sharing a document across threads.
     */
    @Test
    public void testSynchronizedYTextAccess() throws InterruptedException {
        try (YDoc doc = new JniYDoc(); YText text = doc.getText("text")) {
            Object lock = new Object();
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            AtomicBoolean failed = new AtomicBoolean(false);

            for (int t = 0; t < THREAD_COUNT; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                            synchronized (lock) {
                                text.push("T" + threadId + ":" + i + " ");
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());

            synchronized (lock) {
                assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD,
                    text.toString().split(" ").length);
            }
        }
    }

    /**
     * Test synchronized access to YArray from multiple threads.
     */
    @Test
    public void testSynchronizedYArrayAccess() throws InterruptedException {
        try (YDoc doc = new JniYDoc(); YArray array = doc.getArray("array")) {
            Object lock = new Object();
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            AtomicBoolean failed = new AtomicBoolean(false);

            for (int t = 0; t < THREAD_COUNT; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                            synchronized (lock) {
                                array.pushString("T" + threadId + ":" + i);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());

            synchronized (lock) {
                assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, array.length());
            }
        }
    }

    /**
     * Test synchronized access to YMap from multiple threads.
     */
    @Test
    public void testSynchronizedYMapAccess() throws InterruptedException {
        try (YDoc doc = new JniYDoc(); YMap map = doc.getMap("map")) {
            Object lock = new Object();
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            AtomicBoolean failed = new AtomicBoolean(false);

            for (int t = 0; t < THREAD_COUNT; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                            synchronized (lock) {
                                map.setString("T" + threadId + "_" + i, "value" + i);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());

            synchronized (lock) {
                assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, map.size());
            }
        }
    }

    /**
     * Test synchronized explicit transactions from multiple threads.
     */
    @Test
    public void testSynchronizedTransactions() throws InterruptedException {
        try (YDoc doc = new JniYDoc(); YText text = doc.getText("text")) {
            Object lock = new Object();
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            AtomicBoolean failed = new AtomicBoolean(false);

            for (int t = 0; t < THREAD_COUNT; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                            synchronized (lock) {
                                try (YTransaction txn = doc.beginTransaction()) {
                                    text.insert(txn, 0, "T" + threadId + ":" + i + " ");
                                    txn.commit();
                                }
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());

            synchronized (lock) {
                assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD,
                    text.toString().split(" ").length);
            }
        }
    }

    // ==================== Concurrent Document Creation Tests ====================

    /**
     * Test creating many documents concurrently from multiple threads.
     */
    @Test
    public void testConcurrentDocumentCreation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        CopyOnWriteArrayList<YDoc> docs = new CopyOnWriteArrayList<>();

        for (int t = 0; t < THREAD_COUNT; t++) {
            new Thread(() -> {
                try {
                    barrier.await(); // Start all threads simultaneously
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        YDoc doc = new JniYDoc();
                        assertNotNull(doc);
                        docs.add(doc);
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());
        assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, docs.size());

        // Clean up
        for (YDoc doc : docs) {
            doc.close();
        }
    }

    /**
     * Test creating documents with specific client IDs concurrently.
     */
    @Test
    public void testConcurrentDocumentCreationWithClientId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger clientIdCounter = new AtomicInteger(0);
        CopyOnWriteArrayList<YDoc> docs = new CopyOnWriteArrayList<>();

        for (int t = 0; t < THREAD_COUNT; t++) {
            new Thread(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        long clientId = clientIdCounter.getAndIncrement();
                        YDoc doc = new JniYDoc(clientId);
                        assertEquals(clientId, doc.getClientId());
                        docs.add(doc);
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());

        // Clean up
        for (YDoc doc : docs) {
            doc.close();
        }
    }

    /**
     * Test concurrent document creation and immediate destruction.
     */
    @Test
    public void testConcurrentDocumentLifecycle() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            new Thread(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        try (YDoc doc = new JniYDoc()) {
                            try (YText text = doc.getText("text")) {
                                text.push("hello");
                                assertEquals(5, text.length());
                            }
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());
        assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, successCount.get());
    }

    // ==================== Thread-per-Document Pattern Tests ====================

    /**
     * Test the thread-per-document pattern where each thread owns its own document.
     * This is a safe pattern that doesn't require synchronization.
     */
    @Test
    public void testThreadPerDocumentPattern() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        CopyOnWriteArrayList<byte[]> updates = new CopyOnWriteArrayList<>();

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    barrier.await();
                    try (YDoc doc = new JniYDoc(threadId)) {
                        try (YText text = doc.getText("text")) {
                            for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                                text.push("T" + threadId + ":" + i + " ");
                            }
                        }
                        updates.add(doc.encodeStateAsUpdate());
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());
        assertEquals(THREAD_COUNT, updates.size());

        // Merge all updates into a single document
        try (YDoc merged = new JniYDoc()) {
            for (byte[] update : updates) {
                merged.applyUpdate(update);
            }
            try (YText text = merged.getText("text")) {
                // Due to CRDT merge, all content should be present
                String content = text.toString();
                for (int t = 0; t < THREAD_COUNT; t++) {
                    assertTrue("Missing content from thread " + t,
                        content.contains("T" + t + ":0"));
                    assertTrue("Missing content from thread " + t,
                        content.contains("T" + t + ":" + (OPERATIONS_PER_THREAD - 1)));
                }
            }
        }
    }

    /**
     * Test multiple independent documents being modified concurrently.
     */
    @Test
    public void testMultipleIndependentDocuments() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        List<AtomicReference<String>> results = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            results.add(new AtomicReference<>());
        }

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    barrier.await();
                    try (YDoc doc = new JniYDoc()) {
                        try (YText text = doc.getText("text")) {
                            StringBuilder expected = new StringBuilder();
                            for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                                String content = "T" + threadId + ":" + i;
                                text.push(content);
                                expected.append(content);
                            }
                            results.get(threadId).set(text.toString());
                            assertEquals(expected.toString(), text.toString());
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());

        // Verify each thread got its expected result
        for (int t = 0; t < THREAD_COUNT; t++) {
            assertNotNull("Thread " + t + " result is null", results.get(t).get());
            assertTrue("Thread " + t + " content incorrect",
                results.get(t).get().startsWith("T" + t + ":0"));
        }
    }

    // ==================== Concurrent Sync Tests ====================

    /**
     * Test concurrent sync operations between multiple document pairs.
     */
    @Test
    public void testConcurrentSyncOperations() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < OPERATIONS_PER_THREAD / 10; i++) {
                        try (YDoc doc1 = new JniYDoc(threadId * 1000 + i);
                             YDoc doc2 = new JniYDoc(threadId * 1000 + i + 10000)) {

                            // Modify doc1
                            try (YText text1 = doc1.getText("text")) {
                                text1.push("From doc1 thread " + threadId);
                            }

                            // Sync to doc2
                            byte[] update = doc1.encodeStateAsUpdate();
                            doc2.applyUpdate(update);

                            // Verify sync
                            try (YText text2 = doc2.getText("text")) {
                                assertEquals("From doc1 thread " + threadId, text2.toString());
                            }

                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());
        assertEquals(THREAD_COUNT * (OPERATIONS_PER_THREAD / 10), successCount.get());
    }

    /**
     * Test bidirectional sync between documents in different threads.
     */
    @Test
    public void testBidirectionalConcurrentSync() throws InterruptedException {
        try (YDoc doc1 = new JniYDoc(1); YDoc doc2 = new JniYDoc(2)) {
            Object lock1 = new Object();
            Object lock2 = new Object();
            CountDownLatch latch = new CountDownLatch(2);
            AtomicBoolean failed = new AtomicBoolean(false);

            // Thread 1: modify doc1 and sync to doc2
            new Thread(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        byte[] update;
                        synchronized (lock1) {
                            try (YText text = doc1.getText("text")) {
                                text.push("A" + i);
                            }
                            update = doc1.encodeStateAsUpdate();
                        }
                        synchronized (lock2) {
                            doc2.applyUpdate(update);
                        }
                        Thread.sleep(1); // Small delay to interleave with other thread
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();

            // Thread 2: modify doc2 and sync to doc1
            new Thread(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        byte[] update;
                        synchronized (lock2) {
                            try (YText text = doc2.getText("text")) {
                                text.push("B" + i);
                            }
                            update = doc2.encodeStateAsUpdate();
                        }
                        synchronized (lock1) {
                            doc1.applyUpdate(update);
                        }
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());

            // Final sync to ensure convergence
            synchronized (lock1) {
                synchronized (lock2) {
                    doc1.applyUpdate(doc2.encodeStateAsUpdate());
                    doc2.applyUpdate(doc1.encodeStateAsUpdate());
                }
            }

            // Verify convergence
            try (YText text1 = doc1.getText("text");
                 YText text2 = doc2.getText("text")) {
                assertEquals("Documents should converge", text1.toString(), text2.toString());
                String content = text1.toString();
                // Both A and B content should be present
                assertTrue("Should contain A content", content.contains("A0"));
                assertTrue("Should contain B content", content.contains("B0"));
            }
        }
    }

    /**
     * Test differential sync under concurrent load.
     */
    @Test
    public void testConcurrentDifferentialSync() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);

        // Shared source document
        try (YDoc source = new JniYDoc()) {
            Object sourceLock = new Object();

            // Initialize source with some content
            try (YText text = source.getText("text")) {
                text.push("Initial content. ");
            }

            for (int t = 0; t < THREAD_COUNT; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        barrier.await();
                        try (YDoc replica = new JniYDoc(1000 + threadId)) {
                            // Initial sync
                            byte[] fullState;
                            synchronized (sourceLock) {
                                fullState = source.encodeStateAsUpdate();
                            }
                            replica.applyUpdate(fullState);

                            // Differential syncs
                            for (int i = 0; i < 10; i++) {
                                byte[] stateVector = replica.encodeStateVector();
                                byte[] diff;
                                synchronized (sourceLock) {
                                    // Add more content to source
                                    try (YText text = source.getText("text")) {
                                        text.push("T" + threadId + ":" + i + " ");
                                    }
                                    diff = source.encodeDiff(stateVector);
                                }
                                replica.applyUpdate(diff);
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());
        }
    }

    // ==================== Observer Tests Under Concurrent Load ====================

    /**
     * Test update observers firing under concurrent modifications.
     */
    @Test
    public void testObserversUnderConcurrentLoad() throws InterruptedException {
        try (YDoc doc = new JniYDoc()) {
            Object lock = new Object();
            AtomicInteger observerCallCount = new AtomicInteger(0);
            CopyOnWriteArrayList<byte[]> receivedUpdates = new CopyOnWriteArrayList<>();

            YSubscription subscription = doc.observeUpdateV1((update, origin) -> {
                observerCallCount.incrementAndGet();
                receivedUpdates.add(update);
            });

            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
            AtomicBoolean failed = new AtomicBoolean(false);

            for (int t = 0; t < THREAD_COUNT; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        barrier.await();
                        for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                            synchronized (lock) {
                                try (YText text = doc.getText("text")) {
                                    text.push("T" + threadId + ":" + i + " ");
                                }
                            }
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());

            // Observer should have been called for each operation
            assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, observerCallCount.get());
            assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, receivedUpdates.size());

            subscription.close();
        }
    }

    /**
     * Test subscribing and unsubscribing observers concurrently.
     */
    @Test
    public void testConcurrentObserverSubscription() throws InterruptedException {
        try (YDoc doc = new JniYDoc()) {
            Object lock = new Object();
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
            AtomicBoolean failed = new AtomicBoolean(false);
            CopyOnWriteArrayList<YSubscription> subscriptions = new CopyOnWriteArrayList<>();
            AtomicInteger callCount = new AtomicInteger(0);

            for (int t = 0; t < THREAD_COUNT; t++) {
                new Thread(() -> {
                    try {
                        barrier.await();
                        for (int i = 0; i < 10; i++) {
                            // Subscribe
                            YSubscription sub;
                            synchronized (lock) {
                                sub = doc.observeUpdateV1((update, origin) -> {
                                    callCount.incrementAndGet();
                                });
                            }
                            subscriptions.add(sub);

                            // Make a change
                            synchronized (lock) {
                                try (YText text = doc.getText("text")) {
                                    text.push("x");
                                }
                            }

                            // Unsubscribe
                            sub.close();
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());

            // Verify subscriptions were created
            assertEquals(THREAD_COUNT * 10, subscriptions.size());
        }
    }

    /**
     * Test observers applied to synced documents concurrently.
     */
    @Test
    public void testConcurrentSyncWithObservers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger totalObserverCalls = new AtomicInteger(0);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    barrier.await();

                    try (YDoc source = new JniYDoc(threadId);
                         YDoc target = new JniYDoc(threadId + 1000)) {

                        AtomicInteger localCalls = new AtomicInteger(0);
                        YSubscription sub = target.observeUpdateV1((update, origin) -> {
                            localCalls.incrementAndGet();
                        });

                        // Make changes to source
                        for (int i = 0; i < 10; i++) {
                            try (YText text = source.getText("text")) {
                                text.push("T" + threadId + ":" + i);
                            }
                            // Sync to target
                            target.applyUpdate(source.encodeStateAsUpdate());
                        }

                        totalObserverCalls.addAndGet(localCalls.get());
                        sub.close();
                    }
                } catch (Exception e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());

        // Each thread should have observer calls from applying updates
        assertTrue("Expected observer calls", totalObserverCalls.get() > 0);
    }

    // ==================== High Contention Tests ====================

    /**
     * Test high-contention scenario with many threads competing for a single lock.
     */
    @Test
    public void testHighContentionSingleDocument() throws InterruptedException {
        try (YDoc doc = new JniYDoc()) {
            Object lock = new Object();
            int threadCount = 16; // Higher thread count for more contention
            int opsPerThread = 50;
            CountDownLatch latch = new CountDownLatch(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            AtomicBoolean failed = new AtomicBoolean(false);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        barrier.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            synchronized (lock) {
                                try (YText text = doc.getText("text");
                                     YArray array = doc.getArray("array");
                                     YMap map = doc.getMap("map")) {

                                    text.push("T" + threadId);
                                    array.pushDouble(threadId);
                                    map.setString("key" + threadId + "_" + i, "val");
                                }
                            }
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue("Threads did not complete in time",
                latch.await(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS));
            assertFalse("Thread encountered an error", failed.get());
            assertEquals(threadCount * opsPerThread, successCount.get());
        }
    }

    /**
     * Test rapid connect/disconnect pattern simulating real-world usage.
     */
    @Test
    public void testRapidConnectDisconnect() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT * OPERATIONS_PER_THREAD);
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int t = 0; t < THREAD_COUNT; t++) {
            for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                executor.submit(() -> {
                    try {
                        // Simulate a client connecting
                        try (YDoc doc = new JniYDoc()) {
                            try (YText text = doc.getText("text")) {
                                text.push("hello");
                            }
                            // Simulate getting state before disconnect
                            byte[] state = doc.encodeStateAsUpdate();
                            assertTrue(state.length > 0);
                        }
                        // Document auto-closed (client disconnected)
                    } catch (Exception e) {
                        failed.set(true);
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        assertTrue("Tasks did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Task encountered an error", failed.get());

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    // ==================== Stress Tests ====================

    /**
     * Extended stress test for concurrent document operations.
     */
    @Test
    public void testExtendedConcurrentStress() throws InterruptedException {
        int threadCount = 4;
        int duration = 5; // seconds
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger operationCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    while (running.get()) {
                        try (YDoc doc = new JniYDoc()) {
                            try (YText text = doc.getText("text");
                                 YArray array = doc.getArray("array");
                                 YMap map = doc.getMap("map")) {

                                // Mix of operations
                                text.push("test" + threadId);
                                array.pushString("item");
                                array.pushDouble(3.14);
                                map.setString("key", "value");
                                map.setDouble("num", 42.0);

                                // Encode and decode
                                byte[] state = doc.encodeStateAsUpdate();
                                try (YDoc doc2 = new JniYDoc()) {
                                    doc2.applyUpdate(state);
                                }
                            }
                            operationCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                    running.set(false);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Run for specified duration
        Thread.sleep(duration * 1000L);
        running.set(false);

        assertTrue("Threads did not complete in time",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("Thread encountered an error", failed.get());

        System.out.println("Completed " + operationCount.get()
            + " operations in " + duration + " seconds ("
            + (operationCount.get() / duration) + " ops/sec)");
    }

    // ==================== Helper Methods ====================

    private static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }
}
