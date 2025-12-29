package net.carcdr.ycrdt.panama;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.carcdr.ycrdt.YArray;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YMap;
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
}
