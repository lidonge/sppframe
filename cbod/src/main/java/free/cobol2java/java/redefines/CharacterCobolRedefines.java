package free.cobol2java.java.redefines;

/**
 * {@link Character} 类型的 COBOL REDEFINES 视图实现。
 * <p>
 * 读取时取映射区间的首字符；写入时仅写入 1 个字符。
 * </p>
 */
public class CharacterCobolRedefines extends AbstractCobolRedefines<Character> {

    public CharacterCobolRedefines(int totalSize) {
        super(totalSize);
    }

    public CharacterCobolRedefines(byte[] storage) {
        super(storage);
    }

    public CharacterCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public Character get() {
        String value = readRawString();
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    @Override
    public void set(Character value) {
        writeString(String.valueOf(value == null ? ' ' : value));
    }
}
