package free.cobol2java.java;

import org.springframework.stereotype.Component;

/** Default adapter for COBOL transient-storage operations on a CICS runtime. */
@Component
public class CicsTransientStorageAdapter implements QueueAccessService {
    @Override
    public void writeTd(Object queue, Object value, Object length, AccessStatusSink status) {
        var response = CicsRuntime.writeqTd(queue, value, length);
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public Object readTs(Object queue, Object item, Object length, AccessStatusSink status) {
        var response = CicsRuntime.readqTs(queue, item, length);
        status.publish(response.resp(), response.resp2());
        return response.value();
    }

    @Override
    public void writeTs(Object queue, Object value, Object length, Object item, AccessStatusSink status) {
        var response = CicsRuntime.writeqTs(queue, value, length, item);
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public void deleteTs(Object queue, AccessStatusSink status) {
        var response = CicsRuntime.deleteqTs(queue);
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public boolean dispatchControlFlow(AccessStatusSink status) {
        return CicsRuntime.dispatchHandle(status.status());
    }
}
