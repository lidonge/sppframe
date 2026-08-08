package free.cobol2java.java.redefines;

import free.cobol2java.java.CobolConstant;
import free.cobol2java.java.CobolString;

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

    public StringCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
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

    public void set(CobolConstant value) {
        writeString(CobolString.value(value));
    }

    public StringCobolRedefines copy(Object value) {
        set(value == null ? "" : value.toString());
        return this;
    }

    public boolean sameTextAs(Object value) {
        return CobolString.equalsPadded(get(), value);
    }

    /** Tests whether this fixed-width character view is equal to COBOL SPACE/SPACES. */
    public boolean isSpaces() {
        return CobolString.equalsPadded(get(), " ");
    }

    public int compareTextTo(Object value) {
        return CobolString.comparePadded(get(), value);
    }

}
