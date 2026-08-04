package free.cobol2java.java.redefines;

import java.math.BigDecimal;

/**
 * {@link Long} 类型的 COBOL REDEFINES 视图实现。
 */
public class LongCobolRedefines extends AbstractCobolRedefines<Long> {
    private final boolean packedDecimal;

    public LongCobolRedefines(int totalSize) {
        super(totalSize);
        this.packedDecimal = false;
    }

    public LongCobolRedefines(byte[] storage) {
        super(storage);
        this.packedDecimal = false;
    }

    public LongCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
        this.packedDecimal = false;
    }

    public LongCobolRedefines(byte[] storage, int start, int length, boolean packedDecimal) {
        super(storage, start, length);
        this.packedDecimal = packedDecimal;
    }

    public LongCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
        this.packedDecimal = false;
    }

    public LongCobolRedefines(CobolRedefinesBuffer storage, int start, int length, boolean packedDecimal) {
        super(storage, start, length);
        this.packedDecimal = packedDecimal;
    }

    @Override
    public Long get() {
        if (packedDecimal) {
            return PackedDecimalCodec.decode(storage.bytes(), start, length, 0).longValueExact();
        }
        String value = readTrimmedString();
        return value.isEmpty() ? 0L : Long.parseLong(value);
    }

    @Override
    public void set(Long value) {
        if (packedDecimal) {
            PackedDecimalCodec.encode(storage.bytes(), start, length, 0,
                    BigDecimal.valueOf(value == null ? 0L : value));
            return;
        }
        writeNumericString(String.valueOf(value == null ? 0L : value));
    }

    @Override
    public void set(ICobolRedefines<?> value) {
        Object actual = value == null ? null : value.get();
        if (actual instanceof Number number) {
            set(number.longValue());
        } else if (actual == null) {
            set(0L);
        } else {
            String text = actual.toString().trim();
            set(text.isEmpty() ? 0L : Long.parseLong(text));
        }
    }
}
