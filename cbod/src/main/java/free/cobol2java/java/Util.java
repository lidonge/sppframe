package free.cobol2java.java;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import free.cobol2java.java.redefines.AbstractCobolRedefines;

/**
 * Runtime helpers used by generated COBOL-to-Java programs.
 */
public class Util {
    private static final String ALL_MARKER_PREFIX = "\u0000ALL:";

    public static void output(Object... args) {
        if (args == null || args.length == 0) {
            System.out.println();
            return;
        }
        for(int i = 0;i<args.length;i++)
            System.out.print(args[i]);
        System.out.println();
    }

    public static String Input() {
        return new Scanner(System.in).nextLine();
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    public static String nowDate() {
        return now();
    }

    public static String nowDay() {
        return now();
    }

    public static String nowTime() {
        return now();
    }

    public static Object init(Class typeClass, int precision, int scale) {
        if (typeClass == null) {
            return null;
        }
        if (precision < 0 || scale < 0) {
            throw new IllegalArgumentException("Init precision/scale must be >= 0");
        }
        if (typeClass == String.class) {
            return precision > 0 ? initString(true, precision, " ") : " ";
        }
        if (typeClass == Integer.class || typeClass == Integer.TYPE) {
            return 0;
        }
        if (typeClass == Long.class || typeClass == Long.TYPE) {
            return 0L;
        }
        if (typeClass == Short.class || typeClass == Short.TYPE) {
            return (short) 0;
        }
        if (typeClass == Byte.class || typeClass == Byte.TYPE) {
            return (byte) 0;
        }
        if (typeClass == Double.class || typeClass == Double.TYPE) {
            return 0d;
        }
        if (typeClass == Float.class || typeClass == Float.TYPE) {
            return 0f;
        }
        if (typeClass == BigDecimal.class) {
            return BigDecimal.ZERO.setScale(Math.max(scale, 0), RoundingMode.DOWN);
        }
        if (typeClass == BigInteger.class) {
            return BigInteger.ZERO;
        }
        if (typeClass == Boolean.class || typeClass == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (typeClass.isArray()) {
            return Array.newInstance(typeClass.getComponentType(), 0);
        }
        Object instance = instantiate(typeClass);
        if (instance != null) {
            initializeObject(instance);
        }
        return instance;
    }

    public static Object initValue(Class typeClass, int precision, int scale, Object value) {
        return initValue(typeClass, precision, scale, false, value);
    }

    public static Object initValue(Class typeClass, int precision, int scale, boolean isAll, Object value) {
        if (typeClass == null) {
            return null;
        }
        if (precision < 0 || scale < 0) {
            throw new IllegalArgumentException("Init value precision/scale must be >= 0");
        }

        Object normalizedValue = normalizeInitValue(value);
        if (normalizedValue == null) {
            return null;
        }
        if (typeClass == String.class) {
            String text = normalizeInitValueString(normalizedValue);
            boolean expand = isAll || shouldExpandInitValue(value, normalizedValue);
            return precision > 0 ? initString(expand, precision, text) : text;
        }
        if (typeClass == Integer.class || typeClass == Integer.TYPE) {
            return copyInteger(normalizedValue);
        }
        if (typeClass == Long.class || typeClass == Long.TYPE) {
            return copyLong(normalizedValue);
        }
        if (typeClass == Short.class || typeClass == Short.TYPE) {
            return copyShort(normalizedValue);
        }
        if (typeClass == Byte.class || typeClass == Byte.TYPE) {
            return copyByte(normalizedValue);
        }
        if (typeClass == Double.class || typeClass == Double.TYPE) {
            return copyDouble(normalizedValue);
        }
        if (typeClass == Float.class || typeClass == Float.TYPE) {
            return copyFloat(normalizedValue);
        }
        if (typeClass == BigDecimal.class) {
            return bigDecimalValue(normalizedValue).setScale(Math.max(scale, 0), RoundingMode.DOWN);
        }
        if (typeClass == BigInteger.class) {
            return bigIntegerValue(normalizedValue);
        }
        if (typeClass == Boolean.class || typeClass == Boolean.TYPE) {
            return booleanValue(normalizedValue);
        }
        if (typeClass == Character.class || typeClass == Character.TYPE) {
            return characterValue(normalizedValue);
        }

        Object instance = init(typeClass, precision, scale);
        if (instance == null || typeClass.isInstance(normalizedValue)) {
            return normalizedValue;
        }
        return copy(normalizedValue, instance);
    }

    @Deprecated
    public static Object init(Class typeClass, String pictureStr) {
        if (typeClass == String.class) {
            return defaultStringForPicture(pictureStr);
        }
        PictureSpec spec = parsePicture(pictureStr);
        return init(typeClass, picturePrecision(spec), pictureScale(spec));
    }

    public static String subvalue(Object obj, int start, int len) {
        return subvalue(obj, Integer.valueOf(start), Integer.valueOf(len));
    }

    public static String subvalue(Object obj, Integer start, Integer len) {
        if (obj == null) {
            return null;
        }
        String text = copyString(obj);
        if (text == null) {
            return null;
        }
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

    public static Integer copyInteger(Object src) {
        if (src == null) {
            return null;
        }
        if (src instanceof Integer value) {
            return value;
        }
        if (src instanceof Number number) {
            return number.intValue();
        }
        String text = normalizeNumericText(src);
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return new BigDecimal(text).intValue();
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static Double copyDouble(Object src) {
        if (src == null) {
            return null;
        }
        if (src instanceof Double value) {
            return value;
        }
        if (src instanceof Number number) {
            return number.doubleValue();
        }
        String text = normalizeNumericText(src);
        if (text == null || text.isEmpty()) {
            return 0d;
        }
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException ex) {
            return 0d;
        }
    }

    public static String copyString(Object src) {
        if (src == null) {
            return null;
        }
        if (src instanceof String value) {
            return value;
        }
        if (src instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (src instanceof BigInteger integer) {
            return integer.toString();
        }
        if (src instanceof Number || src instanceof Boolean || src instanceof Character || src instanceof Enum<?>) {
            return src.toString();
        }
        if (src instanceof AbstractCobolRedefines<?> redef) {
            return new String(redef.getBytes(), StandardCharsets.UTF_8);
        }
        if (src.getClass().isArray()) {
            return renderArray(src, null, new IdentityHashMap<>());
        }
        return renderGroup(src, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(Object src, T target) {
        if (target == null) {
            return (T) src;
        }
        if (target instanceof String) {
            return (T) copyString(src);
        }
        if (target instanceof Integer) {
            return (T) copyInteger(src);
        }
        if (target instanceof Long) {
            return (T) copyLong(src);
        }
        if (target instanceof Short) {
            return (T) copyShort(src);
        }
        if (target instanceof Byte) {
            return (T) copyByte(src);
        }
        if (target instanceof Double) {
            return (T) copyDouble(src);
        }
        if (target instanceof Float) {
            return (T) copyFloat(src);
        }
        if (target instanceof BigDecimal) {
            return (T) bigDecimalValue(src);
        }
        if (target instanceof BigInteger) {
            return (T) bigIntegerValue(src);
        }
        if (src == null) {
            return target;
        }
        if (target.getClass().isInstance(src)) {
            return (T) src;
        }
        return copySameField((Object) src, target);
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
        String text = normalizeNumericText(value);
        if (text == null || text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
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
        String text = normalizeNumericText(value);
        if (text == null || text.isEmpty()) {
            return BigInteger.ZERO;
        }
        try {
            return new BigDecimal(text).toBigInteger();
        } catch (NumberFormatException ex) {
            return BigInteger.ZERO;
        }
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
        if (obj == null) {
            return 0;
        }
        if (obj instanceof AbstractCobolRedefines<?> redef) {
            return redef.getBytes().length;
        }
        if (obj instanceof String value) {
            return value.length();
        }
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character || obj instanceof Enum<?>) {
            return copyString(obj).length();
        }
        if (obj.getClass().isArray()) {
            return renderArray(obj, null, new IdentityHashMap<>()).length();
        }
        return renderGroup(obj, new IdentityHashMap<>()).length();
    }

    public static int compare(Object left, CobolConstant constant) {
        if (constant == null) {
            return compare(left, (Object) null);
        }
        return compare(left, cobolConstantValue(constant));
    }

    public static int compare(Object left, Object right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }

        if (left instanceof CobolConstant constant) {
            left = cobolConstantValue(constant);
        }
        if (right instanceof CobolConstant constant) {
            right = cobolConstantValue(constant);
        }

        if (isAllMarker(left) || isAllMarker(right)) {
            return compareWithAllMarker(left, right);
        }

        if (isNumeric(left) && isNumeric(right)) {
            return bigDecimalValue(left).compareTo(bigDecimalValue(right));
        }

        String leftText = copyString(left);
        String rightText = copyString(right);
        if (leftText == null) {
            return rightText == null ? 0 : -1;
        }
        if (rightText == null) {
            return 1;
        }
        int maxLength = Math.max(leftText.length(), rightText.length());
        return padRight(leftText, maxLength, ' ').compareTo(padRight(rightText, maxLength, ' '));
    }

    public static boolean equalsTo(Object left, Object right) {
        return compare(left, right) == 0;
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(Object src, T target, Integer start, Integer length) {
        String sourceText = copyString(src);
        if (sourceText == null) {
            sourceText = "";
        }
        String targetText = copyString(target);
        if (targetText == null) {
            targetText = "";
        }

        int begin = start == null ? 0 : Math.max(0, start - 1);
        int copyLength = length == null ? sourceText.length() : Math.max(0, length);
        StringBuilder builder = new StringBuilder(targetText);
        while (builder.length() < begin) {
            builder.append(' ');
        }
        while (builder.length() < begin + copyLength) {
            builder.append(' ');
        }
        String chunk = padRight(sourceText, copyLength, ' ');
        builder.replace(begin, begin + copyLength, chunk.substring(0, copyLength));

        if (target == null || target instanceof String) {
            return (T) builder.toString();
        }
        return copy(builder.toString(), target);
    }

    public static <T> T cast(Class<T> castTo, Object from) {
        if (castTo == null || from == null) {
            return null;
        }
        return castTo.cast(from);
    }

    public static String substring(Object wsStringA, int len) {
        return subvalue(wsStringA, 1, len);
    }

    private static <T, U> U copySameField(T src, U target) {
        if (src == null || target == null) {
            return target;
        }

        Map<String, Field> sourceFields = fieldsByName(src.getClass());
        for (Field targetField : target.getClass().getDeclaredFields()) {
            if (skipField(targetField)) {
                continue;
            }
            Field sourceField = sourceFields.get(targetField.getName());
            if (sourceField == null || skipField(sourceField)) {
                continue;
            }
            try {
                sourceField.setAccessible(true);
                targetField.setAccessible(true);
                Object sourceValue = sourceField.get(src);
                Object targetValue = targetField.get(target);

                if (sourceValue == null && targetValue == null) {
                    continue;
                }

                if (targetValue == null && !isSimpleType(targetField.getType()) && !targetField.getType().isArray()) {
                    targetValue = instantiate(targetField.getType());
                    if (targetValue != null) {
                        initializeObject(targetValue);
                        targetField.set(target, targetValue);
                    }
                }

                Object copied;
                if (targetField.getType().isArray()) {
                    copied = copyArrayValue(sourceValue, targetValue, targetField);
                } else if (isSimpleType(targetField.getType()) || targetValue == null) {
                    copied = copySimpleValue(sourceValue, targetValue, targetField.getType());
                } else {
                    copied = copySameField(sourceValue, targetValue);
                }
                if (copied != null || !targetField.getType().isPrimitive()) {
                    targetField.set(target, copied);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return target;
    }

    public static String initString(boolean isAll, Integer precision, String str) {
        if (precision == null || precision < 0) {
            throw new IllegalArgumentException("String init precision is required");
        }
        String base = str == null ? " " : str;
        if (precision == 0) {
            return "";
        }
        if (isAll) {
            if (base.isEmpty()) {
                return repeat(' ', precision);
            }
            return repeatPattern(base, precision);
        }
        return fitToPrecision(base, precision);
    }

    public static String allString(String str) {
        String base = str == null ? " " : str;
        if (base.isEmpty()) {
            return "";
        }
        return ALL_MARKER_PREFIX + base.charAt(0);
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
        if (value instanceof Number) {
            return true;
        }
        String text = normalizeNumericText(value);
        return text != null && text.matches("[+-]?\\d+(\\.\\d+)?");
    }

    public static boolean isAlphabetic(Object value) {
        if (value == null) {
            return false;
        }
        String text = copyString(value);
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isLetter(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAlphabeticLower(Object value) {
        if (!isAlphabetic(value)) {
            return false;
        }
        String text = copyString(value);
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isLowerCase(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAlphabeticUpper(Object value) {
        if (!isAlphabetic(value)) {
            return false;
        }
        String text = copyString(value);
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isUpperCase(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static Object cobolConstantValue(CobolConstant constant) {
        return switch (constant) {
            case SPACE, SPACES -> " ";
            case ZERO, ZEROS, ZEROES -> "0";
            case QUOTE, QUOTES -> "\"";
            case LOW_VALUE, LOW_VALUES -> "\0";
            case HIGH_VALUE, HIGH_VALUES -> String.valueOf(Character.MAX_VALUE);
            case NULL, NULLS -> null;
            default -> constant.name();
        };
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

    public static Long copyLong(Object src) {
        if (src == null) {
            return null;
        }
        if (src instanceof Long value) {
            return value;
        }
        if (src instanceof Number number) {
            return number.longValue();
        }
        String text = normalizeNumericText(src);
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        try {
            return new BigDecimal(text).longValue();
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public static Short copyShort(Object src) {
        if (src == null) {
            return null;
        }
        if (src instanceof Short value) {
            return value;
        }
        if (src instanceof Number number) {
            return number.shortValue();
        }
        String text = normalizeNumericText(src);
        if (text == null || text.isEmpty()) {
            return (short) 0;
        }
        try {
            return new BigDecimal(text).shortValue();
        } catch (NumberFormatException ex) {
            return (short) 0;
        }
    }

    public static Byte copyByte(Object src) {
        if (src == null) {
            return null;
        }
        if (src instanceof Byte value) {
            return value;
        }
        if (src instanceof Number number) {
            return number.byteValue();
        }
        String text = normalizeNumericText(src);
        if (text == null || text.isEmpty()) {
            return (byte) 0;
        }
        try {
            return new BigDecimal(text).byteValue();
        } catch (NumberFormatException ex) {
            return (byte) 0;
        }
    }

    public static Float copyFloat(Object src) {
        if (src == null) {
            return null;
        }
        if (src instanceof Float value) {
            return value;
        }
        if (src instanceof Number number) {
            return number.floatValue();
        }
        String text = normalizeNumericText(src);
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        try {
            return Float.valueOf(text);
        } catch (NumberFormatException ex) {
            return 0f;
        }
    }

    private static void initializeObject(Object instance) {
        if (instance == null || isSimpleType(instance.getClass()) || instance instanceof AbstractCobolRedefines<?>) {
            return;
        }
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (skipField(field)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                Object currentValue = field.get(instance);

                if (fieldType == String.class && currentValue == null) {
                    field.set(instance, defaultStringForField(field));
                    continue;
                }

                if (isNumericWrapper(fieldType) && currentValue == null) {
                    field.set(instance, defaultNumericValue(fieldType));
                    continue;
                }

                if (fieldType == BigDecimal.class && currentValue == null) {
                    field.set(instance, BigDecimal.ZERO);
                    continue;
                }

                if (fieldType == BigInteger.class && currentValue == null) {
                    field.set(instance, BigInteger.ZERO);
                    continue;
                }

                if (fieldType.isArray()) {
                    initializeArrayField(instance, field, currentValue);
                    continue;
                }

                if (fieldType == byte[].class) {
                    continue;
                }

                if (AbstractCobolRedefines.class.isAssignableFrom(fieldType)) {
                    continue;
                }

                if (currentValue == null) {
                    currentValue = instantiate(fieldType);
                    if (currentValue != null) {
                        field.set(instance, currentValue);
                    }
                }
                if (currentValue != null) {
                    initializeObject(currentValue);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private static void initializeArrayField(Object instance, Field field, Object currentValue) throws IllegalAccessException {
        Class<?> componentType = field.getType().getComponentType();
        if (currentValue == null) {
            return;
        }
        if (componentType == String.class) {
            String defaultValue = defaultStringForField(field);
            setFullArray((String[]) currentValue, defaultValue);
            return;
        }
        if (componentType.isPrimitive() || componentType == byte.class) {
            return;
        }
        if (isNumericWrapper(componentType)) {
            fillNullArrayElements(currentValue, defaultNumericValue(componentType));
            return;
        }
        if (componentType == BigDecimal.class) {
            fillNullArrayElements(currentValue, BigDecimal.ZERO);
            return;
        }
        if (componentType == BigInteger.class) {
            fillNullArrayElements(currentValue, BigInteger.ZERO);
            return;
        }
        for (int i = 0; i < Array.getLength(currentValue); i++) {
            Object element = Array.get(currentValue, i);
            if (element == null) {
                element = instantiate(componentType);
                if (element != null) {
                    initializeObject(element);
                    Array.set(currentValue, i, element);
                }
            } else {
                initializeObject(element);
            }
        }
    }

    private static void fillNullArrayElements(Object array, Object defaultValue) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < Array.getLength(array); i++) {
            if (Array.get(array, i) == null) {
                Array.set(array, i, defaultValue);
            }
        }
    }

    private static Object instantiate(Class<?> typeClass) {
        try {
            Constructor<?> constructor = typeClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isSimpleType(Class<?> typeClass) {
        return typeClass == null
                || typeClass.isPrimitive()
                || typeClass == String.class
                || isNumericWrapper(typeClass)
                || typeClass == BigDecimal.class
                || typeClass == BigInteger.class
                || typeClass == Boolean.class
                || typeClass == Character.class
                || typeClass.isEnum()
                || AbstractCobolRedefines.class.isAssignableFrom(typeClass);
    }

    private static boolean isNumericWrapper(Class<?> typeClass) {
        return typeClass == Integer.class || typeClass == Long.class || typeClass == Short.class
                || typeClass == Byte.class || typeClass == Double.class || typeClass == Float.class;
    }

    private static Object defaultNumericValue(Class<?> typeClass) {
        if (typeClass == Integer.class || typeClass == Integer.TYPE) {
            return 0;
        }
        if (typeClass == Long.class || typeClass == Long.TYPE) {
            return 0L;
        }
        if (typeClass == Short.class || typeClass == Short.TYPE) {
            return (short) 0;
        }
        if (typeClass == Byte.class || typeClass == Byte.TYPE) {
            return (byte) 0;
        }
        if (typeClass == Double.class || typeClass == Double.TYPE) {
            return 0d;
        }
        if (typeClass == Float.class || typeClass == Float.TYPE) {
            return 0f;
        }
        return null;
    }

    private static String defaultStringForField(Field field) {
        FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
        if (fieldInfo == null) {
            return " ";
        }
        return defaultStringForPicture(fieldInfo.cobolType());
    }

    private static String defaultStringForPicture(String picture) {
        PictureSpec spec = parsePicture(picture);
        if (spec == null) {
            return " ";
        }
        if (spec.alphaLength > 0) {
            return (String) init(String.class, spec.alphaLength, 0);
        }
        if (spec.numericDigits > 0) {
            return initString(true, spec.numericDigits, "0");
        }
        return " ";
    }

    private static String renderGroup(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            return "";
        }
        if (visited.containsKey(value)) {
            return "";
        }
        visited.put(value, Boolean.TRUE);

        if (value instanceof AbstractCobolRedefines<?> redef) {
            return new String(redef.getBytes(), StandardCharsets.UTF_8);
        }
        if (isSimpleType(value.getClass())) {
            return copyStringWithoutGroup(value);
        }
        if (value.getClass().isArray()) {
            return renderArray(value, null, visited);
        }

        StringBuilder builder = new StringBuilder();
        Map<String, Boolean> renderedStorages = new LinkedHashMap<>();
        for (Field field : value.getClass().getDeclaredFields()) {
            if (skipField(field)) {
                continue;
            }
            field.setAccessible(true);
            try {
                if (isStorageField(field)) {
                    String storageKey = storageKey(field.getName());
                    if (!renderedStorages.containsKey(storageKey)) {
                        byte[] storage = (byte[]) field.get(value);
                        builder.append(storage == null ? "" : new String(storage, StandardCharsets.UTF_8));
                        renderedStorages.put(storageKey, Boolean.TRUE);
                    }
                    continue;
                }

                if (isRedefinesViewField(field, renderedStorages)) {
                    continue;
                }

                Object fieldValue = field.get(value);
                builder.append(renderFieldValue(field, fieldValue, visited));
            } catch (IllegalAccessException ignored) {
            }
        }
        return builder.toString();
    }

    private static String renderArray(Object value, Field field, IdentityHashMap<Object, Boolean> visited) {
        int length = Array.getLength(value);
        StringBuilder builder = new StringBuilder();
        Class<?> componentType = value.getClass().getComponentType();
        for (int i = 0; i < length; i++) {
            Object element = Array.get(value, i);
            if (element == null) {
                if (field != null && field.getAnnotation(FieldInfo.class) != null) {
                    builder.append(defaultStringForField(field));
                } else if (!isSimpleType(componentType) && !componentType.isPrimitive()) {
                    builder.append(renderDefaultComplexType(componentType));
                }
            } else if (isSimpleType(componentType) || componentType.isPrimitive()) {
                builder.append(copyStringWithoutGroup(element));
            } else {
                builder.append(renderGroup(element, visited));
            }
        }
        return builder.toString();
    }

    private static String renderFieldValue(Field field, Object fieldValue, IdentityHashMap<Object, Boolean> visited) {
        FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
        if (field.getType().isArray()) {
            if (fieldValue == null) {
                return "";
            }
            return renderArray(fieldValue, field, visited);
        }
        if (fieldInfo != null && (field.getType() == String.class || isNumericWrapper(field.getType())
                || field.getType() == BigDecimal.class || field.getType() == BigInteger.class
                || field.getType().isPrimitive())) {
            return renderSimpleField(fieldValue, fieldInfo);
        }
        if (fieldValue == null) {
            if (fieldInfo != null) {
                return defaultStringForPicture(fieldInfo.cobolType());
            }
            return renderDefaultComplexType(field.getType());
        }
        if (isSimpleType(field.getType()) || field.getType().isPrimitive()) {
            return copyStringWithoutGroup(fieldValue);
        }
        return renderGroup(fieldValue, visited);
    }

    private static String renderDefaultComplexType(Class<?> fieldType) {
        if (fieldType == null || isSimpleType(fieldType) || fieldType.isPrimitive() || fieldType.isArray()) {
            return "";
        }
        Object instance = instantiate(fieldType);
        if (instance == null) {
            return "";
        }
        initializeObject(instance);
        return renderGroup(instance, new IdentityHashMap<>());
    }

    private static String renderSimpleField(Object value, FieldInfo fieldInfo) {
        PictureSpec spec = parsePicture(fieldInfo.cobolType());
        if (spec == null) {
            return value == null ? "" : copyStringWithoutGroup(value);
        }
        if (spec.alphaLength > 0) {
            return padRight(value == null ? "" : copyStringWithoutGroup(value), spec.alphaLength, ' ');
        }
        if (spec.numericDigits > 0) {
            if (value == null) {
                return repeat('0', spec.numericDigits);
            }
            BigDecimal numericValue = bigDecimalValue(value);
            boolean negative = numericValue.signum() < 0;
            BigDecimal scaled = numericValue.movePointRight(spec.scale).abs();
            String digits = scaled.setScale(0, RoundingMode.DOWN).toPlainString();
            digits = digits.replace("-", "");
            digits = padLeft(digits, spec.numericDigits, '0');
            if (negative && spec.signed && !digits.isEmpty()) {
                return "-" + digits.substring(1);
            }
            return digits;
        }
        return value == null ? "" : copyStringWithoutGroup(value);
    }

    private static boolean skipField(Field field) {
        return field.isSynthetic()
                || Modifier.isStatic(field.getModifiers())
                || field.getName().startsWith("this$")
                || field.getName().contains("$");
    }

    private static boolean isStorageField(Field field) {
        return field.getType() == byte[].class
                && (field.getName().endsWith("_redefinesStorage") || field.getName().endsWith("_pointerStorage"));
    }

    private static boolean isRedefinesViewField(Field field, Map<String, Boolean> renderedStorages) {
        if (!AbstractCobolRedefines.class.isAssignableFrom(field.getType())) {
            return false;
        }
        for (String storageKey : renderedStorages.keySet()) {
            if (field.getName().startsWith(storageKey)) {
                return true;
            }
        }
        return false;
    }

    private static String storageKey(String fieldName) {
        if (fieldName.endsWith("_redefinesStorage")) {
            return fieldName.substring(0, fieldName.length() - "_redefinesStorage".length());
        }
        if (fieldName.endsWith("_pointerStorage")) {
            return fieldName.substring(0, fieldName.length() - "_pointerStorage".length());
        }
        return fieldName;
    }

    private static Map<String, Field> fieldsByName(Class<?> type) {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (!skipField(field)) {
                fields.put(field.getName(), field);
            }
        }
        return fields;
    }

    private static Object copyArrayValue(Object sourceValue, Object targetValue, Field targetField) {
        if (sourceValue == null) {
            return targetValue;
        }
        int length = Array.getLength(sourceValue);
        Object targetArray = targetValue;
        if (targetArray == null || Array.getLength(targetArray) != length) {
            targetArray = Array.newInstance(targetField.getType().getComponentType(), length);
        }
        Class<?> componentType = targetField.getType().getComponentType();
        for (int i = 0; i < length; i++) {
            Object sourceElement = Array.get(sourceValue, i);
            Object targetElement = Array.get(targetArray, i);
            if (sourceElement == null) {
                continue;
            }
            if (isSimpleType(componentType) || componentType.isPrimitive()) {
                Array.set(targetArray, i, copySimpleValue(sourceElement, targetElement, componentType));
            } else {
                if (targetElement == null) {
                    targetElement = instantiate(componentType);
                    if (targetElement != null) {
                        initializeObject(targetElement);
                    }
                }
                Array.set(targetArray, i, copySameField(sourceElement, targetElement));
            }
        }
        return targetArray;
    }

    private static Object copySimpleValue(Object sourceValue, Object targetValue, Class<?> targetType) {
        if (targetType == String.class) {
            return copyString(sourceValue);
        }
        if (targetType == Integer.class || targetType == Integer.TYPE) {
            return copyInteger(sourceValue);
        }
        if (targetType == Long.class || targetType == Long.TYPE) {
            return copyLong(sourceValue);
        }
        if (targetType == Short.class || targetType == Short.TYPE) {
            return copyShort(sourceValue);
        }
        if (targetType == Byte.class || targetType == Byte.TYPE) {
            return copyByte(sourceValue);
        }
        if (targetType == Double.class || targetType == Double.TYPE) {
            return copyDouble(sourceValue);
        }
        if (targetType == Float.class || targetType == Float.TYPE) {
            return copyFloat(sourceValue);
        }
        if (targetType == BigDecimal.class) {
            return bigDecimalValue(sourceValue);
        }
        if (targetType == BigInteger.class) {
            return bigIntegerValue(sourceValue);
        }
        return copy(sourceValue, targetValue);
    }

    private static boolean isAllMarker(Object value) {
        return value instanceof String str && str.startsWith(ALL_MARKER_PREFIX) && str.length() > ALL_MARKER_PREFIX.length();
    }

    private static int compareWithAllMarker(Object left, Object right) {
        if (isAllMarker(left) && isAllMarker(right)) {
            return Objects.equals(left, right) ? 0 : left.toString().compareTo(right.toString());
        }

        if (isAllMarker(left)) {
            return compare(expandAllMarker((String) left, lengthForComparison(right)), right);
        }
        return compare(left, expandAllMarker((String) right, lengthForComparison(left)));
    }

    private static int lengthForComparison(Object other) {
        int length = lengthOf(other);
        return Math.max(length, 1);
    }

    private static String expandAllMarker(String marker, int length) {
        return repeat(marker.charAt(ALL_MARKER_PREFIX.length()), length);
    }

    private static String repeatPattern(String pattern, int length) {
        if (pattern == null || pattern.isEmpty() || length <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(length);
        while (builder.length() < length) {
            builder.append(pattern);
        }
        return builder.substring(0, length);
    }

    private static String fitToPrecision(String text, int precision) {
        String actual = text == null ? "" : text;
        if (actual.length() <= precision) {
            return actual;
        }
        return actual.substring(0, precision);
    }

    private static Object normalizeInitValue(Object value) {
        if (value instanceof CobolConstant constant) {
            return cobolConstantValue(constant);
        }
        return value;
    }

    private static boolean shouldExpandInitValue(Object rawValue, Object normalizedValue) {
        if (rawValue instanceof CobolConstant constant) {
            return switch (constant) {
                case SPACE, SPACES, ZERO, ZEROS, ZEROES, QUOTE, QUOTES, LOW_VALUE, LOW_VALUES, HIGH_VALUE, HIGH_VALUES -> true;
                default -> false;
            };
        }
        return normalizedValue instanceof String str && str.startsWith(ALL_MARKER_PREFIX);
    }

    private static String normalizeInitValueString(Object value) {
        if (value == null) {
            return null;
        }
        String text = copyStringWithoutGroup(value);
        if (text != null && text.startsWith(ALL_MARKER_PREFIX)) {
            return text.substring(ALL_MARKER_PREFIX.length());
        }
        return text;
    }

    private static Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = copyStringWithoutGroup(value);
        if (text == null || text.isBlank()) {
            return Boolean.FALSE;
        }
        String normalized = text.trim();
        if ("1".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("0".equals(normalized)) {
            return Boolean.FALSE;
        }
        return Boolean.parseBoolean(normalized);
    }

    private static Character characterValue(Object value) {
        if (value instanceof Character character) {
            return character;
        }
        String text = copyStringWithoutGroup(value);
        if (text == null || text.isEmpty()) {
            return '\0';
        }
        return text.charAt(0);
    }

    private static int picturePrecision(PictureSpec spec) {
        if (spec == null) {
            return 0;
        }
        if (spec.alphaLength > 0) {
            return spec.alphaLength;
        }
        return Math.max(spec.numericDigits, 0);
    }

    private static int pictureScale(PictureSpec spec) {
        return spec == null ? 0 : Math.max(spec.scale, 0);
    }

    private static String padRight(String text, int length, char fill) {
        String actual = text == null ? "" : text;
        if (actual.length() >= length) {
            return actual.substring(0, length);
        }
        return actual + repeat(fill, length - actual.length());
    }

    private static String padLeft(String text, int length, char fill) {
        String actual = text == null ? "" : text;
        if (actual.length() >= length) {
            return actual.substring(actual.length() - length);
        }
        return repeat(fill, length - actual.length()) + actual;
    }

    private static String repeat(char value, int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }

    private static PictureSpec parsePicture(String picture) {
        if (picture == null || picture.isBlank()) {
            return null;
        }
        String normalized = picture.replace("PIC", "").replace("pic", "").replace(" ", "").toUpperCase();
        String alphaNormalized = normalized.replaceAll("X\\(\\d+\\)", "X");
        if (alphaNormalized.matches("X+")) {
            int alphaLength = countPictureLength(normalized, 'X');
            return new PictureSpec(alphaLength, 0, 0, false);
        }

        String numericNormalized = normalized.replaceAll("9\\(\\d+\\)", "9").replace("S", "").replace("V", "");
        if (numericNormalized.matches("9+")) {
            int numericDigits = countPictureLength(normalized, '9');
            int scale = 0;
            int vIndex = normalized.indexOf('V');
            if (vIndex >= 0) {
                scale = countPictureLength(normalized.substring(vIndex + 1), '9');
            }
            return new PictureSpec(0, numericDigits, scale, normalized.startsWith("S"));
        }

        if (normalized.chars().allMatch(ch -> ch == 'X')) {
            return new PictureSpec(normalized.length(), 0, 0, false);
        }
        return null;
    }

    private static int countPictureLength(String picture, char symbol) {
        int total = 0;
        for (int i = 0; i < picture.length(); i++) {
            char current = picture.charAt(i);
            if (current != symbol) {
                continue;
            }
            if (i + 1 < picture.length() && picture.charAt(i + 1) == '(') {
                int close = picture.indexOf(')', i + 2);
                if (close > i) {
                    total += Integer.parseInt(picture.substring(i + 2, close));
                    i = close;
                } else {
                    total++;
                }
            } else {
                total++;
            }
        }
        return total;
    }

    private record PictureSpec(int alphaLength, int numericDigits, int scale, boolean signed) {
    }
}
