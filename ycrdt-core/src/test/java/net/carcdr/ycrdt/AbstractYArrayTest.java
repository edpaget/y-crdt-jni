package net.carcdr.ycrdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

/**
 * Abstract test class for YArray implementations.
 *
 * <p>Subclasses must implement {@link #getBinding()} to provide the
 * implementation-specific binding.</p>
 */
public abstract class AbstractYArrayTest {

    /**
     * Returns the YBinding implementation to test.
     *
     * @return the binding to use for creating documents
     */
    protected abstract YBinding getBinding();

    /**
     * Returns whether the implementation supports getting values from arrays.
     * Some implementations may not have getString/getDouble implemented.
     *
     * @return true if getString/getDouble is supported
     */
    protected boolean supportsArrayGet() {
        return true;
    }

    @Test
    public void testGetArray() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("myarray")) {
            assertNotNull("YArray should be created", array);
            assertEquals("Initial length should be 0", 0, array.length());
        }
    }

    @Test
    public void testPushString() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("first");
            array.pushString("second");
            assertEquals(2, array.length());
        }
    }

    @Test
    public void testPushDouble() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushDouble(3.14);
            array.pushDouble(2.71);
            array.pushDouble(42.0);
            assertEquals(3, array.length());
        }
    }

    @Test
    public void testGetString() {
        assumeTrue("Skipping: implementation does not support array get operations",
                supportsArrayGet());
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("hello");
            array.pushString("world");
            assertEquals("hello", array.getString(0));
            assertEquals("world", array.getString(1));
        }
    }

    @Test
    public void testGetDouble() {
        assumeTrue("Skipping: implementation does not support array get operations",
                supportsArrayGet());
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushDouble(3.14);
            array.pushDouble(2.71);
            array.pushDouble(42.0);
            assertEquals(3.14, array.getDouble(0), 0.001);
            assertEquals(2.71, array.getDouble(1), 0.001);
            assertEquals(42.0, array.getDouble(2), 0.001);
        }
    }

    @Test
    public void testInsertString() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.insertString(0, "first");
            array.insertString(1, "second");
            assertEquals(2, array.length());
        }
    }

    @Test
    public void testInsertDouble() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.insertDouble(0, 1.0);
            array.insertDouble(1, 2.0);
            assertEquals(2, array.length());
            assertEquals(1.0, array.getDouble(0), 0.001);
            assertEquals(2.0, array.getDouble(1), 0.001);
        }
    }

    @Test
    public void testRemove() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("first");
            array.pushString("second");
            array.pushString("third");
            assertEquals(3, array.length());

            array.remove(1, 1);
            assertEquals(2, array.length());
        }
    }

    @Test
    public void testRemoveWithVerify() {
        assumeTrue("Skipping: implementation does not support array get operations",
                supportsArrayGet());
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("first");
            array.pushString("second");
            array.pushString("third");
            assertEquals(3, array.length());

            array.remove(1, 1);
            assertEquals(2, array.length());
            assertEquals("first", array.getString(0));
            assertEquals("third", array.getString(1));
        }
    }

    @Test
    public void testRemoveMultiple() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("a");
            array.pushString("b");
            array.pushString("c");
            array.pushString("d");
            assertEquals(4, array.length());

            array.remove(1, 2);
            assertEquals(2, array.length());
        }
    }

    @Test
    public void testRemoveMultipleWithVerify() {
        assumeTrue("Skipping: implementation does not support array get operations",
                supportsArrayGet());
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("a");
            array.pushString("b");
            array.pushString("c");
            array.pushString("d");
            assertEquals(4, array.length());

            array.remove(1, 2);
            assertEquals(2, array.length());
            assertEquals("a", array.getString(0));
            assertEquals("d", array.getString(1));
        }
    }

    @Test
    public void testArraySynchronization() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YArray arr1 = doc1.getArray("test");
             YArray arr2 = doc2.getArray("test")) {

            arr1.pushString("item1");
            arr1.pushString("item2");
            arr1.pushString("item3");
            assertEquals(3, arr1.length());
            assertEquals(0, arr2.length());

            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            assertEquals(3, arr2.length());
        }
    }

    @Test
    public void testArrayDoubleSync() {
        assumeTrue("Skipping: implementation does not support array get operations",
                supportsArrayGet());
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YArray arr1 = doc1.getArray("test");
             YArray arr2 = doc2.getArray("test")) {

            arr1.pushDouble(1.5);
            arr1.pushDouble(2.5);
            arr1.pushDouble(3.5);

            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            assertEquals(1.5, arr2.getDouble(0), 0.001);
            assertEquals(2.5, arr2.getDouble(1), 0.001);
            assertEquals(3.5, arr2.getDouble(2), 0.001);
        }
    }

    @Test
    public void testToJson() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("hello");
            array.pushString("world");
            array.pushDouble(42.0);

            String json = array.toJson();
            assertNotNull(json);
            assertTrue(json.contains("hello"));
            assertTrue(json.contains("world"));
            assertTrue(json.contains("42"));
        }
    }

    @Test
    public void testEmptyArrayToJson() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            String json = array.toJson();
            assertNotNull(json);
            assertEquals("[]", json);
        }
    }

    @Test
    public void testLength() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YArray array = doc.getArray("test")) {
            assertEquals(0, array.length());
            array.pushString("a");
            assertEquals(1, array.length());
            array.pushDouble(1.0);
            assertEquals(2, array.length());
            array.remove(0, 1);
            assertEquals(1, array.length());
        }
    }
}
