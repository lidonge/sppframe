package free.cobol2java.java;

/** Only a present row authorizes host-variable write-back; errors retain the existing exception path. */
public record SqlReadResult<R>(R row) {
    public SqlExecution execution() {
        return row == null ? SqlExecution.fromSourceCode(SqlRuntimeException.NOT_FOUND)
                : SqlExecution.success();
    }

    public int sqlCode() {
        return row == null ? SqlRuntimeException.NOT_FOUND : 0;
    }
}
