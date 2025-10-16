package net.carcdr.yhocuspocus.protocol;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for variable-length integer encoding and decoding.
 */
public class VarIntEncodingTest {

    @Test
    public void testEncodeDecodeZero() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(0);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        int decoded = reader.readVarInt();

        assertEquals("Zero should encode/decode correctly", 0, decoded);
        assertEquals("Zero should encode to 1 byte", 1, encoded.length);
    }

    @Test
    public void testEncodeDecodeSmallIntegers() {
        int[] testValues = {1, 42, 127};

        for (int value : testValues) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(value);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            int decoded = reader.readVarInt();

            assertEquals("Value " + value + " should encode/decode correctly",
                        value, decoded);
            assertEquals("Small values should encode to 1 byte", 1, encoded.length);
        }
    }

    @Test
    public void testEncodeDecodeMediumIntegers() {
        int[] testValues = {128, 255, 16383};

        for (int value : testValues) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(value);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            int decoded = reader.readVarInt();

            assertEquals("Value " + value + " should encode/decode correctly",
                        value, decoded);
        }
    }

    @Test
    public void testEncodeDecodeLargeIntegers() {
        int[] testValues = {16384, 1000000, Integer.MAX_VALUE};

        for (int value : testValues) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(value);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            int decoded = reader.readVarInt();

            assertEquals("Value " + value + " should encode/decode correctly",
                        value, decoded);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeNegativeThrows() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(-1);
    }

    @Test
    public void testEncodeDecodeString() {
        String testString = "Hello, World!";

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(testString);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        String decoded = reader.readVarString();

        assertEquals("String should encode/decode correctly", testString, decoded);
    }

    @Test
    public void testEncodeDecodeEmptyString() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString("");
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        String decoded = reader.readVarString();

        assertEquals("Empty string should encode/decode correctly", "", decoded);
    }

    @Test
    public void testEncodeDecodeUnicodeString() {
        String testString = "Hello 世界 🌍";

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(testString);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        String decoded = reader.readVarString();

        assertEquals("Unicode string should encode/decode correctly", testString, decoded);
    }

    @Test
    public void testEncodeDecodeBytes() {
        byte[] testBytes = {1, 2, 3, 4, 5};

        VarIntWriter writer = new VarIntWriter();
        writer.writeBytes(testBytes);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        byte[] decoded = reader.readBytes(5);

        assertArrayEquals("Bytes should encode/decode correctly", testBytes, decoded);
    }

    @Test
    public void testRemaining() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(42);
        writer.writeBytes(new byte[]{1, 2, 3});
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        reader.readVarInt(); // Skip the integer

        byte[] remaining = reader.remaining();
        assertArrayEquals("Remaining bytes should be correct",
                         new byte[]{1, 2, 3}, remaining);
    }

    @Test
    public void testMultipleValues() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(42);
        writer.writeVarString("test");
        writer.writeVarInt(12345);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        assertEquals("First int should be correct", 42, reader.readVarInt());
        assertEquals("String should be correct", "test", reader.readVarString());
        assertEquals("Second int should be correct", 12345, reader.readVarInt());
    }

    @Test
    public void testReaderPosition() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(42);
        writer.writeVarString("test");
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        assertEquals("Initial position should be 0", 0, reader.getPosition());

        reader.readVarInt();
        assertTrue("Position should advance", reader.getPosition() > 0);

        reader.readVarString();
        assertEquals("Position should be at end", encoded.length, reader.getPosition());
        assertFalse("Should have no remaining bytes", reader.hasRemaining());
    }

    @Test
    public void testReset() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(42);
        writer.reset();
        assertEquals("Writer should be empty after reset", 0, writer.size());

        byte[] data = {1, 2, 3};
        VarIntReader reader = new VarIntReader(data);
        reader.readBytes(2);
        reader.reset();
        assertEquals("Reader position should be 0 after reset", 0, reader.getPosition());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadPastEnd() {
        VarIntReader reader = new VarIntReader(new byte[]{1});
        reader.readBytes(5); // Try to read more than available
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteNullString() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteNullBytes() {
        VarIntWriter writer = new VarIntWriter();
        writer.writeBytes(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadNullData() {
        new VarIntReader(null);
    }

    @Test
    public void testLargeString() {
        // Create a large string (1000 characters)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("a");
        }
        String largeString = sb.toString();

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(largeString);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        String decoded = reader.readVarString();

        assertEquals("Large string should encode/decode correctly",
                    largeString, decoded);
    }
}
