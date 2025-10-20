package net.carcdr.yhocuspocus.protocol;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Compatibility tests with the lib0 JavaScript library encoding.
 *
 * <p>These tests verify that our Java VarIntWriter and VarIntReader produce
 * the same encoded bytes and decode values identically to the lib0 JavaScript
 * library used by Yjs.</p>
 *
 * <p>Test cases are based on:
 * https://github.com/dmonad/lib0/blob/main/encoding.test.js</p>
 *
 * @see <a href="https://github.com/dmonad/lib0">lib0 GitHub</a>
 */
public class Lib0EncodingCompatibilityTest {

    /**
     * Tests variable-length unsigned integer encoding.
     *
     * <p>Corresponds to testVarUintEncoding in lib0.</p>
     *
     * <p>Values tested: 42, 513, 131075, 33619971, 2839012934, 9007199254740991</p>
     */
    @Test
    public void testVarUintEncoding() {
        // Test values from lib0 JavaScript tests
        long[] testValues = {
            42L,
            513L,
            131075L,
            33619971L,
            2839012934L,
            9007199254740991L  // Max safe JavaScript integer (2^53 - 1)
        };

        for (long value : testValues) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(value);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            long decoded = reader.readVarInt();

            assertEquals("Value " + value + " should encode/decode correctly",
                        value, decoded);
        }
    }

    /**
     * Tests encoding of maximum 32-bit unsigned integer.
     *
     * <p>Corresponds to testEncodeMax32bitUint in lib0.</p>
     */
    @Test
    public void testEncodeMax32bitUint() {
        long maxUint32 = 4294967295L; // 2^32 - 1

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(maxUint32);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        long decoded = reader.readVarInt();

        assertEquals("Max 32-bit uint should encode/decode correctly",
                    maxUint32, decoded);
    }

    /**
     * Tests string encoding with various Unicode characters.
     *
     * <p>Corresponds to testStringEncoding in lib0.</p>
     *
     * <p>Tests ASCII, empty strings, and multi-byte UTF-16 characters including:</p>
     * <ul>
     *   <li>ASCII: "hello", "test!", "1234"</li>
     *   <li>Empty string: ""</li>
     *   <li>Unicode: "☺☺☺" (smiling faces)</li>
     *   <li>Korean: "쾟"</li>
     *   <li>Chinese: "龟" (turtle)</li>
     *   <li>Emoji: "😝" (face with tongue)</li>
     * </ul>
     */
    @Test
    public void testStringEncoding() {
        String[] testStrings = {
            "hello",
            "test!",
            "☺☺☺",
            "",
            "1234",
            "쾟",      // Korean character
            "龟",      // Chinese character (turtle)
            "😝"       // Emoji (face with stuck-out tongue)
        };

        for (String testString : testStrings) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarString(testString);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            String decoded = reader.readVarString();

            assertEquals("String '" + testString + "' should encode/decode correctly",
                        testString, decoded);
        }
    }

    /**
     * Tests decoding of long strings (overflow test).
     *
     * <p>Corresponds to testOverflowStringDecoding in lib0.</p>
     *
     * <p>Creates a string exceeding 11,000 characters to verify buffer handling.</p>
     */
    @Test
    public void testOverflowStringDecoding() {
        // Generate a large random string (similar to lib0 test)
        Random random = new Random(42); // Seed for reproducibility
        StringBuilder sb = new StringBuilder();

        // Generate ~11,000 characters
        for (int i = 0; i < 11000; i++) {
            // Random printable ASCII characters
            char c = (char) ('a' + random.nextInt(26));
            sb.append(c);
        }

        String largeString = sb.toString();

        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString(largeString);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        String decoded = reader.readVarString();

        assertEquals("Large string (11000+ chars) should encode/decode correctly",
                    largeString, decoded);
        assertEquals("Length should match", largeString.length(), decoded.length());
    }

    /**
     * Tests compatibility with Golang binary encoding.
     *
     * <p>Corresponds to testGolangBinaryEncodingCompatibility in lib0.</p>
     *
     * <p>Verifies that specific integer values produce expected byte sequences
     * that are compatible with Go's variable-length encoding.</p>
     */
    @Test
    public void testGolangBinaryEncodingCompatibility() {
        // Test cases: [value, expected byte sequence]
        Object[][] testCases = {
            {0L, new byte[]{0}},
            {1L, new byte[]{1}},
            {127L, new byte[]{127}},
            {128L, new byte[]{(byte) 128, 1}},
            {129L, new byte[]{(byte) 129, 1}},
            {256L, new byte[]{(byte) 128, 2}},
            {16383L, new byte[]{(byte) 255, 127}},
            {16384L, new byte[]{(byte) 128, (byte) 128, 1}},
            {16385L, new byte[]{(byte) 129, (byte) 128, 1}},
            {2097151L, new byte[]{(byte) 255, (byte) 255, 127}},
            {2097152L, new byte[]{(byte) 128, (byte) 128, (byte) 128, 1}},
            {268435455L, new byte[]{(byte) 255, (byte) 255, (byte) 255, 127}},
            {268435456L, new byte[]{(byte) 128, (byte) 128, (byte) 128, (byte) 128, 1}},
            {4294967295L, new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255, 15}},
        };

        for (Object[] testCase : testCases) {
            long value = (long) testCase[0];
            byte[] expected = (byte[]) testCase[1];

            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(value);
            byte[] encoded = writer.toByteArray();

            assertArrayEquals("Value " + value + " should produce expected byte sequence",
                            expected, encoded);

            // Also verify decoding
            VarIntReader reader = new VarIntReader(encoded);
            long decoded = reader.readVarInt();
            assertEquals("Value " + value + " should decode correctly",
                        value, decoded);
        }
    }

    /**
     * Tests encoding/decoding of various multi-byte Unicode sequences.
     *
     * <p>Ensures proper handling of characters requiring 2, 3, or 4 bytes in UTF-8.</p>
     */
    @Test
    public void testMultiByteUnicodeCharacters() {
        String[] unicodeStrings = {
            "Ā",           // 2-byte UTF-8 (Latin Extended)
            "€",           // 3-byte UTF-8 (Euro sign)
            "𝕳",           // 4-byte UTF-8 (Mathematical alphanumeric)
            "🚀",          // 4-byte UTF-8 (Rocket emoji)
            "Hello 世界",  // Mixed ASCII and Chinese
            "Привет мир",  // Cyrillic
            "مرحبا",       // Arabic
            "שלום",        // Hebrew
        };

        for (String str : unicodeStrings) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarString(str);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            String decoded = reader.readVarString();

            assertEquals("Unicode string '" + str + "' should encode/decode correctly",
                        str, decoded);
        }
    }

    /**
     * Tests roundtrip encoding/decoding of various integer ranges.
     *
     * <p>Verifies correct behavior for 1-byte, 2-byte, 3-byte, 4-byte, and 5-byte encodings.</p>
     */
    @Test
    public void testIntegerRanges() {
        long[][] ranges = {
            // 1-byte encoding (0-127)
            {0, 127},
            // 2-byte encoding (128-16383)
            {128, 16383},
            // 3-byte encoding (16384-2097151)
            {16384, 2097151},
            // 4-byte encoding (2097152-268435455)
            {2097152, 268435455},
            // 5-byte encoding (268435456-...)
            {268435456, 4294967295L},
        };

        for (long[] range : ranges) {
            long min = range[0];
            long max = range[1];

            // Test boundary values
            testValueRoundtrip(min);
            testValueRoundtrip(max);

            // Test middle value
            long mid = (min + max) / 2;
            testValueRoundtrip(mid);
        }
    }

    /**
     * Helper method to test value roundtrip.
     */
    private void testValueRoundtrip(long value) {
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarInt(value);
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        long decoded = reader.readVarInt();

        assertEquals("Value " + value + " should roundtrip correctly", value, decoded);
    }

    /**
     * Tests sequential encoding of multiple values.
     *
     * <p>Verifies that multiple values can be encoded in sequence and
     * decoded in the same order.</p>
     */
    @Test
    public void testSequentialEncoding() {
        VarIntWriter writer = new VarIntWriter();

        // Encode multiple values
        writer.writeVarInt(42);
        writer.writeVarString("hello");
        writer.writeVarInt(513);
        writer.writeVarString("世界");
        writer.writeVarInt(131075);

        byte[] encoded = writer.toByteArray();

        // Decode in same order
        VarIntReader reader = new VarIntReader(encoded);
        assertEquals(42, reader.readVarInt());
        assertEquals("hello", reader.readVarString());
        assertEquals(513, reader.readVarInt());
        assertEquals("世界", reader.readVarString());
        assertEquals(131075, reader.readVarInt());
    }

    /**
     * Tests byte array encoding/decoding.
     *
     * <p>Verifies that raw byte arrays are correctly encoded with length prefix.</p>
     */
    @Test
    public void testByteArrayEncoding() {
        byte[][] testArrays = {
            new byte[]{},                    // Empty
            new byte[]{0},                   // Single zero
            new byte[]{1, 2, 3, 4, 5},      // Small array
            new byte[255],                   // Medium array (all zeros)
            createLargeByteArray(10000),     // Large array
        };

        for (byte[] testArray : testArrays) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarInt(testArray.length);
            writer.writeBytes(testArray);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            long length = reader.readVarInt();
            byte[] decoded = reader.readBytes((int) length);

            assertArrayEquals("Byte array of length " + testArray.length +
                            " should encode/decode correctly",
                            testArray, decoded);
        }
    }

    /**
     * Creates a large byte array for testing.
     */
    private byte[] createLargeByteArray(int size) {
        byte[] array = new byte[size];
        Random random = new Random(42);
        random.nextBytes(array);
        return array;
    }

    /**
     * Tests empty data edge cases.
     */
    @Test
    public void testEmptyData() {
        // Empty string
        VarIntWriter writer = new VarIntWriter();
        writer.writeVarString("");
        byte[] encoded = writer.toByteArray();

        VarIntReader reader = new VarIntReader(encoded);
        assertEquals("", reader.readVarString());

        // Empty byte array
        writer = new VarIntWriter();
        writer.writeVarInt(0);
        writer.writeBytes(new byte[]{});
        encoded = writer.toByteArray();

        reader = new VarIntReader(encoded);
        assertEquals(0, reader.readVarInt());
        byte[] emptyBytes = reader.readBytes(0);
        assertEquals(0, emptyBytes.length);
    }

    /**
     * Tests specific Unicode edge cases that are problematic in some implementations.
     */
    @Test
    public void testUnicodeEdgeCases() {
        String[] edgeCases = {
            "\u0000",                    // Null character
            "\u0001\u0002\u0003",       // Control characters
            "A\uD800\uDC00B",           // Surrogate pair (represents 𐀀)
            "\uFFFD",                    // Replacement character
            "Test\nNew\tLine",          // Newline and tab
            "\r\n",                      // Windows line ending
        };

        for (String str : edgeCases) {
            VarIntWriter writer = new VarIntWriter();
            writer.writeVarString(str);
            byte[] encoded = writer.toByteArray();

            VarIntReader reader = new VarIntReader(encoded);
            String decoded = reader.readVarString();

            assertEquals("Unicode edge case should encode/decode correctly",
                        str, decoded);
        }
    }
}
