package free.cobol2java.java;

/**
 * COBOL group value that can receive whole-value and reference-modification moves.
 */
public interface CobolGroup {
    /** Returns the canonical COBOL character sequence used by group comparisons. */
    static String stringValue(Object value) {
        return Util.copyString(value);
    }

    static <T> T copyInto(T target, Object source) {
        return Util.copy(source, target);
    }

    static <T> T copyRange(T target, Object source, Integer start, Integer length) {
        return Util.copy(source, target, start, length);
    }

    default Object copy(Object source) {
        Util.copy(source, this);
        return this;
    }

    default Object copy(Object source, Integer start, Integer length) {
        Util.copy(source, this, start, length);
        return this;
    }
}
