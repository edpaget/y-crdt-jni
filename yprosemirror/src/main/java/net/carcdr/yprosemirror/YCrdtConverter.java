package net.carcdr.yprosemirror;

import com.atlassian.prosemirror.model.Fragment;
import com.atlassian.prosemirror.model.Mark;
import com.atlassian.prosemirror.model.Node;
import com.atlassian.prosemirror.model.Schema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;
import net.carcdr.ycrdt.YXmlText;

/**
 * Converts Y-CRDT structures to ProseMirror documents.
 *
 * <p>This class provides utilities for converting Y-CRDT XML structures
 * (YXmlFragment, YXmlElement, YXmlText) into ProseMirror nodes and documents.
 *
 * <p><strong>Mapping Strategy:</strong>
 * <ul>
 *   <li>YXmlElement → ProseMirror Node (XML tag becomes node type)</li>
 *   <li>YXmlFragment → ProseMirror Fragment (container for nodes)</li>
 *   <li>YXmlText → Text node (formatting attributes become marks)</li>
 *   <li>XML attributes → Node attributes</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Convert a YDoc to ProseMirror
 * Node pmDoc = YCrdtConverter.yDocToProsemirror(ydoc, "prosemirror", schema);
 *
 * // Or convert a YXmlFragment directly
 * try (YDoc ydoc = new YDoc();
 *      YXmlFragment fragment = ydoc.getXmlFragment("prosemirror")) {
 *     Node pmDoc = YCrdtConverter.yXmlToNode(fragment, schema);
 * }
 * }</pre>
 *
 * @see ProseMirrorConverter for the reverse conversion (ProseMirror to Y-CRDT)
 * @since 0.1.0
 */
public final class YCrdtConverter {

    private YCrdtConverter() {
        // Utility class, prevent instantiation
    }

    /**
     * Converts a YXmlFragment to a ProseMirror Node.
     *
     * <p>This method traverses the Y-CRDT XML tree and creates corresponding
     * ProseMirror nodes. The conversion preserves:
     * <ul>
     *   <li>XML hierarchy and structure</li>
     *   <li>XML tags (as node types)</li>
     *   <li>XML attributes (as node attributes)</li>
     *   <li>Text content and formatting (as text nodes with marks)</li>
     * </ul>
     *
     * <p>The fragment's children are converted to a ProseMirror Fragment which is then
     * wrapped in a document node if the schema has a top-level document type.
     *
     * @param fragment the YXmlFragment to convert
     * @param schema the ProseMirror schema to conform to
     * @return the ProseMirror document node
     * @throws IllegalArgumentException if fragment or schema is null
     * @throws IllegalStateException if the fragment is closed
     */
    public static Node yXmlToNode(YXmlFragment fragment, Schema schema) {
        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        // Convert fragment children to ProseMirror Fragment
        Fragment pmFragment = yXmlFragmentToFragment(fragment, schema);

        // Wrap in a document node using schema's top node type
        // Typically this is "doc" in most ProseMirror schemas
        return schema.node(schema.getTopNodeType(), null, pmFragment, null);
    }

    /**
     * Converts a YXmlElement to a ProseMirror Node.
     *
     * @param element the YXmlElement to convert
     * @param schema the ProseMirror schema to conform to
     * @return the ProseMirror node
     * @throws IllegalArgumentException if element or schema is null
     * @throws IllegalStateException if the element is closed
     */
    public static Node yXmlElementToNode(YXmlElement element, Schema schema) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        return convertYXmlElementToNode(element, schema);
    }

    /**
     * Converts a YDoc to a ProseMirror document.
     *
     * <p>This is a convenience method that retrieves a YXmlFragment from the YDoc
     * and converts it to a ProseMirror document.
     *
     * @param ydoc the YDoc containing the document
     * @param fragmentName the name of the YXmlFragment in the YDoc (typically "prosemirror")
     * @param schema the ProseMirror schema
     * @return the ProseMirror document node
     * @throws IllegalArgumentException if any parameter is null or fragmentName is empty
     * @throws IllegalStateException if the YDoc is closed
     */
    public static Node yDocToProsemirror(YDoc ydoc, String fragmentName, Schema schema) {
        if (ydoc == null) {
            throw new IllegalArgumentException("YDoc cannot be null");
        }
        if (fragmentName == null || fragmentName.isEmpty()) {
            throw new IllegalArgumentException("Fragment name cannot be null or empty");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        YXmlFragment fragment = ydoc.getXmlFragment(fragmentName);
        try {
            return yXmlToNode(fragment, schema);
        } finally {
            fragment.close();
        }
    }

    /**
     * Converts a YXmlFragment to a ProseMirror Fragment.
     *
     * @param yFragment the Y-CRDT fragment
     * @param schema the ProseMirror schema
     * @return a ProseMirror Fragment containing the converted children
     */
    private static Fragment yXmlFragmentToFragment(YXmlFragment yFragment, Schema schema) {
        List<Node> nodes = new ArrayList<>();

        // Convert each child
        int length = yFragment.length();
        for (int i = 0; i < length; i++) {
            Object child = yFragment.getChild(i);

            if (child instanceof YXmlElement) {
                YXmlElement yElement = (YXmlElement) child;
                Node pmNode = convertYXmlElementToNode(yElement, schema);
                if (pmNode != null) {
                    nodes.add(pmNode);
                }
                yElement.close();
            } else if (child instanceof YXmlText) {
                YXmlText yText = (YXmlText) child;
                Node textNode = convertYXmlTextToNode(yText, schema);
                if (textNode != null) {
                    nodes.add(textNode);
                }
                yText.close();
            }
        }

        // Convert list to Fragment using Fragment constructor
        // Fragment(List, Integer) - size parameter can be null
        return new Fragment(nodes, null);
    }

    /**
     * Converts a YXmlElement to a ProseMirror Node.
     *
     * @param yElement the Y-CRDT element
     * @param schema the ProseMirror schema
     * @return the ProseMirror node
     */
    private static Node convertYXmlElementToNode(YXmlElement yElement, Schema schema) {
        String tag = yElement.getTag();

        // Get node type from schema
        // In ProseMirror, node type names typically match XML tags
        com.atlassian.prosemirror.model.NodeType nodeType = schema.getNodes().get(tag);
        if (nodeType == null) {
            // If tag doesn't exist in schema, skip or use a default
            // For now, we'll skip unknown tags
            return null;
        }

        // Convert XML attributes to ProseMirror attributes
        Map<String, Object> attrs = xmlAttrsToMap(yElement);

        // Convert children
        Fragment content = convertYXmlElementChildren(yElement, schema);

        // Create the node
        return schema.node(nodeType, attrs, content, null);
    }

    /**
     * Converts the children of a YXmlElement to a ProseMirror Fragment.
     *
     * @param yElement the Y-CRDT element
     * @param schema the ProseMirror schema
     * @return a Fragment containing the child nodes
     */
    private static Fragment convertYXmlElementChildren(YXmlElement yElement, Schema schema) {
        List<Node> children = new ArrayList<>();

        int childCount = yElement.childCount();
        for (int i = 0; i < childCount; i++) {
            Object child = yElement.getChild(i);

            if (child instanceof YXmlElement) {
                YXmlElement childElement = (YXmlElement) child;
                Node pmNode = convertYXmlElementToNode(childElement, schema);
                if (pmNode != null) {
                    children.add(pmNode);
                }
                childElement.close();
            } else if (child instanceof YXmlText) {
                YXmlText childText = (YXmlText) child;
                Node textNode = convertYXmlTextToNode(childText, schema);
                if (textNode != null) {
                    children.add(textNode);
                }
                childText.close();
            }
        }

        // Convert list to Fragment using Fragment constructor
        return new Fragment(children, null);
    }

    /**
     * Converts a YXmlText node to a ProseMirror text node.
     *
     * @param yText the Y-CRDT text node
     * @param schema the ProseMirror schema
     * @return the ProseMirror text node
     */
    private static Node convertYXmlTextToNode(YXmlText yText, Schema schema) {
        String text = yText.toString();

        if (text == null || text.isEmpty()) {
            return null;
        }

        // TODO: Extract formatting attributes and convert to marks
        // For now, create plain text without marks
        return schema.text(text, null);
    }

    /**
     * Converts Y-CRDT XML attributes to a ProseMirror attribute map.
     *
     * @param yElement the Y-CRDT element
     * @return a map of attribute name to value
     */
    private static Map<String, Object> xmlAttrsToMap(YXmlElement yElement) {
        Map<String, Object> attrs = new HashMap<>();

        // Get all attribute names
        String[] attrNames = yElement.getAttributeNames();
        if (attrNames != null) {
            for (String name : attrNames) {
                String value = yElement.getAttribute(name);
                if (value != null) {
                    attrs.put(name, value);
                }
            }
        }

        return attrs.isEmpty() ? null : attrs;
    }

    /**
     * Converts Y-CRDT formatting attributes to ProseMirror marks.
     *
     * @param attrs the formatting attributes from YXmlText
     * @param schema the ProseMirror schema
     * @return a list of ProseMirror marks
     */
    @SuppressWarnings("unused")
    private static List<Mark> attributesToMarks(Map<String, Object> attrs, Schema schema) {
        List<Mark> marks = new ArrayList<>();

        if (attrs == null || attrs.isEmpty()) {
            return marks;
        }

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Check if this is a simple mark (boolean true)
            if (value instanceof Boolean && (Boolean) value) {
                // Simple mark like "bold", "italic"
                com.atlassian.prosemirror.model.MarkType markType = schema.getMarks().get(key);
                if (markType != null) {
                    marks.add(schema.mark(markType, null));
                }
            } else if (key.contains("_")) {
                // Complex mark with attributes (e.g., "link_href")
                String[] parts = key.split("_", 2);
                String markName = parts[0];
                String attrName = parts[1];

                com.atlassian.prosemirror.model.MarkType markType = schema.getMarks().get(markName);
                if (markType != null) {
                    Map<String, Object> markAttrs = new HashMap<>();
                    markAttrs.put(attrName, value);
                    marks.add(schema.mark(markType, markAttrs));
                }
            }
        }

        return marks;
    }
}
