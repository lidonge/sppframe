package free.cobol2java.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author lidong@date 2024-09-04@version 1.0
 */
public class Util {
    public static void output(Object... args) {
        if(args == null || args.length == 0) {
            System.out.println();
            return;
        }
        System.out.println(Arrays.toString(args));
    }

    public static String Input() {
        return new Scanner(System.in).nextLine();
    }

    public static String now() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    public static Object init(Class typeClass, String pictureStr) {
        return "";
    }

    public static String subvalue(Object obj, int start, int len) {
        return subvalue(obj, Integer.valueOf(start), Integer.valueOf(len));
    }

    public static String subvalue(Object obj, Integer start, Integer len) {
        if (obj == null) {
            return null;
        }
        String text = obj.toString();
        int begin = start == null ? 0 : Math.max(0, start - 1);
        if (begin >= text.length()) {
            return "";
        }
        if (len == null) {
            return text.substring(begin);
        }
        int end = Math.min(text.length(), begin + Math.max(0, len));
        return text.substring(begin, end);
    }

    public static Integer sizeof(Object obj) {
        return lengthOf(obj);
    }

    public static Integer copyCastToInteger(Object src) {
        return null;
    }
    public static Double copyCastToDouble(Object src) {
        return null;
    }
    public static String copyCastToString(Object src) {
        return null;
    }
    public static <T> T copyCast(Object src, T target) {
        return null;
    }

    public static BigDecimal bigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
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
        String text = value.toString().trim();
        return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    public static BigInteger bigIntegerValue(Object value) {
        if (value == null) {
            return BigInteger.ZERO;
        }
        if (value instanceof BigInteger integer) {
            return integer;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toBigInteger();
        }
        if (value instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        }
        String text = value.toString().trim();
        return text.isEmpty() ? BigInteger.ZERO : new BigDecimal(text).toBigInteger();
    }

    public static BigDecimal bigDecimalAdd(Object left, Object right) {
        return bigDecimalValue(left).add(bigDecimalValue(right));
    }

    public static BigDecimal bigDecimalSubtract(Object left, Object right) {
        return bigDecimalValue(left).subtract(bigDecimalValue(right));
    }

    public static BigDecimal bigDecimalMultiply(Object left, Object right) {
        return bigDecimalValue(left).multiply(bigDecimalValue(right));
    }

    public static BigDecimal bigDecimalDivide(Object left, Object right) {
        return bigDecimalValue(left).divide(bigDecimalValue(right), MathContext.DECIMAL128);
    }

    public static BigDecimal bigDecimalRemainder(Object left, Object right) {
        return bigDecimalValue(left).remainder(bigDecimalValue(right));
    }

    public static BigDecimal bigDecimalNegate(Object value) {
        return bigDecimalValue(value).negate();
    }

    public static BigInteger bigIntegerAdd(Object left, Object right) {
        return bigIntegerValue(left).add(bigIntegerValue(right));
    }

    public static BigInteger bigIntegerSubtract(Object left, Object right) {
        return bigIntegerValue(left).subtract(bigIntegerValue(right));
    }

    public static BigInteger bigIntegerMultiply(Object left, Object right) {
        return bigIntegerValue(left).multiply(bigIntegerValue(right));
    }

    public static BigInteger bigIntegerDivide(Object left, Object right) {
        return bigIntegerValue(left).divide(bigIntegerValue(right));
    }

    public static BigInteger bigIntegerRemainder(Object left, Object right) {
        return bigIntegerValue(left).remainder(bigIntegerValue(right));
    }

    public static BigInteger bigIntegerNegate(Object value) {
        return bigIntegerValue(value).negate();
    }

    public static int lengthOf(Object obj) {
        return obj == null ? 0 : obj.toString().length();
    }
    public static int compare(Object subvalue, CobolConstant constant) {
        if (constant == null) {
            return compare(subvalue, (Object) null);
        }
        return compare(subvalue, cobolConstantValue(constant));
    }

    public static int compare(Object subvalue, Object cSaDrwByPsbk) {
        if (subvalue == cSaDrwByPsbk) {
            return 0;
        }
        if (subvalue == null) {
            return -1;
        }
        if (cSaDrwByPsbk == null) {
            return 1;
        }
        if (isNumeric(subvalue) && isNumeric(cSaDrwByPsbk)) {
            return new java.math.BigDecimal(subvalue.toString().trim())
                    .compareTo(new java.math.BigDecimal(cSaDrwByPsbk.toString().trim()));
        }
        return subvalue.toString().compareTo(cSaDrwByPsbk.toString());
    }

    private static Object cobolConstantValue(CobolConstant constant) {
        return switch (constant) {
            case SPACE, SPACES -> " ";
            case ZERO, ZEROS, ZEROES -> "0";
            case QUOTE, QUOTES -> "\"";
            case NULL, NULLS -> null;
            default -> constant.name();
        };
    }

    public static boolean equalsTo(Object left, Object right) {
        return false;
    }

    public static <T> T copyObject(Object src, T target, Integer start, Integer length) {
        return null;
    }

    public static String substring(Object wsStringA, int len) {
        return subvalue(wsStringA, 0, len);
    }

    /**
     * Cast from to castTo, return the result.
     * @param castTo
     * @param from
     * @return
     * @param <T>
     */
    public static <T> T cast(T castTo, Object from) {
        return null;
    }

    /**
     * Copy the same name property value from src to target
     * @param src
     * @param target
     * @param <T>
     */
    public static <T, U> U copySameField(T src, U target) {
        return target;
    }

    /**
     * Init a string by annotation
     * FIXME
     * @param str
     * @return
     */
    public static String copyInitString(boolean isAll, String str) {
        return str;
    }

    
    public static void setFullArray(String[] target, String value) {
        if (target == null) {
            return;
        }
        Arrays.fill(target, value);
    }
    
    public static boolean isNumeric(Object value) {
        if (value == null) {
            return false;
        }
        String text = value.toString();
        if (text.isEmpty()) {
            return false;
        }
        return text.matches("[+-]?\\d+(\\.\\d+)?");
    }
}
