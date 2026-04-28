package free.cobol2java.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory CICS runtime used by generated Java output.
 *
 * <p>This preserves the converted code structure and gives EXEC CICS
 * statements concrete runtime behavior without requiring a real CICS region.</p>
 */
public final class CicsUtil {
    private static final Map<String, Object> MAP_BUFFERS = new ConcurrentHashMap<>();
    private static final Map<String, String> TEXT_BUFFERS = new ConcurrentHashMap<>();
    private static final ThreadLocal<CicsStatus> LAST_STATUS = ThreadLocal.withInitial(CicsStatus::ok);

    private CicsUtil() {
    }

    public interface CicsMap<T> {
        String getMapName();

        String getMapSetName();

        default java.util.List<BmsFieldMeta> getFields() {
            return java.util.List.of();
        }

        T getPayload();

        void setPayload(T payload);
    }

    public static final class BmsFieldMeta {
        public final String name;
        public final Integer row;
        public final Integer column;
        public final Integer length;
        public final String initialValue;
        public final java.util.List<String> attributes;

        public BmsFieldMeta(String name, Integer row, Integer column, Integer length, String initialValue,
                            java.util.List<String> attributes) {
            this.name = name;
            this.row = row;
            this.column = column;
            this.length = length;
            this.initialValue = initialValue;
            this.attributes = attributes;
        }

        @Override
        public String toString() {
            return "BmsFieldMeta [name=" + name + ", row=" + row + ", column=" + column + ", length=" + length
                    + ", initialValue=" + initialValue + ", attributes=" + attributes + "]";
        }
        
    }

    public static void sendMap(String map, String mapset, Object from, boolean erase) {
        String key = terminalKey(map, mapset);
        if (erase) {
            MAP_BUFFERS.remove(key);
        }
        MAP_BUFFERS.put(key, deepCopy(from));
        setStatus(0, 0, null);
    }

    public static void sendMap(CicsMap<?> request, boolean erase) {
        if (request == null) {
            setStatus(16, 1, null);
            return;
        }
        sendMap(request.getMapName(), request.getMapSetName(), request.getPayload(), erase);
    }

    public static String sendText(String text, Integer length, boolean erase) {
        String value = text == null ? "" : text;
        if (length != null && length >= 0 && length < value.length()) {
            value = value.substring(0, length);
        }
        if (erase) {
            TEXT_BUFFERS.clear();
        }
        TEXT_BUFFERS.put("TEXT", value);
        setStatus(0, 0, value);
        return value;
    }

    public static void returnControl() {
        setStatus(0, 0, null);
    }

    public static int getResp() {
        return LAST_STATUS.get().resp;
    }

    public static int getResp2() {
        return LAST_STATUS.get().resp2;
    }

    public static String getReturnMsg() {
        return LAST_STATUS.get().returnMsg;
    }

    static void setStatus(int resp, int resp2) {
        setStatus(resp, resp2, null);
    }

    static void setStatus(int resp, int resp2, String returnMsg) {
        LAST_STATUS.set(new CicsStatus(resp, resp2, returnMsg));
    }

    private record CicsStatus(int resp, int resp2, String returnMsg) {
        private static CicsStatus ok() {
            return new CicsStatus(0, 0, null);
        }
    }

    private static String terminalKey(String map, String mapset) {
        return (mapset == null ? "" : mapset) + "::" + (map == null ? "" : map);
    }

    @SuppressWarnings("unchecked")
    private static <T> T deepCopy(T source) {
        if (source == null) {
            return null;
        }
        Class<?> type = source.getClass();
        if (isSimpleValue(type)) {
            return source;
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object target = constructor.newInstance();
            copyFields(source, target, type);
            return (T) target;
        } catch (Exception e) {
            return source;
        }
    }

    private static void copyFields(Object source, Object target, Class<?> type) {
        copyFields(source, target, type, true);
    }

    private static void copyFields(Object source, Object target, Class<?> type, boolean strictSourceType) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = readSourceFieldValue(source, field, strictSourceType);
                    if (value != null && !isSimpleValue(field.getType())) {
                        value = deepCopy(value);
                    }
                    field.set(target, value);
                } catch (Exception ignored) {
                }
            }
            current = current.getSuperclass();
        }
    }

    private static Object readSourceFieldValue(Object source, Field targetField, boolean strictSourceType) throws IllegalAccessException {
        if (strictSourceType) {
            return targetField.get(source);
        }
        Field sourceField = findField(source.getClass(), targetField.getName());
        if (sourceField == null) {
            return null;
        }
        sourceField.setAccessible(true);
        return sourceField.get(source);
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || Character.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type);
    }
}
