package free.cobol2java.java;

/** Source-bound CICS SET FILE request; the configured adapter owns file state. */
public interface FileControlService extends IService {
    enum State { OPEN, CLOSED }

    void setState(String fileName, State state, AccessStatusSink status);

    boolean dispatchControlFlow(AccessStatusSink status);
}
