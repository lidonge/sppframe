package free.cobol2java.java;

import java.nio.ByteBuffer;

/** Binary64 representation at the converted Java boundary; this does not infer a native C ABI. */
public final class FloatingScalarCodec {
    private FloatingScalarCodec() { }

    /** Preserve signed zero and NaN payloads, without decimal text formatting. */
    public static String toState(double value) {
        return CobolUtf8Codec.decodeState(ByteBuffer.allocate(Double.BYTES)
                .putLong(Double.doubleToRawLongBits(value)).array());
    }

    public static double fromState(String state) {
        byte[] bytes = CobolUtf8Codec.encodeState(state);
        if (bytes.length != Double.BYTES)
            throw new IllegalArgumentException("Floating Codec requires eight bytes");
        return Double.longBitsToDouble(ByteBuffer.wrap(bytes).getLong());
    }
}
