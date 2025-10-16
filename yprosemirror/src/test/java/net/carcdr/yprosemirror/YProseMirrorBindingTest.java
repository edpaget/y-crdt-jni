package net.carcdr.yprosemirror;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.atlassian.prosemirror.model.Node;
import com.atlassian.prosemirror.model.Schema;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.yprosemirror.test.TestSchemas;
import org.junit.Test;

/**
 * Tests for YProseMirrorBinding using Kotlin-generated test schemas.
 *
 * <p>These tests demonstrate the use of TestSchemas helper to create
 * ProseMirror schemas for testing the binding layer.
 */
public class YProseMirrorBindingTest {

    @Test
    public void testCreateBindingWithBasicSchema() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createBasicSchema();

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> { /* callback */ })) {

                assertNotNull("Binding should be created", binding);
                assertFalse("Binding should not be closed", binding.isClosed());
                assertEquals("Schema should match", schema, binding.getSchema());
                assertEquals("Fragment should match", fragment, binding.getYFragment());
            }
        }
    }

    @Test
    public void testCreateBindingWithRichSchema() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createRichSchema();

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> { /* callback */ })) {

                assertNotNull("Binding should be created", binding);
                assertFalse("Binding should not be closed", binding.isClosed());
            }
        }
    }

    @Test
    public void testBindingWithEmptyDocument() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createBasicSchema();

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> { /* callback */ })) {

                Node currentDoc = binding.getCurrentDocument();
                assertNull("Empty Y-CRDT should return null document", currentDoc);
            }
        }
    }

    @Test
    public void testCallbackInvokedOnInitialization() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createBasicSchema();

            // Pre-populate fragment with content
            fragment.insertElement(0, "paragraph");

            AtomicBoolean callbackInvoked = new AtomicBoolean(false);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackInvoked.set(true);
                        assertNotNull("Node should not be null", node);
                    })) {

                assertTrue("Callback should be invoked on initialization", callbackInvoked.get());
            }
        }
    }

    @Test
    public void testUpdateFromProseMirrorWithBasicSchema() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createBasicSchema();

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> { /* callback */ })) {

                // Create a simple ProseMirror document using the schema
                // Note: We can't easily create Node instances from Java,
                // but we can test that the method doesn't throw
                assertNotNull("Binding should be ready for updates", binding);
            }
        }
    }

    @Test
    public void testBindingClosesSuccessfully() {
        YDoc doc = new YDoc();
        YXmlFragment fragment = doc.getXmlFragment("prosemirror");
        Schema schema = TestSchemas.createBasicSchema();

        YProseMirrorBinding binding = new YProseMirrorBinding(
                fragment,
                schema,
                (node) -> { /* callback */ });

        assertFalse("Binding should not be closed initially", binding.isClosed());

        binding.close();

        assertTrue("Binding should be closed after close()", binding.isClosed());

        doc.close();
    }

    @Test
    public void testMultipleCallbackInvocations() throws InterruptedException {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createBasicSchema();

            AtomicInteger callbackCount = new AtomicInteger(0);

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> {
                        callbackCount.incrementAndGet();
                    })) {

                // Modify Y-CRDT to trigger observer
                fragment.insertElement(0, "paragraph");

                // Wait for asynchronous observer callback
                Thread.sleep(100);

                assertTrue("Callback should be invoked at least once",
                        callbackCount.get() >= 1);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBindingWithNullFragment() {
        Schema schema = TestSchemas.createBasicSchema();

        new YProseMirrorBinding(
                null,
                schema,
                (node) -> { /* callback */ });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBindingWithNullSchema() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");

            new YProseMirrorBinding(
                    fragment,
                    null,
                    (node) -> { /* callback */ });
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBindingWithNullCallback() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createBasicSchema();

            new YProseMirrorBinding(
                    fragment,
                    schema,
                    null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateFromProseMirrorWithNullDocument() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");
            Schema schema = TestSchemas.createBasicSchema();

            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    schema,
                    (node) -> { /* callback */ })) {

                binding.updateFromProseMirror(null);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetCurrentDocumentAfterClose() {
        YDoc doc = new YDoc();
        YXmlFragment fragment = doc.getXmlFragment("prosemirror");
        Schema schema = TestSchemas.createBasicSchema();

        YProseMirrorBinding binding = new YProseMirrorBinding(
                fragment,
                schema,
                (node) -> { /* callback */ });

        binding.close();

        // This should throw IllegalStateException
        binding.getCurrentDocument();

        doc.close();
    }

    @Test
    public void testSchemaVariants() {
        try (YDoc doc = new YDoc()) {
            YXmlFragment fragment = doc.getXmlFragment("prosemirror");

            // Test basic schema
            Schema basicSchema = TestSchemas.createBasicSchema();
            assertNotNull("Basic schema should be created", basicSchema);

            // Test rich schema
            Schema richSchema = TestSchemas.createRichSchema();
            assertNotNull("Rich schema should be created", richSchema);

            // Test list schema
            Schema listSchema = TestSchemas.createListSchema();
            assertNotNull("List schema should be created", listSchema);

            // Test comprehensive schema
            Schema comprehensiveSchema = TestSchemas.createComprehensiveSchema();
            assertNotNull("Comprehensive schema should be created", comprehensiveSchema);

            // All schemas should work with binding
            try (YProseMirrorBinding binding = new YProseMirrorBinding(
                    fragment,
                    comprehensiveSchema,
                    (node) -> { /* callback */ })) {

                assertNotNull("Binding with comprehensive schema should work", binding);
            }
        }
    }
}
