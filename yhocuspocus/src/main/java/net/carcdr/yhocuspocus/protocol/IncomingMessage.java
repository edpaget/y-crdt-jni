package net.carcdr.yhocuspocus.protocol;

/**
 * Represents an incoming message from a client.
 *
 * <p>Incoming messages are decoded from binary format using the protocol:
 * [documentName: varString][messageType: varInt][...payload]</p>
 */
public class IncomingMessage {

    private final String documentName;
    private final MessageType type;
    private final byte[] payload;
    private final byte[] rawData;

    /**
     * Creates a new incoming message.
     *
     * @param documentName the document name
     * @param type the message type
     * @param payload the message payload
     * @param rawData the original raw message data
     */
    public IncomingMessage(String documentName, MessageType type, byte[] payload, byte[] rawData) {
        this.documentName = documentName;
        this.type = type;
        this.payload = payload;
        this.rawData = rawData;
    }

    /**
     * Gets the document name from the message.
     *
     * @return document name (never null)
     */
    public String getDocumentName() {
        return documentName;
    }

    /**
     * Gets the message type.
     *
     * @return message type (never null)
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Gets the message payload.
     *
     * @return payload bytes (never null, may be empty)
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Gets the original raw message data.
     *
     * @return raw message bytes (never null)
     */
    public byte[] getRawData() {
        return rawData;
    }

    /**
     * Gets the authentication token from AUTH messages.
     *
     * <p>For AUTH messages, the payload contains the token as a varString.</p>
     *
     * @return authentication token, or null if not an AUTH message
     */
    public String getToken() {
        if (type != MessageType.AUTH || payload.length == 0) {
            return null;
        }

        try {
            VarIntReader reader = new VarIntReader(payload);
            return reader.readVarString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the stateless message payload as a string.
     *
     * <p>For STATELESS/BROADCAST_STATELESS messages, decodes the payload.</p>
     *
     * @return stateless payload, or null if not a stateless message
     */
    public String getStatelessPayload() {
        if ((type != MessageType.STATELESS && type != MessageType.BROADCAST_STATELESS) ||
            payload.length == 0) {
            return null;
        }

        try {
            VarIntReader reader = new VarIntReader(payload);
            return reader.readVarString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "IncomingMessage{" +
               "documentName='" + documentName + '\'' +
               ", type=" + type +
               ", payloadSize=" + payload.length +
               '}';
    }
}
