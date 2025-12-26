package net.carcdr.ycrdt;

/**
 * Marker interface for XML node types.
 */
public interface YXmlNode {

    /**
     * Types of XML nodes.
     */
    enum NodeType {
        /** An XML element node. */
        ELEMENT,
        /** An XML text node. */
        TEXT
    }

    /**
     * Returns the type of this node.
     *
     * @return the node type
     */
    NodeType getNodeType();

    /**
     * Casts this node to an element.
     *
     * @return this node as a YXmlElement
     * @throws ClassCastException if this node is not an element
     */
    default YXmlElement asElement() {
        if (this instanceof YXmlElement) {
            return (YXmlElement) this;
        }
        throw new ClassCastException("Node is not an element");
    }

    /**
     * Casts this node to a text node.
     *
     * @return this node as a YXmlText
     * @throws ClassCastException if this node is not a text node
     */
    default YXmlText asText() {
        if (this instanceof YXmlText) {
            return (YXmlText) this;
        }
        throw new ClassCastException("Node is not a text node");
    }
}
