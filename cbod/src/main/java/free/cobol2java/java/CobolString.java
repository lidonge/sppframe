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

    public static String replaceRange(Object target, Object source, Integer start, Integer length) {
        String targetText = Util.copyString(target);
        if (targetText == null) {
            targetText = "";
        }
        String sourceText = Util.copyString(source);
        if (sourceText == null) {
            sourceText = "";
        }

        int begin = start == null ? 0 : Math.max(0, start - 1);
        int replaceLength = length == null ? sourceText.length() : Math.max(0, length);
        StringBuilder builder = new StringBuilder(targetText);
        while (builder.length() < begin) {
            builder.append(' ');
        }
        while (builder.length() < begin + replaceLength) {
            builder.append(' ');
        }
        String chunk = padRight(sourceText, replaceLength);
        builder.replace(begin, begin + replaceLength, chunk.substring(0, replaceLength));
        return builder.toString();
    }

    private static String padRight(String text, int length) {
        String actual = text == null ? "" : text;
        if (actual.length() >= length) {
            return actual.substring(0, length);
        }
        return actual + " ".repeat(length - actual.length());
    }
}
