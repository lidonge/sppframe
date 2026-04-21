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
     * Returns the substring before the first occurrence of the given COBOL
     * delimiter. For SPACE/SPACES we stop at the first blank character.
     */
    public static String substring(Object target, Object delimiter) {
        if (target == null) {
            return "";
        }

        String value = String.valueOf(target);
        if (delimiter == null) {
            return value;
        }

        if (delimiter == CobolConstant.SPACE || delimiter == CobolConstant.SPACES) {
            int next = value.indexOf(' ');
            return next < 0 ? value : value.substring(0, next);
        }

        String actualDelimiter = String.valueOf(delimiter);
        if (actualDelimiter.isEmpty()) {
            return value;
        }

        int next = value.indexOf(actualDelimiter);
        return next < 0 ? value : value.substring(0, next);
    }

    public static String stringInto(Object target, Object value, int pointer) {
        String base = target == null ? "" : String.valueOf(target);
        String append = value == null ? "" : String.valueOf(value);
        int start = Math.max(pointer - 1, 0);
        if (base.length() < start) {
            base = base + " ".repeat(start - base.length());
        }
        StringBuilder sb = new StringBuilder(base);
        for (int i = 0; i < append.length(); i++) {
            int idx = start + i;
            if (idx < sb.length()) {
                sb.setCharAt(idx, append.charAt(i));
            } else {
                sb.append(append.charAt(i));
            }
        }
        return sb.toString();
    }

    public static int stringIntoPointer(int pointer, Object value) {
        return Math.max(pointer, 1) + (value == null ? 0 : String.valueOf(value).length());
    }

    /**
     * Splits the source sequentially by a single delimiter and writes results
     * back into each INTO target.
     * countIn stores the piece length, and delimiterIn stores the length of
     * the delimiter consumed for that piece.
     */
    public static void unstring(String target, String delimiter, UnstringInto ... intos){
        unstring(target, delimiter, false, intos);
    }

    public static void unstring(String target, String delimiter, boolean allDelimiter, UnstringInto ... intos) {
        unstring(target, delimiter, allDelimiter, 1, null, null, intos);
    }

    public static void unstring(String target, String delimiter, boolean allDelimiter, int pointer,
                                IValueSetter<Integer> tallySetter, IValueSetter<Integer> pointerSetter,
                                UnstringInto ... intos) {
        unstring(target, new String[]{delimiter}, allDelimiter, pointer, tallySetter, pointerSetter, intos);
    }

    public static void unstring(String target, String[] delimiters, boolean allDelimiter, UnstringInto ... intos) {
        unstring(target, delimiters, allDelimiter, 1, null, null, intos);
    }

    public static void unstring(String target, String[] delimiters, boolean allDelimiter, int pointer,
                                IValueSetter<Integer> tallySetter, IValueSetter<Integer> pointerSetter,
                                UnstringInto ... intos){
        if (intos == null || intos.length == 0) {
            return;
        }

        String source = target == null ? "" : target;
        int cursor = Math.max(pointer - 1, 0);
        int tally = 0;
        String[] normalizedDelimiters = normalizeDelimiters(delimiters);

        for (UnstringInto into : intos) {
            if (into == null) {
                continue;
            }

            // sourceLength is stable; recomputing it here keeps the branch logic straightforward.
            int sourceLength = source.length();
            if (cursor > sourceLength) {
                setInto(into, "", 0, 0);
                continue;
            }

            DelimiterMatch match = findNextDelimiter(source, cursor, normalizedDelimiters);
            String piece;
            int count;
            int delimiterLength;

            if (match == null) {
                piece = source.substring(Math.min(cursor, sourceLength));
                count = piece.length();
                delimiterLength = 0;
                cursor = sourceLength;
            } else {
                piece = source.substring(cursor, match.start());
                count = piece.length();
                delimiterLength = match.delimiter().length();
                int nextCursor = match.start() + delimiterLength;
                if (allDelimiter && delimiterLength > 0) {
                    while (nextCursor <= sourceLength - delimiterLength
                            && source.startsWith(match.delimiter(), nextCursor)) {
                        nextCursor += delimiterLength;
                    }
                }
                cursor = nextCursor;
            }

            setInto(into, piece, count, delimiterLength);
            tally++;
        }

        if (tallySetter != null) {
            tallySetter.set(tally);
        }
        if (pointerSetter != null) {
            pointerSetter.set(cursor + 1);
        }
    }

    private static void setInto(UnstringInto into, String piece, int count, int delimiterLength) {
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

    private static String[] normalizeDelimiters(String[] delimiters) {
        if (delimiters == null || delimiters.length == 0) {
            return new String[0];
        }
        java.util.List<String> normalized = new java.util.ArrayList<>();
        for (String delimiter : delimiters) {
            if (delimiter != null && !delimiter.isEmpty()) {
                normalized.add(delimiter);
            }
        }
        return normalized.toArray(String[]::new);
    }

    private static DelimiterMatch findNextDelimiter(String source, int cursor, String[] delimiters) {
        DelimiterMatch best = null;
        for (String delimiter : delimiters) {
            int idx = source.indexOf(delimiter, cursor);
            if (idx == -1) {
                continue;
            }
            if (best == null || idx < best.start()) {
                best = new DelimiterMatch(idx, delimiter);
            }
        }
        return best;
    }

    private record DelimiterMatch(int start, String delimiter) {
    }
}
