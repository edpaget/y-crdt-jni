package net.carcdr.yhocuspocus.core;

import net.carcdr.yhocuspocus.protocol.IncomingMessage;
import net.carcdr.yhocuspocus.protocol.MessageType;
import net.carcdr.yhocuspocus.protocol.OutgoingMessage;
import net.carcdr.yhocuspocus.protocol.SyncProtocol;
import java.util.Map;

/**
 * Represents a connection to a specific document.
 *
 * <p>One ClientConnection can have multiple DocumentConnections
 * (one per document), enabling document multiplexing over a
 * single transport.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Message routing per message type (sync, awareness, stateless)</li>
 *   <li>Read-only mode enforcement</li>
 *   <li>Initial sync sending</li>
 *   <li>Update broadcasting to other connections</li>
 * </ul>
 */
public class DocumentConnection {

    private final ClientConnection clientConnection;
    private final YDocument document;
    private final String documentName;
    private final Map<String, Object> context;

    private volatile boolean readOnly;

    /**
     * Creates a new document connection.
     *
     * @param clientConnection the client connection
     * @param document the document
     * @param documentName the document name
     * @param context connection context
     */
    DocumentConnection(
        ClientConnection clientConnection,
        YDocument document,
        String documentName,
        Map<String, Object> context
    ) {
        this.clientConnection = clientConnection;
        this.document = document;
        this.documentName = documentName;
        this.context = context;
        this.readOnly = false;

        // Add to document
        document.addConnection(this);

        // Send initial sync
        sendInitialSync();
    }

    /**
     * Handles a message for this document.
     *
     * @param message the incoming message
     */
    public void handleMessage(IncomingMessage message) {
        MessageType type = message.getType();

        switch (type) {
            case SYNC:
                handleSync(message);
                break;
            case AWARENESS:
                handleAwareness(message);
                break;
            case QUERY_AWARENESS:
                handleQueryAwareness();
                break;
            case STATELESS:
                handleStateless(message);
                break;
            case BROADCAST_STATELESS:
                handleBroadcastStateless(message);
                break;
            default:
                // Unknown message type - ignore
                break;
        }
    }

    /**
     * Handles sync protocol messages.
     *
     * <p>Applies the sync message to the document, generates a response
     * if needed, and broadcasts changes to other connections.</p>
     *
     * @param message the sync message
     */
    private void handleSync(IncomingMessage message) {
        // Check if read-only
        if (readOnly && SyncProtocol.hasChanges(message.getPayload())) {
            sendSyncStatus(false);
            return;
        }

        // Apply sync message to document
        byte[] responsePayload = SyncProtocol.applySyncMessage(
            document.getDoc(),
            message.getPayload()
        );

        if (responsePayload != null) {
            // Send sync response
            OutgoingMessage response = OutgoingMessage.sync(
                documentName,
                responsePayload
            );
            send(response.encode());

            // Broadcast to other connections
            document.broadcast(response.encode(), getConnectionId());
        }

        sendSyncStatus(true);
    }

    /**
     * Handles awareness updates.
     *
     * <p>Updates the awareness state and broadcasts to other connections.</p>
     *
     * @param message the awareness message
     */
    private void handleAwareness(IncomingMessage message) {
        document.getAwareness().applyUpdate(
            message.getPayload(),
            getConnectionId()
        );

        // Broadcast awareness to others
        OutgoingMessage awarenessMsg = OutgoingMessage.awareness(
            documentName,
            message.getPayload()
        );
        document.broadcast(awarenessMsg.encode(), getConnectionId());
    }

    /**
     * Handles query awareness messages.
     *
     * <p>Sends the current awareness state to the client.</p>
     */
    private void handleQueryAwareness() {
        byte[] awarenessUpdate = document.getAwareness().getStates();
        OutgoingMessage msg = OutgoingMessage.awareness(documentName, awarenessUpdate);
        send(msg.encode());
    }

    /**
     * Handles stateless messages.
     *
     * <p>Will be fully implemented in Phase 4.</p>
     *
     * @param message the stateless message
     */
    private void handleStateless(IncomingMessage message) {
        // Run onStateless hooks
        // Phase 5 will add extension hook support
    }

    /**
     * Handles broadcast stateless messages.
     *
     * <p>Will be fully implemented in Phase 4.</p>
     *
     * @param message the broadcast stateless message
     */
    private void handleBroadcastStateless(IncomingMessage message) {
        // Broadcast to all connections
        document.broadcastStateless(message.getStatelessPayload(), getConnectionId());
    }

    /**
     * Sends initial sync to client.
     *
     * <p>Sends the full document state and current awareness information.</p>
     */
    private void sendInitialSync() {
        // Get full document state
        byte[] fullState = document.getDoc().encodeStateAsUpdate();

        // Create sync step 2 message
        byte[] syncPayload = SyncProtocol.encodeSyncStep2(fullState);

        OutgoingMessage syncMsg = OutgoingMessage.sync(
            documentName,
            syncPayload
        );
        send(syncMsg.encode());

        // Send awareness
        byte[] awarenessUpdate = document.getAwareness().getStates();
        if (awarenessUpdate.length > 0) {
            OutgoingMessage awarenessMsg = OutgoingMessage.awareness(
                documentName,
                awarenessUpdate
            );
            send(awarenessMsg.encode());
        }
    }

    /**
     * Sends sync status message.
     *
     * @param synced true if sync succeeded, false otherwise
     */
    private void sendSyncStatus(boolean synced) {
        OutgoingMessage status = OutgoingMessage.syncStatus(documentName, synced);
        send(status.encode());
    }

    /**
     * Sends a message via client connection.
     *
     * @param message the message bytes
     */
    public void send(byte[] message) {
        clientConnection.send(message);
    }

    /**
     * Closes this document connection.
     *
     * <p>Removes this connection from the document's connection list.</p>
     */
    public void close() {
        document.removeConnection(this);
    }

    /**
     * Gets the connection ID.
     *
     * @return connection ID
     */
    public String getConnectionId() {
        return clientConnection.getConnectionId();
    }

    /**
     * Checks if this connection is read-only.
     *
     * @return true if read-only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets read-only mode.
     *
     * @param readOnly true to enable read-only mode
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Gets the document.
     *
     * @return the document
     */
    public YDocument getDocument() {
        return document;
    }
}
