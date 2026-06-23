package free.cobol2java.java.redefines;

import free.cobol2java.java.CobolNumeric;

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

    public ByteCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Byte get() {
        return CobolNumeric.toByte(readTrimmedString());
    }

    @Override
    public void set(Byte value) {
        writeString(String.valueOf(value == null ? 0 : value));
    }
}
