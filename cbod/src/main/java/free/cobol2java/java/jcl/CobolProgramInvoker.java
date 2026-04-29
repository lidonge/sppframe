package free.cobol2java.java.jcl;

import free.cobol2java.java.ServiceManager;
import free.cobol2java.java.IService;

public final class CobolProgramInvoker {
    private CobolProgramInvoker() {
    }

    public static int invoke(String programName, String[] args) throws Exception {
        IService service = ServiceManager.getService(programName);
        if (service == null) {
            throw new IllegalStateException("Missing COBOL program service: " + programName);
        }
        service.execute((Object[]) (args == null ? new String[0] : args));
        return ServiceManager.getReturnCode(service);
    }
}
