package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-24@version 1.0
 * String helpers used by generated Java for COBOL string statements.
 */
public class StringUtil {
    /**
     * Safely returns the leftmost size characters.
     * By convention in this runtime helper, null input and non-positive length
     * both produce an empty string.
     */
    public static String substring(Object target, int size) {
        if (size <= 0) {
            return "";
        }
        if (target == null) {
            return "";
        }

        String value = String.valueOf(target);
        return value.length() <= size ? value : value.substring(0, size);
    }

    /**
     * Splits the source sequentially by a single delimiter and writes results
     * back into each INTO target.
     * countIn stores the piece length, and delimiterIn stores the length of
     * the delimiter consumed for that piece.
     */
    public static void unstring(String target, String delimiter, UnstringInto ... intos){
        if (intos == null || intos.length == 0) {
            return;
        }

        String source = target == null ? "" : target;
        String actualDelimiter = delimiter == null ? "" : delimiter;
        int cursor = 0;

        for (UnstringInto into : intos) {
            if (into == null) {
                continue;
            }

            // sourceLength is stable; recomputing it here keeps the branch logic straightforward.
            int sourceLength = source.length();
            String piece;
            int count;
            int delimiterLength;

            if (cursor > sourceLength) {
                piece = "";
                count = 0;
                delimiterLength = 0;
            } else if (actualDelimiter.isEmpty()) {
                piece = source.substring(cursor);
                count = piece.length();
                delimiterLength = 0;
                cursor = sourceLength + 1;
            } else {
                int next = source.indexOf(actualDelimiter, cursor);
                if (next < 0) {
                    piece = source.substring(cursor);
                    count = piece.length();
                    delimiterLength = 0;
                    cursor = sourceLength + 1;
                } else {
                    piece = source.substring(cursor, next);
                    count = piece.length();
                    delimiterLength = actualDelimiter.length();
                    cursor = next + actualDelimiter.length();
                }
            }

            if (into.getInto() != null) {
                into.getInto().set(piece);
            }
            if (into.getCountIn() != null) {
                into.getCountIn().set(count);
            }
            if (into.getDelimiterIn() != null) {
                into.getDelimiterIn().set(delimiterLength);
            }
        }
    }
}
