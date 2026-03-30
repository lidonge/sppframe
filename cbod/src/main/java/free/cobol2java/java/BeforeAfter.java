package free.cobol2java.java;

/**
 * Carries one BEFORE / AFTER clause used by INSPECT-style runtime helpers.
 */
public class BeforeAfter {
    private boolean isBefore;
    private boolean isInitial;
    private String beforeAfterString;

    /**
     * Creates one scope marker.
     *
     * @param isBefore true for BEFORE, false for AFTER
     * @param isInitial true when the COBOL clause included INITIAL
     * @param beforeAfterString the marker string used to cut the active window
     */
    public BeforeAfter(boolean isBefore, boolean isInitial, String beforeAfterString) {
        this.isBefore = isBefore;
        this.isInitial = isInitial;
        this.beforeAfterString = beforeAfterString;
    }

    /**
     * Returns whether this clause is BEFORE rather than AFTER.
     */
    public boolean isBefore() {
        return isBefore;
    }

    /**
     * Returns whether the clause was marked with INITIAL.
     */
    public boolean isInitial() {
        return isInitial;
    }

    /**
     * Returns the marker string associated with this clause.
     */
    public String getBeforeAfterString() {
        return beforeAfterString;
    }
}
