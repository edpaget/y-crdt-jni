package net.carcdr.yhocuspocus.extension;

import net.carcdr.yhocuspocus.core.YDocument;

/**
 * Payload for the beforeUnloadDocument hook.
 *
 * <p>Called before a document is unloaded from memory.</p>
 *
 * @since 1.0.0
 */
public class BeforeUnloadDocumentPayload {

    private final YDocument document;

    /**
     * Creates a new before-unload payload.
     *
     * @param document the document being unloaded
     */
    public BeforeUnloadDocumentPayload(YDocument document) {
        this.document = document;
    }

    /**
     * Gets the document being unloaded.
     *
     * @return document instance
     */
    public YDocument getDocument() {
        return document;
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
