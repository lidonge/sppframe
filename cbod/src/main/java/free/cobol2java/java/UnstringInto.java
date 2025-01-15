package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-24@version 1.0
 */
public class UnstringInto {
    private IValueSetter<String> into;
    private IValueSetter<Integer> countIn;
    private IValueSetter<Integer> delimiterIn;

    public UnstringInto(IValueSetter<String> into, IValueSetter<Integer> countIn, IValueSetter<Integer> delimiterIn) {
        this.into = into;
        this.countIn = countIn;
        this.delimiterIn = delimiterIn;
    }

    public IValueSetter<String> getInto() {
        return into;
    }

    public IValueSetter<Integer> getCountIn() {
        return countIn;
    }

    public IValueSetter<Integer> getDelimiterIn() {
        return delimiterIn;
    }
}
