package free.cobol2java.java.redefines;

/**
 * {@link Short} 类型的 COBOL REDEFINES 视图实现。
 */
public class ShortCobolRedefines extends AbstractCobolRedefines<Short> {

    public ShortCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public ShortCobolRedefines(byte[] storage) {
        super(storage);
    }

    public ShortCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Short get() {
        String value = readTrimmedString();
        return value.isEmpty() ? 0 : Short.parseShort(value);
    }

    @Override
    public void set(Short value) {
        writeString(String.valueOf(value == null ? 0 : value));
    }
}
