package net.carcdr.yhocuspocus.extension;

import java.util.Map;

/**
 * Payload for the onAuthenticate hook.
 *
 * <p>Provides information about an authentication attempt for a document.</p>
 *
 * @since 1.0.0
 */
public class OnAuthenticatePayload {

    private final String connectionId;
    private final String documentName;
    private final String token;
    private final Map<String, Object> context;
    private boolean readOnly;

    /**
     * Creates a new authentication payload.
     *
     * @param connectionId unique connection identifier
     * @param documentName name of document being accessed
     * @param token authentication token (may be null)
     * @param context mutable context map
     */
    public OnAuthenticatePayload(String connectionId, String documentName,
                                   String token, Map<String, Object> context) {
        this.connectionId = connectionId;
        this.documentName = documentName;
        this.token = token;
        this.context = context;
        this.readOnly = false;
    }

    /**
     * Gets the connection identifier.
     *
     * @return connection ID
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Gets the document name being accessed.
     *
     * @return document name
     */
    public String getDocumentName() {
        return documentName;
    }

    /**
     * Gets the authentication token.
     *
     * @return token, or null if not provided
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets the connection context.
     *
     * <p>This is the last opportunity to add information to the context.
     * After this hook completes, the context becomes read-only and attempts
     * to modify it will throw {@link UnsupportedOperationException}.</p>
     *
     * @return mutable context map
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Checks if connection should be read-only.
     *
     * @return true if read-only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets whether the connection should be read-only.
     *
     * <p>Extensions can set this to true to prevent modifications.</p>
     *
     * @param readOnly true for read-only access
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
}
