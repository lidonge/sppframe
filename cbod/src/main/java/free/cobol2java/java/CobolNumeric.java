package free.cobol2java.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

public final class CobolNumeric {
    private CobolNumeric() {
    }

    public static final class SizeError extends ArithmeticException {
        private SizeError(String message) {
            super(message);
        }
    }

    public static BigDecimal nonZeroDivisor(BigDecimal value) {
        if (value.signum() == 0) throw new SizeError("Division by zero");
        return value;
    }

    public static BigInteger nonZeroDivisor(BigInteger value) {
        if (value.signum() == 0) throw new SizeError("Division by zero");
        return value;
    }

    public static int nonZeroDivisor(int value) {
        if (value == 0) throw new SizeError("Division by zero");
        return value;
    }

    public static long nonZeroDivisor(long value) {
        if (value == 0) throw new SizeError("Division by zero");
        return value;
    }

    public static float nonZeroDivisor(float value) {
        if (value == 0) throw new SizeError("Division by zero");
        return value;
    }

    public static double nonZeroDivisor(double value) {
        if (value == 0) throw new SizeError("Division by zero");
        return value;
    }

    public static boolean isNumeric(Object value) {
        return Util.isNumeric(value);
    }

    public static BigDecimal bigDecimalValue(Object value) {
        return Util.bigDecimalValue(value);
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

    /** Integer exponent only; negative powers use the existing decimal division precision. */
    public static BigDecimal powIntegral(BigDecimal base, BigDecimal exponent) {
        int integralExponent = exponent.intValueExact();
        return integralExponent >= 0 ? base.pow(integralExponent)
                : base.pow(integralExponent, java.math.MathContext.DECIMAL128);
    }

    /** Decimal exponentiation without binary floating-point intermediate values. */
    public static BigDecimal powDecimal(BigDecimal base, BigDecimal exponent) {
        if (exponent.remainder(BigDecimal.ONE).signum() == 0) {
            return powIntegral(base, exponent);
        }
        return ch.obermuhlner.math.big.BigDecimalMath.pow(
                base, exponent, java.math.MathContext.DECIMAL128);
    }

    public static BigInteger toBigInteger(Object value) {
        return new BigInteger(numericText(value));
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
        return text;
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
