package free.cobol2java.java;

/** Only a present row authorizes host-variable write-back; errors retain the existing exception path. */
public record SqlReadResult<R>(R row) {
    public int sqlCode() {
        return row == null ? SqlRuntimeException.NOT_FOUND : 0;
    }
}
