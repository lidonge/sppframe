package free.cobol2java.java;

/** Internal completion decision crossing a synchronous generated COBOL CALL. */
public final class CobolProgramTransactionExit extends RuntimeException {
    private final boolean rollback;
    private Runnable after;
    private java.util.function.Consumer<RuntimeException> afterFailure;

    public CobolProgramTransactionExit(boolean rollback, Runnable after) {
        super(null, null, false, false);
        this.rollback = rollback;
        this.after = java.util.Objects.requireNonNull(after, "after");
    }

    public CobolProgramTransactionExit(boolean rollback, Runnable after,
            java.util.function.Consumer<RuntimeException> afterFailure) {
        this(rollback, after);
        this.afterFailure = java.util.Objects.requireNonNull(afterFailure, "afterFailure");
    }

    public boolean rollback() { return rollback; }

    public void appendAfter(Runnable continuation) {
        Runnable first = after;
        after = () -> { first.run(); continuation.run(); };
        if (afterFailure != null) {
            var firstFailure = afterFailure;
            afterFailure = failure -> { firstFailure.accept(failure); continuation.run(); };
        }
    }

    public void runAfter() { after.run(); }

    public void runAfterFailure(RuntimeException failure) {
        if (afterFailure == null)
            throw new IllegalStateException("Source completion has no SQL failure continuation", failure);
        afterFailure.accept(failure);
    }
}
