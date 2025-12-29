package net.carcdr.ycrdt.panama;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YText;

/**
 * Tests for the Panama FFM implementation of YDoc.
 *
 * <p>Note: Tests for YArray and YMap operations that use YInput structs
 * are disabled until the struct-by-value return handling is fixed.</p>
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
        try (YDoc doc = new PanamaYDoc()) {
            assertNotNull(doc.getArray("myarray"));
        }
    }

    @Test
    public void testGetMap() {
        try (YDoc doc = new PanamaYDoc()) {
            assertNotNull(doc.getMap("mymap"));
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
