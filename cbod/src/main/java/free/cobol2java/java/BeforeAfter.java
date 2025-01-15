package free.cobol2java.java;

public class BeforeAfter {
    private boolean isBefore;
    private boolean isInitial;
    private String beforeAfterString;

    public BeforeAfter(boolean isBefore, boolean isInitial, String beforeAfterString) {
        this.isBefore = isBefore;
        this.isInitial = isInitial;
        this.beforeAfterString = beforeAfterString;
    }

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isInitial() {
        return isInitial;
    }

    public String getBeforeAfterString() {
        return beforeAfterString;
    }
}