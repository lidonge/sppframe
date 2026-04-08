package free.cobol2java.java.redefines;

/**
 * {@link Float} 类型的 COBOL REDEFINES 视图实现。
 */
public class FloatCobolRedefines extends AbstractCobolRedefines<Float> {

    public FloatCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public FloatCobolRedefines(byte[] storage) {
        super(storage);
    }

    public FloatCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Float get() {
        String value = readTrimmedString();
        return value.isEmpty() ? 0F : Float.parseFloat(value);
    }

    @Override
    public void set(Float value) {
        writeString(String.valueOf(value == null ? 0F : value));
    }
}
