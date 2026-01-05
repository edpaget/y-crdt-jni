package net.carcdr.yhocuspocus.redis;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Encodes and decodes Redis messages with instance ID prefix.
 *
 * <p>Message format:</p>
 * <pre>
 * +----------------+------------------+--------------+
 * | instanceIdLen  | instanceId       | payload      |
 * | (4 bytes)      | (variable)       | (variable)   |
 * +----------------+------------------+--------------+
 * </pre>
 *
 * <p>The instance ID prefix allows receivers to filter out messages
 * that originated from the same instance.</p>
 */
public final class MessageCodec {

    private MessageCodec() {
        // Utility class
    }

    /**
     * Encodes a message with instance ID prefix.
     *
     * @param instanceId the originating instance ID
     * @param payload the message payload
     * @return the encoded message
     */
    public static byte[] encode(String instanceId, byte[] payload) {
        byte[] instanceIdBytes = instanceId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + instanceIdBytes.length + payload.length);
        buffer.putInt(instanceIdBytes.length);
        buffer.put(instanceIdBytes);
        buffer.put(payload);
        return buffer.array();
    }

    /**
     * Decodes a message, extracting the instance ID.
     *
     * @param message the encoded message
     * @return the decoded message, or null if message is invalid
     */
    public static DecodedMessage decode(byte[] message) {
        if (message == null || message.length < 4) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(message);
        int instanceIdLen = buffer.getInt();

        if (instanceIdLen < 0 || instanceIdLen > message.length - 4) {
            return null;
        }

        byte[] instanceIdBytes = new byte[instanceIdLen];
        buffer.get(instanceIdBytes);
        String instanceId = new String(instanceIdBytes, StandardCharsets.UTF_8);

        int payloadLen = message.length - 4 - instanceIdLen;
        byte[] payload = new byte[payloadLen];
        buffer.get(payload);

        return new DecodedMessage(instanceId, payload);
    }

    /**
     * A decoded message containing instance ID and payload.
     *
     * @param instanceId the originating instance ID
     * @param payload the message payload
     */
    public record DecodedMessage(String instanceId, byte[] payload) {

        /**
         * Checks if this message originated from the given instance.
         *
         * @param myInstanceId the instance ID to check against
         * @return true if the message is from the specified instance
         */
        public boolean isFrom(String myInstanceId) {
            return instanceId.equals(myInstanceId);
        }
    }
}
