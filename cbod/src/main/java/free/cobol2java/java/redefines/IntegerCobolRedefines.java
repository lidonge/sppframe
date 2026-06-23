package free.cobol2java.java.redefines;

import free.cobol2java.java.CobolNumeric;

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

    public IntegerCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Integer get() {
        return CobolNumeric.toInteger(readTrimmedString());
    }

    @Override
    public void set(Integer value) {
        writeNumericString(String.valueOf(value == null ? 0 : value));
    }
}
