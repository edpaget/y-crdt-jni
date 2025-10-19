package net.carcdr.yprosemirror;

import com.atlassian.prosemirror.model.Node;
import com.atlassian.prosemirror.model.Schema;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.carcdr.ycrdt.YEvent;
import net.carcdr.ycrdt.YObserver;
import net.carcdr.ycrdt.YSubscription;
import net.carcdr.ycrdt.YXmlFragment;

/**
 * Binds a ProseMirror document structure to a Y-CRDT document for real-time collaborative editing.
 *
 * <p>This class provides the core synchronization layer between ProseMirror and Y-CRDT,
 * handling bidirectional change propagation:
 * <ul>
 *   <li>ProseMirror document updates → Y-CRDT operations</li>
 *   <li>Y-CRDT changes → ProseMirror document updates</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create Y-CRDT document
 * YDoc ydoc = new YDoc();
 * YXmlFragment yFragment = ydoc.getXmlFragment("prosemirror");
 *
 * // Create ProseMirror schema
 * Schema schema = createMySchema();
 *
 * // Create binding with update callback
 * YProseMirrorBinding binding = new YProseMirrorBinding(
 *     yFragment,
 *     schema,
 *     (newDoc) -> {
 *         // Update your ProseMirror editor with the new document
 *         updateEditor(newDoc);
 *     }
 * );
 *
 * // When ProseMirror content changes locally
 * binding.updateFromProseMirror(proseMirrorDoc);
 *
 * // When done, close the binding
 * binding.close();
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is NOT thread-safe. All method calls
 * should be made from the same thread (typically the UI thread). The onUpdate
 * callback is invoked synchronously on the same thread that triggered the Y-CRDT change.
 *
 * <p><strong>Change Loop Prevention:</strong> The binding automatically prevents
 * infinite update loops by tracking whether changes originated locally or remotely.
 *
 * <p><strong>Note on ProseMirror Integration:</strong> This binding works at the document
 * level rather than the transaction level due to prosemirror-kotlin API differences from
 * the JavaScript version. For full transaction-level integration, consider using the
 * JavaScript y-prosemirror library directly.
 *
 * @since 0.1.0
 */
public class YProseMirrorBinding implements Closeable {

    private final YXmlFragment yFragment;
    private final Schema schema;
    private final Consumer<Node> onUpdate;
    private final YSubscription subscription;
    private final AtomicBoolean isApplyingRemoteChange = new AtomicBoolean(false);
    private final boolean useIncrementalUpdates;
    private volatile Node lastDocument = null;
    private volatile boolean closed = false;

    /**
     * Creates a new binding between a Y-CRDT fragment and ProseMirror document.
     *
     * <p>The binding will:
     * <ul>
     *   <li>Initialize with the current Y-CRDT content</li>
     *   <li>Set up observers to listen for Y-CRDT changes</li>
     *   <li>Call the onUpdate callback when remote changes occur</li>
     * </ul>
     *
     * <p>By default, uses full document replacement for updates.
     *
     * @param yFragment the Y-CRDT fragment to bind to (typically named "prosemirror")
     * @param schema the ProseMirror schema defining document structure
     * @param onUpdate callback invoked when Y-CRDT changes occur (receives new ProseMirror doc)
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException if the Y-CRDT fragment is closed
     */
    public YProseMirrorBinding(
            YXmlFragment yFragment,
            Schema schema,
            Consumer<Node> onUpdate) {
        this(yFragment, schema, onUpdate, false);
    }

    /**
     * Creates a new binding with optional incremental update support.
     *
     * <p>When incremental updates are enabled, the binding will compute diffs
     * between document states and apply only the changes, improving performance
     * for large documents.
     *
     * @param yFragment the Y-CRDT fragment to bind to
     * @param schema the ProseMirror schema
     * @param onUpdate callback for document updates
     * @param useIncrementalUpdates whether to use incremental updates (true) or
     *                             full replacement (false)
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException if the Y-CRDT fragment is closed
     */
    public YProseMirrorBinding(
            YXmlFragment yFragment,
            Schema schema,
            Consumer<Node> onUpdate,
            boolean useIncrementalUpdates) {

        if (yFragment == null) {
            throw new IllegalArgumentException("YXmlFragment cannot be null");
        }
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }
        if (onUpdate == null) {
            throw new IllegalArgumentException("onUpdate callback cannot be null");
        }

        this.yFragment = yFragment;
        this.schema = schema;
        this.onUpdate = onUpdate;
        this.useIncrementalUpdates = useIncrementalUpdates;

        // Set up Y-CRDT observer to listen for remote changes
        this.subscription = yFragment.observe(new YCrdtObserver());

        // Initialize with current Y-CRDT content if any
        if (yFragment.length() > 0) {
            Node initialDoc = YCrdtConverter.yXmlToNode(yFragment, schema);
            this.lastDocument = initialDoc;
            onUpdate.accept(initialDoc);
        }
    }

    /**
     * Updates the Y-CRDT document from a new ProseMirror document state.
     *
     * <p>Call this method when the ProseMirror editor content changes locally.
     * The changes will be synchronized to Y-CRDT and propagated to other clients.
     *
     * <p><strong>Important:</strong> Only call this for LOCAL changes. Remote changes
     * received via the onUpdate callback should NOT be sent back through this method.
     *
     * <p>If incremental updates are enabled, only the differences between the last
     * document and the new document will be applied. Otherwise, full replacement is used.
     *
     * @param proseMirrorDoc the new ProseMirror document
     * @throws IllegalArgumentException if proseMirrorDoc is null
     * @throws IllegalStateException if the binding is closed
     */
    public void updateFromProseMirror(Node proseMirrorDoc) {
        checkClosed();

        if (proseMirrorDoc == null) {
            throw new IllegalArgumentException("ProseMirror document cannot be null");
        }

        // Don't apply if this is triggered by a remote change
        if (isApplyingRemoteChange.get()) {
            return;
        }

        if (useIncrementalUpdates) {
            // Apply incremental updates
            updateIncrementally(proseMirrorDoc);
        } else {
            // Replace Y-CRDT content with new ProseMirror document
            replaceYCrdtContent(proseMirrorDoc);
        }

        // Store this as the last document for future diffs
        if (useIncrementalUpdates) {
            lastDocument = proseMirrorDoc;
        }
    }

    /**
     * Gets the current ProseMirror document from Y-CRDT.
     *
     * <p>This method converts the current Y-CRDT state to a ProseMirror document.
     * Use this to get the initial state or to refresh the editor from Y-CRDT.
     *
     * @return the current ProseMirror document, or null if Y-CRDT fragment is empty
     * @throws IllegalStateException if the binding is closed
     */
    public Node getCurrentDocument() {
        checkClosed();

        if (yFragment.length() == 0) {
            return null;
        }

        return YCrdtConverter.yXmlToNode(yFragment, schema);
    }

    /**
     * Gets the bound Y-CRDT fragment.
     *
     * @return the Y-CRDT fragment
     */
    public YXmlFragment getYFragment() {
        return yFragment;
    }

    /**
     * Gets the ProseMirror schema.
     *
     * @return the schema
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Checks if this binding is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this binding and releases resources.
     *
     * <p>After closing, the binding cannot be used anymore.
     * The Y-CRDT fragment itself is NOT closed - only the observer subscription.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            if (subscription != null) {
                subscription.close();
            }
            closed = true;
        }
    }

    /**
     * Applies incremental updates to Y-CRDT based on document differences.
     *
     * <p>This method computes the diff between the last document and the new document,
     * then applies only the changes to Y-CRDT.
     */
    private void updateIncrementally(Node newDoc) {
        // Compute diff
        java.util.List<DocumentDiff.Change> changes = DocumentDiff.diff(lastDocument, newDoc);

        if (changes.isEmpty()) {
            // No changes to apply
            return;
        }

        // Apply changes to Y-CRDT
        IncrementalUpdater.applyChanges(yFragment, changes, schema);
    }

    /**
     * Replaces the entire Y-CRDT fragment content with a new ProseMirror document.
     *
     * <p>This is a simple but less efficient approach compared to incremental updates.
     */
    private void replaceYCrdtContent(Node newDoc) {
        // Clear existing content
        int length = yFragment.length();
        if (length > 0) {
            yFragment.remove(0, length);
        }

        // Convert and insert new content
        ProseMirrorConverter.nodeToYXml(newDoc, yFragment, schema);
    }

    /**
     * Applies Y-CRDT changes to ProseMirror.
     *
     * <p>This method is called when the Y-CRDT document changes (from remote edits).
     * It converts the Y-CRDT state to ProseMirror and invokes the onUpdate callback.
     */
    private void applyYCrdtChangeToProseMirror() {
        // Set flag to prevent infinite loop
        isApplyingRemoteChange.set(true);

        try {
            // Convert Y-CRDT to ProseMirror document
            Node newDoc = YCrdtConverter.yXmlToNode(yFragment, schema);

            // Update last document for incremental updates
            if (useIncrementalUpdates) {
                lastDocument = newDoc;
            }

            // Notify the callback
            onUpdate.accept(newDoc);

        } finally {
            isApplyingRemoteChange.set(false);
        }
    }

    /**
     * Checks if this binding is closed and throws if it is.
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("YProseMirrorBinding has been closed");
        }
    }

    /**
     * Observer for Y-CRDT changes.
     *
     * <p>This observer listens for changes to the Y-CRDT document and applies
     * them to ProseMirror via the onUpdate callback synchronously.
     */
    private final class YCrdtObserver implements YObserver {
        @Override
        public void onChange(YEvent event) {
            if (!closed) {
                applyYCrdtChangeToProseMirror();
            }
        }
    }
}
