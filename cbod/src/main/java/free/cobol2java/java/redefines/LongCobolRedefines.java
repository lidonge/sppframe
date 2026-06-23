package free.cobol2java.java.redefines;

import free.cobol2java.java.CobolNumeric;

/**
 * {@link Long} 类型的 COBOL REDEFINES 视图实现。
 */
public class LongCobolRedefines extends AbstractCobolRedefines<Long> {

    public LongCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public LongCobolRedefines(byte[] storage) {
        super(storage);
    }

    public LongCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    public LongCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Long get() {
        return CobolNumeric.toLong(readTrimmedString());
    }

    @Override
    public void set(Long value) {
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
            set(CobolNumeric.toLong(actual));
        }
    }
}
