package net.carcdr.ycrdt.jni;

import net.carcdr.ycrdt.DefaultObserverErrorHandler;
import net.carcdr.ycrdt.ObserverErrorHandler;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Tests for the ObserverErrorHandler API.
 */
public class ObserverErrorHandlerTest {

    @Test
    public void testDefaultHandlerIsSet() {
        try (YDoc doc = new JniYDoc()) {
            ObserverErrorHandler handler = doc.getObserverErrorHandler();
            assertNotNull(handler);
            assertSame(DefaultObserverErrorHandler.INSTANCE, handler);
        }
    }

    @Test
    public void testSetCustomHandler() {
        try (YDoc doc = new JniYDoc()) {
            List<Exception> capturedErrors = new ArrayList<>();
            List<Object> capturedSources = new ArrayList<>();

            ObserverErrorHandler customHandler = (exception, source) -> {
                capturedErrors.add(exception);
                capturedSources.add(source);
            };

            doc.setObserverErrorHandler(customHandler);
            assertSame(customHandler, doc.getObserverErrorHandler());
        }
    }

    @Test
    public void testSetNullReturnsDefaultHandler() {
        try (YDoc doc = new JniYDoc()) {
            // Set a custom handler first
            doc.setObserverErrorHandler((e, s) -> { });
            assertTrue(doc.getObserverErrorHandler() != DefaultObserverErrorHandler.INSTANCE);

            // Set null should restore default
            doc.setObserverErrorHandler(null);
            assertSame(DefaultObserverErrorHandler.INSTANCE, doc.getObserverErrorHandler());
        }
    }

    @Test
    public void testCustomHandlerReceivesObserverException() {
        List<Exception> capturedErrors = new ArrayList<>();
        List<Object> capturedSources = new ArrayList<>();

        try (YDoc doc = new JniYDoc()) {
            doc.setObserverErrorHandler((exception, source) -> {
                capturedErrors.add(exception);
                capturedSources.add(source);
            });

            try (YText text = doc.getText("test")) {
                RuntimeException testException = new RuntimeException("Test exception");

                try (YSubscription sub = text.observe(event -> {
                    throw testException;
                })) {
                    // This should trigger the observer, which throws
                    text.insert(0, "hello");
                }

                // The custom handler should have been called
                assertEquals(1, capturedErrors.size());
                assertSame(testException, capturedErrors.get(0));
                assertEquals(1, capturedSources.size());
                assertSame(text, capturedSources.get(0));
            }
        }
    }

    @Test
    public void testMultipleObserversWithOneFailing() {
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> capturedErrors = new ArrayList<>();

        try (YDoc doc = new JniYDoc()) {
            doc.setObserverErrorHandler((exception, source) -> {
                capturedErrors.add(exception);
            });

            try (YText text = doc.getText("test")) {
                // Observer 1: succeeds
                try (YSubscription sub1 = text.observe(event -> {
                    successCount.incrementAndGet();
                })) {
                    // Observer 2: throws
                    try (YSubscription sub2 = text.observe(event -> {
                        throw new RuntimeException("Failing observer");
                    })) {
                        // Observer 3: succeeds
                        try (YSubscription sub3 = text.observe(event -> {
                            successCount.incrementAndGet();
                        })) {
                            // Trigger all observers
                            text.insert(0, "hello");
                        }
                    }
                }

                // Both successful observers should have been called
                assertEquals(2, successCount.get());
                // One error should have been captured
                assertEquals(1, capturedErrors.size());
            }
        }
    }

    @Test
    public void testUpdateObserverErrorsUseHandler() {
        List<Exception> capturedErrors = new ArrayList<>();
        List<Object> capturedSources = new ArrayList<>();

        try (YDoc doc = new JniYDoc()) {
            doc.setObserverErrorHandler((exception, source) -> {
                capturedErrors.add(exception);
                capturedSources.add(source);
            });

            RuntimeException testException = new RuntimeException("Update observer error");

            try (YSubscription sub = doc.observeUpdateV1((update, origin) -> {
                throw testException;
            })) {
                try (YText text = doc.getText("test")) {
                    // This should trigger the update observer, which throws
                    text.insert(0, "hello");
                }
            }

            // The custom handler should have been called
            assertEquals(1, capturedErrors.size());
            assertSame(testException, capturedErrors.get(0));
            assertEquals(1, capturedSources.size());
            assertSame(doc, capturedSources.get(0));
        }
    }

    @Test
    public void testDefaultObserverErrorHandlerInstance() {
        // Verify INSTANCE is the same as calling constructor
        DefaultObserverErrorHandler handler1 = DefaultObserverErrorHandler.INSTANCE;
        DefaultObserverErrorHandler handler2 = new DefaultObserverErrorHandler();

        assertNotNull(handler1);
        assertNotNull(handler2);
        // Both should work (not throw)
        handler1.handleError(new RuntimeException("test"), this);
        handler2.handleError(new RuntimeException("test"), this);
    }
}
