package net.carcdr.yprosemirror;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.atlassian.prosemirror.model.Schema;
import org.junit.Test;

/**
 * Tests Kotlin-Java interoperability with prosemirror-kotlin.
 *
 * <p>This test class verifies that we can successfully use the Kotlin-based
 * prosemirror-kotlin library from Java code without any interop issues.
 *
 * <p>Note: This is a minimal test to verify dependencies are working.
 * More comprehensive tests will be added as we implement the conversion logic.
 */
public class KotlinInteropTest {

    /**
     * Tests that we can load and instantiate Kotlin classes from Java.
     *
     * <p>This test verifies that:
     * <ul>
     *   <li>The prosemirror-kotlin dependency is correctly configured</li>
     *   <li>The kotlin-stdlib is properly included</li>
     *   <li>Kotlin-Java interop works at runtime</li>
     * </ul>
     */
    @Test
    public void testKotlinClassesAccessible() {
        // Verify we can reference Schema class from Kotlin
        Class<?> schemaClass = Schema.class;
        assertNotNull("Schema class should be accessible", schemaClass);
        assertTrue("Schema should be a Kotlin class", schemaClass.getName().contains("prosemirror"));
    }

    /**
     * Tests that Kotlin collections work from Java.
     */
    @Test
    public void testKotlinStdlibAvailable() {
        // Verify kotlin-stdlib is available by checking for Kotlin runtime classes
        try {
            Class.forName("kotlin.Unit");
            Class.forName("kotlin.collections.CollectionsKt");
            // If we get here, kotlin-stdlib is properly loaded
            assertTrue("Kotlin stdlib should be available", true);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Kotlin stdlib not found - check dependencies", e);
        }
    }
}
