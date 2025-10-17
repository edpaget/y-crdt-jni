package net.carcdr.ycrdt;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for YArray transaction support.
 */
public class YArrayTransactionTest {

    @Test
    public void testInsertStringWithTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                array.insertString(txn, 0, "Hello");
                array.insertString(txn, 1, "World");
            }

            assertEquals(2, array.length());
            assertEquals("Hello", array.getString(0));
            assertEquals("World", array.getString(1));
        }
    }

    @Test
    public void testInsertDoubleWithTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                array.insertDouble(txn, 0, 1.0);
                array.insertDouble(txn, 1, 2.0);
                array.insertDouble(txn, 2, 3.0);
            }

            assertEquals(3, array.length());
            assertEquals(1.0, array.getDouble(0), 0.001);
            assertEquals(2.0, array.getDouble(1), 0.001);
            assertEquals(3.0, array.getDouble(2), 0.001);
        }
    }

    @Test
    public void testPushStringWithTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                array.pushString(txn, "First");
                array.pushString(txn, "Second");
                array.pushString(txn, "Third");
            }

            assertEquals(3, array.length());
            assertEquals("First", array.getString(0));
            assertEquals("Second", array.getString(1));
            assertEquals("Third", array.getString(2));
        }
    }

    @Test
    public void testPushDoubleWithTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                array.pushDouble(txn, 10.5);
                array.pushDouble(txn, 20.5);
                array.pushDouble(txn, 30.5);
            }

            assertEquals(3, array.length());
            assertEquals(10.5, array.getDouble(0), 0.001);
            assertEquals(20.5, array.getDouble(1), 0.001);
            assertEquals(30.5, array.getDouble(2), 0.001);
        }
    }

    @Test
    public void testRemoveWithTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("A");
            array.pushString("B");
            array.pushString("C");
            array.pushString("D");

            try (YTransaction txn = doc.beginTransaction()) {
                array.remove(txn, 1, 2); // Remove "B" and "C"
            }

            assertEquals(2, array.length());
            assertEquals("A", array.getString(0));
            assertEquals("D", array.getString(1));
        }
    }

    @Test
    public void testInsertDocWithTransaction() {
        try (YDoc parent = new YDoc();
             YDoc child = new YDoc();
             YArray array = parent.getArray("test")) {
            try (YTransaction txn = parent.beginTransaction()) {
                array.insertDoc(txn, 0, child);
            }

            assertEquals(1, array.length());
            YDoc retrieved = array.getDoc(0);
            assertNotNull(retrieved);
            retrieved.close();
        }
    }

    @Test
    public void testPushDocWithTransaction() {
        try (YDoc parent = new YDoc();
             YDoc child1 = new YDoc();
             YDoc child2 = new YDoc();
             YArray array = parent.getArray("test")) {
            try (YTransaction txn = parent.beginTransaction()) {
                array.pushDoc(txn, child1);
                array.pushDoc(txn, child2);
            }

            assertEquals(2, array.length());
            YDoc retrieved1 = array.getDoc(0);
            YDoc retrieved2 = array.getDoc(1);
            assertNotNull(retrieved1);
            assertNotNull(retrieved2);
            retrieved1.close();
            retrieved2.close();
        }
    }

    @Test
    public void testBatchOperationsWithTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                array.pushString(txn, "A");
                array.pushDouble(txn, 1.0);
                array.pushString(txn, "B");
                array.pushDouble(txn, 2.0);
                array.insertString(txn, 2, "C");
            }

            assertEquals(5, array.length());
            assertEquals("A", array.getString(0));
            assertEquals(1.0, array.getDouble(1), 0.001);
            assertEquals("C", array.getString(2));
            assertEquals("B", array.getString(3));
            assertEquals(2.0, array.getDouble(4), 0.001);
        }
    }

    @Test
    public void testObserverFiresOncePerTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            AtomicInteger observerCallCount = new AtomicInteger(0);

            try (YSubscription sub = array.observe(event -> {
                observerCallCount.incrementAndGet();
            })) {
                // Single transaction should fire observer once
                try (YTransaction txn = doc.beginTransaction()) {
                    array.pushString(txn, "A");
                    array.pushString(txn, "B");
                    array.pushString(txn, "C");
                }

                // Observer should fire once after transaction commits
                assertEquals(1, observerCallCount.get());
            }
        }
    }

    @Test
    public void testObserverFiresMultipleTimesWithoutTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            AtomicInteger observerCallCount = new AtomicInteger(0);

            try (YSubscription sub = array.observe(event -> {
                observerCallCount.incrementAndGet();
            })) {
                // Without transaction, each operation fires observer
                array.pushString("A");
                array.pushString("B");
                array.pushString("C");

                // Observer should fire three times
                assertEquals(3, observerCallCount.get());
            }
        }
    }

    @Test
    public void testMixedTransactionAndNonTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            // Non-transaction operation
            array.pushString("A");

            // Transaction operations
            try (YTransaction txn = doc.beginTransaction()) {
                array.pushString(txn, "B");
                array.pushString(txn, "C");
            }

            // Another non-transaction operation
            array.pushString("D");

            assertEquals(4, array.length());
            assertEquals("A", array.getString(0));
            assertEquals("B", array.getString(1));
            assertEquals("C", array.getString(2));
            assertEquals("D", array.getString(3));
        }
    }

    @Test
    public void testComplexBatchOperations() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            try (YTransaction txn = doc.beginTransaction()) {
                // Build up a complex array in one transaction
                for (int i = 0; i < 10; i++) {
                    array.pushDouble(txn, i * 1.5);
                }
                for (int i = 0; i < 5; i++) {
                    array.pushString(txn, "Item" + i);
                }
                // Remove some elements
                array.remove(txn, 5, 3);
            }

            assertEquals(12, array.length());
            assertEquals(0.0, array.getDouble(0), 0.001);
            assertEquals(6.0, array.getDouble(4), 0.001);
            assertEquals("Item0", array.getString(7));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertStringWithNullTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.insertString(null, 0, "Hello");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushDoubleWithNullTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushDouble(null, 1.0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveWithNullTransaction() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("A");
            array.remove(null, 0, 1);
        }
    }

    @Test
    public void testBackwardCompatibility() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            // Old API (without transaction) should still work
            array.pushString("Legacy1");
            array.pushDouble(42.0);
            array.insertString(1, "Legacy2");

            assertEquals(3, array.length());
            assertEquals("Legacy1", array.getString(0));
            assertEquals("Legacy2", array.getString(1));
            assertEquals(42.0, array.getDouble(2), 0.001);
        }
    }
}
