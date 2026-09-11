package free.cobol2java.java;

import java.util.Objects;
import java.util.function.Consumer;

/** Replaceable typed write boundary using the existing repository binding by default. */
public class SqlWriteService implements IService {
    public <R, T extends SqlInsertRepository<R>> SqlWriteResult insert(SqlWriteTable<R, T> table, R row) {
        return new SqlWriteResult(SqlRuntime.beanByType(table.repositoryType()).insert(row));
    }

    public <R, T extends SqlUpdateRepository<R>> SqlWriteResult update(SqlWriteTable<R, T> table, R row) {
        return new SqlWriteResult(SqlRuntime.beanByType(table.repositoryType()).update(row));
    }

    public <R, T extends SqlReadRepository<R> & SqlUpdateRepository<R>> SqlWriteResult updateByKey(
            SqlWriteTable<R, T> table, R key, Consumer<R> assignments) {
        Objects.requireNonNull(assignments);
        T repository = SqlRuntime.beanByType(table.repositoryType());
        // Preserve the pre-existing generated UPDATE behavior and evaluation order.
        R row = repository.selectByKey(key).orElse(key);
        assignments.accept(row);
        return new SqlWriteResult(repository.update(row));
    }

    public <R, T extends SqlDeleteRepository<R>> SqlWriteResult deleteByKey(SqlWriteTable<R, T> table, R key) {
        return new SqlWriteResult(SqlRuntime.beanByType(table.repositoryType()).deleteByKey(key));
    }
}
