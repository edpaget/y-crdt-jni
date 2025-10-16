package net.carcdr.yprosemirror.test

import com.atlassian.prosemirror.model.Schema
import com.atlassian.prosemirror.model.SchemaSpec
import com.atlassian.prosemirror.testbuilder.NodeSpecImpl
import com.atlassian.prosemirror.testbuilder.MarkSpecImpl
import com.atlassian.prosemirror.testbuilder.AttributeSpecImpl

/**
 * Test helper providing pre-built ProseMirror schemas for testing.
 *
 * This object provides factory methods for creating ProseMirror schemas
 * from Kotlin, which is much simpler than constructing them from Java
 * due to Kotlin's non-null type requirements and data class constructors.
 *
 * Usage from Java tests:
 * ```java
 * import net.carcdr.yprosemirror.test.TestSchemas;
 *
 * Schema schema = TestSchemas.createBasicSchema();
 * ```
 *
 * @since 0.1.0
 */
object TestSchemas {

    /**
     * Creates a basic ProseMirror schema for testing.
     *
     * This schema includes:
     * - **doc**: Root document node (content: "block+")
     * - **paragraph**: Block-level paragraph node (content: "inline*")
     * - **text**: Inline text node
     *
     * This is the minimal schema needed for basic document testing.
     *
     * @return A Schema instance with basic node types
     */
    @JvmStatic
    fun createBasicSchema(): Schema {
        val nodes = mapOf(
            "doc" to NodeSpecImpl(
                content = "block+"
            ),
            "paragraph" to NodeSpecImpl(
                content = "inline*",
                group = "block"
            ),
            "text" to NodeSpecImpl(
                group = "inline"
            )
        )

        return Schema(SchemaSpec(nodes = nodes, marks = emptyMap()))
    }

    /**
     * Creates a rich schema with formatting marks.
     *
     * This schema includes:
     *
     * **Nodes:**
     * - **doc**: Root document node (content: "block+")
     * - **paragraph**: Block-level paragraph node (content: "inline*")
     * - **heading**: Block-level heading with level attribute (content: "inline*")
     * - **text**: Inline text node
     *
     * **Marks:**
     * - **bold**: Bold text formatting
     * - **italic**: Italic text formatting
     * - **link**: Hyperlink with href attribute
     *
     * This schema is suitable for testing rich text editing scenarios.
     *
     * @return A Schema instance with nodes and marks
     */
    @JvmStatic
    fun createRichSchema(): Schema {
        val nodes = mapOf(
            "doc" to NodeSpecImpl(
                content = "block+"
            ),
            "paragraph" to NodeSpecImpl(
                content = "inline*",
                group = "block"
            ),
            "heading" to NodeSpecImpl(
                content = "inline*",
                group = "block",
                attrs = mapOf(
                    "level" to AttributeSpecImpl(default = 1)
                )
            ),
            "text" to NodeSpecImpl(
                group = "inline"
            )
        )

        val marks = mapOf(
            "bold" to MarkSpecImpl(),
            "italic" to MarkSpecImpl(),
            "link" to MarkSpecImpl(
                attrs = mapOf(
                    "href" to AttributeSpecImpl()
                )
            )
        )

        return Schema(SchemaSpec(nodes = nodes, marks = marks))
    }

    /**
     * Creates a schema with list support.
     *
     * This schema includes:
     *
     * **Nodes:**
     * - **doc**: Root document node (content: "block+")
     * - **paragraph**: Block-level paragraph node (content: "inline*")
     * - **bullet_list**: Unordered list (content: "list_item+")
     * - **ordered_list**: Ordered list (content: "list_item+")
     * - **list_item**: List item (content: "paragraph block*")
     * - **text**: Inline text node
     *
     * **Marks:**
     * - **bold**: Bold text formatting
     * - **italic**: Italic text formatting
     *
     * This schema is suitable for testing nested list structures.
     *
     * @return A Schema instance with list support
     */
    @JvmStatic
    fun createListSchema(): Schema {
        val nodes = mapOf(
            "doc" to NodeSpecImpl(
                content = "block+"
            ),
            "paragraph" to NodeSpecImpl(
                content = "inline*",
                group = "block"
            ),
            "bullet_list" to NodeSpecImpl(
                content = "list_item+",
                group = "block"
            ),
            "ordered_list" to NodeSpecImpl(
                content = "list_item+",
                group = "block"
            ),
            "list_item" to NodeSpecImpl(
                content = "paragraph block*"
            ),
            "text" to NodeSpecImpl(
                group = "inline"
            )
        )

        val marks = mapOf(
            "bold" to MarkSpecImpl(),
            "italic" to MarkSpecImpl()
        )

        return Schema(SchemaSpec(nodes = nodes, marks = marks))
    }

    /**
     * Creates a comprehensive schema for advanced testing.
     *
     * This schema includes all features from other schemas plus:
     *
     * **Additional Nodes:**
     * - **blockquote**: Block quote (content: "block+")
     * - **code_block**: Code block (content: "text*")
     * - **horizontal_rule**: Horizontal rule separator
     * - **image**: Image with src and alt attributes
     * - **hard_break**: Hard line break
     *
     * **Additional Marks:**
     * - **code**: Inline code formatting
     * - **underline**: Underlined text
     * - **strikethrough**: Strikethrough text
     *
     * This schema is suitable for comprehensive integration testing.
     *
     * @return A Schema instance with comprehensive node and mark types
     */
    @JvmStatic
    fun createComprehensiveSchema(): Schema {
        val nodes = mapOf(
            "doc" to NodeSpecImpl(
                content = "block+"
            ),
            "paragraph" to NodeSpecImpl(
                content = "inline*",
                group = "block"
            ),
            "heading" to NodeSpecImpl(
                content = "inline*",
                group = "block",
                attrs = mapOf(
                    "level" to AttributeSpecImpl(default = 1)
                )
            ),
            "blockquote" to NodeSpecImpl(
                content = "block+",
                group = "block"
            ),
            "code_block" to NodeSpecImpl(
                content = "text*",
                group = "block",
                code = true
            ),
            "bullet_list" to NodeSpecImpl(
                content = "list_item+",
                group = "block"
            ),
            "ordered_list" to NodeSpecImpl(
                content = "list_item+",
                group = "block"
            ),
            "list_item" to NodeSpecImpl(
                content = "paragraph block*"
            ),
            "horizontal_rule" to NodeSpecImpl(
                group = "block"
            ),
            "text" to NodeSpecImpl(
                group = "inline"
            ),
            "image" to NodeSpecImpl(
                group = "inline",
                inline = true,
                attrs = mapOf(
                    "src" to AttributeSpecImpl(),
                    "alt" to AttributeSpecImpl(default = "")
                )
            ),
            "hard_break" to NodeSpecImpl(
                group = "inline",
                inline = true
            )
        )

        val marks = mapOf(
            "bold" to MarkSpecImpl(),
            "italic" to MarkSpecImpl(),
            "code" to MarkSpecImpl(),
            "underline" to MarkSpecImpl(),
            "strikethrough" to MarkSpecImpl(),
            "link" to MarkSpecImpl(
                attrs = mapOf(
                    "href" to AttributeSpecImpl()
                )
            )
        )

        return Schema(SchemaSpec(nodes = nodes, marks = marks))
    }
}
