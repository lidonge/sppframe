package free.cobol2java.java;

import java.util.Objects;

/** Generated table identity; operation methods constrain the binding's capabilities. */
public record SqlWriteTable<R, T>(String name, Class<R> rowType, Class<T> repositoryType) {
    public SqlWriteTable {
        Objects.requireNonNull(name);
        Objects.requireNonNull(rowType);
        Objects.requireNonNull(repositoryType);
    }
}
