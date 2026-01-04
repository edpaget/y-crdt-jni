package net.carcdr.ycrdt.panama;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;

import org.junit.Test;

/**
 * Tests for parent navigation in PanamaYXmlElement.
 */
public class PanamaYXmlElementParentTest {

    @Test
    public void testGetParentOfRootElement() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);

            Object parent = div.getParent();
            assertNotNull(parent);
            assertTrue(parent instanceof YXmlFragment);

            div.close();
        }
    }

    @Test
    public void testGetParentOfChildElement() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlElement span = div.insertElement(0, "span");

            Object parent = span.getParent();
            assertNotNull(parent);
            assertTrue(parent instanceof YXmlElement);
            assertEquals("div", ((YXmlElement) parent).getTag());

            span.close();
            div.close();
        }
    }

    @Test
    public void testGetParentOfNestedElement() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlElement p = div.insertElement(0, "p");
            YXmlElement span = p.insertElement(0, "span");

            // Check span's parent is p
            Object spanParent = span.getParent();
            assertNotNull(spanParent);
            assertTrue(spanParent instanceof YXmlElement);
            assertEquals("p", ((YXmlElement) spanParent).getTag());

            // Check p's parent is div
            Object pParent = p.getParent();
            assertNotNull(pParent);
            assertTrue(pParent instanceof YXmlElement);
            assertEquals("div", ((YXmlElement) pParent).getTag());

            span.close();
            p.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentSingleChild() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlElement span = div.insertElement(0, "span");

            assertEquals(0, span.getIndexInParent());

            span.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentMultipleChildren() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlElement child0 = div.insertElement(0, "h1");
            YXmlElement child1 = div.insertElement(1, "p");
            YXmlElement child2 = div.insertElement(2, "span");

            assertEquals(0, child0.getIndexInParent());
            assertEquals(1, child1.getIndexInParent());
            assertEquals(2, child2.getIndexInParent());

            child0.close();
            child1.close();
            child2.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentAfterInsertion() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            YXmlElement child0 = div.insertElement(0, "h1");
            YXmlElement child1 = div.insertElement(1, "p");

            assertEquals(0, child0.getIndexInParent());
            assertEquals(1, child1.getIndexInParent());

            // Insert a new element at index 0, shifting others
            YXmlElement newChild = div.insertElement(0, "span");

            assertEquals(0, newChild.getIndexInParent());
            assertEquals(1, child0.getIndexInParent());
            assertEquals(2, child1.getIndexInParent());

            child0.close();
            child1.close();
            newChild.close();
            div.close();
        }
    }

    @Test
    public void testGetIndexInParentRootElement() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("fragment")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);

            int index = div.getIndexInParent();
            assertEquals(0, index); // Root element should be at index 0 in its fragment

            div.close();
        }
    }
}
