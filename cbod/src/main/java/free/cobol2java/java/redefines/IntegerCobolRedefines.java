package free.cobol2java.java.redefines;

import java.math.BigDecimal;

/**
 * {@link Integer} 类型的 COBOL REDEFINES 视图实现。
 */
public class IntegerCobolRedefines extends AbstractCobolRedefines<Integer> {
    private final boolean packedDecimal;

    public IntegerCobolRedefines(int totalSize) {
        super(totalSize);
        this.packedDecimal = false;
    }

    public IntegerCobolRedefines(byte[] storage) {
        super(storage);
        this.packedDecimal = false;
    }

    public IntegerCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
        this.packedDecimal = false;
    }

    public IntegerCobolRedefines(byte[] storage, int start, int length, boolean packedDecimal) {
        super(storage, start, length);
        this.packedDecimal = packedDecimal;
    }

    public IntegerCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
        this.packedDecimal = false;
    }

    public IntegerCobolRedefines(CobolRedefinesBuffer storage, int start, int length, boolean packedDecimal) {
        super(storage, start, length);
        this.packedDecimal = packedDecimal;
    }

    @Override
    public Integer get() {
        if (packedDecimal) {
            return PackedDecimalCodec.decode(storage.bytes(), start, length, 0).intValueExact();
        }
        String value = readTrimmedString();
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    @Override
    public void set(Integer value) {
        if (packedDecimal) {
            PackedDecimalCodec.encode(storage.bytes(), start, length, 0,
                    BigDecimal.valueOf(value == null ? 0 : value));
            return;
        }
        writeNumericString(String.valueOf(value == null ? 0 : value));
    }
}
