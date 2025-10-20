package net.carcdr.yprosemirror;

import com.atlassian.prosemirror.model.Fragment;
import com.atlassian.prosemirror.model.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Computes the differences between two ProseMirror documents.
 *
 * <p>This class analyzes two document states and produces a list of changes
 * that can be efficiently applied to a Y-CRDT structure, avoiding full
 * document replacement.
 *
 * <p><strong>Change Detection Strategy:</strong>
 * <ul>
 *   <li>Traverse both documents in parallel</li>
 *   <li>Detect insertions, deletions, and modifications</li>
 *   <li>Track position mapping for accurate change application</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class DocumentDiff {

    private DocumentDiff() {
        // Utility class
    }

    /**
     * Represents a single change operation in a document.
     */
    public abstract static class Change {
        protected final YPosition position;

        protected Change(YPosition position) {
            this.position = position;
        }

        /**
         * Gets the position where this change occurs.
         *
         * @return the position
         */
        public YPosition getPosition() {
            return position;
        }
    }

    /**
     * Represents content insertion at a specific position.
     */
    public static final class Insert extends Change {
        private final Node content;

        public Insert(YPosition position, Node content) {
            super(position);
            this.content = content;
        }

        /**
         * Gets the content to insert.
         *
         * @return the ProseMirror node to insert
         */
        public Node getContent() {
            return content;
        }

        @Override
        public String toString() {
            return "Insert{position=" + position + ", content=" + content.getType().getName() + "}";
        }
    }

    /**
     * Represents content deletion at a specific position.
     */
    public static final class Delete extends Change {
        private final int length;

        public Delete(YPosition position, int length) {
            super(position);
            this.length = length;
        }

        /**
         * Gets the number of items to delete.
         *
         * @return the deletion length
         */
        public int getLength() {
            return length;
        }

        @Override
        public String toString() {
            return "Delete{position=" + position + ", length=" + length + "}";
        }
    }

    /**
     * Represents content modification at a specific position.
     */
    public static final class Replace extends Change {
        private final Node oldContent;
        private final Node newContent;

        public Replace(YPosition position, Node oldContent, Node newContent) {
            super(position);
            this.oldContent = oldContent;
            this.newContent = newContent;
        }

        /**
         * Gets the old content being replaced.
         *
         * @return the old node
         */
        public Node getOldContent() {
            return oldContent;
        }

        /**
         * Gets the new content.
         *
         * @return the new node
         */
        public Node getNewContent() {
            return newContent;
        }

        @Override
        public String toString() {
            return "Replace{position=" + position
                + ", old=" + oldContent.getType().getName()
                + ", new=" + newContent.getType().getName() + "}";
        }
    }

    /**
     * Computes the differences between two ProseMirror documents.
     *
     * <p>This method performs a structural comparison and returns a list of
     * changes that can be applied to transform oldDoc into newDoc.
     *
     * @param oldDoc the original document (may be null for initial state)
     * @param newDoc the new document
     * @return a list of changes
     * @throws IllegalArgumentException if newDoc is null
     */
    public static List<Change> diff(Node oldDoc, Node newDoc) {
        if (newDoc == null) {
            throw new IllegalArgumentException("New document cannot be null");
        }

        List<Change> changes = new ArrayList<>();

        // If no old document, everything is an insertion
        if (oldDoc == null) {
            Fragment newContent = newDoc.getContent();
            for (int i = 0; i < newContent.getChildCount(); i++) {
                changes.add(new Insert(YPosition.at(i), newContent.child(i)));
            }
            return changes;
        }

        // Compare fragments at root level
        diffFragments(
            oldDoc.getContent(),
            newDoc.getContent(),
            YPosition.at(0),
            changes
        );

        return changes;
    }

    /**
     * Compares two fragments and adds changes to the list.
     *
     * @param oldFragment the original fragment
     * @param newFragment the new fragment
     * @param position the current position in the document
     * @param changes the list to accumulate changes
     */
    private static void diffFragments(
            Fragment oldFragment,
            Fragment newFragment,
            YPosition position,
            List<Change> changes) {

        int oldSize = oldFragment.getChildCount();
        int newSize = newFragment.getChildCount();

        // Simple diff algorithm: compare nodes at each index
        int minSize = Math.min(oldSize, newSize);

        for (int i = 0; i < minSize; i++) {
            Node oldChild = oldFragment.child(i);
            Node newChild = newFragment.child(i);

            if (!nodesEqual(oldChild, newChild)) {
                // Nodes differ - check if it's a replacement or structural change
                if (oldChild.getType().equals(newChild.getType())) {
                    // Same type, might just be content change
                    if (!oldChild.isText() && oldChild.getContent().getChildCount() > 0) {
                        // Recurse into children
                        diffFragments(
                            oldChild.getContent(),
                            newChild.getContent(),
                            position.child(i, 0),
                            changes
                        );
                    } else {
                        // Text or leaf node changed - replace it
                        changes.add(new Replace(
                            YPosition.at(position.getPath(), i),
                            oldChild,
                            newChild
                        ));
                    }
                } else {
                    // Different types - replace
                    changes.add(new Replace(
                        YPosition.at(position.getPath(), i),
                        oldChild,
                        newChild
                    ));
                }
            }
        }

        // Handle insertions (new doc has more children)
        for (int i = minSize; i < newSize; i++) {
            changes.add(new Insert(
                YPosition.at(position.getPath(), i),
                newFragment.child(i)
            ));
        }

        // Handle deletions (old doc has more children)
        if (oldSize > newSize) {
            changes.add(new Delete(
                YPosition.at(position.getPath(), newSize),
                oldSize - newSize
            ));
        }
    }

    /**
     * Checks if two nodes are equal (same type, text, attributes).
     *
     * @param a first node
     * @param b second node
     * @return true if nodes are equal
     */
    private static boolean nodesEqual(Node a, Node b) {
        if (a == b) {
            return true;
        }

        // Check type
        if (!a.getType().equals(b.getType())) {
            return false;
        }

        // Check text content
        if (a.isText()) {
            return Objects.equals(a.getText(), b.getText())
                && Objects.equals(a.getMarks(), b.getMarks());
        }

        // Check attributes
        if (!Objects.equals(a.getAttrs(), b.getAttrs())) {
            return false;
        }

        // Check child count
        if (a.getContent().getChildCount() != b.getContent().getChildCount()) {
            return false;
        }

        // For non-text nodes, we'll check children in diffFragments
        return true;
    }
}
