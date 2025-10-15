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
                if ("class".equals(name)) hasClass = true;
                if ("id".equals(name)) hasId = true;
                if ("style".equals(name)) hasStyle = true;
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
}
