package free.cobol2java.java;

/** Technology-neutral record cursor contract; a runtime adapter supplies its implementation. */
public interface DataAccessService extends IService {
    /** Read one record; the adapter publishes source-visible outcome through status. */
    Object read(AccessContext context, DataAccessResource resource, Object key,
            AccessStatusSink status);

    <K> void begin(AccessContext context, DataAccessResource resource, K key, boolean gteq,
            boolean equal, boolean generic, Integer keyLength, AccessStatusSink status);

    <R, K> K next(AccessContext context, DataAccessResource resource, R into, K key,
            Integer length, AccessStatusSink status);

    <R, K> K previous(AccessContext context, DataAccessResource resource, R into, K key,
            Integer length, AccessStatusSink status);

    void end(AccessContext context, DataAccessResource resource, AccessStatusSink status);

    /** Applies the runtime's control-flow policy to a completed access status. */
    boolean dispatchControlFlow(AccessStatusSink status);
}
