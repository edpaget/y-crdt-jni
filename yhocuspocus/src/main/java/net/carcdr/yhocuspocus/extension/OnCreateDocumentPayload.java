package net.carcdr.yhocuspocus.extension;

import net.carcdr.yhocuspocus.core.YDocument;
import java.util.Map;

/**
 * Payload for the onCreateDocument hook.
 *
 * <p>Called when a document is first created in memory.</p>
 *
 * @since 1.0.0
 */
public class OnCreateDocumentPayload {

    private final YDocument document;
    private final Map<String, Object> context;

    /**
     * Creates a new document creation payload.
     *
     * @param document the document being created
     * @param context connection context
     */
    public OnCreateDocumentPayload(YDocument document, Map<String, Object> context) {
        this.document = document;
        this.context = context;
    }

    /**
     * Gets the document being created.
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
}
