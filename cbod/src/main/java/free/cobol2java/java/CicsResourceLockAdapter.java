package free.cobol2java.java;

import org.springframework.stereotype.Component;

/** Default CICS implementation of the resource-lock service boundary. */
@Component
public class CicsResourceLockAdapter implements ResourceLockAccessService {
    @Override
    public void acquire(Object resource, Object length, AccessStatusSink status) {
        var response = CicsRuntime.enq(resource, length);
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public void release(Object resource, Object length, AccessStatusSink status) {
        var response = CicsRuntime.deq(resource, length);
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public boolean dispatchControlFlow(AccessStatusSink status) {
        return CicsRuntime.dispatchHandle(status.status());
    }
}
