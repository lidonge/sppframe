package free.cobol2java.java;

public class CobolStopRunException extends RuntimeException {
    public CobolStopRunException() {
        super("COBOL STOP RUN");
    }

    public static void stop() {
        throw new CobolStopRunException();
    }
}
