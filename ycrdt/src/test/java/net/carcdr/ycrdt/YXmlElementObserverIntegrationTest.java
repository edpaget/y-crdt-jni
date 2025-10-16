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
 * Integration tests for the YXmlElement Observer API.
 * Tests the full stack from Java → Rust → yrs observers → Rust → Java callbacks.
 */
public class YXmlElementObserverIntegrationTest {

    @Test
    public void testBasicAttributeObserver() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            AtomicInteger callCount = new AtomicInteger(0);
            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = element.observe(event -> {
                callCount.incrementAndGet();
                capturedEvents.add(event);
            })) {

                // Trigger an attribute change
                element.setAttribute("class", "container");

                // Give a moment for async processing (if any)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Verify observer was called
                assertTrue("Observer should be called at least once", callCount.get() >= 1);
                assertFalse("Should have events", capturedEvents.isEmpty());

                YEvent event = capturedEvents.get(0);
                assertNotNull("Event should not be null", event);
                assertNotNull("Event target should not be null", event.getTarget());
                assertEquals("Event target should be the YXmlElement", element, event.getTarget());

                // Check the changes
                List<YChange> changes = event.getChanges();
                assertNotNull("Changes should not be null", changes);
                assertFalse("Changes should not be empty", changes.isEmpty());

                // Should have at least one attribute change
                boolean hasAttributeChange = false;
                for (YChange change : changes) {
                    if (change instanceof YXmlElementChange) {
                        YXmlElementChange elementChange = (YXmlElementChange) change;
                        if (elementChange.getAttributeName() != null) {
                            hasAttributeChange = true;
                            assertEquals("class", elementChange.getAttributeName());
                            assertEquals("container", elementChange.getNewValue());
                        }
                    }
                }
                assertTrue("Should have attribute change", hasAttributeChange);
            }
        }
    }

    @Test
    public void testMultipleAttributeChanges() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            AtomicInteger callCount = new AtomicInteger(0);

            try (YSubscription sub = element.observe(event -> {
                callCount.incrementAndGet();
            })) {

                element.setAttribute("class", "container");
                element.setAttribute("id", "main");
                element.setAttribute("data-value", "test");

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
    public void testChildInsertionObserver() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = element.observe(event -> {
                capturedEvents.add(event);
            })) {

                // Insert child element
                element.insertElement(0, "span");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                assertTrue("Should have event", capturedEvents.size() >= 1);

                // Check for child insertion (should be YArrayChange for children)
                YEvent event = capturedEvents.get(0);
                boolean hasInsert = false;
                for (YChange change : event.getChanges()) {
                    if (change instanceof YArrayChange) {
                        YArrayChange arrayChange = (YArrayChange) change;
                        if (arrayChange.getType() == YChange.Type.INSERT) {
                            hasInsert = true;
                        }
                    }
                }
                assertTrue("Should have INSERT change for child", hasInsert);
            }
        }
    }

    @Test
    public void testChildRemovalObserver() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            // Pre-populate with children
            element.insertElement(0, "span");
            element.insertElement(1, "p");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = element.observe(event -> {
                capturedEvents.add(event);
            })) {

                element.removeChild(0);

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
                        }
                    }
                }
                assertTrue("Should have DELETE change", hasDelete);
            }
        }
    }

    @Test
    public void testAttributeUpdateObserver() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            // Set initial attribute
            element.setAttribute("class", "initial");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = element.observe(event -> {
                capturedEvents.add(event);
            })) {

                // Update attribute
                element.setAttribute("class", "updated");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                assertTrue("Should have event", capturedEvents.size() >= 1);

                // Check for attribute update
                YEvent event = capturedEvents.get(0);
                boolean hasUpdate = false;
                for (YChange change : event.getChanges()) {
                    if (change instanceof YXmlElementChange) {
                        YXmlElementChange elementChange = (YXmlElementChange) change;
                        if ("class".equals(elementChange.getAttributeName())) {
                            hasUpdate = true;
                            assertEquals("updated", elementChange.getNewValue());
                            assertEquals("initial", elementChange.getOldValue());
                        }
                    }
                }
                assertTrue("Should have attribute update", hasUpdate);
            }
        }
    }

    @Test
    public void testAttributeRemovalObserver() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            // Set initial attribute
            element.setAttribute("class", "remove-me");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = element.observe(event -> {
                capturedEvents.add(event);
            })) {

                // Remove attribute
                element.removeAttribute("class");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                assertTrue("Should have event", capturedEvents.size() >= 1);

                // Check for attribute removal
                YEvent event = capturedEvents.get(0);
                boolean hasRemoval = false;
                for (YChange change : event.getChanges()) {
                    if (change instanceof YXmlElementChange) {
                        YXmlElementChange elementChange = (YXmlElementChange) change;
                        if ("class".equals(elementChange.getAttributeName())) {
                            hasRemoval = true;
                            assertEquals(YChange.Type.DELETE, elementChange.getType());
                            assertEquals("remove-me", elementChange.getOldValue());
                        }
                    }
                }
                assertTrue("Should have attribute removal", hasRemoval);
            }
        }
    }

    @Test
    public void testMultipleObservers() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            try (YSubscription sub1 = element.observe(event -> count1.incrementAndGet());
                 YSubscription sub2 = element.observe(event -> count2.incrementAndGet())) {

                element.setAttribute("test", "value");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Both observers should be called
                assertTrue("First observer should be called", count1.get() >= 1);
                assertTrue("Second observer should be called", count2.get() >= 1);
            }
        }
    }

    @Test
    public void testUnobserve() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {

            AtomicInteger callCount = new AtomicInteger(0);

            YSubscription sub = element.observe(event -> callCount.incrementAndGet());

            element.setAttribute("before", "value");

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
            element.setAttribute("after", "value");

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
}
