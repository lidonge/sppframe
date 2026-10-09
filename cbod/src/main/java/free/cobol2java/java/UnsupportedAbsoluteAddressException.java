package free.cobol2java.java;

/** A native address cannot be reconstructed from a Java reference or numeric word. */
public final class UnsupportedAbsoluteAddressException extends IllegalStateException {
    public static final String CODE = "CB2J_ABSOLUTE_ADDRESS_REQUIRES_MANUAL_CONFIRMATION";
    private final String identity;
    private final String operation;
    private final String sourceFile;
    private final int sourceLine;

    public UnsupportedAbsoluteAddressException(String identity, String operation,
                                              String sourceFile, int sourceLine) {
        super(CODE + ": " + operation + " at " + sourceFile + ":" + sourceLine
                + " [" + identity + "]; native address conversion requires manual confirmation");
        this.identity = java.util.Objects.requireNonNull(identity);
        this.operation = java.util.Objects.requireNonNull(operation);
        this.sourceFile = java.util.Objects.requireNonNull(sourceFile);
        this.sourceLine = sourceLine;
    }

    public String code() { return CODE; }
    public boolean requiresManualConfirmation() { return true; }
    public String identity() { return identity; }
    public String operation() { return operation; }
    public String sourceFile() { return sourceFile; }
    public int sourceLine() { return sourceLine; }
}
