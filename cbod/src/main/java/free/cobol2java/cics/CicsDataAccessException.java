package free.cobol2java.cics;

public class CicsDataAccessException extends RuntimeException {
    public CicsDataAccessException(String message) {
        super(message);
    }

    public CicsDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}