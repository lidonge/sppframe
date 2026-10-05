package free.cobol2java.java;

import java.util.Arrays;

/** Boundary conversion for explicitly encoded ordinary groups. */
public final class CobolGroupEncoding {
    private CobolGroupEncoding() { }

    public static String characters(CobolEncodedGroup group) {
        return CobolUtf8Codec.decodeState(group.$cobolBytes());
    }

    public static void receiveCharacters(String source, CobolEncodedGroup target) {
        int width = target.$cobolBytes().length;
        byte[] incoming = CobolUtf8Codec.encodeState(source == null ? "" : source);
        byte[] bytes = new byte[width];
        Arrays.fill(bytes, (byte) 32);
        System.arraycopy(incoming, 0, bytes, 0, Math.min(incoming.length, width));
        target.$cobolBytes(bytes);
    }
}
