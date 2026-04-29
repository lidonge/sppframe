package free.cobol2java.java.jcl;

public final class JclReturnCodes {
    public static final int OK = 0;
    public static final int WARNING = 4;
    public static final int ERROR = 8;
    public static final int SEVERE_ERROR = 12;
    public static final int CRITICAL_ERROR = 16;
    public static final String ABEND = "ABEND";

    private JclReturnCodes() {
    }
}
