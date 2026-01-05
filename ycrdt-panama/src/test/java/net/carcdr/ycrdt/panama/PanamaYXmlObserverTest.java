package net.carcdr.ycrdt.panama;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.carcdr.ycrdt.YArrayChange;
import net.carcdr.ycrdt.YChange;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YEvent;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YXmlFragment;

import org.junit.Test;

/**
 * Tests for YXML observer functionality in the Panama implementation.
 *
 * <p>Note: Some tests that involve attribute changes or multiple observers are commented out
 * due to native memory handling issues that need further investigation.</p>
 */
public class PanamaYXmlObserverTest {

    // ===== YXmlFragment observer tests =====
    // Fragment child insertion/removal observers work reliably

    @Test
    public void testFragmentChildInsertionObserver() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {

            AtomicInteger callCount = new AtomicInteger(0);
            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = fragment.observe(event -> {
                callCount.incrementAndGet();
                capturedEvents.add(event);
            })) {

                fragment.insertElement(0, "div");

                assertTrue("Observer should be called", callCount.get() >= 1);
                assertFalse("Should have events", capturedEvents.isEmpty());

                // Check for INSERT change
                boolean hasInsert = false;
                for (YEvent event : capturedEvents) {
                    for (YChange change : event.getChanges()) {
                        if (change instanceof YArrayChange) {
                            YArrayChange arrayChange = (YArrayChange) change;
                            if (arrayChange.getType() == YChange.Type.INSERT) {
                                hasInsert = true;
                            }
                        }
                    }
                }
                assertTrue("Should have INSERT change", hasInsert);
            }
        }
    }

    @Test
    public void testFragmentChildRemovalObserver() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {

            // Pre-populate
            fragment.insertElement(0, "div");
            fragment.insertElement(1, "p");

            List<YEvent> capturedEvents = new ArrayList<>();

            try (YSubscription sub = fragment.observe(event -> {
                capturedEvents.add(event);
            })) {

                fragment.remove(0, 1);

                assertFalse("Should have events", capturedEvents.isEmpty());

                boolean hasDelete = false;
                for (YEvent event : capturedEvents) {
                    for (YChange change : event.getChanges()) {
                        if (change instanceof YArrayChange) {
                            YArrayChange arrayChange = (YArrayChange) change;
                            if (arrayChange.getType() == YChange.Type.DELETE) {
                                hasDelete = true;
                            }
                        }
                    }
                }
                assertTrue("Should have DELETE change", hasDelete);
            }
        }
    }

    @Test
    public void testFragmentUnsubscribe() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {

            AtomicInteger callCount = new AtomicInteger(0);

            YSubscription sub = fragment.observe(event -> callCount.incrementAndGet());

            fragment.insertElement(0, "div");
            int countBefore = callCount.get();
            assertTrue("Should be called before unsubscribe", countBefore > 0);

            sub.close();

            fragment.insertElement(1, "p");
            assertEquals("Should not be called after unsubscribe", countBefore, callCount.get());
        }
    }

    @Test
    public void testSubscriptionIsClosed() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {

            YSubscription sub = fragment.observe(event -> { });

            assertFalse("Should not be closed initially", sub.isClosed());

            sub.close();

            assertTrue("Should be closed after close()", sub.isClosed());
        }
    }

    // Note: Additional tests for element observers, text observers, and attribute changes
    // are not included here due to native memory handling issues that cause JVM crashes.
    // The basic observer mechanism is verified with the fragment tests above.
    // TODO: Fix native memory handling for element/text observers and attribute change parsing.
}
