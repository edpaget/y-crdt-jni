package net.carcdr.yhocuspocus.extension;

import net.carcdr.yhocuspocus.core.YDocument;
import java.util.Map;

/**
 * Payload for the onChange hook.
 *
 * <p>Called whenever a document is modified.</p>
 *
 * @since 1.0.0
 */
public class OnChangePayload {

    private final YDocument document;
    private final Map<String, Object> context;
    private final byte[] update;

    /**
     * Creates a new change payload.
     *
     * @param document the document that changed
     * @param context connection context
     * @param update the Y-CRDT update bytes
     */
    public OnChangePayload(YDocument document, Map<String, Object> context, byte[] update) {
        this.document = document;
        this.context = context;
        this.update = update;
    }

    /**
     * Gets the document that changed.
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
     * Gets the update bytes.
     *
     * <p>This contains the incremental changes that were applied.</p>
     *
     * @return Y-CRDT update bytes
     */
    public byte[] getUpdate() {
        return update;
    }
}
