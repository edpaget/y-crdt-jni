package net.carcdr.yhocuspocus.protocol;

/**
 * Decoder for incoming binary messages.
 *
 * <p>Decodes messages from the Yjs/Hocuspocus protocol format:
 * [documentName: varString][messageType: varInt][...payload]</p>
 */
public final class MessageDecoder {

    /**
     * Private constructor to prevent instantiation.
     */
    private MessageDecoder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Decodes a binary message.
     *
     * <p>Decoding format depends on message type:</p>
     * <ul>
     *   <li>SYNC messages: [docName][type][payload] - payload parsed by SyncProtocol</li>
     *   <li>Other messages: [docName][type][length][payload] - length-prefixed payload</li>
     * </ul>
     *
     * @param data the encoded message data
     * @return decoded message
     * @throws IllegalArgumentException if data is null or invalid
     */
    public static IncomingMessage decode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Message data cannot be null");
        }

        if (data.length == 0) {
            throw new IllegalArgumentException("Message data cannot be empty");
        }

        try {
            VarIntReader reader = new VarIntReader(data);

            String documentName = reader.readVarString();
            long typeValue = reader.readVarInt();
            MessageType type = MessageType.fromValue((int) typeValue);

            // SYNC and SYNC_REPLY messages have payload directly (parsed by SyncProtocol)
            // Other messages have length-prefixed payload
            byte[] payload;
            if (type == MessageType.SYNC || type == MessageType.SYNC_REPLY) {
                payload = reader.remaining();
            } else {
                payload = reader.readVarBytes();
            }

            return new IncomingMessage(documentName, type, payload, data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode message: " + e.getMessage(), e);
        }
    }
}
