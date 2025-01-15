package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-10@version 1.0
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

    public boolean isBefore() {
        return beforeAfter.isBefore();
    }

    public boolean isInitial() {
        return beforeAfter.isInitial();
    }

    public String getBeforeAfterString() {
        return beforeAfter.getBeforeAfterString();
    }
}
