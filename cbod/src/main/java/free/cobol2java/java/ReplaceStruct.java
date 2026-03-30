package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-10@version 1.0
 * Carries one INSPECT REPLACING rule, including optional BEFORE / AFTER scope.
 */
public class ReplaceStruct {
    private BeforeAfter beforeAfter;
    private String find;
    private String replace;
    public ReplaceStruct(String find, String replace) {
        this(find,replace,null);
    }

    public ReplaceStruct(String find, String replace, BeforeAfter beforeAfter) {
        this.find = find;
        this.replace = replace;
        this.beforeAfter = beforeAfter;
    }

    public String getFind() {
        return find;
    }

    public String getReplace() {
        return replace;
    }

    /**
     * Returns false when no BEFORE / AFTER metadata is present,
     * which keeps caller-side checks null-safe.
     */
    public boolean isBefore() {
        return beforeAfter != null && beforeAfter.isBefore();
    }

    public boolean isInitial() {
        return beforeAfter != null && beforeAfter.isInitial();
    }

    public String getBeforeAfterString() {
        return beforeAfter == null ? null : beforeAfter.getBeforeAfterString();
    }

    /**
     * Lets the caller distinguish "no scope constraint" from the boolean value
     * inside the scope metadata itself.
     */
    public boolean hasBeforeAfter() {
        return beforeAfter != null;
    }
}
