package free.cobol2java.java;

import java.sql.SQLException;

/** Explicit target-driver diagnostics whose source-visible meaning is proven. */
final class SqlDriverErrorCompatibility {
    private SqlDriverErrorCompatibility() {}

    static Integer sourceCode(SQLException failure) {
        if (failure == null) return null;
        // MySQL ER_SP_CURSOR_NOT_OPEN (1326/24000) and Db2 FETCH/CLOSE -501
        // describe the same cursor state. Keep both vendor values in this layer.
        if (failure.getErrorCode() == 1326 && "24000".equals(failure.getSQLState())) {
            return SqlRuntimeException.CURSOR_NOT_OPEN;
        }
        return null;
    }
}
