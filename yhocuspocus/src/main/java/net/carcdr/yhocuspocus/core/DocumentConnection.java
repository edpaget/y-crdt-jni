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
 *   <li>Initial sync response (following y-protocol/sync specification)</li>
 *   <li>Awareness and stateless message broadcasting</li>
 * </ul>
 *
 * <p>Note: Document update broadcasting is handled by YHocuspocus.handleDocumentChange()
 * to ensure updates are sent to all connections including the originator after all
 * extension hooks have been notified.</p>
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
     * <p>Note: Does not send initial sync proactively. Following y-protocol/sync spec,
     * the client should initiate synchronization by sending SyncStep1. The server
     * will respond with SyncStep2 (the diff) followed by SyncStep1 (server's state vector).</p>
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

        // Don't send initial sync - client should initiate with SyncStep1
        // per y-protocol/sync specification
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
     * <p>Implements the y-protocol/sync specification for client-server sync:</p>
     * <ul>
     *   <li>When receiving SyncStep1: Reply with SyncStep2 (diff) + SyncStep1 (our state)</li>
     *   <li>When receiving SyncStep2/Update: Apply changes and send sync status</li>
     * </ul>
     *
     * <p>This ensures the server only responds to client requests, not initiating them.</p>
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
            // Client sent SyncStep1 - we need to reply with SyncStep2 + SyncStep1
            // per y-protocol/sync specification for client-server model

            // 1. Send SyncStep2 (the diff/updates client needs)
            OutgoingMessage step2Response = OutgoingMessage.sync(
                documentName,
                responsePayload
            );
            send(step2Response.encode());

            // 2. Send SyncStep1 (our state vector, so client can send us any missing updates)
            byte[] ourStateVector = document.getDoc().encodeStateVector();
            byte[] step1Payload = SyncProtocol.encodeSyncStep1(ourStateVector);
            OutgoingMessage step1Response = OutgoingMessage.sync(
                documentName,
                step1Payload
            );
            send(step1Response.encode());

            // 3. Send current awareness state (so client knows about other connected users)
            byte[] awarenessUpdate = document.getAwareness().getStates();
            if (awarenessUpdate.length > 0) {
                OutgoingMessage awarenessMsg = OutgoingMessage.awareness(
                    documentName,
                    awarenessUpdate
                );
                send(awarenessMsg.encode());
            }
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
     * <p>Stateless messages are custom application-level messages that don't
     * affect the CRDT state. They are sent only to the requesting client.
     * Extension hooks will be added in Phase 5.</p>
     *
     * @param message the stateless message
     */
    private void handleStateless(IncomingMessage message) {
        String payload = message.getStatelessPayload();
        if (payload == null) {
            return;
        }

        // Echo the stateless message back to the sender
        // In Phase 5, this will run through extension hooks
        OutgoingMessage response = OutgoingMessage.stateless(documentName, payload);
        send(response.encode());
    }

    /**
     * Handles broadcast stateless messages.
     *
     * <p>Broadcasts the stateless message to all connections except the sender.
     * This allows clients to send custom messages that are distributed to all
     * other clients (e.g., chat messages, notifications).</p>
     *
     * @param message the broadcast stateless message
     */
    private void handleBroadcastStateless(IncomingMessage message) {
        String payload = message.getStatelessPayload();
        if (payload == null) {
            return;
        }

        // Broadcast to all connections except sender
        document.broadcastStateless(payload, getConnectionId());
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
