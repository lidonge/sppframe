package free.cobol2java.java;

/** Lifecycle status; SQL errors continue to use the existing exception path. */
public record SqlCursorStatus(int sqlCode) {
    public SqlExecution execution() {
        return SqlExecution.fromSourceCode(sqlCode);
    }
}
