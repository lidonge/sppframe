package free.cobol2java.java;

import java.util.Objects;

/** Typed boundary reusing the existing cursor registry and row identity. */
public class SqlCursorService implements IService {
    public <R> SqlCursorStatus open(String name, SqlCursorQuery<R> query) {
        Objects.requireNonNull(query);
        SqlRuntime.openCursor(name, query.execute());
        return new SqlCursorStatus(0);
    }

    public <R> SqlReadResult<R> fetch(String name, Class<R> rowType) {
        Objects.requireNonNull(rowType);
        return new SqlReadResult<>(rowType.cast(SqlRuntime.fetchCursor(name)));
    }

    public <R> SqlReadResult<R> current(String name, Class<R> rowType) {
        Objects.requireNonNull(rowType);
        return new SqlReadResult<>(rowType.cast(SqlRuntime.getCurrentCursorRow(name)));
    }

    public SqlCursorStatus close(String name) {
        SqlRuntime.closeCursor(name);
        return new SqlCursorStatus(0);
    }
}
