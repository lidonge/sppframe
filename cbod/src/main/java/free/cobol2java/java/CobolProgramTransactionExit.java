package free.cobol2java.java;

/** Internal completion decision crossing a synchronous generated COBOL CALL. */
public final class CobolProgramTransactionExit extends RuntimeException {
    private final boolean rollback;
    private Runnable after;

    public CobolProgramTransactionExit(boolean rollback, Runnable after) {
        super(null, null, false, false);
        this.rollback = rollback;
        this.after = java.util.Objects.requireNonNull(after, "after");
    }

    public boolean rollback() { return rollback; }

    public void appendAfter(Runnable continuation) {
        Runnable first = after;
        after = () -> { first.run(); continuation.run(); };
    }

    public void runAfter() { after.run(); }
}
