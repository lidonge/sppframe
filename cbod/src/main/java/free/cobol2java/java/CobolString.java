package free.cobol2java.java;

public final class CobolString {
    private CobolString() {
    }

    public static String substring(Object value, int begin) {
        String text = value == null ? "" : value.toString();
        int safeBegin = Math.max(0, begin);
        if (safeBegin >= text.length()) {
            return "";
        }
        return text.substring(safeBegin);
    }

    public static String substring(Object value, int begin, int end) {
        String text = value == null ? "" : value.toString();
        int safeBegin = Math.max(0, begin);
        int safeEnd = Math.max(safeBegin, end);
        if (text.length() < safeEnd) {
            text = text + " ".repeat(safeEnd - text.length());
        }
        return text.substring(safeBegin, safeEnd);
    }

    public static String subvalue(Object value, int start, Integer length) {
        int begin = Math.max(0, start - 1);
        if (length == null) {
            return substring(value, begin);
        }
        return substring(value, begin, begin + Math.max(0, length));
    }
}
