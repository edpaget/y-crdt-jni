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
            int typeValue = reader.readVarInt();
            MessageType type = MessageType.fromValue(typeValue);
            byte[] payload = reader.remaining();

            return new IncomingMessage(documentName, type, payload, data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode message: " + e.getMessage(), e);
        }
    }
}
