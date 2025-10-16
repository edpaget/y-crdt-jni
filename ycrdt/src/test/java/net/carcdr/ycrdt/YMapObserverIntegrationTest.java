package net.carcdr.ycrdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Integration tests for the YMap Observer API.
 * Tests the full stack from Java → Rust → yrs observers → Rust → Java callbacks.
 */
public class YMapObserverIntegrationTest {

    @Test
    public void testBasicObserver() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            AtomicInteger callCount = new AtomicInteger(0);
            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = map.observe(event -> {
                callCount.incrementAndGet();
                capturedEvents.add(event);
            })) {

                // Trigger a change
                map.setString("key1", "value1");

                // Give a moment for async processing (if any)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Verify observer was called
                assertEquals("Observer should be called once", 1, callCount.get());
                assertEquals("Should have one event", 1, capturedEvents.size());

                YEvent event = capturedEvents.get(0);
                assertNotNull("Event should not be null", event);
                assertNotNull("Event target should not be null", event.getTarget());
                assertEquals("Event target should be the YMap", map, event.getTarget());

                // Check the changes
                List<YChange> changes = event.getChanges();
                assertNotNull("Changes should not be null", changes);
                assertFalse("Changes should not be empty", changes.isEmpty());

                // Should have at least one INSERT change
                boolean hasInsert = false;
                for (YChange change : changes) {
                    if (change instanceof YMapChange) {
                        YMapChange mapChange = (YMapChange) change;
                        if (mapChange.getType() == YChange.Type.INSERT) {
                            hasInsert = true;
                            assertEquals("key1", mapChange.getKey());
                            assertNotNull(mapChange.getNewValue());
                        }
                    }
                }
                assertTrue("Should have INSERT change", hasInsert);
            }
        }
    }

    @Test
    public void testMultipleChanges() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            AtomicInteger callCount = new AtomicInteger(0);

            try (YSubscription sub = map.observe(event -> {
                callCount.incrementAndGet();
            })) {

                map.setString("name", "Alice");
                map.setDouble("age", 30.0);
                map.setString("city", "NYC");

                // Give a moment for processing
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Should be called for each transaction
                assertTrue("Observer should be called multiple times",
                        callCount.get() >= 3);
            }
        }
    }

    @Test
    public void testMultipleObservers() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            try (YSubscription sub1 = map.observe(event -> count1.incrementAndGet());
                 YSubscription sub2 = map.observe(event -> count2.incrementAndGet())) {

                map.setString("test", "value");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Both observers should be called
                assertEquals("First observer should be called", 1, count1.get());
                assertEquals("Second observer should be called", 1, count2.get());
            }
        }
    }

    @Test
    public void testUnobserve() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            AtomicInteger callCount = new AtomicInteger(0);

            YSubscription sub = map.observe(event -> callCount.incrementAndGet());

            map.setString("before", "value");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            int countBeforeUnobserve = callCount.get();
            assertTrue("Should be called before unobserve", countBeforeUnobserve > 0);

            // Unobserve
            sub.close();

            // This should NOT trigger the observer
            map.setString("after", "value");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            // Count should not have increased
            assertEquals("Observer should not be called after unobserve",
                    countBeforeUnobserve, callCount.get());
        }
    }

    @Test
    public void testObserverException() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            // Observer that throws an exception
            try (YSubscription sub1 = map.observe(event -> {
                count1.incrementAndGet();
                throw new RuntimeException("Test exception");
            });
                 YSubscription sub2 = map.observe(event -> count2.incrementAndGet())) {

                map.setString("test", "value");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Both should be called, exception should not break second observer
                assertEquals("First observer should be called", 1, count1.get());
                assertEquals("Second observer should be called", 1, count2.get());
            }
        }
    }

    @Test
    public void testInsertChange() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = map.observe(event -> {
                capturedEvents.add(event);
            })) {

                map.setString("key1", "value1");
                map.setDouble("key2", 42.0);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                assertTrue("Should have events", capturedEvents.size() >= 2);

                // Check first event for INSERT
                YEvent event1 = capturedEvents.get(0);
                boolean hasInsert = false;
                for (YChange change : event1.getChanges()) {
                    if (change instanceof YMapChange) {
                        YMapChange mapChange = (YMapChange) change;
                        if (mapChange.getType() == YChange.Type.INSERT) {
                            hasInsert = true;
                            assertEquals("key1", mapChange.getKey());
                        }
                    }
                }
                assertTrue("Should have INSERT change", hasInsert);
            }
        }
    }

    @Test
    public void testUpdateChange() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            // Pre-populate map
            map.setString("key1", "initial");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = map.observe(event -> {
                capturedEvents.add(event);
            })) {

                // Update existing key
                map.setString("key1", "updated");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                assertTrue("Should have event", capturedEvents.size() >= 1);

                // Check for ATTRIBUTE (update) change
                YEvent event = capturedEvents.get(0);
                boolean hasUpdate = false;
                for (YChange change : event.getChanges()) {
                    if (change instanceof YMapChange) {
                        YMapChange mapChange = (YMapChange) change;
                        if (mapChange.getType() == YChange.Type.ATTRIBUTE) {
                            hasUpdate = true;
                            assertEquals("key1", mapChange.getKey());
                            assertNotNull("Should have old value", mapChange.getOldValue());
                            assertNotNull("Should have new value", mapChange.getNewValue());
                        }
                    }
                }
                assertTrue("Should have ATTRIBUTE (update) change", hasUpdate);
            }
        }
    }

    @Test
    public void testRemoveChange() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {

            // Pre-populate map
            map.setString("key1", "value1");
            map.setString("key2", "value2");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = map.observe(event -> {
                capturedEvents.add(event);
            })) {

                map.remove("key1");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                assertTrue("Should have event", capturedEvents.size() >= 1);

                // Check for DELETE change
                YEvent event = capturedEvents.get(0);
                boolean hasDelete = false;
                for (YChange change : event.getChanges()) {
                    if (change instanceof YMapChange) {
                        YMapChange mapChange = (YMapChange) change;
                        if (mapChange.getType() == YChange.Type.DELETE) {
                            hasDelete = true;
                            assertEquals("key1", mapChange.getKey());
                            assertNotNull("Should have old value", mapChange.getOldValue());
                        }
                    }
                }
                assertTrue("Should have DELETE change", hasDelete);
            }
        }
    }
}
