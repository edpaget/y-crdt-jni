package net.carcdr.ycrdt.panama;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlText;

import org.junit.Test;

/**
 * Tests for parent navigation in PanamaYXmlText.
 */
public class PanamaYXmlTextParentTest {

    @Test
    public void testGetParentOfTextInFragment() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertText(0, "Hello");
            YXmlText text = fragment.getText(0);

            Object parent = text.getParent();
            assertNotNull(parent);
            assertTrue(parent instanceof YXmlFragment);

            text.close();
        }
    }

    @Test
    public void testGetParentOfTextInElement() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlText text = div.insertText(0);
            text.insert(0, "Hello");

            Object parent = text.getParent();
            assertNotNull(parent);
            assertTrue(parent instanceof YXmlElement);
            assertEquals("div", ((YXmlElement) parent).getTag());

            text.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentSingleText() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlText text = div.insertText(0);

            assertEquals(0, text.getIndexInParent());

            text.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentMixedChildren() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlText text0 = div.insertText(0);
            YXmlElement span = div.insertElement(1, "span");
            YXmlText text1 = div.insertText(2);

            assertEquals(0, text0.getIndexInParent());
            assertEquals(1, span.getIndexInParent());
            assertEquals(2, text1.getIndexInParent());

            text0.close();
            span.close();
            text1.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentAfterInsertion() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlText text0 = div.insertText(0);
            YXmlText text1 = div.insertText(1);

            assertEquals(0, text0.getIndexInParent());
            assertEquals(1, text1.getIndexInParent());

            // Insert a new element at index 0, shifting others
            YXmlElement newChild = div.insertElement(0, "span");

            assertEquals(0, newChild.getIndexInParent());
            assertEquals(1, text0.getIndexInParent());
            assertEquals(2, text1.getIndexInParent());

            text0.close();
            text1.close();
            newChild.close();
            div.close();
        }
    }
}
