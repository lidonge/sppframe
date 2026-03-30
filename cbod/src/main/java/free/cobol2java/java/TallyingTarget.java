package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-12@version 1.0
 * One INSPECT TALLYING target plus its optional BEFORE / AFTER constraints.
 */
public class TallyingTarget {
    private String target;
    private BeforeAfter[] beforeAfters;

    public TallyingTarget(String target, BeforeAfter... beforeAfters) {
        this.target = target;
        this.beforeAfters = beforeAfters;
    }
    public TallyingTarget(CobolConstant constTarget, BeforeAfter... beforeAfters) {
        this.target = convertConstant(constTarget);
        this.beforeAfters = beforeAfters;
    }

    /**
     * Converts a COBOL constant into the runtime string used for matching.
     */
    private String convertConstant(CobolConstant constTarget) {
        if (constTarget == null) {
            return null;
        }
        return switch (constTarget) {
            case SPACE, SPACES -> " ";
            case QUOTE, QUOTES -> "\"";
            case ZERO, ZEROS, ZEROES -> "0";
            case LOW_VALUE, LOW_VALUES, NULL, NULLS -> "\u0000";
            case HIGH_VALUE, HIGH_VALUES -> String.valueOf(Character.MAX_VALUE);
            case SQLCODE -> "SQLCODE";
            default -> null;
        };
    }

    public String getTarget() {
        return target;
    }

    public BeforeAfter[] getBeforeAfters() {
        return beforeAfters;
    }
}
