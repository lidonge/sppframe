package free.cobol2java.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import free.cobol2java.java.redefines.AbstractCobolRedefines;

public final class CobolNumeric {
    private CobolNumeric() {
    }

    public static Integer toInteger(Object value) {
        return Integer.valueOf(numericText(value));
    }

    public static Long toLong(Object value) {
        return Long.valueOf(numericText(value));
    }

    public static Short toShort(Object value) {
        return Short.valueOf(numericText(value));
    }

    public static Byte toByte(Object value) {
        return Byte.valueOf(numericText(value));
    }

    public static Double toDouble(Object value) {
        return Double.valueOf(numericText(value));
    }

    public static Float toFloat(Object value) {
        return Float.valueOf(numericText(value));
    }

    public static BigDecimal toBigDecimal(Object value) {
        return new BigDecimal(numericText(value));
    }

    public static BigInteger toBigInteger(Object value) {
        return new BigInteger(numericText(value));
    }

    public static BigDecimal bigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof CobolConstant constant) {
            return switch (constant) {
                case ZERO, ZEROS, ZEROES, SPACE, SPACES -> BigDecimal.ZERO;
                default -> BigDecimal.ZERO;
            };
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Float || value instanceof Double) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        String text = numericText(value);
        if (text == null || text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text);
    }

    public static boolean isNumeric(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Number) {
            return true;
        }
        String text = normalizeNumericText(value);
        return isStandardNumber(text);
    }

    public static String displayText(Object value, int precision, int scale) {
        if (value == null) {
            return "0".repeat(precision);
        }
        BigDecimal number;
        if (value instanceof BigDecimal bigDecimal) {
            number = bigDecimal;
        } else if (value instanceof BigInteger bigInteger) {
            number = new BigDecimal(bigInteger);
        } else if (value instanceof Number numberValue) {
            number = BigDecimal.valueOf(numberValue.longValue());
        } else {
            try {
                number = new BigDecimal(value.toString().trim());
            } catch (RuntimeException ignored) {
                return "0".repeat(precision);
            }
        }
        boolean negative = number.signum() < 0;
        String digits = number.movePointRight(Math.max(scale, 0)).abs()
                .setScale(0, java.math.RoundingMode.DOWN).toPlainString();
        if (digits.length() < precision) {
            digits = "0".repeat(precision - digits.length()) + digits;
        } else if (digits.length() > precision) {
            digits = digits.substring(digits.length() - precision);
        }
        return negative && precision > 0 ? "-" + digits.substring(1) : digits;
    }

    public static BigDecimal decimalValue(String value, int scale) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        try {
            return new BigDecimal(text).movePointLeft(Math.max(scale, 0)).setScale(scale);
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO.setScale(scale);
        }
    }

    public static BigInteger bigIntegerValue(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return BigInteger.ZERO;
        }
        try {
            return new BigInteger(text);
        } catch (RuntimeException ignored) {
            return BigInteger.ZERO;
        }
    }

    public static int intValue(String value) {
        return bigIntegerValue(value).intValue();
    }

    public static long longValue(String value) {
        return bigIntegerValue(value).longValue();
    }

    public static short shortValue(String value) {
        return bigIntegerValue(value).shortValue();
    }

    public static byte byteValue(String value) {
        return bigIntegerValue(value).byteValue();
    }

    public static String numericText(Object value) {
        if (value == null) {
            return "0";
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return "0";
        }
        String weekday = weekdayNumber(text);
        if (weekday != null) {
            return weekday;
        }
        if (isStandardNumber(text)) {
            return text;
        }
        StringBuilder digits = new StringBuilder(text.length());
        boolean decimalSeen = false;
        boolean signSeen = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch == '+' || ch == '-') && !signSeen && digits.length() == 0) {
                digits.append(ch);
                signSeen = true;
            } else if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            } else if (ch == '.' && !decimalSeen) {
                digits.append(ch);
                decimalSeen = true;
            }
        }
        String normalized = digits.toString();
        return normalized.isEmpty() || "+".equals(normalized) || "-".equals(normalized) || ".".equals(normalized)
                ? "0"
                : normalized;
    }

    private static String normalizeNumericText(Object value) {
        if (value == null) {
            return null;
        }
        String text = copyStringWithoutGroup(value);
        if (text == null) {
            return null;
        }
        return text.trim();
    }

    private static boolean isStandardNumber(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        int index = 0;
        if (text.charAt(0) == '+' || text.charAt(0) == '-') {
            index = 1;
        }
        boolean sawDigit = false;
        boolean sawDecimal = false;
        for (; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch >= '0' && ch <= '9') {
                sawDigit = true;
            } else if (ch == '.' && !sawDecimal) {
                sawDecimal = true;
            } else {
                return false;
            }
        }
        return sawDigit;
    }

    private static String copyStringWithoutGroup(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof BigInteger integer) {
            return integer.toString();
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof Enum<?>) {
            return value.toString();
        }
        if (value instanceof AbstractCobolRedefines<?> redef) {
            return new String(redef.getBytes(), StandardCharsets.UTF_8);
        }
        return value.toString();
    }

    private static String weekdayNumber(String text) {
        return switch (text.toUpperCase(Locale.ROOT)) {
            case "MO", "MON", "MONDAY" -> "1";
            case "TU", "TUE", "TUESDAY" -> "2";
            case "WE", "WED", "WEDNESDAY" -> "3";
            case "TH", "THU", "THURSDAY" -> "4";
            case "FR", "FRI", "FRIDAY" -> "5";
            case "SA", "SAT", "SATURDAY" -> "6";
            case "SU", "SUN", "SUNDAY" -> "7";
            default -> null;
        };
    }
}
