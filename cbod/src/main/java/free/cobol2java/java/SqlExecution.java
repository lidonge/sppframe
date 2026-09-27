package free.cobol2java.java;

import java.sql.SQLException;

/** Immutable result of one SQL operation, including diagnostics owned by the runtime. */
public record SqlExecution(SqlStatus status, int sourceCode, String sqlState,
                           Integer vendorCode, String nativeErrorMsg,
                           boolean sourceCodeKnown) {
    public SqlExecution {
        if (status == null) throw new IllegalArgumentException("status");
    }

    public static SqlExecution success() {
        return new SqlExecution(SqlStatus.SUCCESS, 0, null, null, null, true);
    }

    public static SqlExecution fromSourceCode(int code) {
        return new SqlExecution(SqlStatus.fromSourceCode(code), code, null, null, null, true);
    }

    public static SqlExecution fromFailure(RuntimeException failure) {
        int sourceCode = failure instanceof SqlRuntimeException sqlFailure
                ? sqlFailure.getSqlCode() : -1;
        SQLException sqlFailure = sqlException(failure);
        return new SqlExecution(SqlStatus.ERROR, sourceCode,
                sqlFailure == null ? null : sqlFailure.getSQLState(),
                sqlFailure == null ? null : sqlFailure.getErrorCode(),
                sqlFailure == null ? failure.getMessage() : sqlFailure.getMessage(),
                failure instanceof SqlRuntimeException);
    }

    @Override
    public int sourceCode() {
        if (!sourceCodeKnown) throw new UnsupportedOperationException(
                "No proven source SQLCODE mapping for SQLSTATE=" + sqlState
                        + ", vendorCode=" + vendorCode);
        return sourceCode;
    }

    /** Technical mapping used only for source-observable SQLCODE moves. */
    public int sourceCompatibleDisplayValue() {
        if (sourceCodeKnown) return sourceCode;
        if (status == SqlStatus.SUCCESS) return 0;
        if (status == SqlStatus.NO_DATA) return 100;
        throw new UnsupportedOperationException(
                "No proven source SQLCODE display mapping for SQLSTATE=" + sqlState
                        + ", vendorCode=" + vendorCode);
    }

    private static SQLException sqlException(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlFailure) return sqlFailure;
        }
        return null;
    }
}
