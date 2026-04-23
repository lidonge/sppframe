package free.cobol2java.java;

public class SqlRuntimeException extends RuntimeException {
    public static final int NOT_FOUND = 100;
    public static final int CURSOR_NOT_OPEN = -501;
    public static final int INVALID_REQUEST = -518;

    private final int sqlCode;

    public SqlRuntimeException(int sqlCode, String message) {
        super(message);
        this.sqlCode = sqlCode;
    }

    public int getSqlCode() {
        return sqlCode;
    }
}
