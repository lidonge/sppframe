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

    @Override
    public Long get() {
        String value = readTrimmedString();
        return value.isEmpty() ? 0L : Long.parseLong(value);
    }

    @Override
    public void set(Long value) {
        writeString(String.valueOf(value == null ? 0L : value));
    }
}
