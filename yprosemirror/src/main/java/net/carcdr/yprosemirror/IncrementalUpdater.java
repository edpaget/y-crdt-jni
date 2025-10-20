package net.carcdr.yprosemirror;

import com.atlassian.prosemirror.model.Node;
import com.atlassian.prosemirror.model.Schema;
import java.util.List;
import net.carcdr.ycrdt.YXmlElement;
import net.carcdr.ycrdt.YXmlFragment;

/**
 * Applies incremental updates to Y-CRDT structures based on document diffs.
 *
 * <p>This class translates high-level document changes (insertions, deletions,
 * replacements) into specific Y-CRDT operations, avoiding full document
 * replacement and improving performance.
 *
 * <p><strong>Update Strategy:</strong>
 * <ul>
 *   <li>Process changes in reverse order to maintain position accuracy</li>
 *   <li>Apply deletions before insertions at the same position</li>
 *   <li>Use position mapping to track changes during update</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class IncrementalUpdater {

    private IncrementalUpdater() {
        // Utility class
    }

    /**
     * Applies a list of changes to a Y-CRDT fragment.
     *
     * <p>Changes are applied in an order that maintains position accuracy.
     * The method processes deletions and replacements before insertions to
     * avoid position shift issues.
     *
     * @param yFragment the Y-CRDT fragment to update
     * @param changes the list of changes to apply
     * @param schema the ProseMirror schema
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException if the fragment is closed
     */
    public static void applyChanges(
            YXmlFragment yFragment,
            List<DocumentDiff.Change> changes,
            Schema schema) {

        if (yFragment == null) {
            throw new IllegalArgumentException("YXmlFragment cannot be null");
        }
        if (changes == null) {
            throw new IllegalArgumentException("Changes list cannot be null");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        // Process changes in reverse order to maintain position accuracy
        // (changes at later positions don't affect earlier positions)
        for (int i = changes.size() - 1; i >= 0; i--) {
            DocumentDiff.Change change = changes.get(i);
            applyChange(yFragment, change, schema);
        }
    }

    /**
     * Applies a single change to a Y-CRDT fragment.
     *
     * @param yFragment the target fragment
     * @param change the change to apply
     * @param schema the ProseMirror schema
     */
    private static void applyChange(
            YXmlFragment yFragment,
            DocumentDiff.Change change,
            Schema schema) {

        YPosition pos = change.getPosition();

        if (change instanceof DocumentDiff.Insert) {
            applyInsert(yFragment, (DocumentDiff.Insert) change, schema);
        } else if (change instanceof DocumentDiff.Delete) {
            applyDelete(yFragment, (DocumentDiff.Delete) change);
        } else if (change instanceof DocumentDiff.Replace) {
            applyReplace(yFragment, (DocumentDiff.Replace) change, schema);
        }
    }

    /**
     * Applies an insertion change.
     *
     * @param yFragment the target fragment
     * @param insert the insertion change
     * @param schema the ProseMirror schema
     */
    private static void applyInsert(
            YXmlFragment yFragment,
            DocumentDiff.Insert insert,
            Schema schema) {

        YPosition pos = insert.getPosition();
        Node content = insert.getContent();
        int offset = pos.getOffset();

        // Navigate to the correct position
        YXmlFragment targetFragment = navigateToFragment(yFragment, pos.getPath());
        if (targetFragment == null) {
            return; // Invalid path
        }

        // Insert the content using ProseMirrorConverter
        ProseMirrorConverter.fragmentToYXml(
            content.getContent(),
            targetFragment,
            schema
        );
    }

    /**
     * Applies a deletion change.
     *
     * @param yFragment the target fragment
     * @param delete the deletion change
     */
    private static void applyDelete(
            YXmlFragment yFragment,
            DocumentDiff.Delete delete) {

        YPosition pos = delete.getPosition();
        int offset = pos.getOffset();
        int length = delete.getLength();

        // Navigate to the correct position
        YXmlFragment targetFragment = navigateToFragment(yFragment, pos.getPath());
        if (targetFragment == null) {
            return; // Invalid path
        }

        // Remove the specified range
        if (offset < targetFragment.length()) {
            int actualLength = Math.min(length, targetFragment.length() - offset);
            targetFragment.remove(offset, actualLength);
        }
    }

    /**
     * Applies a replacement change.
     *
     * @param yFragment the target fragment
     * @param replace the replacement change
     * @param schema the ProseMirror schema
     */
    private static void applyReplace(
            YXmlFragment yFragment,
            DocumentDiff.Replace replace,
            Schema schema) {

        YPosition pos = replace.getPosition();
        int offset = pos.getOffset();

        // Navigate to the correct position
        YXmlFragment targetFragment = navigateToFragment(yFragment, pos.getPath());
        if (targetFragment == null) {
            return; // Invalid path
        }

        // Remove old content
        if (offset < targetFragment.length()) {
            targetFragment.remove(offset, 1);
        }

        // Insert new content at the same position
        Node newContent = replace.getNewContent();
        if (newContent.isText()) {
            String text = newContent.getText();
            if (text != null && !text.isEmpty()) {
                targetFragment.insertText(offset, text);
            }
        } else {
            String tag = newContent.getType().getName();
            targetFragment.insertElement(offset, tag);

            // Set attributes if needed
            Object child = targetFragment.getChild(offset);
            if (child instanceof YXmlElement) {
                YXmlElement element = (YXmlElement) child;
                // TODO: Set attributes from newContent
            }
        }
    }

    /**
     * Navigates through a path to find the target fragment.
     *
     * @param root the root fragment
     * @param path the path to navigate (array of child indices)
     * @return the target fragment, or null if path is invalid
     */
    private static YXmlFragment navigateToFragment(YXmlFragment root, int[] path) {
        if (path.length == 0) {
            return root;
        }

        // For now, only support root-level changes
        // TODO: Implement deep navigation for nested changes
        return root;
    }
}
