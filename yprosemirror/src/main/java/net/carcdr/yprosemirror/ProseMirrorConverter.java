package net.carcdr.yprosemirror;

import com.atlassian.prosemirror.model.Fragment;
import com.atlassian.prosemirror.model.Mark;
import com.atlassian.prosemirror.model.Node;
import com.atlassian.prosemirror.model.Schema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlText;

/**
 * Converts ProseMirror documents to Y-CRDT structures.
 *
 * <p>This class provides utilities for converting ProseMirror nodes, fragments,
 * and documents into their Y-CRDT XML equivalents (YXmlFragment, YXmlElement, YXmlText).
 *
 * <p><strong>Mapping Strategy:</strong>
 * <ul>
 *   <li>ProseMirror Node → YXmlElement (node type becomes XML tag)</li>
 *   <li>ProseMirror Fragment → YXmlFragment (container for nodes)</li>
 *   <li>Text content → YXmlText (with marks as formatting attributes)</li>
 *   <li>Node attributes → XML element attributes</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Convert a ProseMirror document to Y-CRDT
 * YDoc ydoc = ProseMirrorConverter.prosemirrorToYDoc(pmNode, schema);
 *
 * // Or convert to an existing YXmlFragment
 * try (YDoc ydoc = new YDoc();
 *      YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
 *     ProseMirrorConverter.nodeToYXml(pmNode, fragment, schema);
 * }
 * }</pre>
 *
 * @see YCrdtConverter for the reverse conversion (Y-CRDT to ProseMirror)
 * @since 0.1.0
 */
public final class ProseMirrorConverter {

    private ProseMirrorConverter() {
        // Utility class, prevent instantiation
    }

    /**
     * Converts a ProseMirror Node to a YXmlFragment.
     *
     * <p>This method traverses the ProseMirror node tree and creates corresponding
     * YXmlElement and YXmlText nodes in the target fragment. The conversion preserves:
     * <ul>
     *   <li>Node hierarchy and structure</li>
     *   <li>Node types (as XML tags)</li>
     *   <li>Node attributes (as XML attributes)</li>
     *   <li>Text content and formatting (as YXmlText with attributes)</li>
     * </ul>
     *
     * @param node the ProseMirror node to convert
     * @param fragment the target YXmlFragment to populate
     * @param schema the ProseMirror schema for validation
     * @throws IllegalArgumentException if node or fragment is null
     * @throws IllegalStateException if the fragment is closed
     */
    public static void nodeToYXml(Node node, YXmlFragment fragment, Schema schema) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        // Convert the node's content to Y-CRDT
        fragmentToYXml(node.getContent(), fragment, schema);
    }

    /**
     * Converts a ProseMirror Fragment to a YXmlFragment.
     *
     * @param pmFragment the ProseMirror fragment to convert
     * @param yFragment the target YXmlFragment to populate
     * @param schema the ProseMirror schema for validation
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException if the fragment is closed
     */
    public static void fragmentToYXml(Fragment pmFragment, YXmlFragment yFragment, Schema schema) {
        if (pmFragment == null) {
            throw new IllegalArgumentException("ProseMirror fragment cannot be null");
        }
        if (yFragment == null) {
            throw new IllegalArgumentException("YXmlFragment cannot be null");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        // Iterate through each child in the fragment
        for (int i = 0; i < pmFragment.getChildCount(); i++) {
            Node child = pmFragment.child(i);
            convertNodeToYXml(child, yFragment, i, schema);
        }
    }

    /**
     * Converts a single ProseMirror node and inserts it into the Y-CRDT fragment.
     *
     * @param pmNode the ProseMirror node to convert
     * @param yFragment the target YXmlFragment
     * @param index the index at which to insert
     * @param schema the ProseMirror schema
     */
    private static void convertNodeToYXml(
            Node pmNode,
            YXmlFragment yFragment,
            int index,
            Schema schema) {

        if (pmNode.isText()) {
            // Handle text nodes
            String text = pmNode.getText();
            if (text != null && !text.isEmpty()) {
                // YXmlFragment.insertText is void, need to get the text node after
                yFragment.insertText(index, text);

                // Apply marks as formatting attributes - need to get the text node
                List<Mark> marks = pmNode.getMarks();
                if (marks != null && !marks.isEmpty()) {
                    Map<String, Object> attrs = marksToAttributes(marks);
                    if (!attrs.isEmpty()) {
                        // Get the text node we just inserted
                        Object child = yFragment.getChild(index);
                        if (child instanceof YXmlText) {
                            YXmlText yText = (YXmlText) child;
                            yText.format(0, text.length(), attrs);
                        }
                    }
                }
            }
        } else {
            // Handle element nodes
            String tag = pmNode.getType().getName();
            // YXmlFragment.insertElement is void
            yFragment.insertElement(index, tag);

            // Get the element we just inserted and set attributes
            Object child = yFragment.getChild(index);
            if (child instanceof YXmlElement) {
                YXmlElement yElement = (YXmlElement) child;
                // Set node attributes
                Map<String, String> attrs = nodeAttrsToMap(pmNode);
                for (Map.Entry<String, String> entry : attrs.entrySet()) {
                    yElement.setAttribute(entry.getKey(), entry.getValue());
                }

                // Recursively convert children
                Fragment content = pmNode.getContent();
                if (content != null && content.getChildCount() > 0) {
                    for (int i = 0; i < content.getChildCount(); i++) {
                        Node contentChild = content.child(i);
                        convertNodeToYXml(contentChild, yElement, i, schema);
                    }
                }
            }
        }
    }

    /**
     * Converts a single ProseMirror node and inserts it into a Y-CRDT element.
     *
     * @param pmNode the ProseMirror node to convert
     * @param yElement the target YXmlElement
     * @param index the index at which to insert
     * @param schema the ProseMirror schema
     */
    private static void convertNodeToYXml(
            Node pmNode,
            YXmlElement yElement,
            int index,
            Schema schema) {

        if (pmNode.isText()) {
            // Handle text nodes
            String text = pmNode.getText();
            if (text != null && !text.isEmpty()) {
                YXmlText yText = yElement.insertText(index);
                // YXmlText from YXmlElement needs to push text
                yText.push(text);

                // Apply marks as formatting attributes
                List<Mark> marks = pmNode.getMarks();
                if (marks != null && !marks.isEmpty()) {
                    Map<String, Object> attrs = marksToAttributes(marks);
                    if (!attrs.isEmpty()) {
                        yText.format(0, text.length(), attrs);
                    }
                }
            }
        } else {
            // Handle element nodes
            String tag = pmNode.getType().getName();
            YXmlElement childElement = yElement.insertElement(index, tag);

            // Set node attributes
            Map<String, String> attrs = nodeAttrsToMap(pmNode);
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                childElement.setAttribute(entry.getKey(), entry.getValue());
            }

            // Recursively convert children
            Fragment content = pmNode.getContent();
            if (content != null && content.getChildCount() > 0) {
                for (int i = 0; i < content.getChildCount(); i++) {
                    Node child = content.child(i);
                    convertNodeToYXml(child, childElement, i, schema);
                }
            }
        }
    }

    /**
     * Converts ProseMirror marks to Y-CRDT formatting attributes.
     *
     * @param marks the list of ProseMirror marks
     * @return a map of attribute name to value
     */
    private static Map<String, Object> marksToAttributes(List<Mark> marks) {
        Map<String, Object> attrs = new HashMap<>();

        for (Mark mark : marks) {
            String markType = mark.getType().getName();

            // Check if mark has attributes
            Map<String, Object> markAttrs = mark.getAttrs();
            if (markAttrs != null && !markAttrs.isEmpty()) {
                // Include mark attributes with mark type as prefix
                for (Map.Entry<String, Object> entry : markAttrs.entrySet()) {
                    attrs.put(markType + "_" + entry.getKey(), entry.getValue());
                }
            } else {
                // Simple boolean mark (e.g., bold, italic)
                attrs.put(markType, true);
            }
        }

        return attrs;
    }

    /**
     * Converts ProseMirror node attributes to a string map.
     *
     * @param node the ProseMirror node
     * @return a map of attribute name to string value
     */
    private static Map<String, String> nodeAttrsToMap(Node node) {
        Map<String, String> result = new HashMap<>();
        Map<String, Object> attrs = node.getAttrs();

        if (attrs != null) {
            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    result.put(entry.getKey(), value.toString());
                }
            }
        }

        return result;
    }

    /**
     * Converts a ProseMirror document to a new YDoc.
     *
     * <p>This is a convenience method that creates a new YDoc and populates
     * it with the converted ProseMirror document. The document is stored in
     * a YXmlFragment named "prosemirror".
     *
     * @param doc the ProseMirror document to convert
     * @param schema the ProseMirror schema
     * @return a new YDoc containing the converted document
     * @throws IllegalArgumentException if doc or schema is null
     */
    public static YDoc prosemirrorToYDoc(Node doc, Schema schema) {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        YDoc ydoc = new YDoc();
        YXmlFragment fragment = ydoc.getXmlFragment("prosemirror");

        try {
            nodeToYXml(doc, fragment, schema);
            return ydoc;
        } catch (Exception e) {
            // Clean up on error
            ydoc.close();
            throw e;
        }
    }
}
