package net.carcdr.yprosemirror;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.carcdr.ycrdt.YBinding;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlText;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for Y-CRDT structures used in ProseMirror integration.
 *
 * <p>This test suite verifies Y-CRDT XML structure creation and manipulation
 * that will be used for ProseMirror document representation.
 *
 * <p>Full integration tests with ProseMirror Schema and actual conversion will be
 * added when we have concrete use cases with ProseMirror editors. The conversion
 * classes (ProseMirrorConverter and YCrdtConverter) are implemented and ready to use.
 */
public class ConversionTest {

    private YDoc ydoc;

    /**
     * Set up YDoc before each test.
     */
    @Before
    public void setUp() {
        ydoc = YBinding.getInstance().createDoc();
    }

    /**
     * Clean up resources after each test.
     */
    @After
    public void tearDown() {
        if (ydoc != null) {
            ydoc.close();
        }
    }

    /**
     * Tests creating Y-CRDT structure for a simple ProseMirror document.
     *
     * <p>This demonstrates the Y-CRDT structure for: doc > paragraph > text("Hello World")
     */
    @Test
    public void testSimpleDocumentStructure() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Create structure: paragraph with text
            fragment.insertElement(0, "paragraph");
            YXmlElement para = fragment.getElement(0);
            para.insertText(0).push("Hello World");

            // Verify structure
            assertEquals("Fragment should have one child", 1, fragment.length());
            assertEquals("Should be a paragraph", "paragraph", para.getTag());
            assertEquals("Paragraph should have one text child", 1, para.childCount());

            YXmlText text = (YXmlText) para.getChild(0);
            assertEquals("Text content should match", "Hello World", text.toString());

            para.close();
            text.close();
        }
    }

    /**
     * Tests creating nested Y-CRDT elements.
     *
     * <p>This demonstrates nested structures like: div > heading > text
     */
    @Test
    public void testNestedElements() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Create structure: div > heading > text("Title")
            fragment.insertElement(0, "div");
            YXmlElement div = fragment.getElement(0);

            YXmlElement heading = div.insertElement(0, "heading");
            heading.insertText(0).push("Title");

            // Verify structure
            assertEquals("Fragment should have one div", 1, fragment.length());
            assertEquals("Should be a div", "div", div.getTag());
            assertEquals("Div should have one heading child", 1, div.childCount());

            YXmlElement retrievedHeading = (YXmlElement) div.getChild(0);
            assertEquals("Should be a heading", "heading", retrievedHeading.getTag());

            div.close();
            heading.close();
            retrievedHeading.close();
        }
    }

    /**
     * Tests creating elements with attributes.
     *
     * <p>This demonstrates attribute handling for styled elements.
     */
    @Test
    public void testElementAttributes() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Create heading with level attribute
            fragment.insertElement(0, "heading");
            YXmlElement heading = fragment.getElement(0);
            heading.setAttribute("level", "1");
            heading.setAttribute("id", "main-title");

            // Verify attributes
            assertEquals("Level should match", "1", heading.getAttribute("level"));
            assertEquals("ID should match", "main-title", heading.getAttribute("id"));

            heading.close();
        }
    }

    /**
     * Tests creating multiple sibling elements.
     *
     * <p>This demonstrates document structures with multiple paragraphs.
     */
    @Test
    public void testMultipleElements() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Create three paragraphs
            for (int i = 0; i < 3; i++) {
                fragment.insertElement(i, "paragraph");
                YXmlElement para = fragment.getElement(i);
                para.insertText(0).push("Paragraph " + (i + 1));
                para.close();
            }

            // Verify structure
            assertEquals("Should have three paragraphs", 3, fragment.length());

            for (int i = 0; i < 3; i++) {
                YXmlElement para = fragment.getElement(i);
                assertEquals("Should be a paragraph", "paragraph", para.getTag());

                YXmlText text = (YXmlText) para.getChild(0);
                assertEquals("Text should match",
                    "Paragraph " + (i + 1), text.toString());

                para.close();
                text.close();
            }
        }
    }

    /**
     * Tests text formatting attributes (for marks).
     *
     * <p>This demonstrates how ProseMirror marks are represented as Y-CRDT
     * text formatting attributes.
     */
    @Test
    public void testTextFormatting() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Create paragraph with formatted text
            fragment.insertElement(0, "paragraph");
            YXmlElement para = fragment.getElement(0);

            // Insert text with formatting
            YXmlText text = para.insertText(0);
            java.util.Map<String, Object> attrs = new java.util.HashMap<>();
            attrs.put("bold", true);
            attrs.put("italic", true);

            text.insertWithAttributes(0, "Bold and italic text", attrs);

            // Verify text was inserted
            YXmlText retrievedText = (YXmlText) para.getChild(0);
            assertNotNull("Text should not be null", retrievedText);
            // Note: toString() includes formatting tags like <bold><italic>text</italic></bold>
            // For now, just verify the text node exists and is not empty
            String content = retrievedText.toString();
            assertNotNull("Text content should not be null", content);
            assertTrue("Text should contain expected content",
                content.contains("Bold and italic text"));

            para.close();
            text.close();
            retrievedText.close();
        }
    }

    /**
     * Tests creating complex document structure.
     *
     * <p>This demonstrates a realistic document structure with mixed content.
     */
    @Test
    public void testComplexStructure() {
        try (YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
            // Create: heading + paragraph + paragraph
            fragment.insertElement(0, "heading");
            YXmlElement heading = fragment.getElement(0);
            heading.setAttribute("level", "1");
            heading.insertText(0).push("Document Title");
            heading.close();

            fragment.insertElement(1, "paragraph");
            YXmlElement para1 = fragment.getElement(1);
            para1.insertText(0).push("First paragraph");
            para1.close();

            fragment.insertElement(2, "paragraph");
            YXmlElement para2 = fragment.getElement(2);
            para2.insertText(0).push("Second paragraph");
            para2.close();

            // Verify complete structure
            assertEquals("Should have three elements", 3, fragment.length());

            YXmlElement h = fragment.getElement(0);
            assertEquals("First should be heading", "heading", h.getTag());
            assertEquals("Heading level should be 1", "1", h.getAttribute("level"));
            h.close();

            YXmlElement p1 = fragment.getElement(1);
            assertEquals("Second should be paragraph", "paragraph", p1.getTag());
            p1.close();

            YXmlElement p2 = fragment.getElement(2);
            assertEquals("Third should be paragraph", "paragraph", p2.getTag());
            p2.close();
        }
    }

    /**
     * Tests synchronization between two YDocs.
     *
     * <p>This demonstrates collaborative editing scenario where two editors
     * share the same document structure.
     */
    @Test
    public void testDocumentSynchronization() {
        try (YDoc doc1 = YBinding.getInstance().createDoc();
             YDoc doc2 = YBinding.getInstance().createDoc();
             YXmlFragment frag1 = doc1.getXmlFragment("prosemirror");
             YXmlFragment frag2 = doc2.getXmlFragment("prosemirror")) {

            // Create structure in doc1
            frag1.insertElement(0, "paragraph");
            YXmlElement para1 = frag1.getElement(0);
            para1.insertText(0).push("Shared content");
            para1.close();

            // Synchronize to doc2
            byte[] state = doc1.encodeStateAsUpdate();
            doc2.applyUpdate(state);

            // Verify doc2 has the same structure
            assertEquals("doc2 should have one element", 1, frag2.length());

            YXmlElement para2 = frag2.getElement(0);
            assertEquals("Should be a paragraph", "paragraph", para2.getTag());

            YXmlText text2 = (YXmlText) para2.getChild(0);
            assertEquals("Text should match", "Shared content", text2.toString());

            para2.close();
            text2.close();
        }
    }
}
