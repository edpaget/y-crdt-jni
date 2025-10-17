package net.carcdr.ycrdt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for YMap.
 */
public class YMapTest {

    @Test
    public void testMapCreation() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            assertNotNull(map);
            assertEquals(0, map.size());
            assertTrue(map.isEmpty());
        }
    }

    @Test
    public void testSetAndGetString() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("name", "Alice");
            map.setString("city", "Wonderland");

            assertEquals(2, map.size());
            assertEquals("Alice", map.getString("name"));
            assertEquals("Wonderland", map.getString("city"));
        }
    }

    @Test
    public void testSetAndGetDouble() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setDouble("age", 30.0);
            map.setDouble("height", 165.5);

            assertEquals(2, map.size());
            assertEquals(30.0, map.getDouble("age"), 0.001);
            assertEquals(165.5, map.getDouble("height"), 0.001);
        }
    }

    @Test
    public void testMixedTypes() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("name", "Bob");
            map.setDouble("age", 25.0);
            map.setString("occupation", "Engineer");
            map.setDouble("salary", 75000.0);

            assertEquals(4, map.size());
            assertEquals("Bob", map.getString("name"));
            assertEquals(25.0, map.getDouble("age"), 0.001);
            assertEquals("Engineer", map.getString("occupation"));
            assertEquals(75000.0, map.getDouble("salary"), 0.001);
        }
    }

    @Test
    public void testRemove() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            map.setString("key3", "value3");

            assertEquals(3, map.size());

            map.remove("key2");

            assertEquals(2, map.size());
            assertNull(map.getString("key2"));
            assertEquals("value1", map.getString("key1"));
            assertEquals("value3", map.getString("key3"));
        }
    }

    @Test
    public void testContainsKey() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("exists", "yes");

            assertTrue(map.containsKey("exists"));
            assertFalse(map.containsKey("nothere"));
        }
    }

    @Test
    public void testKeys() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            map.setString("key3", "value3");

            String[] keys = map.keys();
            assertEquals(3, keys.length);

            // Verify all keys are present (order may vary)
            boolean hasKey1 = false;
            boolean hasKey2 = false;
            boolean hasKey3 = false;

            for (String key : keys) {
                if ("key1".equals(key)) {
                    hasKey1 = true;
                }
                if ("key2".equals(key)) {
                    hasKey2 = true;
                }
                if ("key3".equals(key)) {
                    hasKey3 = true;
                }
            }

            assertTrue(hasKey1);
            assertTrue(hasKey2);
            assertTrue(hasKey3);
        }
    }

    @Test
    public void testClear() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            map.setString("key3", "value3");

            assertEquals(3, map.size());

            map.clear();

            assertEquals(0, map.size());
            assertTrue(map.isEmpty());
        }
    }

    @Test
    public void testToJson() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("name", "Alice");
            map.setDouble("age", 30.0);

            String json = map.toJson();
            assertNotNull(json);
            assertTrue(json.contains("Alice"));
            assertTrue(json.contains("30"));
        }
    }

    @Test
    public void testGetNonExistentKey() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            assertNull(map.getString("nothere"));
            assertEquals(0.0, map.getDouble("nothere"), 0.001);
        }
    }

    @Test
    public void testOverwriteValue() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key", "value1");
            assertEquals("value1", map.getString("key"));

            map.setString("key", "value2");
            assertEquals("value2", map.getString("key"));
            assertEquals(1, map.size());
        }
    }

    @Test
    public void testSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Make changes in doc1
            try (YMap map1 = doc1.getMap("shared")) {
                map1.setString("name", "Alice");
                map1.setDouble("age", 30.0);
            }

            // Sync from doc1 to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Get map from doc2 after sync
            try (YMap map2 = doc2.getMap("shared")) {
                assertEquals(2, map2.size());
                assertEquals("Alice", map2.getString("name"));
                assertEquals(30.0, map2.getDouble("age"), 0.001);
            }
        }
    }

    @Test
    public void testBidirectionalSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Changes in doc1
            try (YMap map1 = doc1.getMap("shared")) {
                map1.setString("key1", "From Doc1");
            }

            // Changes in doc2
            try (YMap map2 = doc2.getMap("shared")) {
                map2.setString("key2", "From Doc2");
            }

            // Sync doc1 to doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Sync doc2 to doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have both entries
            try (YMap map1 = doc1.getMap("shared");
                 YMap map2 = doc2.getMap("shared")) {
                assertEquals(map1.size(), map2.size());
                assertEquals(2, map1.size());

                // Verify both maps have both keys with correct values
                assertEquals("From Doc1", map1.getString("key1"));
                assertEquals("From Doc2", map1.getString("key2"));
                assertEquals("From Doc1", map2.getString("key1"));
                assertEquals("From Doc2", map2.getString("key2"));
            }
        }
    }

    @Test
    public void testMultipleMapsInDocument() {
        try (YDoc doc = new YDoc();
             YMap map1 = doc.getMap("map1");
             YMap map2 = doc.getMap("map2")) {

            map1.setString("location", "First");
            map2.setString("location", "Second");

            assertEquals("First", map1.getString("location"));
            assertEquals("Second", map2.getString("location"));
        }
    }

    @Test
    public void testMapClosed() {
        YDoc doc = new YDoc();
        YMap map = doc.getMap("test");
        map.setString("key", "value");

        map.close();
        assertTrue(map.isClosed());

        try {
            map.setString("key2", "value2");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        doc.close();
    }

    @Test
    public void testMapWithClosedDoc() {
        YDoc doc = new YDoc();
        YMap map = doc.getMap("test");

        doc.close();

        map.close();
        assertTrue(map.isClosed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetStringNullKey() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString(null, "value");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetStringNullValue() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString("key", null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDoubleNullKey() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setDouble(null, 42.0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStringNullKey() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.getString(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDoubleNullKey() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.getDouble(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNullKey() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.remove(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContainsKeyNull() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.containsKey(null);
        }
    }

    @Test
    public void testEmptyMap() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            assertEquals(0, map.size());
            assertTrue(map.isEmpty());
            assertFalse(map.isClosed());

            String[] keys = map.keys();
            assertEquals(0, keys.length);
        }
    }

    @Test
    public void testGetSameMapTwice() {
        try (YDoc doc = new YDoc();
             YMap map1 = doc.getMap("shared")) {
            map1.setString("key", "value");

            // Get the same map again
            try (YMap map2 = doc.getMap("shared")) {
                // Both should show the same content
                assertEquals(1, map1.size());
                assertEquals(1, map2.size());
                assertEquals("value", map1.getString("key"));
                assertEquals("value", map2.getString("key"));

                map2.setString("key2", "value2");

                // Both should reflect the change
                assertEquals(2, map1.size());
                assertEquals(2, map2.size());
                assertEquals("value2", map1.getString("key2"));
                assertEquals("value2", map2.getString("key2"));
            }
        }
    }

    @Test
    public void testComplexOperationSequence() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            // Build up map
            map.setString("name", "Alice");
            map.setString("city", "NYC");
            map.setDouble("age", 30.0);
            assertEquals(3, map.size());

            // Update value
            map.setString("city", "SF");
            assertEquals("SF", map.getString("city"));
            assertEquals(3, map.size());

            // Remove key
            map.remove("age");
            assertEquals(2, map.size());
            assertFalse(map.containsKey("age"));

            // Add more
            map.setDouble("score", 95.5);
            map.setString("status", "active");
            assertEquals(4, map.size());

            String json = map.toJson();
            assertTrue(json.contains("Alice"));
            assertTrue(json.contains("SF"));
            assertTrue(json.contains("95.5"));
            assertTrue(json.contains("active"));
        }
    }

    // Transaction-based tests

    @Test
    public void testSetStringWithTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                map.setString(txn, "name", "Alice");
                map.setString(txn, "city", "NYC");
            }

            assertEquals(2, map.size());
            assertEquals("Alice", map.getString("name"));
            assertEquals("NYC", map.getString("city"));
        }
    }

    @Test
    public void testSetDoubleWithTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                map.setDouble(txn, "age", 30.0);
                map.setDouble(txn, "height", 165.5);
            }

            assertEquals(2, map.size());
            assertEquals(30.0, map.getDouble("age"), 0.001);
            assertEquals(165.5, map.getDouble("height"), 0.001);
        }
    }

    @Test
    public void testRemoveWithTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            // Setup data
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            map.setString("key3", "value3");
            assertEquals(3, map.size());

            // Remove multiple keys in one transaction
            try (YTransaction txn = doc.beginTransaction()) {
                map.remove(txn, "key1");
                map.remove(txn, "key3");
            }

            assertEquals(1, map.size());
            assertEquals("value2", map.getString("key2"));
            assertNull(map.getString("key1"));
            assertNull(map.getString("key3"));
        }
    }

    @Test
    public void testClearWithTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            // Setup data
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            map.setDouble("key3", 42.0);
            assertEquals(3, map.size());

            // Clear in transaction
            try (YTransaction txn = doc.beginTransaction()) {
                map.clear(txn);
            }

            assertEquals(0, map.size());
            assertTrue(map.isEmpty());
        }
    }

    @Test
    public void testSetDocWithTransaction() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YMap map = parent.getMap("test")) {
            try (YTransaction txn = parent.beginTransaction()) {
                map.setDoc(txn, "nested", child);
                map.setString(txn, "type", "subdocument");
            }

            assertEquals(2, map.size());
            assertEquals("subdocument", map.getString("type"));

            try (YDoc retrieved = map.getDoc("nested")) {
                assertNotNull(retrieved);
            }
        }
    }

    @Test
    public void testBatchMultipleOperationsInTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            // Batch multiple different operations in one transaction
            try (YTransaction txn = doc.beginTransaction()) {
                map.setString(txn, "name", "Bob");
                map.setDouble(txn, "age", 25.0);
                map.setString(txn, "city", "NYC");
                map.setDouble(txn, "score", 95.5);
            }

            assertEquals(4, map.size());
            assertEquals("Bob", map.getString("name"));
            assertEquals(25.0, map.getDouble("age"), 0.001);
            assertEquals("NYC", map.getString("city"));
            assertEquals(95.5, map.getDouble("score"), 0.001);
        }
    }

    @Test
    public void testTransactionWithMixedOperations() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            // Setup initial data
            map.setString("key1", "value1");
            map.setString("key2", "value2");
            map.setDouble("score", 50.0);

            // Perform mixed operations in transaction
            try (YTransaction txn = doc.beginTransaction()) {
                map.setString(txn, "key1", "updated");
                map.remove(txn, "key2");
                map.setDouble(txn, "score", 100.0);
                map.setString(txn, "key3", "new");
            }

            assertEquals(3, map.size());
            assertEquals("updated", map.getString("key1"));
            assertNull(map.getString("key2"));
            assertEquals(100.0, map.getDouble("score"), 0.001);
            assertEquals("new", map.getString("key3"));
        }
    }

    @Test
    public void testTransactionCommitBehavior() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            // Changes should not be visible until transaction commits
            try (YTransaction txn = doc.beginTransaction()) {
                map.setString(txn, "key", "value");
            } // Commits here

            // Now visible
            assertEquals(1, map.size());
            assertEquals("value", map.getString("key"));
        }
    }

    @Test
    public void testMultipleSequentialTransactions() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            // First transaction
            try (YTransaction txn = doc.beginTransaction()) {
                map.setString(txn, "key1", "value1");
            }

            assertEquals(1, map.size());

            // Second transaction
            try (YTransaction txn = doc.beginTransaction()) {
                map.setString(txn, "key2", "value2");
            }

            assertEquals(2, map.size());

            // Third transaction
            try (YTransaction txn = doc.beginTransaction()) {
                map.setDouble(txn, "key3", 42.0);
            }

            assertEquals(3, map.size());
            assertEquals("value1", map.getString("key1"));
            assertEquals("value2", map.getString("key2"));
            assertEquals(42.0, map.getDouble("key3"), 0.001);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetStringWithNullTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setString(null, "key", "value");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDoubleWithNullTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.setDouble(null, "key", 42.0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveWithNullTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.remove(null, "key");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClearWithNullTransaction() {
        try (YDoc doc = new YDoc();
             YMap map = doc.getMap("test")) {
            map.clear(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDocWithNullTransaction() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YMap map = parent.getMap("test")) {
            map.setDoc(null, "key", child);
        }
    }
}
