package free.cobol2java.java.jcl;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JclIfCondition {
    private static final Pattern COMPARISON = Pattern.compile(
            "(MAXCC|LASTCC|RC|[A-Z0-9_$#@]+\\.RC)\\s*(=|EQ|\\^=|!=|NE|>|GT|>=|GE|<|LT|<=|LE)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private final String expression;

    private JclIfCondition(String expression) {
        this.expression = expression;
    }

    public static JclIfCondition parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return new JclIfCondition(text.trim());
    }

    public boolean matches(Map<String, Integer> stepReturnCodes, int maxCc, int lastCc) {
        return evalOr(expression, stepReturnCodes, maxCc, lastCc);
    }

    private boolean evalOr(String text, Map<String, Integer> stepReturnCodes, int maxCc, int lastCc) {
        for (String part : text.split("(?i)\\s+OR\\s+|\\s*\\|\\|\\s*")) {
            if (evalAnd(part, stepReturnCodes, maxCc, lastCc)) {
                return true;
            }
        }
        return false;
    }

    private boolean evalAnd(String text, Map<String, Integer> stepReturnCodes, int maxCc, int lastCc) {
        for (String part : text.split("(?i)\\s+AND\\s+|\\s*&&\\s*")) {
            if (!evalComparison(part, stepReturnCodes, maxCc, lastCc)) {
                return false;
            }
        }
        return true;
    }

    private boolean evalComparison(String text, Map<String, Integer> stepReturnCodes, int maxCc, int lastCc) {
        String normalized = stripParens(text.trim());
        Matcher matcher = COMPARISON.matcher(normalized);
        if (!matcher.matches()) {
            return false;
        }
        int left = valueOf(matcher.group(1), stepReturnCodes, maxCc, lastCc);
        String op = matcher.group(2).toUpperCase(Locale.ROOT);
        int right = Integer.parseInt(matcher.group(3));
        return switch (op) {
            case "=", "EQ" -> left == right;
            case "^=", "!=", "NE" -> left != right;
            case ">", "GT" -> left > right;
            case ">=", "GE" -> left >= right;
            case "<", "LT" -> left < right;
            case "<=", "LE" -> left <= right;
            default -> false;
        };
    }

    private int valueOf(String token, Map<String, Integer> stepReturnCodes, int maxCc, int lastCc) {
        String upper = token.toUpperCase(Locale.ROOT);
        if ("MAXCC".equals(upper)) {
            return maxCc;
        }
        if ("LASTCC".equals(upper) || "RC".equals(upper)) {
            return lastCc;
        }
        if (upper.endsWith(".RC")) {
            String stepName = upper.substring(0, upper.length() - 3);
            Integer exact = stepReturnCodes.get(stepName);
            if (exact != null) {
                return exact;
            }
            for (Map.Entry<String, Integer> entry : stepReturnCodes.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(stepName)) {
                    return entry.getValue();
                }
            }
        }
        return JclReturnCodes.OK;
    }

    private String stripParens(String value) {
        String result = value;
        while (result.startsWith("(") && result.endsWith(")") && result.length() > 1) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }
}
