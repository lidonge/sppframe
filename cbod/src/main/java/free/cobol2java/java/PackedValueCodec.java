package free.cobol2java.java;

import free.cobol2java.java.redefines.PackedDecimalCodec;
import java.math.BigDecimal;

/** Packed value boundary; the sign nibble is encoding metadata, including negative zero. */
public final class PackedValueCodec {
    public static byte[] encode(BigDecimal value, int width, int precision, int scale, int sign) {
        if (width < 1 || precision != width * 2 - 1 || scale < 0 || scale > precision)
            throw new IllegalArgumentException("Unsupported packed value extent");
        BigDecimal decimal = value.setScale(scale);
        if (decimal.unscaledValue().abs().toString().length() > precision)
            throw new IllegalArgumentException("Packed value outside source PICTURE precision");
        boolean negative = sign == 0x0b || sign == 0x0d;
        boolean positive = sign == 0x0a || sign == 0x0c || sign == 0x0e || sign == 0x0f;
        if (!negative && !positive) throw new IllegalArgumentException("Invalid packed sign metadata");
        if (decimal.signum() < 0 && !negative) sign = 0x0d;
        else if (decimal.signum() > 0 && negative) sign = 0x0c;
        byte[] bytes = new byte[width];
        PackedDecimalCodec.encode(bytes, 0, width, scale, decimal);
        bytes[width - 1] = (byte) ((bytes[width - 1] & 0xf0) | sign);
        return bytes;
    }

    public static BigDecimal decode(byte[] bytes, int precision, int scale) {
        if (bytes.length < 1 || precision != bytes.length * 2 - 1)
            throw new IllegalArgumentException("Unsupported packed value extent");
        return PackedDecimalCodec.decode(bytes, 0, bytes.length, scale);
    }

    private PackedValueCodec() { }
}
