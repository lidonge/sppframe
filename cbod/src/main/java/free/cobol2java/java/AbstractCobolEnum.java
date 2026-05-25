package free.cobol2java.java;

/**
 * Base class for every generated COBOL condition (88-level) type.
 * <p>
 * The converter no longer emits these as Java {@code enum}s (an {@code enum}
 * cannot extend a class, so its {@code codes} state could not be shared).
 * Instead each generated type is a final class with {@code public static final}
 * instances that extends this base, so the {@code codes} storage,
 * {@link #getCode()}, {@link #getCodes()}, {@link #matches(Object)} and the
 * {@link #fromCode(AbstractCobolEnum[], Object, String)} lookup are defined once
 * here and inherited.
 *
 * @param <T> the code value type (generated types use {@code String} or {@code Integer})
 */
public abstract class AbstractCobolEnum<T> {
    private final T[] codes;

    protected AbstractCobolEnum(T[] codes) {
        this.codes = codes;
    }

    /** All COBOL condition values mapped to this constant. */
    public T[] getCodes() {
        return codes;
    }

    /** The primary (first) code, or {@code null} when none is declared. */
    public T getCode() {
        return codes == null || codes.length == 0 ? null : codes[0];
    }

    /** Whether {@code candidate} equals any of this constant's codes. */
    public boolean matches(Object candidate) {
        if (codes == null) {
            return false;
        }
        for (T codeValue : codes) {
            if (Util.compare(codeValue, candidate) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the constant whose codes contain {@code code}.
     *
     * @throws IllegalArgumentException when no constant matches
     */
    protected static <E extends AbstractCobolEnum<?>> E fromCode(E[] values, Object code, String cobolName) {
        for (E candidate : values) {
            if (candidate.matches(code)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Invalid " + cobolName + ": " + code);
    }
}
