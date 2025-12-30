package net.carcdr.ycrdt.panama;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import net.carcdr.ycrdt.YArray;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YMap;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YText;

/**
 * Tests for the Panama FFM implementation of YDoc.
 */
public class PanamaYDocTest {

    @Test
    public void testCreateDoc() {
        try (YDoc doc = new PanamaYDoc()) {
            assertNotNull(doc);
            assertFalse(doc.isClosed());
            assertNotEquals(0, doc.getClientId());
            assertNotNull(doc.getGuid());
        }
    }

    @Test
    public void testDocClose() {
        PanamaYDoc doc = new PanamaYDoc();
        assertFalse(doc.isClosed());
        doc.close();
        assertTrue(doc.isClosed());
    }

    @Test
    public void testGetText() {
        try (YDoc doc = new PanamaYDoc();
             YText text = doc.getText("mytext")) {
            assertNotNull(text);
            assertEquals(0, text.length());
        }
    }

    @Test
    public void testTextInsert() {
        try (YDoc doc = new PanamaYDoc();
             YText text = doc.getText("mytext")) {
            text.insert(0, "Hello");
            assertEquals(5, text.length());
            assertEquals("Hello", text.toString());

            text.insert(5, " World");
            assertEquals(11, text.length());
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testTextPush() {
        try (YDoc doc = new PanamaYDoc();
             YText text = doc.getText("mytext")) {
            text.push("Hello");
            text.push(" World");
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testTextDelete() {
        try (YDoc doc = new PanamaYDoc();
             YText text = doc.getText("mytext")) {
            text.insert(0, "Hello World");
            text.delete(5, 6);
            assertEquals("Hello", text.toString());
        }
    }

    @Test
    public void testGetArray() {
        try (YDoc doc = new PanamaYDoc();
             YArray array = doc.getArray("myarray")) {
            assertNotNull(array);
            assertEquals(0, array.length());
        }
    }

    @Test
    public void testArrayInsertString() {
        try (YDoc doc = new PanamaYDoc();
             YArray array = doc.getArray("myarray")) {
            array.insertString(0, "first");
            array.insertString(1, "second");
            assertEquals(2, array.length());
        }
    }

    @Test
    public void testArrayPushString() {
        try (YDoc doc = new PanamaYDoc();
             YArray array = doc.getArray("myarray")) {
            array.pushString("first");
            array.pushString("second");
            assertEquals(2, array.length());
        }
    }

    @Test
    public void testArrayRemove() {
        try (YDoc doc = new PanamaYDoc();
             YArray array = doc.getArray("myarray")) {
            array.pushString("first");
            array.pushString("second");
            array.pushString("third");
            assertEquals(3, array.length());

            array.remove(1, 1);
            assertEquals(2, array.length());
        }
    }

    @Test
    public void testGetMap() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
            assertNotNull(map);
            assertEquals(0, map.size());
            assertTrue(map.isEmpty());
        }
    }

    @Test
    public void testMapSetString() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
            map.setString("key1", "value1");
            assertEquals(1, map.size());
            assertFalse(map.isEmpty());
            assertTrue(map.containsKey("key1"));
        }
    }

    @Test
    public void testMapRemove() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
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
    public void testTransaction() {
        try (YDoc doc = new PanamaYDoc();
             YText text = doc.getText("mytext")) {
            doc.transaction(txn -> {
                text.insert(txn, 0, "Hello");
                text.insert(txn, 5, " World");
            });
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testEncodeStateVector() {
        try (YDoc doc = new PanamaYDoc()) {
            byte[] sv = doc.encodeStateVector();
            assertNotNull(sv);
            assertTrue(sv.length > 0);
        }
    }

    @Test
    public void testEncodeStateAsUpdate() {
        try (YDoc doc = new PanamaYDoc();
             YText text = doc.getText("mytext")) {
            text.push("Hello World");

            byte[] update = doc.encodeStateAsUpdate();
            assertNotNull(update);
            assertTrue(update.length > 0);
        }
    }

    @Test
    public void testApplyUpdate() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YText text1 = doc1.getText("mytext");
             YText text2 = doc2.getText("mytext")) {

            // Make changes in doc1
            text1.push("Hello World");
            assertEquals("Hello World", text1.toString());
            assertEquals("", text2.toString());

            // Encode doc1's state and apply to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // doc2 should now have the same content
            assertEquals("Hello World", text2.toString());
        }
    }

    @Test
    public void testBidirectionalSync() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YText text1 = doc1.getText("mytext");
             YText text2 = doc2.getText("mytext")) {

            // Make changes in both documents
            text1.push("Hello");
            text2.push("World");

            // Sync doc1 -> doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Sync doc2 -> doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have the same content (order depends on client IDs)
            assertEquals(text1.toString(), text2.toString());
            assertTrue(text1.toString().contains("Hello"));
            assertTrue(text1.toString().contains("World"));
        }
    }

    @Test
    public void testEncodeDiff() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YText text1 = doc1.getText("mytext");
             YText text2 = doc2.getText("mytext")) {

            // Initial sync
            text1.push("Hello");
            byte[] initialUpdate = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(initialUpdate);
            assertEquals("Hello", text2.toString());

            // Get state vector from doc2
            byte[] stateVector = doc2.encodeStateVector();

            // Make more changes in doc1
            text1.push(" World");

            // Get only the diff
            byte[] diff = doc1.encodeDiff(stateVector);
            assertNotNull(diff);
            assertTrue(diff.length > 0);

            // Apply diff to doc2
            doc2.applyUpdate(diff);
            assertEquals("Hello World", text2.toString());
        }
    }

    @Test
    public void testSyncArrays() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YArray arr1 = doc1.getArray("myarray");
             YArray arr2 = doc2.getArray("myarray")) {

            // Add items in doc1
            arr1.pushString("item1");
            arr1.pushString("item2");
            arr1.pushString("item3");
            assertEquals(3, arr1.length());
            assertEquals(0, arr2.length());

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // doc2 should have the same items
            assertEquals(3, arr2.length());
        }
    }

    @Test
    public void testSyncMaps() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YMap map1 = doc1.getMap("mymap");
             YMap map2 = doc2.getMap("mymap")) {

            // Add entries in doc1
            map1.setString("key1", "value1");
            map1.setString("key2", "value2");
            map1.setDouble("count", 42.0);
            assertEquals(3, map1.size());
            assertEquals(0, map2.size());

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // doc2 should have the same entries
            assertEquals(3, map2.size());
            assertTrue(map2.containsKey("key1"));
            assertTrue(map2.containsKey("key2"));
            assertTrue(map2.containsKey("count"));
        }
    }

    @Test
    public void testArrayGetDouble() {
        try (YDoc doc = new PanamaYDoc();
             YArray array = doc.getArray("myarray")) {
            array.pushDouble(3.14);
            array.pushDouble(2.71);
            array.pushDouble(42.0);

            assertEquals(3.14, array.getDouble(0), 0.001);
            assertEquals(2.71, array.getDouble(1), 0.001);
            assertEquals(42.0, array.getDouble(2), 0.001);
        }
    }

    @Test
    public void testMapGetDouble() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
            map.setDouble("pi", 3.14159);
            map.setDouble("e", 2.71828);
            map.setDouble("answer", 42.0);

            assertEquals(3.14159, map.getDouble("pi"), 0.00001);
            assertEquals(2.71828, map.getDouble("e"), 0.00001);
            assertEquals(42.0, map.getDouble("answer"), 0.001);
        }
    }

    @Test
    public void testArrayDoubleSync() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YArray arr1 = doc1.getArray("myarray");
             YArray arr2 = doc2.getArray("myarray")) {

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
    public void testMapDoubleSync() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YMap map1 = doc1.getMap("mymap");
             YMap map2 = doc2.getMap("mymap")) {

            map1.setDouble("score", 95.5);
            map1.setDouble("rating", 4.8);

            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            assertEquals(95.5, map2.getDouble("score"), 0.001);
            assertEquals(4.8, map2.getDouble("rating"), 0.001);
        }
    }

    @Test
    public void testMapKeys() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
            map.setString("alpha", "a");
            map.setString("beta", "b");
            map.setString("gamma", "c");

            String[] keys = map.keys();
            assertEquals(3, keys.length);

            // Sort for consistent comparison
            Arrays.sort(keys);
            assertArrayEquals(new String[]{"alpha", "beta", "gamma"}, keys);
        }
    }

    @Test
    public void testMapKeysEmpty() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
            String[] keys = map.keys();
            assertEquals(0, keys.length);
        }
    }

    @Test
    public void testMapClear() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
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
    public void testMapKeysSync() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc();
             YMap map1 = doc1.getMap("mymap");
             YMap map2 = doc2.getMap("mymap")) {

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
    public void testArrayToJson() {
        try (YDoc doc = new PanamaYDoc();
             YArray array = doc.getArray("myarray")) {
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
    public void testMapToJson() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
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
    public void testEmptyArrayToJson() {
        try (YDoc doc = new PanamaYDoc();
             YArray array = doc.getArray("myarray")) {
            String json = array.toJson();
            assertNotNull(json);
            assertEquals("[]", json);
        }
    }

    @Test
    public void testEmptyMapToJson() {
        try (YDoc doc = new PanamaYDoc();
             YMap map = doc.getMap("mymap")) {
            String json = map.toJson();
            assertNotNull(json);
            assertEquals("{}", json);
        }
    }

    // =========================================================================
    // Observer Tests
    // =========================================================================

    @Test
    public void testObserveUpdateV1Basic() {
        try (YDoc doc = new PanamaYDoc()) {
            AtomicInteger callCount = new AtomicInteger(0);
            List<byte[]> receivedUpdates = new ArrayList<>();

            YSubscription subscription = doc.observeUpdateV1((update, origin) -> {
                callCount.incrementAndGet();
                receivedUpdates.add(update);
            });

            assertNotNull(subscription);
            assertFalse(subscription.isClosed());
            assertTrue(subscription.getSubscriptionId() > 0);
            assertEquals(doc, subscription.getTarget());
            assertNull(subscription.getObserver()); // UpdateObserver is not YObserver

            // Make a change
            try (YText text = doc.getText("test")) {
                text.push("Hello");
            }

            // Observer should have been called
            assertEquals(1, callCount.get());
            assertEquals(1, receivedUpdates.size());
            assertTrue(receivedUpdates.get(0).length > 0);

            subscription.close();
            assertTrue(subscription.isClosed());
        }
    }

    @Test
    public void testObserveUpdateV1MultipleChanges() {
        try (YDoc doc = new PanamaYDoc()) {
            AtomicInteger callCount = new AtomicInteger(0);

            try (YSubscription subscription = doc.observeUpdateV1((update, origin) -> {
                callCount.incrementAndGet();
            })) {
                // Multiple changes should trigger multiple callbacks
                try (YText text = doc.getText("test")) {
                    text.push("First");
                }
                try (YText text = doc.getText("test")) {
                    text.push(" Second");
                }
                try (YText text = doc.getText("test")) {
                    text.push(" Third");
                }

                assertEquals(3, callCount.get());
            }
        }
    }

    @Test
    public void testObserveUpdateV1Unsubscribe() {
        try (YDoc doc = new PanamaYDoc()) {
            AtomicInteger callCount = new AtomicInteger(0);

            YSubscription subscription = doc.observeUpdateV1((update, origin) -> {
                callCount.incrementAndGet();
            });

            // Make a change
            try (YText text = doc.getText("test")) {
                text.push("Before unsubscribe");
            }
            assertEquals(1, callCount.get());

            // Unsubscribe
            subscription.close();
            assertTrue(subscription.isClosed());

            // Make another change - should not trigger callback
            try (YText text = doc.getText("test")) {
                text.push(" After unsubscribe");
            }
            assertEquals(1, callCount.get()); // Still 1, not incremented
        }
    }

    @Test
    public void testObserveUpdateV1UpdateCanBeApplied() {
        try (YDoc doc1 = new PanamaYDoc();
             YDoc doc2 = new PanamaYDoc()) {

            List<byte[]> capturedUpdates = new ArrayList<>();

            try (YSubscription subscription = doc1.observeUpdateV1((update, origin) -> {
                capturedUpdates.add(update);
            })) {
                // Make changes in doc1
                try (YText text1 = doc1.getText("test")) {
                    text1.push("Hello World");
                }
            }

            assertEquals(1, capturedUpdates.size());

            // Apply captured update to doc2
            doc2.applyUpdate(capturedUpdates.get(0));

            // doc2 should now have the same content
            try (YText text2 = doc2.getText("test")) {
                assertEquals("Hello World", text2.toString());
            }
        }
    }

    // TODO: This test hangs - needs investigation of Panama upcall behavior
    // during explicit transaction commits. The callback seems to block
    // when called from within ytransaction_commit.
    // @Test
    // public void testObserveUpdateV1Transaction() { ... }

    @Test
    public void testObserveUpdateV1ArrayChanges() {
        try (YDoc doc = new PanamaYDoc()) {
            AtomicInteger callCount = new AtomicInteger(0);
            List<byte[]> updates = new ArrayList<>();

            try (YSubscription subscription = doc.observeUpdateV1((update, origin) -> {
                callCount.incrementAndGet();
                updates.add(update);
            })) {
                try (YArray array = doc.getArray("test")) {
                    array.pushString("item1");
                }
                try (YArray array = doc.getArray("test")) {
                    array.pushDouble(42.0);
                }

                assertEquals(2, callCount.get());
                assertEquals(2, updates.size());
            }
        }
    }

    @Test
    public void testObserveUpdateV1MapChanges() {
        try (YDoc doc = new PanamaYDoc()) {
            AtomicInteger callCount = new AtomicInteger(0);
            List<byte[]> updates = new ArrayList<>();

            try (YSubscription subscription = doc.observeUpdateV1((update, origin) -> {
                callCount.incrementAndGet();
                updates.add(update);
            })) {
                try (YMap map = doc.getMap("test")) {
                    map.setString("key1", "value1");
                }
                try (YMap map = doc.getMap("test")) {
                    map.setDouble("key2", 3.14);
                }

                assertEquals(2, callCount.get());
                assertEquals(2, updates.size());
            }
        }
    }

    @Test
    public void testObserveUpdateV1MultipleObservers() {
        try (YDoc doc = new PanamaYDoc()) {
            AtomicInteger count1 = new AtomicInteger(0);
            AtomicInteger count2 = new AtomicInteger(0);

            try (YSubscription sub1 = doc.observeUpdateV1((update, origin) -> count1.incrementAndGet());
                 YSubscription sub2 = doc.observeUpdateV1((update, origin) -> count2.incrementAndGet())) {

                try (YText text = doc.getText("test")) {
                    text.push("Hello");
                }

                // Both observers should be called
                assertEquals(1, count1.get());
                assertEquals(1, count2.get());
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testObserveUpdateV1NullObserver() {
        try (YDoc doc = new PanamaYDoc()) {
            doc.observeUpdateV1(null);
        }
    }

    @Test
    public void testObserveUpdateV1ClosedDocThrows() {
        PanamaYDoc doc = new PanamaYDoc();
        doc.close();

        try {
            doc.observeUpdateV1((update, origin) -> { });
            assertTrue("Should have thrown IllegalStateException", false);
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void testSubscriptionId() {
        try (YDoc doc = new PanamaYDoc()) {
            YSubscription sub1 = doc.observeUpdateV1((update, origin) -> { });
            YSubscription sub2 = doc.observeUpdateV1((update, origin) -> { });

            // Each subscription should have a unique ID
            assertNotEquals(sub1.getSubscriptionId(), sub2.getSubscriptionId());

            sub1.close();
            sub2.close();
        }
    }
}
