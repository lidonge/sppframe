package free.cobol2java.java;

import java.util.Objects;

/**
 * Source exit intent and the separate, observed completion of a declarative region.
 * The generated method may only return a pending result; an interceptor publishes
 * the observed completion after the configured backend has acted.
 */
public record TransactionRegionResult<S, E, R>(
        S state, E exit, CompletionRequest request,
        CompletionStatus completion, R response) {

    public enum CompletionRequest { COMMIT, ROLLBACK, NONE }
    public enum CompletionStatus { PENDING, COMMITTED, ROLLED_BACK, UNCHANGED, FAILED }

    public TransactionRegionResult {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(exit, "exit");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(completion, "completion");
        if (!valid(request, completion))
            throw new IllegalArgumentException("Completion " + completion
                    + " does not match source request " + request);
    }

    public static <S, E, R> TransactionRegionResult<S, E, R> requested(
            S state, E exit, CompletionRequest request) {
        return new TransactionRegionResult<>(state, exit, request, CompletionStatus.PENDING, null);
    }

    public TransactionRegionResult<S, E, R> observed(CompletionStatus status, R observedResponse) {
        if (completion != CompletionStatus.PENDING)
            throw new IllegalStateException("Transaction completion has already been observed");
        if (status == CompletionStatus.PENDING)
            throw new IllegalArgumentException("An observation must be terminal");
        return new TransactionRegionResult<>(state, exit, request, status, observedResponse);
    }

    private static boolean valid(CompletionRequest request, CompletionStatus status) {
        return switch (status) {
            case PENDING, FAILED -> true;
            case COMMITTED -> request == CompletionRequest.COMMIT;
            case ROLLED_BACK -> request == CompletionRequest.ROLLBACK;
            case UNCHANGED -> request == CompletionRequest.NONE;
        };
    }
}
