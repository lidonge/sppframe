package free.cobol2java.java;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Reversible COBOL single-byte data encodings, separate from Java source-file encodings.
 * The control mapping is the IBM/Unicode round-trip mapping: 15=NEL, 25=LF.
 * See Unicode ICU ibm-37_P100-1995, ibm-1047_P100-1995 and ibm-1140_P100-1997.
 * JDK IBM037/1140 collapse both bytes to LF, and JDK IBM1047 swaps LF/NEL.
 * Those Java newline policies are not the IBM round-trip data mapping.
 */
public enum CobolDataCodec {
    IBM_037(37, "IBM037"),
    IBM_1047(1047, "IBM1047"),
    IBM_1140(1140, "IBM01140");

    private final int codepage;
    private final char[] characters = new char[256];
    private final short[] bytesByCharacter = new short[Character.MAX_VALUE + 1];

    CobolDataCodec(int codepage, String charsetName) {
        this.codepage = codepage;
        Arrays.fill(bytesByCharacter, (short) -1);
        Charset charset = Charset.forName(charsetName);
        for (int value = 0; value < characters.length; value++) {
            char character;
            if (value == 0x15) character = 0x85;
            else if (value == 0x25) character = 0x0a;
            else {
                String decoded = new String(new byte[]{(byte) value}, charset);
                if (decoded.length() != 1) throw new IllegalStateException("Non-single-byte codepage " + codepage);
                character = decoded.charAt(0);
            }
            if (bytesByCharacter[character] != -1) {
                throw new IllegalStateException("Non-injective data mapping for CODEPAGE " + codepage);
            }
            characters[value] = character;
            bytesByCharacter[character] = (short) value;
        }
    }

    public int codepage() { return codepage; }

    public static CobolDataCodec forCodepage(int codepage) {
        return switch (codepage) {
            case 37 -> IBM_037;
            case 1047 -> IBM_1047;
            case 1140 -> IBM_1140;
            default -> throw new IllegalArgumentException("Unsupported data CODEPAGE " + codepage);
        };
    }

    public String decodeByte(int unsignedByte) {
        return String.valueOf(characters[java.util.Objects.checkIndex(unsignedByte, 256)]);
    }

    public int encodeCharacter(char character) {
        int value = bytesByCharacter[character];
        if (value < 0) {
            throw new IllegalArgumentException("Character U+" + Integer.toHexString(character)
                    + " has no round-trip mapping in CODEPAGE " + codepage);
        }
        return value;
    }

    public String decode(byte[] encoded) {
        char[] result = new char[encoded.length];
        for (int index = 0; index < encoded.length; index++) result[index] = characters[encoded[index] & 255];
        return new String(result);
    }

    public byte[] encode(String value) {
        byte[] result = new byte[value.length()];
        for (int index = 0; index < value.length(); index++) result[index] = (byte) encodeCharacter(value.charAt(index));
        return result;
    }
}
