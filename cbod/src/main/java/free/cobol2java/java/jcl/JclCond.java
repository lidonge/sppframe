package free.cobol2java.java.jcl;

import java.util.Locale;
import java.util.Map;

public class JclCond {
    private final int code;
    private final String operator;
    private final String stepName;

    private JclCond(int code, String operator, String stepName) {
        this.code = code;
        this.operator = operator;
        this.stepName = stepName;
    }

    public static JclCond parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        String[] parts = normalized.split(",");
        if (parts.length < 2) {
            return null;
        }
        int code = Integer.parseInt(parts[0].trim());
        String operator = parts[1].trim().toUpperCase(Locale.ROOT);
        String stepName = parts.length > 2 ? parts[2].trim() : null;
        return new JclCond(code, operator, stepName == null || stepName.isBlank() ? null : stepName);
    }

    public boolean matches(Map<String, Integer> stepReturnCodes, int jobRc) {
        int actual = stepName == null ? jobRc : stepReturnCodes.getOrDefault(stepName, 0);
        return switch (operator) {
            case "LT" -> code < actual;
            case "LE" -> code <= actual;
            case "EQ" -> code == actual;
            case "NE" -> code != actual;
            case "GT" -> code > actual;
            case "GE" -> code >= actual;
            default -> false;
        };
    }
}
