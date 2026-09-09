package free.cobol2java.java;

/** Shared replaceable read service. The default binding executes existing generated repositories. */
public class SqlReadService implements IService {
    public <R> SqlReadResult<R> read(SqlReadRequest<R> request) {
        SqlReadRepository<R> repository = SqlRuntime.beanByType(request.table().repositoryType());
        R row = switch (request.kind()) {
            case BY_KEY -> repository.selectByKey(request.key()).orElse(null);
            case FIRST -> repository.selectAll().stream().findFirst().orElse(null);
        };
        return new SqlReadResult<>(row);
    }
}
