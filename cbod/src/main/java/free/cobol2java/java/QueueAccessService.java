package free.cobol2java.java;

/** Queue operation contract used by generated programs; a runtime adapter supplies the storage mechanism. */
public interface QueueAccessService extends IService {
    Object readTs(Object queue, Object item, Object length, AccessStatusSink status);

    void writeTs(Object queue, Object value, Object length, Object item, AccessStatusSink status);

    void deleteTs(Object queue, AccessStatusSink status);

    boolean dispatchControlFlow(AccessStatusSink status);
}
