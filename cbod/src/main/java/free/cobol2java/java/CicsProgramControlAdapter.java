package free.cobol2java.java;

import org.springframework.stereotype.Component;

/** Default CICS implementation of program control operations. */
@Component
public class CicsProgramControlAdapter implements ProgramControlService {
    @Override
    public void returnToCaller(boolean saveCommonWorkAreas, AccessStatusSink status) {
        if (saveCommonWorkAreas) CicsRuntime.saveCurrentCommonWorkAreas();
        var response = CicsRuntime.returnControl();
        status.publish(response.resp(), response.resp2());
    }

    @Override
    public boolean dispatchControlFlow(AccessStatusSink status) {
        return CicsRuntime.dispatchHandle(status.status());
    }

    @Override
    public void pushHandle() {
        CicsRuntime.pushHandle();
    }

    @Override
    public void popHandle() {
        CicsRuntime.popHandle();
    }
}
