package free.cobol2java.java;

/**
 * Runtime helper for COBOL {@code INSPECT} statements.
 *
 * <p>This class implements the core string-oriented behavior behind
 * {@code INSPECT TALLYING} and {@code INSPECT REPLACING}. The caller translates
 * parsed COBOL syntax into {@link TallyingTarget}, {@link ReplaceStruct}, and
 * {@link BeforeAfter} objects, then delegates the actual counting or replacing
 * work to this helper.
 *
 * <p>Supported grammar shape:
 *
 * <pre>
 * inspectStatement
 *    : INSPECT identifier
 *      ( inspectTallyingPhrase
 *      | inspectReplacingPhrase
 *      | inspectTallyingReplacingPhrase
 *      | inspectConvertingPhrase
 *      )
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
 * </pre>
 *
 * <p>{@code CONVERTING} is documented here because it belongs to the same
 * COBOL statement family, although this class currently focuses on tallying and
 * replacing behavior.
 */
public class Inspector {
    /**
     * Match modes used by COBOL {@code INSPECT}.
     *
     * <p>A {@code null} value is intentionally reserved by the caller to mean
     * {@code CHARACTER} or {@code CHARACTERS}, where the operation applies to
     * each character in the active slice rather than to a concrete search token.
     */
    public enum LeadType{
        ALL , LEADING , FIRST
    }
    /**
     * Counts matches for one or more tally targets using character mode.
     *
     * <p>This overload is the runtime equivalent of COBOL
     * {@code FOR CHARACTER(S)}. Each target can contribute its own
     * {@code BEFORE}/{@code AFTER} constraints, and the counts are summed.
     *
     * @param src source text inspected by the COBOL statement
     * @param targets tally targets to evaluate
     * @return total count across all non-null targets
     */
    public static int tallyingFor(String src,  TallyingTarget... targets){
        return tallyingFor(src, null, targets);
    }
    /**
     * Counts matches for one or more tally targets inside the effective window.
     *
     * <p>For each target, this method first computes the active slice defined by
     * its {@code BEFORE}/{@code AFTER} clauses, then applies the requested
     * matching rule:
     * {@code ALL}, {@code LEADING}, {@code FIRST}, or character mode when
     * {@code leadType} is {@code null}.
     *
     * @param src source text inspected by the COBOL statement
     * @param leadType matching mode; {@code null} means {@code CHARACTER(S)}
     * @param targets tally targets to evaluate
     * @return total count across all non-null targets
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
     * Applies one COBOL {@code INSPECT REPLACING} rule to the source text.
     *
     * <p>The method keeps content outside the effective
     * {@code BEFORE}/{@code AFTER} window unchanged and only replaces text
     * within the slice selected for this rule.
     *
     * @param src source text inspected by the COBOL statement
     * @param leadType replacement mode; {@code null} means {@code CHARACTER(S)}
     * @param struct replacement rule including search text, replacement text,
     *               and optional slice constraint
     * @return a new string with the requested replacement applied
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
     * Counts matches in one already-sliced segment.
     *
     * <p>A {@code null} lead type means COBOL {@code CHARACTER}/{@code CHARACTERS}
     * tallying, so the count is simply the segment length.
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
     * Replaces content in one already-sliced segment.
     *
     * <p>A {@code null} lead type means COBOL {@code CHARACTER}/{@code CHARACTERS}
     * replacing, so every character in the segment is replaced by the same
     * runtime value.
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
     * Applies {@code BEFORE}/{@code AFTER} conditions in declaration order.
     *
     * <p>The returned slice represents the effective window seen by one
     * tallying or replacing rule.
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
     * Applies a single scope marker to the current active window.
     *
     * <p>{@code BEFORE} cuts the window at the located marker, while
     * {@code AFTER} moves the window start to the first character after the
     * marker. {@code INITIAL} is currently interpreted as "use the first
     * occurrence", which matches the current implementation based on
     * {@link String#indexOf(String, int)}.
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

    /**
     * Immutable representation of the active inspection window.
     *
     * @param start inclusive start offset in the original source string
     * @param end exclusive end offset in the original source string
     * @param value substring covered by {@code [start, end)}
     */
    private record Slice(int start, int end, String value) {
    }
}
