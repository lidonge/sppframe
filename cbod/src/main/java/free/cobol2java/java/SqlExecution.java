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

    public static SqlExecution noData() {
        return new SqlExecution(SqlStatus.NO_DATA, 100, null, null, null, true);
    }

    /** Explicit source assignment, retained only at the technical compatibility boundary. */
    public static SqlExecution fromSourceAssignment(int code) {
        return fromSourceCode(code);
    }

    public static SqlExecution fromSourceCode(int code) {
        return new SqlExecution(SqlStatus.fromSourceCode(code), code, null, null, null, true);
    }

    public static SqlExecution fromFailure(RuntimeException failure) {
        SQLException sqlFailure = sqlException(failure);
        Integer mappedCode = failure instanceof SqlRuntimeException sourceFailure
                ? sourceFailure.getSqlCode() : SqlDriverErrorCompatibility.sourceCode(sqlFailure);
        return new SqlExecution(SqlStatus.ERROR, mappedCode == null ? -1 : mappedCode,
                sqlFailure == null ? null : sqlFailure.getSQLState(),
                sqlFailure == null ? null : sqlFailure.getErrorCode(),
                sqlFailure == null ? failure.getMessage() : sqlFailure.getMessage(),
                mappedCode != null);
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

    /** Source-compatible text for a proven numeric-edited receiving picture. */
    public String sourceCompatibleDisplayValue(String picture) {
        return SqlCodeDisplayCompatibility.fixedLeadingMinus(this, picture);
    }

    /** Database-independent duplicate-key result for the current operation. */
    public boolean isDuplicateKey() {
        return sourceCodeKnown && sourceCode == -803;
    }

    /** Cursor lifecycle failure produced by the portable cursor registry. */
    public boolean isCursorNotOpen() {
        return sourceCodeKnown && sourceCode == SqlRuntimeException.CURSOR_NOT_OPEN;
    }

    private static SQLException sqlException(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlFailure) return sqlFailure;
        }
        return null;
    }
}
