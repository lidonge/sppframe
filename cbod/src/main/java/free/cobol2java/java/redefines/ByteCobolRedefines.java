package free.cobol2java.java.redefines;

/**
 * {@link Byte} 类型的 COBOL REDEFINES 视图实现。
 */
public class ByteCobolRedefines extends AbstractCobolRedefines<Byte> {

    public ByteCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public ByteCobolRedefines(byte[] storage) {
        super(storage);
    }

    public ByteCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Byte get() {
        String value = readTrimmedString();
        return value.isEmpty() ? 0 : Byte.parseByte(value);
    }

    @Override
    public void set(Byte value) {
        writeString(String.valueOf(value == null ? 0 : value));
    }
}
