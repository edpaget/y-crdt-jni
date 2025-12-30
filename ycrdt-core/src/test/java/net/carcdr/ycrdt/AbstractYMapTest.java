package net.carcdr.ycrdt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * Abstract test class for YMap implementations.
 *
 * <p>Subclasses must implement {@link #getBinding()} to provide the
 * implementation-specific binding.</p>
 */
public abstract class AbstractYMapTest {

    /**
     * Returns the YBinding implementation to test.
     *
     * @return the binding to use for creating documents
     */
    protected abstract YBinding getBinding();

    /**
     * Returns whether the implementation supports getting values from maps.
     * Some implementations may not have getString/getDouble implemented.
     *
     * @return true if getString/getDouble is supported
     */
    protected boolean supportsMapGet() {
        return true;
    }

    @Test
    public void testGetMap() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("mymap")) {
            assertNotNull("YMap should be created", map);
            assertEquals("Initial size should be 0", 0, map.size());
            assertTrue("Map should be empty", map.isEmpty());
        }
    }

    @Test
    public void testSetString() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key1", "value1");
            assertEquals(1, map.size());
            assertFalse(map.isEmpty());
            assertTrue(map.containsKey("key1"));
        }
    }

    @Test
    public void testSetDouble() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setDouble("pi", 3.14159);
            map.setDouble("e", 2.71828);
            assertEquals(2, map.size());
        }
    }

    @Test
    public void testGetString() {
        assumeTrue("Skipping: implementation does not support map get operations",
                supportsMapGet());
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            assertEquals("value1", map.getString("key1"));
            assertEquals("value2", map.getString("key2"));
        }
    }

    @Test
    public void testGetDouble() {
        assumeTrue("Skipping: implementation does not support map get operations",
                supportsMapGet());
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setDouble("pi", 3.14159);
            map.setDouble("e", 2.71828);
            map.setDouble("answer", 42.0);
            assertEquals(3.14159, map.getDouble("pi"), 0.00001);
            assertEquals(2.71828, map.getDouble("e"), 0.00001);
            assertEquals(42.0, map.getDouble("answer"), 0.001);
        }
    }

    @Test
    public void testRemove() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            assertEquals(2, map.size());

            map.remove("key1");
            assertEquals(1, map.size());
            assertFalse(map.containsKey("key1"));
            assertTrue(map.containsKey("key2"));
        }
    }

    @Test
    public void testClear() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            map.setDouble("key3", 42.0);
            assertEquals(3, map.size());

            map.clear();
            assertEquals(0, map.size());
            assertTrue(map.isEmpty());
        }
    }

    @Test
    public void testKeys() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("alpha", "a");
            map.setString("beta", "b");
            map.setString("gamma", "c");

            String[] keys = map.keys();
            assertEquals(3, keys.length);

            Arrays.sort(keys);
            assertArrayEquals(new String[]{"alpha", "beta", "gamma"}, keys);
        }
    }

    @Test
    public void testKeysEmpty() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            String[] keys = map.keys();
            assertEquals(0, keys.length);
        }
    }

    @Test
    public void testSize() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            assertEquals(0, map.size());
            map.setString("a", "1");
            assertEquals(1, map.size());
            map.setString("b", "2");
            assertEquals(2, map.size());
            map.remove("a");
            assertEquals(1, map.size());
        }
    }

    @Test
    public void testContainsKey() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            assertFalse(map.containsKey("key"));
            map.setString("key", "value");
            assertTrue(map.containsKey("key"));
            map.remove("key");
            assertFalse(map.containsKey("key"));
        }
    }

    @Test
    public void testMapSynchronization() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YMap map1 = doc1.getMap("test");
             YMap map2 = doc2.getMap("test")) {

            map1.setString("key1", "value1");
            map1.setString("key2", "value2");
            map1.setDouble("count", 42.0);
            assertEquals(3, map1.size());
            assertEquals(0, map2.size());

            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            assertEquals(3, map2.size());
            assertTrue(map2.containsKey("key1"));
            assertTrue(map2.containsKey("key2"));
            assertTrue(map2.containsKey("count"));
        }
    }

    @Test
    public void testMapDoubleSync() {
        assumeTrue("Skipping: implementation does not support map get operations",
                supportsMapGet());
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YMap map1 = doc1.getMap("test");
             YMap map2 = doc2.getMap("test")) {

            map1.setDouble("score", 95.5);
            map1.setDouble("rating", 4.8);

            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            assertEquals(95.5, map2.getDouble("score"), 0.001);
            assertEquals(4.8, map2.getDouble("rating"), 0.001);
        }
    }

    @Test
    public void testMapKeysSync() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YMap map1 = doc1.getMap("test");
             YMap map2 = doc2.getMap("test")) {

            map1.setString("one", "1");
            map1.setString("two", "2");
            map1.setString("three", "3");

            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            String[] keys1 = map1.keys();
            String[] keys2 = map2.keys();

            Arrays.sort(keys1);
            Arrays.sort(keys2);
            assertArrayEquals(keys1, keys2);
        }
    }

    @Test
    public void testToJson() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("name", "Alice");
            map.setDouble("age", 30.0);

            String json = map.toJson();
            assertNotNull(json);
            assertTrue(json.contains("name"));
            assertTrue(json.contains("Alice"));
            assertTrue(json.contains("age"));
            assertTrue(json.contains("30"));
        }
    }

    @Test
    public void testEmptyMapToJson() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            String json = map.toJson();
            assertNotNull(json);
            assertEquals("{}", json);
        }
    }

    @Test
    public void testOverwriteValue() {
        assumeTrue("Skipping: implementation does not support map get operations",
                supportsMapGet());
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key", "old");
            assertEquals("old", map.getString("key"));
            map.setString("key", "new");
            assertEquals("new", map.getString("key"));
            assertEquals(1, map.size());
        }
    }

    @Test
    public void testOverwriteValueBasic() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key", "old");
            assertEquals(1, map.size());
            map.setString("key", "new");
            assertEquals(1, map.size());
        }
    }
}
