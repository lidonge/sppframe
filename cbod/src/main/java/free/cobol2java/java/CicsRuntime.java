package free.cobol2java.java;

/**
 * Minimal runtime hooks for EXEC CICS commands that do not yet have a
 * concrete simulator behind {@link CicsUtil}. Keeping them here gives the
 * generated code a stable extension point without faking a CICS response.
 */
public final class CicsRuntime {
    private CicsRuntime() {
    }

    public static void start(String transid, Object interval, Object from, Integer length) {
        // Reserved for future async transaction simulation.
    }

    public static void link(String program, Object commarea, Integer length) {
        IService service = ServiceManager.getService(program);
        if (service == null) {
            return;
        }
        service.execute(commarea, length);
    }
}
