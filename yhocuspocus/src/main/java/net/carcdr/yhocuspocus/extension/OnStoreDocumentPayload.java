package net.carcdr.yhocuspocus.extension;

import net.carcdr.yhocuspocus.core.YDocument;
import java.util.Map;

/**
 * Payload for the onStoreDocument hook.
 *
 * <p>Called when a document should be persisted. This is debounced to avoid
 * excessive writes.</p>
 *
 * @since 1.0.0
 */
public class OnStoreDocumentPayload {

    private final YDocument document;
    private final Map<String, Object> context;
    private final byte[] state;

    /**
     * Creates a new store document payload.
     *
     * @param document the document to store
     * @param context connection context
     * @param state the full document state
     */
    public OnStoreDocumentPayload(YDocument document, Map<String, Object> context, byte[] state) {
        this.document = document;
        this.context = context;
        this.state = state;
    }

    /**
     * Gets the document to store.
     *
     * @return document instance
     */
    public YDocument getDocument() {
        return document;
    }

    /**
     * Gets the connection context.
     *
     * @return context map
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Gets the full document state.
     *
     * <p>Extensions should persist this state.</p>
     *
     * @return Y-CRDT state bytes
     */
    public byte[] getState() {
        return state;
    }

    /**
     * Gets the document name.
     *
     * @return document name
     */
    public String getDocumentName() {
        return document.getName();
    }
}
