package free.cobol2java.java.redefines;

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
        String value = readTrimmedString();
        return value.isEmpty() ? 0L : Long.parseLong(value);
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
            String text = actual.toString().trim();
            set(text.isEmpty() ? 0L : Long.parseLong(text));
        }
    }
}
