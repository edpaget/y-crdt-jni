package net.carcdr.ycrdt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for YArray.
 */
public class YArrayTest {

    @Test
    public void testArrayCreation() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            assertNotNull(array);
            assertEquals(0, array.length());
        }
    }

    @Test
    public void testPushString() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.pushString("World");
            assertEquals(2, array.length());
            assertEquals("Hello", array.getString(0));
            assertEquals("World", array.getString(1));
        }
    }

    @Test
    public void testPushDouble() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushDouble(42.0);
            array.pushDouble(3.14);
            assertEquals(2, array.length());
            assertEquals(42.0, array.getDouble(0), 0.001);
            assertEquals(3.14, array.getDouble(1), 0.001);
        }
    }

    @Test
    public void testMixedTypes() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.pushDouble(42.0);
            array.pushString("World");
            assertEquals(3, array.length());
            assertEquals("Hello", array.getString(0));
            assertEquals(42.0, array.getDouble(1), 0.001);
            assertEquals("World", array.getString(2));
        }
    }

    @Test
    public void testInsertString() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.pushString("World");
            array.insertString(1, "Beautiful");
            assertEquals(3, array.length());
            assertEquals("Hello", array.getString(0));
            assertEquals("Beautiful", array.getString(1));
            assertEquals("World", array.getString(2));
        }
    }

    @Test
    public void testInsertDouble() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushDouble(1.0);
            array.pushDouble(3.0);
            array.insertDouble(1, 2.0);
            assertEquals(3, array.length());
            assertEquals(1.0, array.getDouble(0), 0.001);
            assertEquals(2.0, array.getDouble(1), 0.001);
            assertEquals(3.0, array.getDouble(2), 0.001);
        }
    }

    @Test
    public void testRemove() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("One");
            array.pushString("Two");
            array.pushString("Three");
            array.remove(1, 1);
            assertEquals(2, array.length());
            assertEquals("One", array.getString(0));
            assertEquals("Three", array.getString(1));
        }
    }

    @Test
    public void testRemoveMultiple() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("A");
            array.pushString("B");
            array.pushString("C");
            array.pushString("D");
            array.remove(1, 2);
            assertEquals(2, array.length());
            assertEquals("A", array.getString(0));
            assertEquals("D", array.getString(1));
        }
    }

    @Test
    public void testToJson() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.pushDouble(42.0);
            array.pushString("World");
            String json = array.toJson();
            assertNotNull(json);
            assertTrue(json.contains("Hello"));
            assertTrue(json.contains("42"));
            assertTrue(json.contains("World"));
        }
    }

    @Test
    public void testGetOutOfBounds() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            assertNull(array.getString(10));
            assertEquals(0.0, array.getDouble(10), 0.001);
        }
    }

    @Test
    public void testGetNegativeIndex() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            assertNull(array.getString(-1));
            assertEquals(0.0, array.getDouble(-1), 0.001);
        }
    }

    @Test
    public void testSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Make changes in doc1
            try (YArray array1 = doc1.getArray("shared")) {
                array1.pushString("Hello");
                array1.pushDouble(42.0);
            }

            // Sync from doc1 to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Get array from doc2 after sync
            try (YArray array2 = doc2.getArray("shared")) {
                assertEquals(2, array2.length());
                assertEquals("Hello", array2.getString(0));
                assertEquals(42.0, array2.getDouble(1), 0.001);
            }
        }
    }

    @Test
    public void testBidirectionalSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Changes in doc1
            try (YArray array1 = doc1.getArray("shared")) {
                array1.pushString("From Doc1");
            }

            // Changes in doc2
            try (YArray array2 = doc2.getArray("shared")) {
                array2.pushString("From Doc2");
            }

            // Sync doc1 to doc2
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Sync doc2 to doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have both elements
            try (YArray array1 = doc1.getArray("shared");
                 YArray array2 = doc2.getArray("shared")) {
                assertEquals(array1.length(), array2.length());
                assertEquals(2, array1.length());

                String json1 = array1.toJson();
                String json2 = array2.toJson();
                assertEquals(json1, json2);
                assertTrue(json1.contains("From Doc1"));
                assertTrue(json1.contains("From Doc2"));
            }
        }
    }

    @Test
    public void testMultipleArraysInDocument() {
        try (YDoc doc = new YDoc();
             YArray array1 = doc.getArray("array1");
             YArray array2 = doc.getArray("array2")) {

            array1.pushString("First");
            array2.pushString("Second");

            assertEquals("First", array1.getString(0));
            assertEquals("Second", array2.getString(0));
        }
    }

    @Test
    public void testArrayClosed() {
        YDoc doc = new YDoc();
        YArray array = doc.getArray("test");
        array.pushString("Hello");

        array.close();
        assertTrue(array.isClosed());

        try {
            array.pushString("World");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        doc.close();
    }

    @Test
    public void testArrayWithClosedDoc() {
        YDoc doc = new YDoc();
        YArray array = doc.getArray("test");

        doc.close();

        array.close();
        assertTrue(array.isClosed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPushNullString() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertNullString() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.insertString(0, null);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertNegativeIndex() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.insertString(-1, "Hello");
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertIndexTooLarge() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.insertString(10, "World");
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveNegativeIndex() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.remove(-1, 1);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveNegativeLength() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.remove(0, -1);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveRangeTooLarge() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            array.pushString("Hello");
            array.remove(0, 10);
        }
    }

    @Test
    public void testEmptyArray() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            assertEquals(0, array.length());
            assertFalse(array.isClosed());
        }
    }

    @Test
    public void testGetSameArrayTwice() {
        try (YDoc doc = new YDoc();
             YArray array1 = doc.getArray("shared")) {
            array1.pushString("Hello");

            // Get the same array again
            try (YArray array2 = doc.getArray("shared")) {
                // Both should show the same content
                assertEquals(1, array1.length());
                assertEquals(1, array2.length());
                assertEquals("Hello", array1.getString(0));
                assertEquals("Hello", array2.getString(0));

                array2.pushString("World");

                // Both should reflect the change
                assertEquals(2, array1.length());
                assertEquals(2, array2.length());
                assertEquals("World", array1.getString(1));
                assertEquals("World", array2.getString(1));
            }
        }
    }

    @Test
    public void testComplexOperationSequence() {
        try (YDoc doc = new YDoc();
             YArray array = doc.getArray("test")) {
            // Build up array
            array.pushString("A");
            array.pushString("B");
            array.pushString("C");
            assertEquals(3, array.length());

            // Insert in middle
            array.insertString(1, "X");
            assertEquals(4, array.length());
            assertEquals("A", array.getString(0));
            assertEquals("X", array.getString(1));
            assertEquals("B", array.getString(2));
            assertEquals("C", array.getString(3));

            // Remove middle
            array.remove(1, 2);
            assertEquals(2, array.length());
            assertEquals("A", array.getString(0));
            assertEquals("C", array.getString(1));

            // Add more
            array.pushDouble(42.0);
            array.pushString("D");
            assertEquals(4, array.length());

            String json = array.toJson();
            assertTrue(json.contains("A"));
            assertTrue(json.contains("C"));
            assertTrue(json.contains("42"));
            assertTrue(json.contains("D"));
        }
    }
}
