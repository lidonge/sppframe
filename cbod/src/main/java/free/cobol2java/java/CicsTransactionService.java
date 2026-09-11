package free.cobol2java.java;

/**
 * Replaceable CICS unit-of-work operations.
 * The default preserves the existing CWA runtime behavior; it does not implement
 * database transactions. Platform bindings must operate on the current unit of work,
 * not create an unrelated transaction for each service invocation.
 */
public class CicsTransactionService implements IService {
    public CicsCommandStatus syncpoint() {
        var response = CicsRuntime.syncpoint();
        return new CicsCommandStatus(response.resp(), response.resp2());
    }

    public CicsCommandStatus rollback() {
        var response = CicsRuntime.syncpointRollback();
        return new CicsCommandStatus(response.resp(), response.resp2());
    }
}
