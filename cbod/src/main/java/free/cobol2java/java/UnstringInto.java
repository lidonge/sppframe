package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-24@version 1.0
 * Collects the write-back targets for one UNSTRING INTO receiver.
 */
public class UnstringInto {
    private IValueSetter<String> into;
    private IValueSetter<Integer> countIn;
    private IValueSetter<Integer> delimiterIn;

    /**
     * Creates one UNSTRING INTO descriptor.
     *
     * @param into receives the extracted string piece
     * @param countIn receives the character count of that piece
     * @param delimiterIn receives the length of the delimiter consumed for that piece
     */
    public UnstringInto(IValueSetter<String> into, IValueSetter<Integer> countIn, IValueSetter<Integer> delimiterIn) {
        this.into = into;
        this.countIn = countIn;
        this.delimiterIn = delimiterIn;
    }

    /**
     * Returns the write-back target for the extracted piece.
     */
    public IValueSetter<String> getInto() {
        return into;
    }

    /**
     * Returns the write-back target for the piece length.
     */
    public IValueSetter<Integer> getCountIn() {
        return countIn;
    }

    /**
     * Returns the write-back target for the consumed delimiter length.
     */
    public IValueSetter<Integer> getDelimiterIn() {
        return delimiterIn;
    }
}
