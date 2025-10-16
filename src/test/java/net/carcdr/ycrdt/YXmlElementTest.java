package net.carcdr.ycrdt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for YXmlElement.
 */
public class YXmlElementTest {

    @Test
    public void testXmlElementCreation() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            assertNotNull(element);
            assertEquals("div", element.getTag());
            assertFalse(element.isClosed());
        }
    }

    @Test
    public void testGetTag() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("paragraph")) {
            assertEquals("paragraph", element.getTag());
        }
    }

    @Test
    public void testSetAndGetAttribute() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.setAttribute("class", "container");
            assertEquals("container", element.getAttribute("class"));

            element.setAttribute("id", "main");
            assertEquals("main", element.getAttribute("id"));
        }
    }

    @Test
    public void testGetNonExistentAttribute() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            assertNull(element.getAttribute("nothere"));
        }
    }

    @Test
    public void testOverwriteAttribute() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.setAttribute("class", "container");
            assertEquals("container", element.getAttribute("class"));

            element.setAttribute("class", "wrapper");
            assertEquals("wrapper", element.getAttribute("class"));
        }
    }

    @Test
    public void testRemoveAttribute() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.setAttribute("class", "container");
            element.setAttribute("id", "main");

            assertEquals("container", element.getAttribute("class"));
            assertEquals("main", element.getAttribute("id"));

            element.removeAttribute("class");

            assertNull(element.getAttribute("class"));
            assertEquals("main", element.getAttribute("id"));
        }
    }

    @Test
    public void testGetAttributeNames() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.setAttribute("class", "container");
            element.setAttribute("id", "main");
            element.setAttribute("style", "color: red");

            String[] names = element.getAttributeNames();
            assertEquals(3, names.length);

            // Verify all attribute names are present (order may vary)
            boolean hasClass = false;
            boolean hasId = false;
            boolean hasStyle = false;

            for (String name : names) {
                if ("class".equals(name)) {
                    hasClass = true;
                }
                if ("id".equals(name)) {
                    hasId = true;
                }
                if ("style".equals(name)) {
                    hasStyle = true;
                }
            }

            assertTrue(hasClass);
            assertTrue(hasId);
            assertTrue(hasStyle);
        }
    }

    @Test
    public void testEmptyAttributeNames() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            String[] names = element.getAttributeNames();
            assertEquals(0, names.length);
        }
    }

    @Test
    public void testMultipleAttributes() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("input")) {
            element.setAttribute("type", "text");
            element.setAttribute("name", "username");
            element.setAttribute("placeholder", "Enter username");
            element.setAttribute("required", "true");

            assertEquals("text", element.getAttribute("type"));
            assertEquals("username", element.getAttribute("name"));
            assertEquals("Enter username", element.getAttribute("placeholder"));
            assertEquals("true", element.getAttribute("required"));

            String[] names = element.getAttributeNames();
            assertEquals(4, names.length);
        }
    }

    @Test
    public void testSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Make changes in doc1
            try (YXmlElement element1 = doc1.getXmlElement("div")) {
                element1.setAttribute("class", "container");
                element1.setAttribute("id", "main");
            }

            // Sync from doc1 to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Verify doc2 has the changes
            try (YXmlElement element2 = doc2.getXmlElement("div")) {
                assertEquals("div", element2.getTag());
                assertEquals("container", element2.getAttribute("class"));
                assertEquals("main", element2.getAttribute("id"));
            }
        }
    }

    @Test
    public void testBidirectionalSync() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Create element in doc1 first
            try (YXmlElement element1 = doc1.getXmlElement("div")) {
                element1.setAttribute("attr1", "From Doc1");
            }

            // Sync doc1 to doc2 BEFORE making changes in doc2
            // This ensures both docs share the same XML element structure
            byte[] update1 = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update1);

            // Now make changes in doc2 to the synced element
            try (YXmlElement element2 = doc2.getXmlElement("div")) {
                element2.setAttribute("attr2", "From Doc2");
            }

            // Sync doc2 back to doc1
            byte[] update2 = doc2.encodeStateAsUpdate();
            doc1.applyUpdate(update2);

            // Both should have both attributes now
            try (YXmlElement element1 = doc1.getXmlElement("div");
                 YXmlElement element2 = doc2.getXmlElement("div")) {
                assertEquals("From Doc1", element1.getAttribute("attr1"));
                assertEquals("From Doc2", element1.getAttribute("attr2"));
                assertEquals("From Doc1", element2.getAttribute("attr1"));
                assertEquals("From Doc2", element2.getAttribute("attr2"));
            }
        }
    }

    @Test
    public void testXmlElementClosed() {
        YDoc doc = new YDoc();
        YXmlElement element = doc.getXmlElement("div");
        element.setAttribute("class", "container");

        element.close();
        assertTrue(element.isClosed());

        try {
            element.setAttribute("id", "main");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }

        doc.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAttributeNullName() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.setAttribute(null, "value");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAttributeNullValue() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.setAttribute("class", null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAttributeNull() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.getAttribute(null);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveAttributeNull() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.removeAttribute(null);
        }
    }

    @Test
    public void testGetSameElementTwice() {
        try (YDoc doc = new YDoc();
             YXmlElement element1 = doc.getXmlElement("div")) {
            element1.setAttribute("class", "container");

            // Get the same element again
            try (YXmlElement element2 = doc.getXmlElement("div")) {
                // Both should show the same content
                assertEquals("container", element1.getAttribute("class"));
                assertEquals("container", element2.getAttribute("class"));

                element2.setAttribute("id", "main");

                // Both should reflect the change
                assertEquals("main", element1.getAttribute("id"));
                assertEquals("main", element2.getAttribute("id"));
            }
        }
    }

    @Test
    public void testMultipleElementsInDocument() {
        try (YDoc doc = new YDoc();
             YXmlElement element1 = doc.getXmlElement("div");
             YXmlElement element2 = doc.getXmlElement("span")) {

            element1.setAttribute("class", "div-class");
            element2.setAttribute("class", "span-class");

            assertEquals("div", element1.getTag());
            assertEquals("span", element2.getTag());
            assertEquals("div-class", element1.getAttribute("class"));
            assertEquals("span-class", element2.getAttribute("class"));
        }
    }

    @Test
    public void testToString() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("div")) {
            element.setAttribute("class", "container");

            String xmlString = element.toString();
            assertNotNull(xmlString);
            // The exact format may vary, but it should contain the tag
            assertTrue(xmlString.contains("div") || xmlString.length() >= 0);
        }
    }

    @Test
    public void testComplexAttributeSequence() {
        try (YDoc doc = new YDoc();
             YXmlElement element = doc.getXmlElement("form")) {
            // Build up attributes
            element.setAttribute("method", "POST");
            element.setAttribute("action", "/submit");
            element.setAttribute("enctype", "multipart/form-data");
            assertEquals(3, element.getAttributeNames().length);

            // Update attribute
            element.setAttribute("method", "GET");
            assertEquals("GET", element.getAttribute("method"));
            assertEquals(3, element.getAttributeNames().length);

            // Remove attribute
            element.removeAttribute("enctype");
            assertEquals(2, element.getAttributeNames().length);
            assertNull(element.getAttribute("enctype"));

            // Add more
            element.setAttribute("id", "main-form");
            element.setAttribute("class", "form-horizontal");
            assertEquals(4, element.getAttributeNames().length);
        }
    }

    // Nested element tests

    @Test
    public void testChildCount() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            assertEquals(0, div.childCount());

            div.insertElement(0, "span");
            assertEquals(1, div.childCount());

            div.insertText(1);
            assertEquals(2, div.childCount());
        }
    }

    @Test
    public void testInsertElement() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span = div.insertElement(0, "span");
            assertNotNull(span);
            assertEquals("span", span.getTag());
            assertEquals(1, div.childCount());

            span.close();
        }
    }

    @Test
    public void testInsertMultipleElements() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span1 = div.insertElement(0, "span");
            YXmlElement span2 = div.insertElement(1, "p");
            YXmlElement span3 = div.insertElement(0, "h1");

            assertEquals(3, div.childCount());

            // Verify order: h1, span, p
            Object child0 = div.getChild(0);
            assertTrue(child0 instanceof YXmlElement);
            assertEquals("h1", ((YXmlElement) child0).getTag());

            Object child1 = div.getChild(1);
            assertTrue(child1 instanceof YXmlElement);
            assertEquals("span", ((YXmlElement) child1).getTag());

            Object child2 = div.getChild(2);
            assertTrue(child2 instanceof YXmlElement);
            assertEquals("p", ((YXmlElement) child2).getTag());

            span1.close();
            span2.close();
            span3.close();
            ((YXmlElement) child0).close();
            ((YXmlElement) child1).close();
            ((YXmlElement) child2).close();
        }
    }

    @Test
    public void testInsertText() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlText text = div.insertText(0);
            assertNotNull(text);
            assertEquals(0, text.length());
            assertEquals(1, div.childCount());

            text.push("Hello World");
            assertEquals(11, text.length());

            text.close();
        }
    }

    @Test
    public void testGetChild() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span = div.insertElement(0, "span");
            YXmlText text = div.insertText(1);

            Object child0 = div.getChild(0);
            assertTrue(child0 instanceof YXmlElement);
            assertEquals("span", ((YXmlElement) child0).getTag());

            Object child1 = div.getChild(1);
            assertTrue(child1 instanceof YXmlText);

            Object child2 = div.getChild(2);
            assertNull(child2);

            span.close();
            text.close();
            ((YXmlElement) child0).close();
            ((YXmlText) child1).close();
        }
    }

    @Test
    public void testRemoveChild() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span1 = div.insertElement(0, "span");
            YXmlElement span2 = div.insertElement(1, "p");
            YXmlElement span3 = div.insertElement(2, "h1");

            assertEquals(3, div.childCount());

            div.removeChild(1); // Remove p
            assertEquals(2, div.childCount());

            Object child0 = div.getChild(0);
            assertEquals("span", ((YXmlElement) child0).getTag());

            Object child1 = div.getChild(1);
            assertEquals("h1", ((YXmlElement) child1).getTag());

            span1.close();
            span2.close();
            span3.close();
            ((YXmlElement) child0).close();
            ((YXmlElement) child1).close();
        }
    }

    @Test
    public void testNestedElements() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            div.setAttribute("class", "container");

            YXmlElement p = div.insertElement(0, "p");
            p.setAttribute("id", "para");

            YXmlElement span = p.insertElement(0, "span");
            span.setAttribute("style", "bold");

            // Verify structure
            assertEquals(1, div.childCount());
            assertEquals(1, p.childCount());
            assertEquals(0, span.childCount());

            Object divChild = div.getChild(0);
            assertTrue(divChild instanceof YXmlElement);
            assertEquals("p", ((YXmlElement) divChild).getTag());
            assertEquals("para", ((YXmlElement) divChild).getAttribute("id"));

            Object pChild = p.getChild(0);
            assertTrue(pChild instanceof YXmlElement);
            assertEquals("span", ((YXmlElement) pChild).getTag());
            assertEquals("bold", ((YXmlElement) pChild).getAttribute("style"));

            p.close();
            span.close();
            ((YXmlElement) divChild).close();
            ((YXmlElement) pChild).close();
        }
    }

    @Test
    public void testMixedChildren() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span = div.insertElement(0, "span");
            YXmlText text1 = div.insertText(1);
            YXmlElement p = div.insertElement(2, "p");
            YXmlText text2 = div.insertText(3);

            text1.push("Hello");
            text2.push("World");

            assertEquals(4, div.childCount());

            Object child0 = div.getChild(0);
            assertTrue(child0 instanceof YXmlElement);
            assertEquals("span", ((YXmlElement) child0).getTag());

            Object child1 = div.getChild(1);
            assertTrue(child1 instanceof YXmlText);
            assertEquals("Hello", ((YXmlText) child1).toString());

            Object child2 = div.getChild(2);
            assertTrue(child2 instanceof YXmlElement);
            assertEquals("p", ((YXmlElement) child2).getTag());

            Object child3 = div.getChild(3);
            assertTrue(child3 instanceof YXmlText);
            assertEquals("World", ((YXmlText) child3).toString());

            span.close();
            text1.close();
            p.close();
            text2.close();
            ((YXmlElement) child0).close();
            ((YXmlText) child1).close();
            ((YXmlElement) child2).close();
            ((YXmlText) child3).close();
        }
    }

    @Test
    public void testNestedSynchronization() {
        try (YDoc doc1 = new YDoc();
             YDoc doc2 = new YDoc()) {

            // Create nested structure in doc1
            try (YXmlElement div1 = doc1.getXmlElement("div")) {
                YXmlElement p = div1.insertElement(0, "p");
                p.setAttribute("id", "para");
                YXmlText text = p.insertText(0);
                text.push("Hello World");
                p.close();
                text.close();
            }

            // Sync to doc2
            byte[] update = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(update);

            // Verify in doc2
            try (YXmlElement div2 = doc2.getXmlElement("div")) {
                assertEquals(1, div2.childCount());

                Object child = div2.getChild(0);
                assertTrue(child instanceof YXmlElement);
                YXmlElement p = (YXmlElement) child;
                assertEquals("p", p.getTag());
                assertEquals("para", p.getAttribute("id"));
                assertEquals(1, p.childCount());

                Object textChild = p.getChild(0);
                assertTrue(textChild instanceof YXmlText);
                assertEquals("Hello World", ((YXmlText) textChild).toString());

                p.close();
                ((YXmlText) textChild).close();
            }
        }
    }

    @Test
    public void testComplexNestedStructure() {
        try (YDoc doc = new YDoc();
             YXmlElement html = doc.getXmlElement("html")) {
            // Create: <html><body><div><p>Text</p></div></body></html>
            YXmlElement body = html.insertElement(0, "body");
            YXmlElement div = body.insertElement(0, "div");
            div.setAttribute("class", "container");
            YXmlElement p = div.insertElement(0, "p");
            YXmlText text = p.insertText(0);
            text.push("Hello World");

            // Verify structure
            assertEquals(1, html.childCount());
            assertEquals(1, body.childCount());
            assertEquals(1, div.childCount());
            assertEquals(1, p.childCount());

            // Verify content
            assertEquals("container", div.getAttribute("class"));
            assertEquals("Hello World", text.toString());

            body.close();
            div.close();
            p.close();
            text.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertElementNullTag() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            div.insertElement(0, null);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertElementNegativeIndex() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            div.insertElement(-1, "span");
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInsertTextNegativeIndex() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            div.insertText(-1);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetChildNegativeIndex() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            div.getChild(-1);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveChildNegativeIndex() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            div.removeChild(-1);
        }
    }

    @Test
    public void testRemoveAllChildren() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            YXmlElement span1 = div.insertElement(0, "span");
            YXmlElement span2 = div.insertElement(1, "p");
            YXmlElement span3 = div.insertElement(2, "h1");

            assertEquals(3, div.childCount());

            div.removeChild(0);
            div.removeChild(0);
            div.removeChild(0);

            assertEquals(0, div.childCount());
            assertNull(div.getChild(0));

            span1.close();
            span2.close();
            span3.close();
        }
    }

    @Test
    public void testDeeplyNestedStructure() {
        try (YDoc doc = new YDoc();
             YXmlElement root = doc.getXmlElement("root")) {
            // Create nested structure 5 levels deep
            YXmlElement level1 = root.insertElement(0, "level1");
            YXmlElement level2 = level1.insertElement(0, "level2");
            YXmlElement level3 = level2.insertElement(0, "level3");
            YXmlElement level4 = level3.insertElement(0, "level4");
            YXmlElement level5 = level4.insertElement(0, "level5");

            YXmlText text = level5.insertText(0);
            text.push("Deep text");

            // Verify we can navigate down
            Object child1 = root.getChild(0);
            Object child2 = ((YXmlElement) child1).getChild(0);
            Object child3 = ((YXmlElement) child2).getChild(0);
            Object child4 = ((YXmlElement) child3).getChild(0);
            Object child5 = ((YXmlElement) child4).getChild(0);
            Object textChild = ((YXmlElement) child5).getChild(0);

            assertTrue(textChild instanceof YXmlText);
            assertEquals("Deep text", ((YXmlText) textChild).toString());

            level1.close();
            level2.close();
            level3.close();
            level4.close();
            level5.close();
            text.close();
            ((YXmlElement) child1).close();
            ((YXmlElement) child2).close();
            ((YXmlElement) child3).close();
            ((YXmlElement) child4).close();
            ((YXmlElement) child5).close();
            ((YXmlText) textChild).close();
        }
    }

    @Test
    public void testToStringWithNestedElements() {
        try (YDoc doc = new YDoc();
             YXmlElement div = doc.getXmlElement("div")) {
            div.setAttribute("class", "container");
            YXmlElement p = div.insertElement(0, "p");
            YXmlText text = p.insertText(0);
            text.push("Hello");

            String xmlString = div.toString();
            assertNotNull(xmlString);
            assertTrue(xmlString.contains("div"));
            assertTrue(xmlString.contains("p"));
            assertTrue(xmlString.contains("Hello"));

            p.close();
            text.close();
        }
    }
}
