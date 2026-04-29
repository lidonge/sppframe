package free.cobol2java.java.jcl;

public final class ProcRuntime {
    private ProcRuntime() {
    }

    public static int execute(JclStep step) {
        return JclReturnCodes.OK;
    }
}
