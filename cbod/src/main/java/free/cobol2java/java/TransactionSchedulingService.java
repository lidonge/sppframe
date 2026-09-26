package free.cobol2java.java;

/** Starts a deferred transaction using source-bound payload and status contracts. */
public interface TransactionSchedulingService extends IService {
    void schedule(String transactionId, Object interval, Object payload, Integer payloadLength,
                  Object requestId, Object terminalId, AccessStatusSink status);

    boolean dispatchControlFlow(AccessStatusSink status);
}
