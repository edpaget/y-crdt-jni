package net.carcdr.yhocuspocus.protocol;

/**
 * Message types for Y-CRDT synchronization protocol.
 *
 * <p>These message types are compatible with the Hocuspocus/Yjs protocol
 * and define the different kinds of messages exchanged between client and server.</p>
 *
 * <p>Message format: [documentName: varString][messageType: varInt][...payload]</p>
 */
public enum MessageType {
    /**
     * Unknown or invalid message type.
     */
    UNKNOWN(-1),

    /**
     * Sync protocol message (Y.js sync step 1, step 2, or update).
     */
    SYNC(0),

    /**
     * Awareness update (user presence, cursor position, selection).
     */
    AWARENESS(1),

    /**
     * Authentication message.
     */
    AUTH(2),

    /**
     * Query awareness request (ask for current awareness states).
     */
    QUERY_AWARENESS(3),

    /**
     * Sync reply message (deprecated, use SYNC).
     */
    SYNC_REPLY(4),

    /**
     * Stateless custom message to server.
     */
    STATELESS(5),

    /**
     * Broadcast stateless custom message to all clients.
     */
    BROADCAST_STATELESS(6),

    /**
     * Graceful connection close.
     */
    CLOSE(7),

    /**
     * Sync status acknowledgment.
     */
    SYNC_STATUS(8);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    /**
     * Gets the numeric value of this message type.
     *
     * @return message type value
     */
    public int getValue() {
        return value;
    }

    /**
     * Converts a numeric value to a message type.
     *
     * @param value the numeric message type value
     * @return corresponding MessageType, or UNKNOWN if not recognized
     */
    public static MessageType fromValue(int value) {
        for (MessageType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * Checks if this is a valid message type (not UNKNOWN).
     *
     * @return true if valid, false if UNKNOWN
     */
    public boolean isValid() {
        return this != UNKNOWN;
    }
}
