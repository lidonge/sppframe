package free.cobol2java.java.redefines;

import java.math.BigDecimal;

public class BigDecimalCobolRedefines extends AbstractCobolRedefines<BigDecimal> {
    private final int scale;

    public BigDecimalCobolRedefines(int length) {
        super(length);
        this.scale = 0;
    }

    public BigDecimalCobolRedefines(byte[] storage) {
        super(storage);
        this.scale = 0;
    }

    public BigDecimalCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
        this.scale = 0;
    }

    public BigDecimalCobolRedefines(byte[] storage, int start, int length, int scale) {
        super(storage, start, length);
        this.scale = scale;
    }

    public BigDecimalCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
        this.scale = 0;
    }

    public BigDecimalCobolRedefines(CobolRedefinesBuffer storage, int start, int length, int scale) {
        super(storage, start, length);
        this.scale = scale;
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
        return PackedDecimalCodec.decode(storage.bytes(), start, length, scale);
    }

    @Override
    public void set(BigDecimal value) {
        writePackedDecimal(value == null ? BigDecimal.ZERO : value);
    }

    private void writePackedDecimal(BigDecimal value) {
        PackedDecimalCodec.encode(storage.bytes(), start, length, scale, value);
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
            String text = actual.toString().trim();
            set(text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text));
        }
    }

}
