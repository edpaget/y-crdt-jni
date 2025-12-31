package net.carcdr.yhocuspocus.extension;

import net.carcdr.yhocuspocus.core.YDocument;
import java.util.Map;

/**
 * Payload for the onLoadDocument hook.
 *
 * <p>Extensions should load persisted state and set it via {@link #setState(byte[])}.</p>
 *
 * @since 1.0.0
 */
public class OnLoadDocumentPayload {

    private final YDocument document;
    private final Map<String, Object> context;
    private byte[] state;

    /**
     * Creates a new document load payload.
     *
     * @param document the document being loaded
     * @param context connection context
     */
    public OnLoadDocumentPayload(YDocument document, Map<String, Object> context) {
        this.document = document;
        this.context = context;
        this.state = null;
    }

    /**
     * Gets the document being loaded.
     *
     * @return document instance
     */
    public YDocument getDocument() {
        return document;
    }

    /**
     * Gets the connection context.
     *
     * <p>This context is read-only. Attempts to modify it will throw
     * {@link UnsupportedOperationException}. Context can only be modified
     * during the onConnect and onAuthenticate hooks.</p>
     *
     * @return read-only context map
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Gets the loaded state.
     *
     * @return state bytes, or null if not set
     */
    public byte[] getState() {
        return state;
    }

    /**
     * Sets the document state to be loaded.
     *
     * <p>The first extension to set state wins. Subsequent calls
     * will not overwrite the state.</p>
     *
     * @param state Y-CRDT state bytes
     */
    public void setState(byte[] state) {
        if (this.state == null && state != null) {
            this.state = state;
        }
    }
}
