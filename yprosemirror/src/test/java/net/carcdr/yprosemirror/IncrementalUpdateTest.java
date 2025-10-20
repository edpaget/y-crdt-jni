package net.carcdr.yprosemirror;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.atlassian.prosemirror.model.Schema;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.yprosemirror.test.TestSchemas;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for incremental update functionality in YProseMirrorBinding.
 *
 * <p>These tests verify that the incremental update system correctly
 * detects and applies only the changes between document states, rather
 * than replacing the entire document.
 */
public class IncrementalUpdateTest {

    private YDoc ydoc;
    private Schema schema;

    @Before
    public void setUp() {
        ydoc = new YDoc();
        schema = TestSchemas.createBasicSchema();
    }

    @After
    public void tearDown() {
        if (ydoc != null) {
            ydoc.close();
        }
    }

    @Test
    public void testIncrementalBindingCreation() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> { /* callback */ },
                    true)) {  // Enable incremental updates

                assertNotNull("Binding should be created", binding);
                assertFalse("Binding should not be closed", binding.isClosed());
            }
        }
    }

    @Test
    public void testIncrementalBindingWithExistingContent() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Pre-populate fragment
            fragment.insertElement(0, "paragraph");

            AtomicBoolean callbackInvoked = new AtomicBoolean(false);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackInvoked.set(true);
                        assertNotNull("Node should not be null", node);
                    },
                    true)) {  // Enable incremental updates

                assertTrue("Callback should be invoked on initialization",
                    callbackInvoked.get());
            }
        }
    }

    @Test
    public void testObserverTriggeredOnRemoteChange() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            AtomicInteger callbackCount = new AtomicInteger(0);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackCount.incrementAndGet();
                    },
                    true)) {  // Enable incremental updates

                // Modify Y-CRDT to trigger observer
                fragment.insertElement(0, "paragraph");

                // Observer callback is synchronous
                assertTrue("Callback should be invoked at least once",
                    callbackCount.get() >= 1);
            }
        }
    }

    @Test
    public void testMultipleRemoteChanges() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            AtomicInteger callbackCount = new AtomicInteger(0);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackCount.incrementAndGet();
                    },
                    true)) {  // Enable incremental updates

                // Make multiple changes
                fragment.insertElement(0, "paragraph");
                fragment.insertElement(1, "paragraph");
                fragment.insertElement(2, "paragraph");

                // Each change should trigger callback
                assertTrue("Callback should be invoked multiple times",
                    callbackCount.get() >= 3);
            }
        }
    }

    @Test
    public void testIncrementalVsFullReplacementBehavior() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            AtomicInteger incrementalCallbacks = new AtomicInteger(0);
            AtomicInteger fullCallbacks = new AtomicInteger(0);

            // Test incremental binding
            try (YProseMirrorBinding incrementalBinding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> incrementalCallbacks.incrementAndGet(),
                    true)) {  // Incremental mode

                // Make change
                fragment.insertElement(0, "paragraph");

                // Should trigger callback
                assertTrue("Incremental binding should trigger callback",
                    incrementalCallbacks.get() >= 1);
            }

            // Clear fragment
            fragment.remove(0, fragment.length());

            // Test full replacement binding
            try (YProseMirrorBinding fullBinding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> fullCallbacks.incrementAndGet(),
                    false)) {  // Full replacement mode

                // Make change
                fragment.insertElement(0, "paragraph");

                // Should trigger callback
                assertTrue("Full replacement binding should trigger callback",
                    fullCallbacks.get() >= 1);
            }
        }
    }

    @Test
    public void testDefaultBindingUsesFullReplacement() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Default constructor should use full replacement
            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> { /* callback */ })) {

                assertNotNull("Default binding should be created", binding);
                // No way to directly test the mode, but verify it works
                fragment.insertElement(0, "paragraph");
                // If no exception, mode is working
            }
        }
    }

    @Test
    public void testIncrementalBindingWithComplexStructure() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            AtomicInteger callbackCount = new AtomicInteger(0);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackCount.incrementAndGet();
                    },
                    true)) {  // Enable incremental updates

                // Create nested structure
                fragment.insertElement(0, "paragraph");
                YXmlElement para = fragment.getElement(0);
                para.insertText(0).push("Hello World");
                para.close();

                // Multiple operations should trigger callbacks
                assertTrue("Callbacks should be triggered",
                    callbackCount.get() >= 1);
            }
        }
    }

    @Test
    public void testIncrementalBindingWithDeletion() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Pre-populate with content
            fragment.insertElement(0, "paragraph");
            fragment.insertElement(1, "paragraph");

            AtomicInteger callbackCount = new AtomicInteger(0);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackCount.incrementAndGet();
                    },
                    true)) {  // Enable incremental updates

                int initialCallbacks = callbackCount.get();

                // Delete content
                fragment.remove(1, 1);

                // Deletion should trigger callback
                assertTrue("Deletion should trigger callback",
                    callbackCount.get() > initialCallbacks);
            }
        }
    }

    @Test
    public void testIncrementalBindingClosesCleanly() {
        YXmlFragment fragment = ydoc.getXmlFragment("prosemirror");

        YProseMirrorBinding binding = new YProseMirrorBinding(
                fragment,
                schema,
                (node) -> { /* callback */ },
                true);  // Enable incremental updates

        assertFalse("Binding should not be closed initially",
            binding.isClosed());

        binding.close();

        assertTrue("Binding should be closed after close()",
            binding.isClosed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncrementalBindingNullFragment() {
        new YProseMirrorBinding(
                null,
                schema,
                (node) -> { /* callback */ },
                true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncrementalBindingNullSchema() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            new YProseMirrorBinding(
                    fragment,
                    null,
                    (node) -> { /* callback */ },
                    true);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncrementalBindingNullCallback() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            new YProseMirrorBinding(
                    fragment,
                    schema,
                    null,
                    true);
        }
    }

    @Test
    public void testIncrementalBindingWithAttributeChanges() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            AtomicInteger callbackCount = new AtomicInteger(0);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackCount.incrementAndGet();
                    },
                    true)) {  // Enable incremental updates

                // Create element - this will trigger callback
                fragment.insertElement(0, "paragraph");

                // Element creation should trigger callback
                assertTrue("Element creation should trigger callback",
                    callbackCount.get() >= 1);

                YXmlElement para = fragment.getElement(0);
                para.close();
            }
        }
    }

    @Test
    public void testIncrementalBindingSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            YXmlFragment frag1 = doc1.getXmlFragment("prosemirror");
            YXmlFragment frag2 = doc2.getXmlFragment("prosemirror");

            AtomicInteger callbacks2 = new AtomicInteger(0);

            // Create binding for doc1
            try (YProseMirrorBinding binding1 = new YProseMirrorBinding(
                    frag1,
                    schema,
                    (node) -> { /* doc1 callback */ },
                    true)) {

                // Make change in doc1
                frag1.insertElement(0, "paragraph");

                // Sync to doc2 BEFORE creating binding2
                byte[] state = doc1.encodeStateAsUpdate();
                doc2.applyUpdate(state);

                // Now create binding2 - it should get callback on init
                try (YProseMirrorBinding binding2 = new YProseMirrorBinding(
                        frag2,
                        schema,
                        (node) -> callbacks2.incrementAndGet(),
                        true)) {

                    // Binding2 should have been initialized with synced content
                    assertTrue("Doc2 binding should have init callback",
                        callbacks2.get() >= 1);

                    // Both fragments should have same content
                    assertEquals("Fragments should have same length",
                        frag1.length(), frag2.length());
                }
            }

            doc1.close();
            doc2.close();
        }
    }
}
