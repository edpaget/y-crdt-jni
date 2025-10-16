package net.carcdr.ycrdt;

/**
 * Base interface for all XML nodes in a Y-CRDT document.
 * XML nodes can be either elements ({@link YXmlElement}) or text ({@link YXmlText}).
 *
 * <p>This interface provides type-safe polymorphic access to XML nodes,
 * allowing navigation and manipulation of hierarchical XML structures.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * YXmlFragment fragment = doc.getXmlFragment("document");
 * YXmlNode node = fragment.get(0);
 *
 * if (node.getNodeType() == YXmlNode.NodeType.ELEMENT) {
 *     YXmlElement element = node.asElement();
 *     System.out.println("Element tag: " + element.getTag());
 * } else {
 *     YXmlText text = node.asText();
 *     System.out.println("Text content: " + text.toString());
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
public interface YXmlNode {

    /**
     * Enumeration of XML node types.
     */
    enum NodeType {
        /** XML element node (e.g., {@code <div>}) */
        ELEMENT,
        /** XML text node */
        TEXT
    }

    /**
     * Returns the type of this XML node.
     *
     * @return the node type (ELEMENT or TEXT)
     */
    NodeType getNodeType();

    /**
     * Casts this node to an element if it is an element type.
     *
     * @return this node as a {@link YXmlElement}, or {@code null} if this is not an element
     */
    YXmlElement asElement();

    /**
     * Casts this node to text if it is a text type.
     *
     * @return this node as a {@link YXmlText}, or {@code null} if this is not text
     */
    YXmlText asText();

    /**
     * Returns a string representation of this node.
     *
     * @return string representation of the node
     */
    String toString();
}
