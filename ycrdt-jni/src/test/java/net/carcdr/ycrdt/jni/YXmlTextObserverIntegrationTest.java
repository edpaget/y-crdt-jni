package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlText;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YChange;
import net.carcdr.ycrdt.YEvent;
import net.carcdr.ycrdt.YTextChange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Integration tests for the YXmlText Observer API.
 * Tests the full stack from Java → Rust → yrs observers → Rust → Java callbacks.
 */
public class YXmlTextObserverIntegrationTest {

    @Test
    public void testBasicObserver() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            AtomicInteger callCount = new AtomicInteger(0);
            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = xmlText.observe(event -> {
                callCount.incrementAndGet();
                capturedEvents.add(event);
            })) {

                // Trigger a change
                xmlText.insert(0, "Hello");


                // Verify observer was called
                assertEquals("Observer should be called once", 1, callCount.get());
                assertEquals("Should have one event", 1, capturedEvents.size());

                YEvent event = capturedEvents.get(0);
                assertNotNull("Event should not be null", event);
                assertNotNull("Event target should not be null", event.getTarget());
                assertEquals("Event target should be the YXmlText", xmlText, event.getTarget());

                // Check the changes
                List<? extends YChange> changes = event.getChanges();
                assertNotNull("Changes should not be null", changes);
                assertFalse("Changes should not be empty", changes.isEmpty());

                // Should have at least one INSERT change
                boolean hasInsert = false;
                for (YChange change : changes) {
                    if (change instanceof YTextChange) {
                        YTextChange textChange = (YTextChange) change;
                        if (textChange.getType() == YChange.Type.INSERT) {
                            hasInsert = true;
                            assertEquals("Hello", textChange.getContent());
                        }
                    }
                }
                assertTrue("Should have INSERT change", hasInsert);
            }
        }
    }

    @Test
    public void testMultipleChanges() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            AtomicInteger callCount = new AtomicInteger(0);

            try (YSubscription sub = xmlText.observe(event -> {
                callCount.incrementAndGet();
            })) {

                xmlText.insert(0, "Hello");
                xmlText.insert(5, " World");
                xmlText.delete(0, 5);


                // Should be called for each transaction
                assertTrue("Observer should be called multiple times",
                        callCount.get() >= 3);
            }
        }
    }

    @Test
    public void testMultipleObservers() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            try (YSubscription sub1 = xmlText.observe(event -> count1.incrementAndGet());
                 YSubscription sub2 = xmlText.observe(event -> count2.incrementAndGet())) {

                xmlText.insert(0, "Test");


                // Both observers should be called
                assertEquals("First observer should be called", 1, count1.get());
                assertEquals("Second observer should be called", 1, count2.get());
            }
        }
    }

    @Test
    public void testUnobserve() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            AtomicInteger callCount = new AtomicInteger(0);

            YSubscription sub = xmlText.observe(event -> callCount.incrementAndGet());

            xmlText.insert(0, "Before");


            int countBeforeUnobserve = callCount.get();
            assertTrue("Should be called before unobserve", countBeforeUnobserve > 0);

            // Unobserve
            sub.close();

            // This should NOT trigger the observer
            xmlText.insert(6, " After");


            // Count should not have increased
            assertEquals("Observer should not be called after unobserve",
                    countBeforeUnobserve, callCount.get());
        }
    }

    @Test
    public void testObserverException() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            // Observer that throws an exception
            try (YSubscription sub1 = xmlText.observe(event -> {
                count1.incrementAndGet();
                throw new RuntimeException("Test exception");
            });
                 YSubscription sub2 = xmlText.observe(event -> count2.incrementAndGet())) {

                xmlText.insert(0, "Test");


                // Both should be called, exception should not break second observer
                assertEquals("First observer should be called", 1, count1.get());
                assertEquals("Second observer should be called", 1, count2.get());
            }
        }
    }

    @Test
    public void testInsertWithAttributes() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            AtomicInteger callCount = new AtomicInteger(0);
            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = xmlText.observe(event -> {
                callCount.incrementAndGet();
                capturedEvents.add(event);
            })) {

                // Insert with bold attribute
                Map<String, Object> bold = Map.of("b", true);
                xmlText.insertWithAttributes(0, "Bold", bold);


                // Verify observer was called
                assertEquals("Observer should be called once", 1, callCount.get());
                assertEquals("Should have one event", 1, capturedEvents.size());

                YEvent event = capturedEvents.get(0);
                List<? extends YChange> changes = event.getChanges();
                assertFalse("Changes should not be empty", changes.isEmpty());

                // Should have INSERT change with attributes
                boolean hasInsertWithAttrs = false;
                for (YChange change : changes) {
                    if (change instanceof YTextChange) {
                        YTextChange textChange = (YTextChange) change;
                        if (textChange.getType() == YChange.Type.INSERT) {
                            hasInsertWithAttrs = true;
                            assertEquals("Bold", textChange.getContent());
                            assertNotNull("Should have attributes", textChange.getAttributes());
                        }
                    }
                }
                assertTrue("Should have INSERT change with attributes", hasInsertWithAttrs);
            }
        }
    }

    @Test
    public void testFormatObserver() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            // First insert some text
            xmlText.insert(0, "Hello World");

            List<YEvent> capturedEvents = new ArrayList<>();

            // Now observe and format
            try (YSubscription sub = xmlText.observe(event -> {
                capturedEvents.add(event);
            })) {

                // Format the text
                Map<String, Object> bold = Map.of("b", true);
                xmlText.format(0, 5, bold);


                // Verify observer was called
                assertFalse("Should have events", capturedEvents.isEmpty());

                // Should have changes in the event
                boolean hasChange = false;
                for (YEvent event : capturedEvents) {
                    if (!event.getChanges().isEmpty()) {
                        hasChange = true;
                        break;
                    }
                }
                assertTrue("Should have changes from format operation", hasChange);
            }
        }
    }

    @Test
    public void testDeleteObserver() {
        try (YDoc doc = new JniYDoc();
             YXmlText xmlText = doc.getXmlText("test")) {

            // First insert some text
            xmlText.insert(0, "Hello World");

            List<YEvent> capturedEvents = new ArrayList<>();

            // Now observe and delete
            try (YSubscription sub = xmlText.observe(event -> {
                capturedEvents.add(event);
            })) {

                // Delete some text
                xmlText.delete(5, 6); // Delete " World"


                // Verify observer was called
                assertFalse("Should have events", capturedEvents.isEmpty());

                // Should have DELETE change
                boolean hasDelete = false;
                for (YEvent event : capturedEvents) {
                    for (YChange change : event.getChanges()) {
                        if (change instanceof YTextChange) {
                            YTextChange textChange = (YTextChange) change;
                            if (textChange.getType() == YChange.Type.DELETE) {
                                hasDelete = true;
                                assertEquals("Should delete 6 characters", 6, textChange.getLength());
                            }
                        }
                    }
                }
                assertTrue("Should have DELETE change", hasDelete);
            }
        }
    }
}
