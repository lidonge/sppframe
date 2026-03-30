package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-10@version 1.0
 * inspect statement
 *
 * inspectStatement
 *    : INSPECT identifier (inspectTallyingPhrase | inspectReplacingPhrase | inspectTallyingReplacingPhrase | inspectConvertingPhrase)
 *    ;
 *
 * inspectTallyingPhrase
 *    : TALLYING inspectFor+
 *    ;
 *
 * inspectReplacingPhrase
 *    : REPLACING (inspectReplacingCharacters | inspectReplacingAllLeadings)+
 *    ;
 *
 * inspectTallyingReplacingPhrase
 *    : TALLYING inspectFor+ inspectReplacingPhrase+
 *    ;
 *
 * inspectConvertingPhrase
 *    : CONVERTING (identifier | literal) inspectTo inspectBeforeAfter*
 *    ;
 *
 * inspectFor
 *    : identifier FOR (inspectCharacters | inspectAllLeadings)+
 *    ;
 *
 * inspectCharacters
 *    : (CHARACTER | CHARACTERS) inspectBeforeAfter*
 *    ;
 *
 * inspectReplacingCharacters
 *    : (CHARACTER | CHARACTERS) inspectBy inspectBeforeAfter*
 *    ;
 *
 * inspectAllLeadings
 *    : (ALL | LEADING) inspectAllLeading+
 *    ;
 *
 * inspectReplacingAllLeadings
 *    : (ALL | LEADING | FIRST) inspectReplacingAllLeading+
 *    ;
 *
 * inspectAllLeading
 *    : (identifier | literal) inspectBeforeAfter*
 *    ;
 *
 * inspectReplacingAllLeading
 *    : (identifier | literal) inspectBy inspectBeforeAfter*
 *    ;
 *
 * inspectBy
 *    : BY (identifier | literal)
 *    ;
 *
 * inspectTo
 *    : TO (identifier | literal)
 *    ;
 *
 * inspectBeforeAfter
 *    : (BEFORE | AFTER) INITIAL? (identifier | literal)
 *    ;
 */
public class Inspector {
    /**
     * Variants used by INSPECT for ALL / LEADING / FIRST.
     * A null value is reserved by the caller to represent CHARACTERS.
     */
    public enum LeadType{
        ALL , LEADING , FIRST
    }

    /**
     * Counts matches inside the effective INSPECT window.
     * Any BEFORE / AFTER constraints attached to each target are applied first.
     */
    public static int tallyingFor(String src, LeadType leadType, TallyingTarget... targets){
        if (src == null || targets == null || targets.length == 0) {
            return 0;
        }

        int total = 0;
        for (TallyingTarget target : targets) {
            if (target == null) {
                continue;
            }
            Slice slice = slice(src, target.getBeforeAfters());
            total += count(slice.value(), leadType, target.getTarget());
        }
        return total;
    }

    /**
     * Applies one INSPECT REPLACING rule inside the effective window.
     * Content outside the window is preserved unchanged.
     */
    public static String replacingBy(String src,LeadType leadType, ReplaceStruct struct){
        if (src == null || struct == null) {
            return src;
        }

        Slice slice = slice(src, struct.hasBeforeAfter()
                ? new BeforeAfter[]{new BeforeAfter(struct.isBefore(), struct.isInitial(), struct.getBeforeAfterString())}
                : null);

        String replaced = replace(slice.value(), leadType, struct.getFind(), struct.getReplace());
        return src.substring(0, slice.start()) + replaced + src.substring(slice.end());
    }

    /**
     * A null lead type means CHARACTER / CHARACTERS tallying,
     * so the result is the length of the active window.
     */
    private static int count(String value, LeadType leadType, String target) {
        if (value == null) {
            return 0;
        }
        if (leadType == null) {
            return value.length();
        }
        if (target == null || target.isEmpty()) {
            return 0;
        }

        return switch (leadType) {
            case ALL -> countAll(value, target);
            case LEADING -> countLeading(value, target);
            case FIRST -> value.contains(target) ? 1 : 0;
        };
    }

    /**
     * A null lead type means CHARACTER / CHARACTERS replacing,
     * so every character in the active window is replaced with the same value.
     */
    private static String replace(String value, LeadType leadType, String find, String replacement) {
        if (value == null) {
            return null;
        }
        String safeReplacement = replacement == null ? "" : replacement;
        if (leadType == null) {
            return replaceCharacters(value, safeReplacement);
        }
        if (find == null || find.isEmpty()) {
            return value;
        }

        return switch (leadType) {
            case ALL -> value.replace(find, safeReplacement);
            case LEADING -> replaceLeading(value, find, safeReplacement);
            case FIRST -> replaceFirst(value, find, safeReplacement);
        };
    }

    private static int countAll(String value, String target) {
        int count = 0;
        int from = 0;
        while (from <= value.length() - target.length()) {
            int index = value.indexOf(target, from);
            if (index < 0) {
                break;
            }
            count++;
            from = index + target.length();
        }
        return count;
    }

    private static int countLeading(String value, String target) {
        int count = 0;
        int index = 0;
        while (index <= value.length() - target.length() && value.startsWith(target, index)) {
            count++;
            index += target.length();
        }
        return count;
    }

    private static String replaceCharacters(String value, String replacement) {
        if (value.isEmpty()) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value.length() * Math.max(replacement.length(), 1));
        for (int i = 0; i < value.length(); i++) {
            builder.append(replacement);
        }
        return builder.toString();
    }

    private static String replaceLeading(String value, String find, String replacement) {
        int count = countLeading(value, find);
        if (count == 0) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(replacement.repeat(count));
        builder.append(value.substring(count * find.length()));
        return builder.toString();
    }

    private static String replaceFirst(String value, String find, String replacement) {
        int index = value.indexOf(find);
        if (index < 0) {
            return value;
        }
        return value.substring(0, index) + replacement + value.substring(index + find.length());
    }

    /**
     * Applies BEFORE / AFTER conditions in order and returns the effective INSPECT slice.
     */
    private static Slice slice(String src, BeforeAfter[] beforeAfters) {
        int start = 0;
        int end = src.length();
        if (beforeAfters == null) {
            return new Slice(start, end, src);
        }

        for (BeforeAfter beforeAfter : beforeAfters) {
            if (beforeAfter == null) {
                continue;
            }
            int[] updated = applyBeforeAfter(src, start, end, beforeAfter);
            start = updated[0];
            end = updated[1];
            if (start >= end) {
                return new Slice(start, start, "");
            }
        }
        return new Slice(start, end, src.substring(start, end));
    }

    /**
     * BEFORE cuts the window at the marker, AFTER starts after the marker.
     * INITIAL is currently treated as "first occurrence".
     */
    private static int[] applyBeforeAfter(String src, int start, int end, BeforeAfter beforeAfter) {
        String marker = beforeAfter.getBeforeAfterString();
        if (marker == null || marker.isEmpty()) {
            return new int[]{start, end};
        }

        if (beforeAfter.isBefore()) {
            int index = src.indexOf(marker, start);
            if (index < 0 || index > end) {
                return new int[]{start, end};
            }
            return new int[]{start, index};
        }

        int index = src.indexOf(marker, start);
        if (index < 0 || index >= end) {
            return new int[]{end, end};
        }
        int nextStart = index + marker.length();
        return new int[]{Math.min(nextStart, end), end};
    }

    private record Slice(int start, int end, String value) {
    }
}
