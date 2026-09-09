package free.cobol2java.java;

import java.util.Objects;

/** A typed request for the existing key lookup or first-row SELECT execution paths. */
public record SqlReadRequest<R>(SqlReadTable<R> table, Kind kind, R key) {
    public enum Kind { BY_KEY, FIRST }

    public SqlReadRequest {
        Objects.requireNonNull(table);
        Objects.requireNonNull(kind);
        if (kind == Kind.BY_KEY) Objects.requireNonNull(key);
        if (kind == Kind.FIRST && key != null) {
            throw new IllegalArgumentException("FIRST has no key");
        }
    }

    public static <R> SqlReadRequest<R> byKey(SqlReadTable<R> table, R key) {
        return new SqlReadRequest<>(table, Kind.BY_KEY, key);
    }

    public static <R> SqlReadRequest<R> first(SqlReadTable<R> table) {
        return new SqlReadRequest<>(table, Kind.FIRST, null);
    }
}
