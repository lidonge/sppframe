package free.cobol2java.java;

/** Internal control transfer across a Spring proxy for a source ROLLBACK exit. */
public final class CobolRollbackSignal extends RuntimeException {
    public static final CobolRollbackSignal INSTANCE = new CobolRollbackSignal();

    private CobolRollbackSignal() {
        super(null, null, false, false);
    }
}
