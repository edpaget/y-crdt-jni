package net.carcdr.yhocuspocus.protocol;

import net.carcdr.ycrdt.YDoc;
import net.carcdr.ycrdt.YTransaction;

/**
 * Y.js sync protocol implementation.
 *
 * <p>Implements the sync protocol used by Yjs for efficient
 * document synchronization using state vectors and diffs.</p>
 *
 * <p>The sync protocol has three message types:</p>
 * <ul>
 *   <li><b>SyncStep1</b> (0) - Client sends state vector, server responds with diff</li>
 *   <li><b>SyncStep2</b> (1) - Response containing document updates</li>
 *   <li><b>Update</b> (2) - Incremental update from client changes</li>
 * </ul>
 *
 * <p>Message format:</p>
 * <pre>
 * [syncType: varInt][...payload]
 * </pre>
 *
 * @see <a href="https://docs.yjs.dev/api/document-updates">Yjs Document Updates</a>
 */
public final class SyncProtocol {

    /** SyncStep1: State vector exchange (client → server). */
    private static final int SYNC_STEP_1 = 0;

    /** SyncStep2: Document diff response (server → client). */
    private static final int SYNC_STEP_2 = 1;

    /** Update: Incremental change (client → server). */
    private static final int UPDATE = 2;

    /**
     * Private constructor to prevent instantiation.
     */
    private SyncProtocol() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Applies a sync message to a document.
     *
     * @param doc the document
     * @param payload the sync message payload
     * @return response payload, or null if no response needed
     * @throws IllegalArgumentException if payload is invalid
     */
    public static byte[] applySyncMessage(YDoc doc, byte[] payload) {
        if (doc == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }

        VarIntReader reader = new VarIntReader(payload);
        int syncType = (int) reader.readVarInt();

        switch (syncType) {
            case SYNC_STEP_1:
                return handleSyncStep1(doc, reader);
            case SYNC_STEP_2:
            case UPDATE:
                handleUpdate(doc, reader);
                return null;
            default:
                throw new IllegalArgumentException("Unknown sync type: " + syncType);
        }
    }

    /**
     * Handles SyncStep1 (state vector exchange).
     *
     * <p>Client sends their state vector, server responds with
     * a diff containing updates the client hasn't seen yet.</p>
     *
     * @param doc the document
     * @param reader the payload reader positioned after sync type
     * @return SyncStep2 response payload
     */
    private static byte[] handleSyncStep1(YDoc doc, VarIntReader reader) {
        byte[] clientStateVector = reader.readVarBytes();

        // Encode diff from client state
        byte[] diff;
        if (clientStateVector.length == 0) {
            // Client has no state - send full document
            diff = doc.encodeStateAsUpdate();
        } else {
            // Client has some state - send only what they're missing
            diff = doc.encodeDiff(clientStateVector);
        }

        // Build SyncStep2 response
        return encodeSyncStep2(diff);
    }

    /**
     * Handles Update (incremental changes).
     *
     * <p>Client sends incremental changes to apply to the document.
     * Updates are applied within a transaction for atomicity and performance.
     * This ensures observers fire only once per update batch.</p>
     *
     * @param doc the document
     * @param reader the payload reader positioned after sync type
     */
    private static void handleUpdate(YDoc doc, VarIntReader reader) {
        byte[] update = reader.readVarBytes();
        if (update.length > 0) {
            // Wrap in transaction for atomicity and to batch observer notifications
            try (YTransaction txn = doc.beginTransaction()) {
                doc.applyUpdate(update);
            }
        }
    }

    /**
     * Encodes a SyncStep1 message.
     *
     * <p>Format: [SYNC_STEP_1: varInt][stateVector: varBytes]</p>
     *
     * <p>SyncStep1 is sent by a client/server to communicate their current state.
     * The recipient can use this state vector to determine what updates to send back.</p>
     *
     * @param stateVector the state vector bytes
     * @return encoded SyncStep1 payload
     */
    public static byte[] encodeSyncStep1(byte[] stateVector) {
        if (stateVector == null) {
            throw new IllegalArgumentException("State vector cannot be null");
        }

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(SYNC_STEP_1);
        writer.writeVarBytes(stateVector);
        return writer.toByteArray();
    }

    /**
     * Encodes a SyncStep2 message.
     *
     * <p>Format: [SYNC_STEP_2: varInt][update: varBytes]</p>
     *
     * @param update the update bytes
     * @return encoded SyncStep2 payload
     */
    public static byte[] encodeSyncStep2(byte[] update) {
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(SYNC_STEP_2);
        writer.writeVarBytes(update);
        return writer.toByteArray();
    }

    /**
     * Encodes an Update message.
     *
     * <p>Format: [UPDATE: varInt][update: varBytes]</p>
     *
     * @param update the update bytes
     * @return encoded Update payload
     */
    public static byte[] encodeUpdate(byte[] update) {
        if (update == null) {
            throw new IllegalArgumentException("Update cannot be null");
        }

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(UPDATE);
        writer.writeVarBytes(update);
        return writer.toByteArray();
    }

    /**
     * Checks if a sync message contains changes.
     *
     * <p>Used to enforce read-only mode by checking if a message
     * contains updates that would modify the document.</p>
     *
     * @param payload the sync message payload
     * @return true if message contains changes
     */
    public static boolean hasChanges(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return false;
        }

        VarIntReader reader = new VarIntReader(payload);
        int syncType = (int) reader.readVarInt();

        if (syncType == UPDATE || syncType == SYNC_STEP_2) {
            byte[] update = reader.readVarBytes();
            return update.length > 0; // Simplified check
        }

        return false;
    }
}
