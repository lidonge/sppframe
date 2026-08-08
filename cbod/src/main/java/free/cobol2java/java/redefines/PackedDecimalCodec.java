package free.cobol2java.java.redefines;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

final class PackedDecimalCodec {
    private PackedDecimalCodec() {
    }

    static BigDecimal decode(byte[] storage, int start, int length, int scale) {
        StringBuilder digits = new StringBuilder(length * 2 - 1);
        boolean negative = false;
        for (int i = start; i < start + length; i++) {
            int value = storage[i] & 0xFF;
            int high = value >>> 4 & 0x0F;
            int low = value & 0x0F;
            appendDigit(digits, high);
            if (i == start + length - 1) {
                if (low != 0x0A && low != 0x0B && low != 0x0C
                        && low != 0x0D && low != 0x0E && low != 0x0F) {
                    throw new NumberFormatException("Invalid packed-decimal sign nibble: " + low);
                }
                negative = low == 0x0D || low == 0x0B;
            } else {
                appendDigit(digits, low);
            }
        }
        int firstDigit = 0;
        while (firstDigit < digits.length() - 1 && digits.charAt(firstDigit) == '0') {
            firstDigit++;
        }
        String number = digits.length() == 0 ? "0" : digits.substring(firstDigit);
        return new BigDecimal((negative ? "-" : "") + number).movePointLeft(scale);
    }

    static void encode(byte[] storage, int start, int length, int scale, BigDecimal value) {
        BigDecimal scaled = value.setScale(scale);
        BigInteger unscaled = scaled.unscaledValue().abs();
        String digits = unscaled.toString();
        int precision = length * 2 - 1;
        if (digits.length() > precision) {
            throw new ArithmeticException("Packed decimal value " + value.toPlainString()
                    + " exceeds " + precision + " digits");
        }
        if (digits.length() < precision) {
            digits = "0".repeat(precision - digits.length()) + digits;
        }
        byte[] encoded = new byte[length];
        int digitIndex = 0;
        for (int i = 0; i < length; i++) {
            int high = digits.charAt(digitIndex++) - '0';
            int low = i == length - 1
                    ? (scaled.signum() < 0 ? 0x0D : 0x0C)
                    : digits.charAt(digitIndex++) - '0';
            encoded[i] = (byte) (high << 4 | low);
        }
        Arrays.fill(storage, start, start + length, (byte) ' ');
        System.arraycopy(encoded, 0, storage, start, length);
    }

    private static void appendDigit(StringBuilder digits, int value) {
        if (value >= 0 && value <= 9) {
            digits.append((char) ('0' + value));
            return;
        }
        throw new NumberFormatException("Invalid packed-decimal digit nibble: " + value);
    }
}
