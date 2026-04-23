package net.carcdr.ycrdt.panama;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;

import org.junit.Test;

/**
 * Tests for typed XML attribute round-trip through the Panama backend.
 */
public class PanamaYXmlElementAttributeTest {

    @Test
    public void testTypedAttributesRoundTrip() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("frag")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);

            div.setAttribute("name", "main");
            div.setAttribute("level", 3L);
            div.setAttribute("width", 100);
            div.setAttribute("ratio", 1.5);
            div.setAttribute("factor", 2.5f);
            div.setAttribute("draft", true);
            div.setAttribute("nothing", null);

            Object name = div.getAttribute("name");
            Object level = div.getAttribute("level");
            Object width = div.getAttribute("width");
            Object ratio = div.getAttribute("ratio");
            Object factor = div.getAttribute("factor");
            Object draft = div.getAttribute("draft");
            Object nothing = div.getAttribute("nothing");

            assertTrue(name instanceof String);
            assertEquals("main", name);
            assertTrue(level instanceof Long);
            assertEquals(3L, level);
            assertTrue(width instanceof Long);
            assertEquals(100L, width);
            assertTrue(ratio instanceof Double);
            assertEquals(1.5, (Double) ratio, 0.0);
            assertTrue(factor instanceof Double);
            assertEquals(2.5, (Double) factor, 0.0);
            assertTrue(draft instanceof Boolean);
            assertEquals(Boolean.TRUE, draft);
            assertNull(nothing);

            div.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAttributeRejectsUnsupportedType() {
        try (YDoc doc = new PanamaYDoc();
             YXmlFragment fragment = doc.getXmlFragment("frag")) {
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);
            try {
                div.setAttribute("bad", new Object());
            } finally {
                div.close();
            }
        }
    }
}
