package net.carcdr.yhocuspocus.protocol;

import java.nio.charset.StandardCharsets;

/**
 * Variable-length integer reader compatible with lib0 encoding.
 *
 * <p>This implements the variable-length integer decoding used by lib0
 * and the Yjs ecosystem. Integers are decoded from 7-bit chunks with
 * continuation bits.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VarIntReader reader = new VarIntReader(encodedData);
 * int value = reader.readVarInt();
 * String text = reader.readVarString();
 * byte[] remaining = reader.remaining();
 * }</pre>
 */
public class VarIntReader {

    private final byte[] data;
    private int position;

    /**
     * Creates a new variable-length integer reader.
     *
     * @param data the encoded data to read from
     * @throws IllegalArgumentException if data is null
     */
    public VarIntReader(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        this.data = data;
        this.position = 0;
    }

    /**
     * Reads a variable-length unsigned integer.
     *
     * <p>Decodes integers using 7 bits per byte with continuation bit.
     * Compatible with lib0's readVarUint format. Returns a 64-bit long
     * to support the full range of JavaScript safe integers (up to 2^53-1).</p>
     *
     * @return the decoded long value
     * @throws IndexOutOfBoundsException if not enough data to read
     */
    public long readVarInt() {
        long value = 0;
        int shift = 0;

        while (position < data.length) {
            byte b = data[position++];
            value |= (long) (b & 0x7F) << shift;

            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;

            if (shift >= 64) {
                throw new IllegalStateException("VarInt overflow: too many continuation bits");
            }
        }

        return value;
    }

    /**
     * Reads a variable-length string.
     *
     * <p>Format: [length as varInt][UTF-8 bytes]</p>
     *
     * @return the decoded string
     * @throws IndexOutOfBoundsException if not enough data to read
     */
    public String readVarString() {
        long length = readVarInt();

        if (position + length > data.length) {
            throw new IndexOutOfBoundsException(
                "Not enough data for string: need " + length + " bytes, have " +
                (data.length - position)
            );
        }

        String str = new String(data, position, (int) length, StandardCharsets.UTF_8);
        position += (int) length;
        return str;
    }

    /**
     * Reads a specific number of bytes.
     *
     * @param count the number of bytes to read
     * @return byte array containing the read bytes
     * @throws IndexOutOfBoundsException if not enough data to read
     * @throws IllegalArgumentException if count is negative
     */
    public byte[] readBytes(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative, got: " + count);
        }

        if (position + count > data.length) {
            throw new IndexOutOfBoundsException(
                "Not enough data: need " + count + " bytes, have " + (data.length - position)
            );
        }

        byte[] result = new byte[count];
        System.arraycopy(data, position, result, 0, count);
        position += count;
        return result;
    }

    /**
     * Reads a variable-length byte array.
     *
     * <p>Format: [length as varInt][bytes]
     * Compatible with lib0's writeVarUint8Array.</p>
     *
     * @return the decoded byte array
     * @throws IndexOutOfBoundsException if not enough data to read
     */
    public byte[] readVarBytes() {
        long length = readVarInt();

        if (position + length > data.length) {
            throw new IndexOutOfBoundsException(
                "Not enough data for byte array: need " + length + " bytes, have " +
                (data.length - position)
            );
        }

        byte[] result = new byte[(int) length];
        System.arraycopy(data, position, result, 0, (int) length);
        position += (int) length;
        return result;
    }

    /**
     * Returns all remaining bytes from the current position.
     *
     * @return byte array containing remaining data (may be empty)
     */
    public byte[] remaining() {
        byte[] result = new byte[data.length - position];
        System.arraycopy(data, position, result, 0, result.length);
        position = data.length;
        return result;
    }

    /**
     * Gets the current read position.
     *
     * @return current position in bytes
     */
    public int getPosition() {
        return position;
    }

    /**
     * Gets the number of bytes remaining to read.
     *
     * @return number of unread bytes
     */
    public int remainingBytes() {
        return data.length - position;
    }

    /**
     * Checks if there are more bytes to read.
     *
     * @return true if more data available, false otherwise
     */
    public boolean hasRemaining() {
        return position < data.length;
    }

    /**
     * Resets the reader to the beginning.
     */
    public void reset() {
        position = 0;
    }
}
