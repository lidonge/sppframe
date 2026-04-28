package free.cobol2java.java;

import free.cobol2java.cics.CicsCrudRepository;
import free.cobol2java.cics.CicsDataAccessException;
import free.cobol2java.cics.DuplicateKeyException;
import free.cobol2java.cics.RecordNotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory CICS runtime used by generated Java output.
 *
 * <p>This preserves the converted code structure and gives EXEC CICS
 * statements concrete runtime behavior without requiring a real CICS region.</p>
 */
public final class CicsUtil {
    private static final Map<String, Map<Object, Object>> VSAM_FILES = new ConcurrentHashMap<>();
    private static final Map<String, Object> MAP_BUFFERS = new ConcurrentHashMap<>();
    private static final Map<String, Object> LAST_READ_KEYS = new ConcurrentHashMap<>();
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

    public static <T> Response<T> receiveMap(String map, String mapset, T into) {
        String key = terminalKey(map, mapset);
        Object saved = MAP_BUFFERS.get(key);
        if (saved != null && into != null) {
            copyState(saved, into);
        }
        return ok(into);
    }

    public static <T> Response<T> receiveMap(CicsMap<T> request) {
        if (request == null) {
            return error(16, 1, null);
        }
        Response<T> response = receiveMap(request.getMapName(), request.getMapSetName(), request.getPayload());
        if (response != null) {
            request.setPayload(response.getPayload());
        }
        return response;
    }

    public static <T> CicsMap<T> bindMapFields(CicsMap<T> request, Map<String, ?> values) {
        if (request == null || values == null || values.isEmpty()) {
            return request;
        }
        T payload = request.getPayload();
        if (payload == null) {
            payload = instantiatePayload(request);
            request.setPayload(payload);
        }
        if (payload == null) {
            return request;
        }
        Map<String, Object> normalizedValues = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = normalizeName(entry.getKey());
            if (!key.isEmpty()) {
                normalizedValues.put(key, entry.getValue());
            }
        }
        if (normalizedValues.isEmpty()) {
            return request;
        }
        bindPayloadFromMap(request, payload, normalizedValues);
        MAP_BUFFERS.put(terminalKey(request.getMapName(), request.getMapSetName()), deepCopy(payload));
        return request;
    }

    public static Map<String, Object> extractMapFields(CicsMap<?> request) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (request == null) {
            return result;
        }
        Object payload = MAP_BUFFERS.get(terminalKey(request.getMapName(), request.getMapSetName()));
        if (payload == null) {
            payload = request.getPayload();
        }
        if (payload == null) {
            return result;
        }
        for (Field field : getAllInstanceFields(payload.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(payload);
                String javaName = field.getName();
                String bmsName = findBmsName(request, javaName);
                result.put(bmsName == null ? javaName : bmsName, value);
            } catch (Exception ignored) {
            }
        }
        return result;
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

    public static Response<Void> write(String file, Object from, Object ridfld, Integer length) {
        Object key = normalizeKey(ridfld);
        if (key == null) {
            return error(16, 1, null);
        }
        CicsCrudRepository<Object, Object> repository = resolveCrudRepository(file);
        if (repository != null) {
            try {
                repository.write(key, deepCopy(from));
                LAST_READ_KEYS.put(file, key);
                return ok(null);
            } catch (DuplicateKeyException e) {
                return error(22, 1, null);
            } catch (CicsDataAccessException e) {
                return error(16, 9, null);
            }
        }
        VSAM_FILES.computeIfAbsent(file, unused -> new ConcurrentHashMap<>())
                .put(key, deepCopy(from));
        LAST_READ_KEYS.put(file, key);
        return ok(null);
    }

    public static <T> Response<T> read(String file, T into, Object ridfld, Integer length) {
        Object key = normalizeKey(ridfld);
        if (key == null) {
            return error(16, 1, into);
        }
        CicsCrudRepository<Object, Object> repository = resolveCrudRepository(file);
        if (repository != null) {
            try {
                Optional<Object> stored = repository.read(key);
                if (stored == null || stored.isEmpty()) {
                    return error(13, 1, into);
                }
                if (into != null) {
                    copyState(stored.get(), into);
                }
                LAST_READ_KEYS.put(file, key);
                return ok(into);
            } catch (CicsDataAccessException e) {
                return error(16, 9, into);
            }
        }
        Object stored = VSAM_FILES.computeIfAbsent(file, unused -> new ConcurrentHashMap<>()).get(key);
        if (stored == null) {
            return error(13, 1, into);
        }
        if (into != null) {
            copyState(stored, into);
        }
        LAST_READ_KEYS.put(file, key);
        return ok(into);
    }

    public static Response<Void> rewrite(String file, Object from, Integer length) {
        Object key = LAST_READ_KEYS.get(file);
        if (key == null) {
            return error(16, 2, null);
        }
        CicsCrudRepository<Object, Object> repository = resolveCrudRepository(file);
        if (repository != null) {
            try {
                repository.rewrite(key, deepCopy(from));
                return ok(null);
            } catch (RecordNotFoundException e) {
                return error(13, 1, null);
            } catch (CicsDataAccessException e) {
                return error(16, 9, null);
            }
        }
        VSAM_FILES.computeIfAbsent(file, unused -> new ConcurrentHashMap<>())
                .put(key, deepCopy(from));
        return ok(null);
    }

    public static Response<Void> delete(String file, Object ridfld) {
        Object key = normalizeKey(ridfld);
        if (key == null) {
            return error(16, 1, null);
        }
        CicsCrudRepository<Object, Object> repository = resolveCrudRepository(file);
        if (repository != null) {
            try {
                repository.delete(key);
                LAST_READ_KEYS.remove(file);
                return ok(null);
            } catch (RecordNotFoundException e) {
                return error(13, 1, null);
            } catch (CicsDataAccessException e) {
                return error(16, 9, null);
            }
        }
        Map<Object, Object> fileStore = VSAM_FILES.computeIfAbsent(file, unused -> new ConcurrentHashMap<>());
        Object removed = fileStore.remove(key);
        if (removed == null) {
            Object matchedKey = null;
            for (Object existingKey : fileStore.keySet()) {
                if (Objects.equals(normalizeKey(existingKey), key)
                        || Objects.equals(Objects.toString(existingKey, null), Objects.toString(key, null))) {
                    matchedKey = existingKey;
                    break;
                }
            }
            if (matchedKey != null) {
                removed = fileStore.remove(matchedKey);
            }
        }
        LAST_READ_KEYS.remove(file);
        return removed == null ? error(13, 1, null) : ok(null);
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

    private static Object normalizeKey(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return value;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return value;
        }
        return Objects.toString(value);
    }

    @SuppressWarnings("unchecked")
    private static CicsCrudRepository<Object, Object> resolveCrudRepository(String file) {
        if (file == null || file.isBlank()) {
            return null;
        }
        Object serviceContainer = resolveServiceContainer();
        if (serviceContainer == null) {
            return null;
        }
        for (String beanName : repositoryBeanNames(file)) {
            try {
                Object bean = serviceContainer.getClass().getMethod("getService", String.class).invoke(serviceContainer, beanName);
                if (bean instanceof CicsCrudRepository<?, ?> repository) {
                    return (CicsCrudRepository<Object, Object>) repository;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Object resolveServiceContainer() {
        try {
            Class<?> managerClass = Class.forName("free.cobol2java.java.ServiceManager");
            return managerClass.getMethod("getServiceContainer").invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<String> repositoryBeanNames(String file) {
        String normalized = file.trim();
        String camel = toCamel(normalized);
        String classLike = toClassLike(normalized);
        return List.of(
                camel + "Repository",
                camel + "Mapper",
                "cics" + classLike + "Repository",
                "cics" + classLike + "Mapper",
                normalized,
                normalized.toLowerCase() + "Repository",
                normalized.toLowerCase() + "Mapper"
        );
    }

    private static String toCamel(String name) {
        String classLike = toClassLike(name);
        if (classLike.isEmpty()) {
            return classLike;
        }
        return Character.toLowerCase(classLike.charAt(0)) + classLike.substring(1);
    }

    private static String toClassLike(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.trim().split("[^A-Za-z0-9]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String lower = part.toLowerCase();
            sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiatePayload(CicsMap<T> request) {
        try {
            java.lang.reflect.ParameterizedType generic = null;
            for (java.lang.reflect.Type type : request.getClass().getGenericInterfaces()) {
                if (type instanceof java.lang.reflect.ParameterizedType parameterizedType
                        && parameterizedType.getRawType() instanceof Class<?> rawType
                        && CicsMap.class.isAssignableFrom(rawType)) {
                    generic = parameterizedType;
                    break;
                }
            }
            if (generic == null || generic.getActualTypeArguments().length == 0) {
                return null;
            }
            java.lang.reflect.Type payloadType = generic.getActualTypeArguments()[0];
            if (!(payloadType instanceof Class<?> clazz)) {
                return null;
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private static void bindPayloadFromMap(CicsMap<?> request, Object payload, Map<String, Object> normalizedValues) {
        for (Field field : getAllInstanceFields(payload.getClass())) {
            String javaName = field.getName();
            String normalizedJavaName = normalizeName(javaName);
            String bmsName = findBmsName(request, javaName);
            String normalizedBmsName = normalizeName(bmsName);
            Object rawValue = normalizedValues.containsKey(normalizedBmsName)
                    ? normalizedValues.get(normalizedBmsName)
                    : normalizedValues.get(normalizedJavaName);
            if (rawValue == null && !normalizedValues.containsKey(normalizedBmsName)
                    && !normalizedValues.containsKey(normalizedJavaName)) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(payload, coerceValue(field.getType(), rawValue));
            } catch (Exception ignored) {
            }
        }
    }

    private static java.util.List<Field> getAllInstanceFields(Class<?> type) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String findBmsName(CicsMap<?> request, String javaFieldName) {
        String normalizedJavaName = normalizeName(javaFieldName);
        for (BmsFieldMeta field : request.getFields()) {
            if (field == null || field.name == null) {
                continue;
            }
            String candidate = toJavaFieldName(field.name);
            if (normalizeName(candidate).equals(normalizedJavaName)) {
                return field.name;
            }
        }
        return null;
    }

    private static String toJavaFieldName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.toLowerCase().split("[^a-z0-9]+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isBlank()) {
                continue;
            }
            if (sb.length() == 0) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private static Object coerceValue(Class<?> targetType, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }
        String text = Objects.toString(rawValue, null);
        if (text == null) {
            return null;
        }
        try {
            if (targetType == String.class) {
                return text;
            }
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(text.trim());
            }
            if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(text.trim());
            }
            if (targetType == Short.class || targetType == short.class) {
                return Short.parseShort(text.trim());
            }
            if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(text.trim());
            }
            if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(text.trim());
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(text.trim());
            }
            if (targetType == Character.class || targetType == char.class) {
                return text.isEmpty() ? null : text.charAt(0);
            }
        } catch (Exception ignored) {
        }
        return rawValue;
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

    private static void copyState(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        copyFields(source, target, target.getClass(), false);
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
