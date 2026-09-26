package free.cobol2java.java;

/** Technology-neutral boundary for acquiring and releasing named resource locks. */
public interface ResourceLockAccessService extends IService {
    void acquire(Object resource, Object length, AccessStatusSink status);

    void release(Object resource, Object length, AccessStatusSink status);

    boolean dispatchControlFlow(AccessStatusSink status);
}
