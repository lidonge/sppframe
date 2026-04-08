package free.cobol2java.java.redefines;

/**
 * {@link String} 类型的 COBOL REDEFINES 视图实现。
 */
public class StringCobolRedefines extends AbstractCobolRedefines<String> {

    public StringCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public StringCobolRedefines(byte[] storage) {
        super(storage);
    }

    public StringCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public String get() {
        return readRawString();
    }

    @Override
    public void set(String value) {
        writeString(value);
    }
}
