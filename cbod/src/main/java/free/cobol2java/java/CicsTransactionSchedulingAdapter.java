package free.cobol2java.java;

import org.springframework.stereotype.Component;

/** Default adapter for transaction scheduling on a CICS runtime. */
@Component
public class CicsTransactionSchedulingAdapter implements TransactionSchedulingService {
    @Override
    public void schedule(String transactionId, Object interval, Object payload, Integer payloadLength,
                         Object requestId, Object terminalId, AccessStatusSink status) {
        var response = CicsRuntime.start(transactionId, interval, payload, payloadLength, requestId, terminalId);
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public boolean dispatchControlFlow(AccessStatusSink status) {
        return CicsRuntime.dispatchHandle(status.status());
    }
}
