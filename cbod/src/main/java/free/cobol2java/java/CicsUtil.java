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

    private CicsUtil() {
    }

    public static final class Response<T> {
        private final int resp;
        private final int resp2;
        private final T payload;
        private final String returnMsg;

        private Response(int resp, int resp2, T payload) {
            this(resp, resp2, payload, null);
        }

        private Response(int resp, int resp2, T payload, String returnMsg) {
            this.resp = resp;
            this.resp2 = resp2;
            this.payload = payload;
            this.returnMsg = returnMsg;
        }

        public int getResp() {
            return resp;
        }

        public int getResp2() {
            return resp2;
        }

        public T getPayload() {
            return payload;
        }

        public String getReturnMsg() {
            return returnMsg;
        }

        @Override
        public String toString() {
            return "Response [resp=" + resp + ", resp2=" + resp2 + ", payload=" + payload
                    + ", returnMsg=" + returnMsg + "]";
        }

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

    public static Response<Void> sendMap(String map, String mapset, Object from, boolean erase) {
        String key = terminalKey(map, mapset);
        if (erase) {
            MAP_BUFFERS.remove(key);
        }
        MAP_BUFFERS.put(key, deepCopy(from));
        return ok(null);
    }

    public static Response<Void> sendMap(CicsMap<?> request, boolean erase) {
        if (request == null) {
            return error(16, 1, null);
        }
        return sendMap(request.getMapName(), request.getMapSetName(), request.getPayload(), erase);
    }

    public static Response<Void> sendText(String text, Integer length, boolean erase) {
        String value = text == null ? "" : text;
        if (length != null && length >= 0 && length < value.length()) {
            value = value.substring(0, length);
        }
        if (erase) {
            TEXT_BUFFERS.clear();
        }
        TEXT_BUFFERS.put("TEXT", value);
        return new Response<>(0, 0, null, value);
    }

    public static Response<Void> returnControl() {
        return ok(null);
    }

    private static <T> Response<T> ok(T payload) {
        return new Response<>(0, 0, payload);
    }

    private static <T> Response<T> error(int resp, int resp2, T payload) {
        return new Response<>(resp, resp2, payload);
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
