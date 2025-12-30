package net.carcdr.ycrdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Abstract test class for YText implementations.
 *
 * <p>Subclasses must implement {@link #getBinding()} to provide the
 * implementation-specific binding.</p>
 */
public abstract class AbstractYTextTest {

    /**
     * Returns the YBinding implementation to test.
     *
     * @return the binding to use for creating documents
     */
    protected abstract YBinding getBinding();

    @Test
    public void testGetText() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("mytext")) {
            assertNotNull("YText should be created", text);
            assertEquals("Initial length should be 0", 0, text.length());
        }
    }

    @Test
    public void testInsert() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "Hello");
            assertEquals(5, text.length());
            assertEquals("Hello", text.toString());
        }
    }

    @Test
    public void testInsertAtIndex() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "Hello");
            text.insert(5, " World");
            assertEquals(11, text.length());
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testInsertInMiddle() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "HelloWorld");
            text.insert(5, " ");
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testPush() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.push("Hello");
            text.push(" World");
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testDelete() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "Hello World");
            text.delete(5, 6);
            assertEquals("Hello", text.toString());
        }
    }

    @Test
    public void testDeleteFromStart() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "Hello World");
            text.delete(0, 6);
            assertEquals("World", text.toString());
        }
    }

    @Test
    public void testLength() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            assertEquals(0, text.length());
            text.insert(0, "Hello");
            assertEquals(5, text.length());
            text.push(" World");
            assertEquals(11, text.length());
        }
    }

    @Test
    public void testToString() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            assertEquals("", text.toString());
            text.insert(0, "Hello World");
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testTextSynchronization() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YText text1 = doc1.getText("test");
             YText text2 = doc2.getText("test")) {

            text1.insert(0, "Hello World");
            assertEquals("Hello World", text1.toString());
            assertEquals("", text2.toString());

            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            assertEquals("Hello World", text2.toString());
        }
    }

    @Test
    public void testTextBidirectionalSync() {
        YBinding binding = getBinding();
        try (YDoc doc1 = binding.createDoc();
             YDoc doc2 = binding.createDoc();
             YText text1 = doc1.getText("test");
             YText text2 = doc2.getText("test")) {

            text1.push("Hello");
            text2.push("World");

            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            assertEquals(text1.toString(), text2.toString());
            String content = text1.toString();
            assertEquals(10, content.length());
        }
    }

    @Test
    public void testInsertWithTransaction() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test");
             YTransaction txn = doc.beginTransaction()) {
            text.insert(txn, 0, "Hello");
            text.insert(txn, 5, " World");
            txn.commit();
            assertEquals("Hello World", text.toString());
        }
    }

    @Test
    public void testComplexEditingSequence() {
        YBinding binding = getBinding();
        try (YDoc doc = binding.createDoc();
             YText text = doc.getText("test")) {
            text.insert(0, "abc");
            text.insert(1, "X");
            text.delete(2, 1);
            text.push("Z");
            // Result: aXcZ
            assertEquals("aXcZ", text.toString());
        }
    }
}
