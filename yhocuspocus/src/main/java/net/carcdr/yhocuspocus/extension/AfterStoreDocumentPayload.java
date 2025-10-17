package net.carcdr.yhocuspocus.extension;

import net.carcdr.yhocuspocus.core.YDocument;
import java.util.Map;

/**
 * Payload for the afterStoreDocument hook.
 *
 * <p>Called after a document has been successfully persisted.</p>
 *
 * @since 1.0.0
 */
public class AfterStoreDocumentPayload {

    private final YDocument document;
    private final Map<String, Object> context;

    /**
     * Creates a new after-store payload.
     *
     * @param document the stored document
     * @param context connection context
     */
    public AfterStoreDocumentPayload(YDocument document, Map<String, Object> context) {
        this.document = document;
        this.context = context;
    }

    /**
     * Gets the stored document.
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
