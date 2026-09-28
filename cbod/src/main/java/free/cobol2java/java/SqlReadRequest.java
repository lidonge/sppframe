package free.cobol2java.java;

import java.util.Objects;

/** Source query binding together with the existing table and input-key contract. */
public record SqlReadRequest<R>(SqlReadTable<R> table, Kind kind, R key, SqlReadQuery<R> query) {
    public enum Kind { BY_KEY, FIRST }

    public SqlReadRequest {
        Objects.requireNonNull(table);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(query);
        if (kind == Kind.BY_KEY) Objects.requireNonNull(key);
        if (kind == Kind.FIRST && key != null) {
            throw new IllegalArgumentException("FIRST has no key");
        }
    }

    public SqlReadRequest(SqlReadTable<R> table, Kind kind, R key) {
        this(table, kind, key, () -> {
            SqlReadRepository<R> repository = ServiceManager.getBean(table.repositoryType());
            R row = switch (kind) {
                case BY_KEY -> repository.selectByKey(key).orElse(null);
                case FIRST -> repository.selectAll().stream().findFirst().orElse(null);
            };
            return new SqlReadResult<>(row);
        });
    }

    public static <R> SqlReadRequest<R> byKey(SqlReadTable<R> table, R key) {
        return new SqlReadRequest<>(table, Kind.BY_KEY, key);
    }

    public static <R> SqlReadRequest<R> first(SqlReadTable<R> table) {
        return new SqlReadRequest<>(table, Kind.FIRST, null);
    }

    public static <R> SqlReadRequest<R> byKey(SqlReadTable<R> table, R key, SqlReadQuery<R> query) {
        return new SqlReadRequest<>(table, Kind.BY_KEY, key, query);
    }

    public static <R> SqlReadRequest<R> first(SqlReadTable<R> table, SqlReadQuery<R> query) {
        return new SqlReadRequest<>(table, Kind.FIRST, null, query);
    }
}
