package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-24@version 1.0
 * Generic write-back callback used by runtime helpers such as UNSTRING.
 */
public interface IValueSetter<T> {
    /**
     * Stores a value into the target selected by the caller.
     */
    void set(T value);
}
