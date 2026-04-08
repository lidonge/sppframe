package free.cobol2java.java.redefines;

/**
 * {@link Integer} 类型的 COBOL REDEFINES 视图实现。
 */
public class IntegerCobolRedefines extends AbstractCobolRedefines<Integer> {

    public IntegerCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public IntegerCobolRedefines(byte[] storage) {
        super(storage);
    }

    public IntegerCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Integer get() {
        String value = readTrimmedString();
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    @Override
    public void set(Integer value) {
        writeString(String.valueOf(value == null ? 0 : value));
    }
}
