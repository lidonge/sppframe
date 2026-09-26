package free.cobol2java.java;

/** Technology-neutral boundary for returning control from a program. */
public interface ProgramControlService extends IService {
    void returnToCaller(boolean saveCommonWorkAreas, AccessStatusSink status);

    boolean dispatchControlFlow(AccessStatusSink status);

    void pushHandle();

    void popHandle();
}
