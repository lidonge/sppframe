package free.cobol2java.java;

/** SQL exceptions remain exceptions; the count is the actual repository result. */
public record SqlWriteResult(int affectedRows) { }
