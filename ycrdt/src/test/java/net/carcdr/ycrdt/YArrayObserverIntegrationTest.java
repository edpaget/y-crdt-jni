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
 * Integration tests for the YArray Observer API.
 * Tests the full stack from Java → Rust → yrs observers → Rust → Java callbacks.
 */
public class YArrayObserverIntegrationTest {

    @Test
    public void testBasicObserver() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {

            AtomicInteger callCount = new AtomicInteger(0);
            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = array.observe(event -> {
                callCount.incrementAndGet();
                capturedEvents.add(event);
            })) {

                // Trigger a change
                array.pushString("Hello");

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
                assertEquals("Event target should be the YArray", array, event.getTarget());

                // Check the changes
                List<YChange> changes = event.getChanges();
                assertNotNull("Changes should not be null", changes);
                assertFalse("Changes should not be empty", changes.isEmpty());

                // Should have at least one INSERT change
                boolean hasInsert = false;
                for (YChange change : changes) {
                    if (change instanceof YArrayChange) {
                        YArrayChange arrayChange = (YArrayChange) change;
                        if (arrayChange.getType() == YChange.Type.INSERT) {
                            hasInsert = true;
                            assertEquals(1, arrayChange.getItems().size());
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
             YArray array = doc.getArray("test")) {

            AtomicInteger callCount = new AtomicInteger(0);

            try (YSubscription sub = array.observe(event -> {
                callCount.incrementAndGet();
            })) {

                array.pushString("Hello");
                array.pushString("World");
                array.pushDouble(42.0);

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
             YArray array = doc.getArray("test")) {

            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            try (YSubscription sub1 = array.observe(event -> count1.incrementAndGet());
                 YSubscription sub2 = array.observe(event -> count2.incrementAndGet())) {

                array.pushString("Test");

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
             YArray array = doc.getArray("test")) {

            AtomicInteger callCount = new AtomicInteger(0);

            YSubscription sub = array.observe(event -> callCount.incrementAndGet());

            array.pushString("Before");

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
            array.pushString("After");

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
             YArray array = doc.getArray("test")) {

            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            // Observer that throws an exception
            try (YSubscription sub1 = array.observe(event -> {
                count1.incrementAndGet();
                throw new RuntimeException("Test exception");
            });
                 YSubscription sub2 = array.observe(event -> count2.incrementAndGet())) {

                array.pushString("Test");

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
             YArray array = doc.getArray("test")) {

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = array.observe(event -> {
                capturedEvents.add(event);
            })) {

                array.insertString(0, "item1");
                array.insertDouble(1, 42.0);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                assertTrue("Should have events", capturedEvents.size() >= 2);

                // Check first event
                YEvent event1 = capturedEvents.get(0);
                boolean hasInsert = false;
                for (YChange change : event1.getChanges()) {
                    if (change instanceof YArrayChange) {
                        YArrayChange arrayChange = (YArrayChange) change;
                        if (arrayChange.getType() == YChange.Type.INSERT) {
                            hasInsert = true;
                        }
                    }
                }
                assertTrue("Should have INSERT change", hasInsert);
            }
        }
    }

    @Test
    public void testRemoveChange() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {

            // Pre-populate array
            array.pushString("item1");
            array.pushString("item2");
            array.pushString("item3");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = array.observe(event -> {
                capturedEvents.add(event);
            })) {

                array.remove(0, 1);

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
                    if (change instanceof YArrayChange) {
                        YArrayChange arrayChange = (YArrayChange) change;
                        if (arrayChange.getType() == YChange.Type.DELETE) {
                            hasDelete = true;
                            assertEquals(1, arrayChange.getLength());
                        }
                    }
                }
                assertTrue("Should have DELETE change", hasDelete);
            }
        }
    }
}
