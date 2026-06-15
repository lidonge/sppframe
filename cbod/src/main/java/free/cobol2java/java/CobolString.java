package free.cobol2java.java;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import free.cobol2java.java.redefines.AbstractCobolRedefines;

public final class CobolString {
    private static final int DEFAULT_REDEFINES_STORAGE_SIZE = 4096;

    private CobolString() {
    }

    public static String value(Object value) {
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
        if (value.getClass().isArray()) {
            return renderArray(value, null, new IdentityHashMap<>());
        }
        return renderGroup(value, new IdentityHashMap<>());
    }

    public static String init(boolean isAll, int precision, String value) {
        if (precision < 0) {
            throw new IllegalArgumentException("String init precision is required");
        }
        if (precision == 0) {
            return "";
        }
        String text = value == null ? " " : value;
        if (!isAll) {
            return text.length() <= precision ? text : text.substring(0, precision);
        }
        if (text.isEmpty()) {
            return " ".repeat(precision);
        }
        StringBuilder builder = new StringBuilder(precision);
        while (builder.length() < precision) {
            builder.append(text);
        }
        return builder.substring(0, precision);
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

    public static String slice(String value, int start, int length) {
        int begin = Math.max(0, start);
        int end = begin + Math.max(0, length);
        StringBuilder builder = new StringBuilder(value == null ? "" : value);
        while (builder.length() < end) {
            builder.append(' ');
        }
        return builder.substring(begin, end);
    }

    public static String fixed(Object value, int length) {
        String text = value == null ? "" : value.toString();
        if (text.length() > length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }

    public static String replaceRange(Object target, Object source, Integer start, Integer length) {
        String targetText = value(target);
        if (targetText == null) {
            targetText = "";
        }
        String sourceText = value(source);
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
        String chunk = padRight(sourceText, replaceLength, ' ');
        builder.replace(begin, begin + replaceLength, chunk.substring(0, replaceLength));
        return builder.toString();
    }

    private static String valueWithoutGroup(Object value) {
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

    private static String renderGroup(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || visited.containsKey(value)) {
            return "";
        }
        visited.put(value, Boolean.TRUE);

        if (value instanceof AbstractCobolRedefines<?> redef) {
            return new String(redef.getBytes(), StandardCharsets.UTF_8);
        }
        if (isSimpleType(value.getClass())) {
            return valueWithoutGroup(value);
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
                builder.append(renderFieldValue(field, field.get(value), visited));
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
                builder.append(valueWithoutGroup(element));
            } else {
                builder.append(renderGroup(element, visited));
            }
        }
        return builder.toString();
    }

    private static String renderFieldValue(Field field, Object fieldValue, IdentityHashMap<Object, Boolean> visited) {
        FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
        if (field.getType().isArray()) {
            return fieldValue == null ? "" : renderArray(fieldValue, field, visited);
        }
        if (fieldInfo != null && (field.getType() == String.class || isNumericWrapper(field.getType())
                || field.getType() == BigDecimal.class || field.getType() == BigInteger.class
                || field.getType().isPrimitive())) {
            return renderSimpleField(fieldValue, fieldInfo);
        }
        if (fieldValue == null) {
            return fieldInfo == null ? renderDefaultComplexType(field.getType())
                    : defaultStringForPicture(fieldInfo.cobolType());
        }
        if (isSimpleType(field.getType()) || field.getType().isPrimitive()) {
            return valueWithoutGroup(fieldValue);
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
                if (fieldType == byte[].class || AbstractCobolRedefines.class.isAssignableFrom(fieldType)) {
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

    private static void initializeArrayField(Object instance, Field field, Object currentValue)
            throws IllegalAccessException {
        Class<?> componentType = field.getType().getComponentType();
        if (currentValue == null) {
            return;
        }
        if (componentType == String.class) {
            String defaultValue = defaultStringForField(field);
            String[] values = (String[]) currentValue;
            Arrays.fill(values, defaultValue);
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

    private static String renderSimpleField(Object value, FieldInfo fieldInfo) {
        PictureSpec spec = parsePicture(fieldInfo.cobolType());
        if (spec == null) {
            return value == null ? "" : valueWithoutGroup(value);
        }
        if (spec.alphaLength > 0) {
            return padRight(value == null ? "" : valueWithoutGroup(value), spec.alphaLength, ' ');
        }
        if (spec.numericDigits > 0) {
            if (value == null) {
                return repeat('0', spec.numericDigits);
            }
            BigDecimal numericValue = bigDecimalValue(value);
            boolean negative = numericValue.signum() < 0;
            BigDecimal scaled = numericValue.movePointRight(spec.scale).abs();
            String digits = scaled.setScale(0, RoundingMode.DOWN).toPlainString().replace("-", "");
            digits = padLeft(digits, spec.numericDigits, '0');
            if (negative && spec.signed && !digits.isEmpty()) {
                return "-" + digits.substring(1);
            }
            return digits;
        }
        return value == null ? "" : valueWithoutGroup(value);
    }

    private static Object instantiate(Class<?> typeClass) {
        try {
            Constructor<?> constructor = typeClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception ignored) {
            return instantiateRedefines(typeClass);
        }
    }

    private static Object instantiateRedefines(Class<?> typeClass) {
        if (!AbstractCobolRedefines.class.isAssignableFrom(typeClass)) {
            return null;
        }
        try {
            Constructor<?> constructor = typeClass.getDeclaredConstructor(byte[].class, int.class, int.class);
            constructor.setAccessible(true);
            byte[] storage = new byte[DEFAULT_REDEFINES_STORAGE_SIZE];
            return constructor.newInstance(storage, 0, storage.length);
        } catch (Exception ignored) {
            return null;
        }
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

    private static String defaultStringForField(Field field) {
        FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
        return fieldInfo == null ? " " : defaultStringForPicture(fieldInfo.cobolType());
    }

    private static String defaultStringForPicture(String picture) {
        PictureSpec spec = parsePicture(picture);
        if (spec == null) {
            return " ";
        }
        if (spec.alphaLength > 0) {
            return init(true, spec.alphaLength, " ");
        }
        if (spec.numericDigits > 0) {
            return init(true, spec.numericDigits, "0");
        }
        return " ";
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

    private static BigDecimal bigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(valueWithoutGroup(value).trim());
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
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
            return new PictureSpec(countPictureLength(normalized, 'X'), 0, 0, false);
        }

        String numericNormalized = normalized.replaceAll("9\\(\\d+\\)", "9").replace("S", "").replace("V", "");
        if (numericNormalized.matches("9+")) {
            int vIndex = normalized.indexOf('V');
            int scale = vIndex < 0 ? 0 : countPictureLength(normalized.substring(vIndex + 1), '9');
            return new PictureSpec(0, countPictureLength(normalized, '9'), scale, normalized.startsWith("S"));
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
