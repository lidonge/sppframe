package free.cobol2java.java;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FileCtl {
    private static final Map<String, LinkedHashMap<String, Object>> COBOL_FILES = new ConcurrentHashMap<>();
    private static final Map<String, String> OPEN_MODES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> READ_CURSORS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<FileControlMeta>> CONTROLS_CACHE = new ConcurrentHashMap<>();
    private static final Map<FieldLookupKey, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> FALLBACK_STATUS_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<UseFrame> USE_PROCEDURES = ThreadLocal.withInitial(UseFrame::new);
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private FileCtl() {
    }

    /**
     * USE AFTER STANDARD ERROR/EXCEPTION PROCEDURE registrations for the current thread.
     * COBOL declaratives are program scoped and execute synchronously, so a thread-local
     * frame mirrors {@code CicsRuntime}'s HANDLE state without depending on which generated
     * object instance ({@code this} vs. an inner handler) issues the file operation.
     */
    private static final class UseFrame {
        private final Map<String, Runnable> byFile = new LinkedHashMap<>();
        private final Map<String, Runnable> byMode = new LinkedHashMap<>();
        private Runnable global;
    }

    private static final class FileControlMeta {
        String fileName;
        String recordAreaName;
        String recordKeyName;
        String statusFieldName;
    }

    private static final class FieldLookupKey {
        private final Class<?> type;
        private final String fieldName;

        private FieldLookupKey(Class<?> type, String fieldName) {
            this.type = type;
            this.fieldName = fieldName;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FieldLookupKey)) {
                return false;
            }
            FieldLookupKey that = (FieldLookupKey) other;
            return type.equals(that.type) && fieldName.equals(that.fieldName);
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + fieldName.hashCode();
        }
    }

    /**
     * Registers a USE AFTER STANDARD ERROR/EXCEPTION PROCEDURE declarative.
     *
     * @param procedure the declarative body to run when a matching file error occurs
     * @param fileNames the files named after {@code ON file-1 [file-2 ...]}; when empty the
     *                  procedure becomes the catch-all handler for every file
     */
    public static void useAfterError(Runnable procedure, String... fileNames) {
        if (procedure == null) {
            return;
        }
        UseFrame frame = USE_PROCEDURES.get();
        if (fileNames == null || fileNames.length == 0) {
            frame.global = procedure;
            return;
        }
        for (String fileName : fileNames) {
            if (fileName != null && !fileName.isBlank()) {
                frame.byFile.put(normalizeName(fileName), procedure);
            }
        }
    }

    /** Registers {@code USE AFTER ... ON INPUT}. */
    public static void useAfterErrorOnInput(Runnable procedure) {
        registerModeProcedure("INPUT", procedure);
    }

    /** Registers {@code USE AFTER ... ON OUTPUT}. */
    public static void useAfterErrorOnOutput(Runnable procedure) {
        registerModeProcedure("OUTPUT", procedure);
    }

    /** Registers {@code USE AFTER ... ON I-O}. */
    public static void useAfterErrorOnIo(Runnable procedure) {
        registerModeProcedure("I-O", procedure);
    }

    /** Registers {@code USE AFTER ... ON EXTEND}. */
    public static void useAfterErrorOnExtend(Runnable procedure) {
        registerModeProcedure("EXTEND", procedure);
    }

    private static void registerModeProcedure(String mode, Runnable procedure) {
        if (procedure == null || mode == null) {
            return;
        }
        USE_PROCEDURES.get().byMode.put(mode.trim().toUpperCase(), procedure);
    }

    /** Clears every registered USE procedure for the current thread. */
    public static void clearUseProcedures() {
        USE_PROCEDURES.remove();
        DISPATCHING.remove();
    }

    /** Clears all simulated file state and registered USE procedures. */
    public static void reset() {
        COBOL_FILES.clear();
        OPEN_MODES.clear();
        READ_CURSORS.clear();
        CONTROLS_CACHE.clear();
        FIELD_CACHE.clear();
        FALLBACK_STATUS_FIELD_CACHE.clear();
        clearUseProcedures();
    }

    public static void cobolOpen(Object owner, String fileName, String mode) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        String normalizedMode = mode == null ? "" : mode.trim().toUpperCase();
        String fileKey = fileStoreKey(owner, fileName);
        // Record the (attempted) open mode up front so that an OPEN that fails before the file
        // exists can still route its error to an ON INPUT/OUTPUT/I-O/EXTEND declarative.
        OPEN_MODES.put(fileKey, normalizedMode);
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileKey);

        if ("OUTPUT".equals(normalizedMode)) {
            COBOL_FILES.put(fileKey, new LinkedHashMap<>());
            READ_CURSORS.remove(fileKey);
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

        if ("INPUT".equals(normalizedMode) || "I-O".equals(normalizedMode)) {
            READ_CURSORS.put(fileKey, 0);
        }
        setFileStatus(owner, meta, "00");
    }

    public static void cobolClose(Object owner, String fileName) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        READ_CURSORS.remove(fileStoreKey(owner, fileName));
        setFileStatus(owner, meta, "00");
    }

    public static void setFileStatusByFileName(Object owner, String fileName, String statusCode) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        setFileStatus(owner, meta, statusCode);
    }

    public static boolean cobolWrite(Object owner, String recordName, Object recordObj) {
        if (owner == null || recordObj == null) {
            return false;
        }
        FileControlMeta meta = findControlByRecordName(owner, recordName);
        if (meta == null) {
            return false;
        }
        LinkedHashMap<String, Object> store = COBOL_FILES.computeIfAbsent(fileStoreKey(owner, meta.fileName),
                ignored -> new LinkedHashMap<>());
        String key = hasRecordKey(meta.recordKeyName) ? extractKey(recordObj, meta.recordKeyName) : nextSequentialKey(store);
        if (key == null) {
            setFileStatus(owner, meta, "23");
            return false;
        }
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
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileStoreKey(owner, meta.fileName));
        if (!hasRecordKey(meta.recordKeyName)) {
            return readSequential(owner, meta, recordArea, store);
        }
        String key = extractKey(recordArea, meta.recordKeyName);
        if (store == null || key == null || !store.containsKey(key)) {
            setFileStatus(owner, meta, "23");
            return false;
        }
        copyFields(store.get(key), recordArea);
        setFileStatus(owner, meta, "00");
        return true;
    }

    private static boolean readSequential(Object owner, FileControlMeta meta, Object recordArea,
            LinkedHashMap<String, Object> store) {
        String fileKey = fileStoreKey(owner, meta.fileName);
        int index = READ_CURSORS.getOrDefault(fileKey, 0);
        if (store == null || index >= store.size()) {
            READ_CURSORS.put(fileKey, index);
            setFileStatus(owner, meta, "00");
            return false;
        }
        Object record = new ArrayList<>(store.values()).get(index);
        copyFields(record, recordArea);
        READ_CURSORS.put(fileKey, index + 1);
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
        if (owner == null) {
            return List.of();
        }
        return CONTROLS_CACHE.computeIfAbsent(owner.getClass(), FileCtl::readControlsForClass);
    }

    private static List<FileControlMeta> readControlsForClass(Class<?> ownerClass) {
        List<FileControlMeta> result = new ArrayList<>();
        try {
            Field envField = ownerClass.getDeclaredField("ENV_FILE_CONTROLS");
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
        return List.copyOf(result);
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
        if (hasRecordKey(recordKeyName)) {
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

    private static boolean hasRecordKey(String recordKeyName) {
        if (recordKeyName == null) {
            return false;
        }
        String normalized = recordKeyName.trim();
        return !normalized.isEmpty() && !"null".equalsIgnoreCase(normalized);
    }

    private static String nextSequentialKey(LinkedHashMap<String, Object> store) {
        int next = store == null ? 1 : store.size() + 1;
        String key;
        do {
            key = String.format("%020d", next++);
        } while (store != null && store.containsKey(key));
        return key;
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
        FieldLookupKey key = new FieldLookupKey(type, fieldName);
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field field = cursor.getDeclaredField(fieldName);
                field.setAccessible(true);
                FIELD_CACHE.put(key, field);
                return field;
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
            field = FALLBACK_STATUS_FIELD_CACHE.computeIfAbsent(owner.getClass(), FileCtl::findFallbackStatusField);
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
        } finally {
            maybeDispatchUseProcedure(owner, meta, statusCode);
        }
    }

    private static Field findFallbackStatusField(Class<?> ownerClass) {
        for (Field candidate : ownerClass.getDeclaredFields()) {
            String name = candidate.getName().toLowerCase();
            if (name.contains("status") && isStatusLeafField(candidate)) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        return null;
    }

    private static boolean isStatusLeafField(Field field) {
        Class<?> type = field.getType();
        return type == String.class || type == Integer.class || type == int.class;
    }

    /**
     * Triggers the registered USE AFTER ERROR procedure when an operation produced a
     * permanent/logic/implementor error (status classes 3x, 4x and 9x).
     * <p>
     * AT END (1x) and INVALID KEY (2x) are intentionally excluded: those conditions are
     * delivered to the statement's AT END / INVALID KEY phrase by the generated code, and a
     * COBOL declarative does not fire for a condition that has an applicable phrase.
     */
    private static void maybeDispatchUseProcedure(Object owner, FileControlMeta meta, String statusCode) {
        if (meta == null || !isErrorClass(statusCode) || Boolean.TRUE.equals(DISPATCHING.get())) {
            return;
        }
        Runnable procedure = resolveUseProcedure(owner, meta);
        if (procedure == null) {
            return;
        }
        DISPATCHING.set(Boolean.TRUE);
        try {
            procedure.run();
        } finally {
            DISPATCHING.set(Boolean.FALSE);
        }
    }

    private static Runnable resolveUseProcedure(Object owner, FileControlMeta meta) {
        UseFrame frame = USE_PROCEDURES.get();
        Runnable byFile = frame.byFile.get(normalizeName(meta.fileName));
        if (byFile != null) {
            return byFile;
        }
        String mode = OPEN_MODES.get(fileStoreKey(owner, meta.fileName));
        if (mode != null && !mode.isEmpty()) {
            Runnable byMode = frame.byMode.get(mode);
            if (byMode != null) {
                return byMode;
            }
        }
        return frame.global;
    }

    private static boolean isErrorClass(String statusCode) {
        if (statusCode == null || statusCode.isEmpty()) {
            return false;
        }
        char statusClass = statusCode.charAt(0);
        return statusClass == '3' || statusClass == '4' || statusClass == '9';
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
