package net.carcdr.yhocuspocus.protocol;

/**
 * Represents an outgoing message to a client.
 *
 * <p>Outgoing messages are encoded to binary format using the protocol:
 * [documentName: varString][messageType: varInt][...payload]</p>
 */
public final class OutgoingMessage {

    private final String documentName;
    private final MessageType type;
    private final byte[] payload;

    private OutgoingMessage(String documentName, MessageType type, byte[] payload) {
        if (documentName == null) {
            throw new IllegalArgumentException("Document name cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        this.documentName = documentName;
        this.type = type;
        this.payload = payload;
    }

    /**
     * Encodes this message to binary format.
     *
     * @return encoded message bytes
     */
    public byte[] encode() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(documentName);
        writer.writeVarInt(type.getValue());
        writer.writeBytes(payload);
        return writer.toByteArray();
    }

    /**
     * Gets the document name.
     *
     * @return document name
     */
    public String getDocumentName() {
        return documentName;
    }

    /**
     * Gets the message type.
     *
     * @return message type
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Gets the payload.
     *
     * @return payload bytes
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Creates a SYNC message.
     *
     * @param documentName document name
     * @param payload sync protocol payload
     * @return outgoing sync message
     */
    public static OutgoingMessage sync(String documentName, byte[] payload) {
        return new OutgoingMessage(documentName, MessageType.SYNC, payload);
    }

    /**
     * Creates a SYNC_REPLY message (deprecated, use sync instead).
     *
     * @param documentName document name
     * @param payload sync protocol payload
     * @return outgoing sync reply message
     */
    public static OutgoingMessage syncReply(String documentName, byte[] payload) {
        return new OutgoingMessage(documentName, MessageType.SYNC_REPLY, payload);
    }

    /**
     * Creates an AWARENESS message.
     *
     * @param documentName document name
     * @param payload awareness update payload
     * @return outgoing awareness message
     */
    public static OutgoingMessage awareness(String documentName, byte[] payload) {
        return new OutgoingMessage(documentName, MessageType.AWARENESS, payload);
    }

    /**
     * Creates a SYNC_STATUS message.
     *
     * @param documentName document name
     * @param synced true if synced, false otherwise
     * @return outgoing sync status message
     */
    public static OutgoingMessage syncStatus(String documentName, boolean synced) {
        byte[] payload = new byte[]{(byte) (synced ? 1 : 0)};
        return new OutgoingMessage(documentName, MessageType.SYNC_STATUS, payload);
    }

    /**
     * Creates a STATELESS message.
     *
     * @param documentName document name
     * @param payload stateless message payload
     * @return outgoing stateless message
     */
    public static OutgoingMessage stateless(String documentName, String payload) {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(payload);
        return new OutgoingMessage(documentName, MessageType.STATELESS, writer.toByteArray());
    }

    /**
     * Creates a BROADCAST_STATELESS message.
     *
     * @param documentName document name
     * @param payload stateless message payload
     * @return outgoing broadcast stateless message
     */
    public static OutgoingMessage broadcastStateless(String documentName, String payload) {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(payload);
        return new OutgoingMessage(documentName, MessageType.BROADCAST_STATELESS,
                                   writer.toByteArray());
    }

    @Override
    public String toString() {
        return "OutgoingMessage{" +
               "documentName='" + documentName + '\'' +
               ", type=" + type +
               ", payloadSize=" + payload.length +
               '}';
    }
}
