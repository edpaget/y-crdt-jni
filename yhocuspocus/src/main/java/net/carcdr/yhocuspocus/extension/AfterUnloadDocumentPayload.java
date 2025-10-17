package net.carcdr.yhocuspocus.extension;

/**
 * Payload for the afterUnloadDocument hook.
 *
 * <p>Called after a document has been unloaded from memory.
 * The document is no longer accessible at this point.</p>
 *
 * @since 1.0.0
 */
public class AfterUnloadDocumentPayload {

    private final String documentName;

    /**
     * Creates a new after-unload payload.
     *
     * @param documentName name of the unloaded document
     */
    public AfterUnloadDocumentPayload(String documentName) {
        this.documentName = documentName;
    }

    /**
     * Gets the name of the unloaded document.
     *
     * @return document name
     */
    public String getDocumentName() {
        return documentName;
    }
}
