package free.cobol2java.java;

/** Generic failure raised when a business service invocation cannot complete. */
public class ServiceInvocationException extends RuntimeException {
    public ServiceInvocationException(String message) {
        super(message);
    }

    public ServiceInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
