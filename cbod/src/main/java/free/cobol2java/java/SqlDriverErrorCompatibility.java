package free.cobol2java.java;

import java.sql.SQLException;

/** Explicit target-driver diagnostics whose source-visible meaning is proven. */
final class SqlDriverErrorCompatibility {
    private SqlDriverErrorCompatibility() {}

    static Integer sourceCode(SQLException failure) {
        if (failure == null) return null;
        // Missing table/view in a dynamically prepared query: Db2 -204.
        // Match the documented vendor diagnostic AND SQLSTATE, never message text.
        if ((failure.getErrorCode() == 1146 || failure.getErrorCode() == 42102)
                && "42S02".equals(failure.getSQLState())) {
            return -204;
        }
        // Unknown column in that query: Db2 -206 (invalid column reference).
        if ((failure.getErrorCode() == 1054 || failure.getErrorCode() == 42122)
                && "42S22".equals(failure.getSQLState())) {
            return -206;
        }
        // MySQL ER_SP_CURSOR_NOT_OPEN (1326/24000) and Db2 FETCH/CLOSE -501
        // describe the same cursor state. Keep both vendor values in this layer.
        if (failure.getErrorCode() == 1326 && "24000".equals(failure.getSQLState())) {
            return SqlRuntimeException.CURSOR_NOT_OPEN;
        }
        // MySQL ER_DUP_ENTRY and Db2 -803 both report a duplicate unique key.
        if (failure.getErrorCode() == 1062 && "23000".equals(failure.getSQLState())) {
            return -803;
        }
        return null;
    }
}
