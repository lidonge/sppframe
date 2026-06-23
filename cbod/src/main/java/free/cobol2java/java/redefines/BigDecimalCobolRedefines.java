package free.cobol2java.java.redefines;

import java.math.BigDecimal;
import java.math.BigInteger;

import free.cobol2java.java.CobolNumeric;

public class BigDecimalCobolRedefines extends AbstractCobolRedefines<BigDecimal> {

    public BigDecimalCobolRedefines(int length) {
        super(length);
    }

    public BigDecimalCobolRedefines(byte[] storage) {
        super(storage);
    }

    public BigDecimalCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    public BigDecimalCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public BigDecimal get() {
        if (looksLikeDisplayNumeric()) {
            return readDisplayNumeric();
        }
        return readPackedDecimal();
    }

    private boolean looksLikeDisplayNumeric() {
        byte[] bytes = storage.bytes();
        for (int i = start; i < start + length; i++) {
            int value = bytes[i] & 0xFF;
            if (value == 0 || value == ' ') {
                continue;
            }
            if (value >= '0' && value <= '9') {
                continue;
            }
            if (value == '+' || value == '-' || value == '.') {
                continue;
            }
            return false;
        }
        return true;
    }

    private BigDecimal readDisplayNumeric() {
        String value = readTrimmedString();
        return value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    private BigDecimal readPackedDecimal() {
        byte[] bytes = storage.bytes();
        StringBuilder digits = new StringBuilder(length * 2 - 1);
        for (int i = start; i < start + length; i++) {
            int value = bytes[i] & 0xFF;
            int high = (value >>> 4) & 0x0F;
            int low = value & 0x0F;
            if (i == start + length - 1) {
                appendPackedDigit(digits, high);
                boolean negative = low == 0x0D || low == 0x0B;
                String number = normalizeDigits(digits);
                return new BigDecimal((negative ? "-" : "") + number);
            }
            appendPackedDigit(digits, high);
            appendPackedDigit(digits, low);
        }
        return BigDecimal.ZERO;
    }

    private void appendPackedDigit(StringBuilder digits, int value) {
        if (value >= 0 && value <= 9) {
            digits.append((char) ('0' + value));
        }
    }

    private String normalizeDigits(StringBuilder digits) {
        int index = 0;
        while (index < digits.length() - 1 && digits.charAt(index) == '0') {
            index++;
        }
        if (digits.length() == 0) {
            return "0";
        }
        return digits.substring(index);
    }

    @Override
    public void set(BigDecimal value) {
        writePackedDecimal(value == null ? BigDecimal.ZERO : value);
    }

    private void writePackedDecimal(BigDecimal value) {
        BigInteger unscaled = value.unscaledValue().abs();
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
            int high = digitIndex < digits.length() ? digits.charAt(digitIndex++) - '0' : 0;
            int low;
            if (i == length - 1) {
                low = value.signum() < 0 ? 0x0D : 0x0C;
            } else {
                low = digitIndex < digits.length() ? digits.charAt(digitIndex++) - '0' : 0;
            }
            encoded[i] = (byte) ((high << 4) | low);
        }
        writeBytes(encoded);
    }

    @Override
    public void set(ICobolRedefines<?> value) {
        Object actual = value == null ? null : value.get();
        if (actual instanceof BigDecimal decimal) {
            set(decimal);
        } else if (actual instanceof Number number) {
            set(BigDecimal.valueOf(number.longValue()));
        } else if (actual == null) {
            set(BigDecimal.ZERO);
        } else {
            set(CobolNumeric.bigDecimalValue(actual));
        }
    }

}
