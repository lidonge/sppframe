package free.cobol2java.java;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FileCtl {
    private static final Map<String, LinkedHashMap<String, Object>> COBOL_FILES = new ConcurrentHashMap<>();

    private FileCtl() {
    }

    private static final class FileControlMeta {
        String fileName;
        String recordAreaName;
        String recordKeyName;
        String statusFieldName;
    }

    public static void cobolOpen(Object owner, String fileName, String mode) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        String normalizedMode = mode == null ? "" : mode.trim().toUpperCase();
        String fileKey = fileStoreKey(owner, fileName);
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileKey);

        if ("OUTPUT".equals(normalizedMode)) {
            COBOL_FILES.put(fileKey, new LinkedHashMap<>());
            setFileStatus(owner, meta, "00");
            return;
        }

        if (store == null) {
            if ("I-O".equals(normalizedMode) || "INPUT".equals(normalizedMode)) {
                setFileStatus(owner, meta, "35");
                return;
            }
            store = new LinkedHashMap<>();
            COBOL_FILES.put(fileKey, store);
        }

        setFileStatus(owner, meta, "00");
    }

    public static void cobolClose(Object owner, String fileName) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        setFileStatus(owner, meta, "00");
    }

    public static boolean cobolWrite(Object owner, String recordName, Object recordObj) {
        if (owner == null || recordObj == null) {
            return false;
        }
        FileControlMeta meta = findControlByRecordName(owner, recordName);
        if (meta == null) {
            return false;
        }
        String key = extractKey(recordObj, meta.recordKeyName);
        if (key == null) {
            setFileStatus(owner, meta, "23");
            return false;
        }
        LinkedHashMap<String, Object> store = COBOL_FILES.computeIfAbsent(fileStoreKey(owner, meta.fileName),
                ignored -> new LinkedHashMap<>());
        if (store.containsKey(key)) {
            setFileStatus(owner, meta, "22");
            return false;
        }
        store.put(key, cloneRecord(recordObj));
        setFileStatus(owner, meta, "00");
        return true;
    }

    public static boolean cobolRead(Object owner, String fileName) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        if (meta == null) {
            return false;
        }
        Object recordArea = getRecordArea(owner, meta);
        if (recordArea == null) {
            setFileStatus(owner, meta, "23");
            return false;
        }
        String key = extractKey(recordArea, meta.recordKeyName);
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileStoreKey(owner, meta.fileName));
        if (store == null || key == null || !store.containsKey(key)) {
            setFileStatus(owner, meta, "23");
            return false;
        }
        copyFields(store.get(key), recordArea);
        setFileStatus(owner, meta, "00");
        return true;
    }

    public static boolean cobolRewrite(Object owner, String recordName, Object recordObj) {
        if (owner == null || recordObj == null) {
            return false;
        }
        FileControlMeta meta = findControlByRecordName(owner, recordName);
        if (meta == null) {
            return false;
        }
        String key = extractKey(recordObj, meta.recordKeyName);
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileStoreKey(owner, meta.fileName));
        if (store == null || key == null || !store.containsKey(key)) {
            setFileStatus(owner, meta, "23");
            return false;
        }
        store.put(key, cloneRecord(recordObj));
        setFileStatus(owner, meta, "00");
        return true;
    }

    public static boolean cobolDelete(Object owner, String fileName) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        if (meta == null) {
            return false;
        }
        Object recordArea = getRecordArea(owner, meta);
        String key = extractKey(recordArea, meta.recordKeyName);
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileStoreKey(owner, meta.fileName));
        if (store == null || key == null || store.remove(key) == null) {
            setFileStatus(owner, meta, "23");
            return false;
        }
        setFileStatus(owner, meta, "00");
        return true;
    }

    public static boolean cobolStart(Object owner, String fileName) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        if (meta == null) {
            return false;
        }
        Object recordArea = getRecordArea(owner, meta);
        String key = extractKey(recordArea, meta.recordKeyName);
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileStoreKey(owner, meta.fileName));
        boolean exists = store != null && key != null && store.containsKey(key);
        setFileStatus(owner, meta, exists ? "00" : "23");
        return exists;
    }

    private static String fileStoreKey(Object owner, String fileName) {
        String cls = owner == null ? "GLOBAL" : owner.getClass().getName();
        return cls + "::" + normalizeName(fileName);
    }

    private static FileControlMeta findControlByFileName(Object owner, String fileName) {
        List<FileControlMeta> controls = readControls(owner);
        String normalized = normalizeName(fileName);
        for (FileControlMeta control : controls) {
            if (normalized.equals(normalizeName(control.fileName))) {
                return control;
            }
        }
        return controls.isEmpty() ? null : controls.get(0);
    }

    private static FileControlMeta findControlByRecordName(Object owner, String recordName) {
        List<FileControlMeta> controls = readControls(owner);
        String normalized = normalizeName(recordName);
        for (FileControlMeta control : controls) {
            if (normalized.equals(normalizeName(control.recordAreaName))) {
                return control;
            }
        }
        return controls.isEmpty() ? null : controls.get(0);
    }

    private static List<FileControlMeta> readControls(Object owner) {
        List<FileControlMeta> result = new ArrayList<>();
        if (owner == null) {
            return result;
        }
        try {
            Field envField = owner.getClass().getDeclaredField("ENV_FILE_CONTROLS");
            envField.setAccessible(true);
            Object envValue = envField.get(null);
            if (!(envValue instanceof List<?> controls)) {
                return result;
            }
            for (Object control : controls) {
                if (control == null) {
                    continue;
                }
                FileControlMeta meta = new FileControlMeta();
                meta.fileName = readPublicField(control, "fileName");
                meta.recordAreaName = readPublicField(control, "recordAreaName");
                meta.recordKeyName = readPublicField(control, "recordKeyName");
                meta.statusFieldName = readPublicField(control, "statusFieldName");
                result.add(meta);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static String readPublicField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getField(fieldName);
            Object value = field.get(target);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object getRecordArea(Object owner, FileControlMeta meta) {
        if (owner == null || meta == null) {
            return null;
        }
        Field field = null;
        if (meta.recordAreaName != null && !meta.recordAreaName.isEmpty()) {
            field = findField(owner.getClass(), toJavaFieldName(meta.recordAreaName));
        }
        if (field == null) {
            for (Field candidate : owner.getClass().getDeclaredFields()) {
                Class<?> type = candidate.getType();
                if (!type.isPrimitive() && !String.class.equals(type)
                        && !Number.class.isAssignableFrom(type) && !type.getName().startsWith("java.")) {
                    field = candidate;
                    break;
                }
            }
            if (field == null) {
                return null;
            }
        }
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String extractKey(Object recordObj, String recordKeyName) {
        if (recordObj == null) {
            return null;
        }
        Field keyField = null;
        if (recordKeyName != null && !recordKeyName.isEmpty()) {
            keyField = findField(recordObj.getClass(), toJavaFieldName(recordKeyName));
        }
        if (keyField == null) {
            Field[] fields = recordObj.getClass().getDeclaredFields();
            if (fields.length > 0) {
                keyField = fields[0];
            }
        }
        if (keyField == null) {
            return null;
        }
        try {
            keyField.setAccessible(true);
            Object keyValue = keyField.get(recordObj);
            return keyValue == null ? null : keyValue.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object cloneRecord(Object source) {
        if (source == null) {
            return null;
        }
        try {
            Object target = source.getClass().getDeclaredConstructor().newInstance();
            copyFields(source, target);
            return target;
        } catch (Exception ignored) {
            return source;
        }
    }

    private static void copyFields(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        for (Field field : source.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                Field targetField = findField(target.getClass(), field.getName());
                if (targetField != null) {
                    targetField.setAccessible(true);
                    targetField.set(target, value);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        if (type == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private static void setFileStatus(Object owner, FileControlMeta meta, String statusCode) {
        if (owner == null || meta == null) {
            return;
        }
        Field field = null;
        if (meta.statusFieldName != null && !meta.statusFieldName.isEmpty()) {
            field = findField(owner.getClass(), toJavaFieldName(meta.statusFieldName));
        }
        if (field == null) {
            for (Field candidate : owner.getClass().getDeclaredFields()) {
                String name = candidate.getName().toLowerCase();
                if (name.contains("status")) {
                    field = candidate;
                    break;
                }
            }
            if (field == null) {
                return;
            }
        }
        try {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type == Integer.class || type == int.class) {
                field.set(owner, Integer.parseInt(statusCode));
            } else if (type == String.class) {
                field.set(owner, statusCode);
            } else {
                field.set(owner, statusCode);
            }
        } catch (Exception ignored) {
        }
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.replace("-", "").replace("_", "").trim().toUpperCase();
    }

    private static String toJavaFieldName(String cobolName) {
        if (cobolName == null) {
            return "";
        }
        String[] parts = cobolName.trim().toLowerCase().split("[-_\\s]+");
        if (parts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }
}
