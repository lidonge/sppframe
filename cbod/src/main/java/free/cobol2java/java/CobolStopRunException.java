package free.cobol2java.java;

import java.lang.reflect.InvocationTargetException;

public class CobolStopRunException extends RuntimeException {
    private static final ThreadLocal<Boolean> STOP_REQUESTED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public CobolStopRunException() {
        super("COBOL STOP RUN");
    }

    public static void stop() {
        STOP_REQUESTED.set(Boolean.TRUE);
    }

    public static boolean isStopRequested() {
        return STOP_REQUESTED.get();
    }

    public static boolean consumeStopRequested() {
        boolean requested = isStopRequested();
        STOP_REQUESTED.set(Boolean.FALSE);
        return requested;
    }

    public static void clearStopRequested() {
        STOP_REQUESTED.set(Boolean.FALSE);
    }

    public static boolean isStopRun(Throwable throwable) {
        return unwrap(throwable) instanceof CobolStopRunException;
    }

    public static CobolStopRunException unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CobolStopRunException stopRun) {
                return stopRun;
            }
            if (current instanceof InvocationTargetException invocationTargetException) {
                current = invocationTargetException.getTargetException();
                continue;
            }
            current = current.getCause();
        }
        return null;
    }
}
