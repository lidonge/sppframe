package free.cobol2java.java;

/** Database-independent category of one SQL operation. */
public enum SqlStatus {
    SUCCESS, NO_DATA, WARNING, ERROR;

    /** Source SQLCA interpretation stays in the technical runtime. */
    public static SqlStatus fromSourceCode(int code) {
        if (code == 0) return SUCCESS;
        if (code == 100) return NO_DATA;
        return code > 0 ? WARNING : ERROR;
    }
}
