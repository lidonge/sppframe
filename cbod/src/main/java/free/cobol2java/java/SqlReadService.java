package free.cobol2java.java;

/** Shared replaceable read service; each request explicitly binds its query execution. */
public class SqlReadService implements IService {
    public <R> SqlReadResult<R> read(SqlReadRequest<R> request) {
        return request.query().execute();
    }
}
