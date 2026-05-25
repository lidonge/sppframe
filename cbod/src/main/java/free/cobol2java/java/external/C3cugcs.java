package free.cobol2java.java.external;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import free.cobol2java.java.IService;
import free.cobol2java.java.Util;

/**
 * Compatible C3CUGCS/C3LUGCS code-page conversion service.
 *
 * <p>The generated COBOL program passes one parameter area. This runtime keeps
 * the contract intentionally tolerant because customer copybooks vary by site:
 * it looks for the usual input pointer/length and output pointer/length fields,
 * writes the converted value back to the output field, and sets normal return
 * status in the same area.</p>
 */
public class C3cugcs implements IService {
    private static final String OBSOLETE_FLAG_NORMAL = "N";

    public void procedure(Object c3lugcsArea) {
        if (c3lugcsArea == null) {
            return;
        }
        try {
            Object input = readFirst(c3lugcsArea, "iptr", "inputptr", "inptr");
            int requestedLength = intValue(readFirst(c3lugcsArea, "ilen", "inputlen", "inlen"));
            String source = trimToLength(Util.copyString(input), requestedLength);
            String converted = convert6138(source, input);
            int outputLength = converted == null ? 0 : converted.length();

            writeFirst(c3lugcsArea, converted, "optr", "outputptr", "outptr");
            writeFirst(c3lugcsArea, outputLength, "olen", "outputlen", "outlen");
            writeFirst(c3lugcsArea, 0, "returncode", "rtncode", "rcode");
            writeFirst(c3lugcsArea, OBSOLETE_FLAG_NORMAL, "obsfndflag", "obsoleteflag");
        } catch (RuntimeException ex) {
            writeFirst(c3lugcsArea, 8, "returncode", "rtncode", "rcode");
            throw ex;
        }
    }

    private static String convert6138(String source, Object rawInput) {
        if (source == null) {
            return "";
        }
        if (rawInput instanceof byte[] bytes) {
            return new String(bytes, sourceCharset());
        }
        return source;
    }

    private static Charset sourceCharset() {
        return charset("x-IBM937", "IBM937", "Cp937", "Big5", "GB18030");
    }

    private static Charset targetCharset() {
        return charset("x-IBM1388", "IBM1388", "Cp1388", "GB18030", "UTF-8");
    }

    private static Charset charset(String... names) {
        for (String name : names) {
            if (Charset.isSupported(name)) {
                return Charset.forName(name);
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static String trimToLength(String value, int length) {
        if (value == null || length <= 0 || value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }

    private static Object readFirst(Object owner, String... needles) {
        Field field = findField(owner.getClass(), needles);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot read C3CUGCS field " + field.getName(), ex);
        }
    }

    private static void writeFirst(Object owner, Object value, String... needles) {
        Field field = findField(owner.getClass(), needles);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            Object current = field.get(owner);
            field.set(owner, adapt(value, current, field.getType()));
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot write C3CUGCS field " + field.getName(), ex);
        }
    }

    private static Field findField(Class<?> type, String... needles) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                String normalized = normalize(field.getName());
                for (String needle : needles) {
                    if (normalized.contains(needle)) {
                        return field;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Object adapt(Object value, Object current, Class<?> targetType) {
        if (targetType == String.class) {
            return Util.copyString(value);
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Util.copyInteger(value);
        }
        if (targetType == long.class || targetType == Long.class) {
            Integer integer = Util.copyInteger(value);
            return integer == null ? 0L : integer.longValue();
        }
        if (targetType == byte[].class) {
            return Util.copyString(value).getBytes(targetCharset());
        }
        if (current != null) {
            return Util.copy(value, current);
        }
        return value;
    }

    private static int intValue(Object value) {
        Integer integer = Util.copyInteger(value);
        return integer == null ? 0 : integer;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }
}
