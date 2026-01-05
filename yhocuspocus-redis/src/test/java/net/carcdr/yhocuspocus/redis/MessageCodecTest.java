package net.carcdr.yhocuspocus.redis;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for MessageCodec.
 */
public class MessageCodecTest {

    @Test
    public void testEncodeAndDecode() {
        String instanceId = "instance-123";
        byte[] payload = "test-payload".getBytes(StandardCharsets.UTF_8);

        byte[] encoded = MessageCodec.encode(instanceId, payload);
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(encoded);

        assertNotNull("Decoded message should not be null", decoded);
        assertEquals("Instance ID should match", instanceId, decoded.instanceId());
        assertArrayEquals("Payload should match", payload, decoded.payload());
    }

    @Test
    public void testEncodeEmptyPayload() {
        String instanceId = "instance-456";
        byte[] payload = new byte[0];

        byte[] encoded = MessageCodec.encode(instanceId, payload);
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(encoded);

        assertNotNull("Decoded message should not be null", decoded);
        assertEquals("Instance ID should match", instanceId, decoded.instanceId());
        assertEquals("Empty payload should have zero length", 0, decoded.payload().length);
    }

    @Test
    public void testEncodeBinaryPayload() {
        String instanceId = "binary-instance";
        byte[] payload = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};

        byte[] encoded = MessageCodec.encode(instanceId, payload);
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(encoded);

        assertNotNull("Decoded message should not be null", decoded);
        assertArrayEquals("Binary payload should match", payload, decoded.payload());
    }

    @Test
    public void testDecodeNullMessage() {
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(null);
        assertNull("Null message should return null", decoded);
    }

    @Test
    public void testDecodeTooShortMessage() {
        // Less than 4 bytes (minimum for length prefix)
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(new byte[]{0x01, 0x02});
        assertNull("Too short message should return null", decoded);
    }

    @Test
    public void testDecodeInvalidLength() {
        // Length prefix claims 1000 bytes but message is only 8 bytes
        byte[] message = new byte[]{0x00, 0x00, 0x03, (byte) 0xE8, 0x01, 0x02, 0x03, 0x04};
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(message);
        assertNull("Invalid length should return null", decoded);
    }

    @Test
    public void testIsFromSameInstance() {
        String myInstanceId = "my-instance";
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);

        byte[] encoded = MessageCodec.encode(myInstanceId, payload);
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(encoded);

        assertNotNull(decoded);
        assertTrue("Should be from same instance", decoded.isFrom(myInstanceId));
        assertFalse("Should not be from different instance", decoded.isFrom("other-instance"));
    }

    @Test
    public void testUnicodeInstanceId() {
        String instanceId = "instance-\u4e2d\u6587-test";
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

        byte[] encoded = MessageCodec.encode(instanceId, payload);
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(encoded);

        assertNotNull(decoded);
        assertEquals("Unicode instance ID should match", instanceId, decoded.instanceId());
    }

    @Test
    public void testLargePayload() {
        String instanceId = "large-payload-instance";
        byte[] payload = new byte[10000];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i % 256);
        }

        byte[] encoded = MessageCodec.encode(instanceId, payload);
        MessageCodec.DecodedMessage decoded = MessageCodec.decode(encoded);

        assertNotNull(decoded);
        assertArrayEquals("Large payload should match", payload, decoded.payload());
    }
}
