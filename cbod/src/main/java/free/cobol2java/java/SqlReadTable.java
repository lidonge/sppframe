package free.cobol2java.java;

import java.util.Objects;

/** Generated schema identity and explicit default binding, never inferred from class names. */
public record SqlReadTable<R>(String name, Class<R> rowType,
        Class<? extends SqlReadRepository<R>> repositoryType) {
    public SqlReadTable {
        Objects.requireNonNull(name);
        Objects.requireNonNull(rowType);
        Objects.requireNonNull(repositoryType);
    }
}
