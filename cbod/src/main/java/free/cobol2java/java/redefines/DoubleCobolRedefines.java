package free.cobol2java.java.redefines;

/**
 * {@link Double} 类型的 COBOL REDEFINES 视图实现。
 */
public class DoubleCobolRedefines extends AbstractCobolRedefines<Double> {

    public DoubleCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public DoubleCobolRedefines(byte[] storage) {
        super(storage);
    }

    public DoubleCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Double get() {
        String value = readTrimmedString();
        return value.isEmpty() ? 0D : Double.parseDouble(value);
    }

    @Override
    public void set(Double value) {
        writeString(String.valueOf(value == null ? 0D : value));
    }
}
