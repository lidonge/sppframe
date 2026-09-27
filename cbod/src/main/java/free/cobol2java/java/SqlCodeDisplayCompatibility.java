package free.cobol2java.java;

/** Source-visible SQLCODE display, kept at the runtime boundary. */
public final class SqlCodeDisplayCompatibility {
    private SqlCodeDisplayCompatibility() {}

    /** Formats a fixed leading sign followed by decimal digits, as in PIC -9(05). */
    public static String fixedLeadingMinus(SqlExecution execution, String picture) {
        int digits = decimalPlaces(picture);
        int code = execution.sourceCompatibleDisplayValue();
        String magnitude = Long.toString(Math.abs((long) code));
        if (magnitude.length() > digits) {
            throw new IllegalArgumentException("SQLCODE exceeds display picture " + picture);
        }
        return (code < 0 ? "-" : " ") + "0".repeat(digits - magnitude.length()) + magnitude;
    }

    private static int decimalPlaces(String picture) {
        if (picture == null || !picture.startsWith("-9")) {
            throw new IllegalArgumentException("Unsupported SQLCODE display picture: " + picture);
        }
        if (picture.length() == 2) return 1;
        if (picture.charAt(2) == '(' && picture.endsWith(")")) {
            String count = picture.substring(3, picture.length() - 1);
            if (count.isEmpty()) throw new IllegalArgumentException("Empty picture count");
            int places = 0;
            for (int i = 0; i < count.length(); i++) {
                char digit = count.charAt(i);
                if (digit < '0' || digit > '9') {
                    throw new IllegalArgumentException("Unsupported SQLCODE display picture: " + picture);
                }
                places = Math.addExact(Math.multiplyExact(places, 10), digit - '0');
            }
            if (places > 0) return places;
        } else {
            for (int i = 2; i < picture.length(); i++) {
                if (picture.charAt(i) != '9') {
                    throw new IllegalArgumentException("Unsupported SQLCODE display picture: " + picture);
                }
            }
            return picture.length() - 1;
        }
        throw new IllegalArgumentException("Unsupported SQLCODE display picture: " + picture);
    }
}
