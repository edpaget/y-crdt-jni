package net.carcdr.yhocuspocus.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Variable-length integer writer compatible with lib0 encoding.
 *
 * <p>This implements the variable-length integer encoding used by lib0
 * and the Yjs ecosystem. Integers are encoded using 7 bits per byte,
 * with the 8th bit indicating continuation.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VarIntWriter writer = new VarIntWriter();
 * writer.writeVarInt(42);
 * writer.writeVarString("hello");
 * byte[] encoded = writer.toByteArray();
 * }</pre>
 */
public class VarIntWriter {

    private final ByteArrayOutputStream buffer;

    /**
     * Creates a new variable-length integer writer.
     */
    public VarIntWriter() {
        this.buffer = new ByteArrayOutputStream();
    }

    /**
     * Writes a variable-length unsigned integer.
     *
     * <p>Encodes integers using 7 bits per byte with continuation bit.
     * Compatible with lib0's writeVarUint format.</p>
     *
     * @param value the value to write (must be non-negative)
     * @throws IllegalArgumentException if value is negative
     */
    public void writeVarInt(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("VarInt values must be non-negative, got: " + value);
        }

        while (value >= 0x80) {
            buffer.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer.write(value);
    }

    /**
     * Writes a variable-length string.
     *
     * <p>Format: [length as varInt][UTF-8 bytes]</p>
     *
     * @param str the string to write
     * @throws IllegalArgumentException if str is null
     */
    public void writeVarString(String str) {
        if (str == null) {
            throw new IllegalArgumentException("String cannot be null");
        }

        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        buffer.write(bytes, 0, bytes.length);
    }

    /**
     * Writes raw bytes without any encoding.
     *
     * @param data the bytes to write
     * @throws IllegalArgumentException if data is null
     */
    public void writeBytes(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        buffer.write(data, 0, data.length);
    }

    /**
     * Returns the encoded bytes.
     *
     * @return byte array containing all written data
     */
    public byte[] toByteArray() {
        return buffer.toByteArray();
    }

    /**
     * Returns the current size in bytes.
     *
     * @return number of bytes written
     */
    public int size() {
        return buffer.size();
    }

    /**
     * Resets the writer, clearing all written data.
     */
    public void reset() {
        buffer.reset();
    }
}
