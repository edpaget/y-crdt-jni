package net.carcdr.yhocuspocus.extension;

import net.carcdr.yhocuspocus.core.YDocument;
import java.util.Map;

/**
 * Payload for the afterLoadDocument hook.
 *
 * <p>Called after a document has been loaded and state applied.</p>
 *
 * @since 1.0.0
 */
public class AfterLoadDocumentPayload {

    private final YDocument document;
    private final Map<String, Object> context;

    /**
     * Creates a new after-load payload.
     *
     * @param document the loaded document
     * @param context connection context
     */
    public AfterLoadDocumentPayload(YDocument document, Map<String, Object> context) {
        this.document = document;
        this.context = context;
    }

    /**
     * Gets the loaded document.
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
}
