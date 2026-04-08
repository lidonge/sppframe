package free.cobol2java.java.redefines;

/**
 * {@link Boolean} 类型的 COBOL REDEFINES 视图实现。
 * <p>
 * 约定：去除首尾空格后的内容为 {@code true}、{@code 1}、{@code y}、{@code yes}
 * 时返回 {@code true}，其余情况返回 {@code false}。
 * </p>
 */
public class BooleanCobolRedefines extends AbstractCobolRedefines<Boolean> {

    public BooleanCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public BooleanCobolRedefines(byte[] storage) {
        super(storage);
    }

    public BooleanCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Boolean get() {
        String value = readTrimmedString().toLowerCase();
        return "true".equals(value) || "1".equals(value) || "y".equals(value) || "yes".equals(value);
    }

    @Override
    public void set(Boolean value) {
        writeString(Boolean.TRUE.equals(value) ? "1" : "0");
    }
}
