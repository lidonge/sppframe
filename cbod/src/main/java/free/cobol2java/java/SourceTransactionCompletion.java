package free.cobol2java.java;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Resumes a source completion failure only after its owning transaction has ended. */
public final class SourceTransactionCompletion {
    private SourceTransactionCompletion() {}

    public static RuntimeException failedRollback(RuntimeException sourceFailure, RuntimeException rollbackFailure) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Source SQL failure resumed inside an active transaction", rollbackFailure);
        java.util.Objects.requireNonNull(sourceFailure, "sourceFailure");
        java.util.Objects.requireNonNull(rollbackFailure, "rollbackFailure");
        if (sourceFailure != rollbackFailure) sourceFailure.addSuppressed(rollbackFailure);
        return sourceFailure;
    }

    public static void failed(CobolProgramTransactionExit completion, RuntimeException failure) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Source completion failure resumed inside an active transaction", failure);
        java.util.Objects.requireNonNull(completion, "completion").runAfterFailure(failure);
    }
}
