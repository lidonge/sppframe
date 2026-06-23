package free.cobol2java.java;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FileCtl {
    private static final Map<String, LinkedHashMap<String, Object>> COBOL_FILES = new ConcurrentHashMap<>();
    private static final Map<String, String> OPEN_MODES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> READ_POSITIONS = new ConcurrentHashMap<>();
    private static final Map<String, Iterator<Object>> SEQUENTIAL_READ_CURSORS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<FileControlMeta>> CONTROL_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, FileControlMeta>> CONTROL_BY_FILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, FileControlMeta>> CONTROL_BY_RECORD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final java.util.Set<String> FIELD_MISS_CACHE = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> JAVA_FIELD_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> FALLBACK_STATUS_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final sun.misc.Unsafe UNSAFE = findUnsafe();
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
        READ_POSITIONS.clear();
        SEQUENTIAL_READ_CURSORS.clear();
        CONTROL_CACHE.clear();
        CONTROL_BY_FILE_CACHE.clear();
        CONTROL_BY_RECORD_CACHE.clear();
        FIELD_CACHE.clear();
        FIELD_MISS_CACHE.clear();
        JAVA_FIELD_NAME_CACHE.clear();
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
            READ_POSITIONS.remove(fileKey);
            SEQUENTIAL_READ_CURSORS.remove(fileKey);
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
            READ_POSITIONS.put(fileKey, 0);
            SEQUENTIAL_READ_CURSORS.remove(fileKey);
        }
        setFileStatus(owner, meta, "00");
    }

    public static void cobolClose(Object owner, String fileName) {
        FileControlMeta meta = findControlByFileName(owner, fileName);
        READ_POSITIONS.remove(fileStoreKey(owner, fileName));
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
        String fileKey = fileStoreKey(owner, meta.fileName);
        LinkedHashMap<String, Object> store = COBOL_FILES.computeIfAbsent(fileKey,
                ignored -> new LinkedHashMap<>());
        String key = sequential(meta) ? nextSequentialKey(store) : extractKey(recordObj, meta.recordKeyName);
        if (key == null) {
            setFileStatus(owner, meta, "23");
            return false;
        }
        if (!sequential(meta) && store.containsKey(key)) {
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
        if (store == null) {
            setFileStatus(owner, meta, sequential(meta) ? "00" : "23");
            return false;
        }
        Object record;
        if (sequential(meta)) {
            record = nextSequentialRecord(owner, meta, store);
        } else {
            String key = extractKey(recordArea, meta.recordKeyName);
            record = key == null ? null : store.get(key);
        }
        if (record == null) {
            setFileStatus(owner, meta, sequential(meta) ? "00" : "23");
            return false;
        }
        copyFields(record, recordArea);
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
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileStoreKey(owner, meta.fileName));
        String key = extractKey(recordObj, meta.recordKeyName);
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
        String fileKey = fileStoreKey(owner, meta.fileName);
        LinkedHashMap<String, Object> store = COBOL_FILES.get(fileKey);
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
        if (owner == null) {
            return null;
        }
        String normalized = normalizeName(fileName);
        Map<String, FileControlMeta> controls = CONTROL_BY_FILE_CACHE.computeIfAbsent(owner.getClass(), type -> {
            Map<String, FileControlMeta> byFile = new ConcurrentHashMap<>();
            for (FileControlMeta control : readControls(type)) {
                byFile.put(normalizeName(control.fileName), control);
            }
            return byFile;
        });
        FileControlMeta meta = controls.get(normalized);
        if (meta != null) {
            return meta;
        }
        List<FileControlMeta> all = readControls(owner.getClass());
        return all.isEmpty() ? null : all.get(0);
    }

    private static FileControlMeta findControlByRecordName(Object owner, String recordName) {
        if (owner == null) {
            return null;
        }
        String normalized = normalizeName(recordName);
        Map<String, FileControlMeta> controls = CONTROL_BY_RECORD_CACHE.computeIfAbsent(owner.getClass(), type -> {
            Map<String, FileControlMeta> byRecord = new ConcurrentHashMap<>();
            for (FileControlMeta control : readControls(type)) {
                byRecord.put(normalizeName(control.recordAreaName), control);
            }
            return byRecord;
        });
        FileControlMeta meta = controls.get(normalized);
        if (meta != null) {
            return meta;
        }
        List<FileControlMeta> all = readControls(owner.getClass());
        return all.isEmpty() ? null : all.get(0);
    }

    private static List<FileControlMeta> readControls(Class<?> ownerType) {
        return CONTROL_CACHE.computeIfAbsent(ownerType, FileCtl::loadControls);
    }

    private static List<FileControlMeta> loadControls(Class<?> ownerType) {
        List<FileControlMeta> result = new ArrayList<>();
        if (ownerType == null) {
            return result;
        }
        try {
            Field envField = ownerType.getDeclaredField("ENV_FILE_CONTROLS");
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
        if (recordKeyName == null || recordKeyName.isBlank() || "null".equalsIgnoreCase(recordKeyName.trim())) {
            return null;
        }
        Field keyField = null;
        keyField = findField(recordObj.getClass(), toJavaFieldName(recordKeyName));
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
            Object target = newRecordInstance(source.getClass());
            copyFields(source, target);
            return target;
        } catch (Exception ignored) {
            return source;
        }
    }

    private static Object newRecordInstance(Class<?> type) throws ReflectiveOperationException, InstantiationException {
        if (UNSAFE != null) {
            return UNSAFE.allocateInstance(type);
        }
        return type.getDeclaredConstructor().newInstance();
    }

    private static sun.misc.Unsafe findUnsafe() {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void copyFields(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        boolean sameType = source.getClass() == target.getClass();
        for (Field field : source.getClass().getDeclaredFields()) {
            try {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(source);
                Field targetField = sameType ? field : findField(target.getClass(), field.getName());
                if (targetField != null) {
                    int targetModifiers = targetField.getModifiers();
                    if (Modifier.isStatic(targetModifiers) || Modifier.isFinal(targetModifiers)) {
                        continue;
                    }
                    if (targetField != field) {
                        targetField.setAccessible(true);
                    }
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
        String cacheKey = type.getName() + "#" + fieldName;
        Field cached = FIELD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (FIELD_MISS_CACHE.contains(cacheKey)) {
            return null;
        }
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field field = cursor.getDeclaredField(fieldName);
                FIELD_CACHE.put(cacheKey, field);
                return field;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        FIELD_MISS_CACHE.add(cacheKey);
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

    private static boolean sequential(FileControlMeta meta) {
        return meta == null
                || meta.recordKeyName == null
                || meta.recordKeyName.isBlank()
                || "null".equalsIgnoreCase(meta.recordKeyName.trim());
    }

    private static String nextSequentialKey(LinkedHashMap<String, Object> store) {
        return "__SEQ__" + store.size();
    }

    private static Object nextSequentialRecord(Object owner, FileControlMeta meta, LinkedHashMap<String, Object> store) {
        String fileKey = fileStoreKey(owner, meta.fileName);
        int position = READ_POSITIONS.getOrDefault(fileKey, 0);
        if (position < 0 || position >= store.size()) {
            return null;
        }
        Iterator<Object> cursor = SEQUENTIAL_READ_CURSORS.computeIfAbsent(fileKey,
                ignored -> new ArrayList<>(store.values()).iterator());
        if (!cursor.hasNext()) {
            return null;
        }
        READ_POSITIONS.put(fileKey, position + 1);
        return cursor.next();
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.replace("-", "").replace("_", "").trim().toUpperCase();
    }

    private static String toJavaFieldName(String cobolName) {
        if (cobolName == null) {
            return "";
        }
        return JAVA_FIELD_NAME_CACHE.computeIfAbsent(cobolName, FileCtl::toJavaFieldNameUncached);
    }

    private static String toJavaFieldNameUncached(String cobolName) {
        String trimmed = cobolName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(trimmed.length());
        boolean capitalizeNext = false;
        boolean wroteAny = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '-' || ch == '_' || Character.isWhitespace(ch)) {
                capitalizeNext = wroteAny;
                continue;
            }
            char lower = Character.toLowerCase(ch);
            if (capitalizeNext) {
                sb.append(Character.toUpperCase(lower));
                capitalizeNext = false;
            } else {
                sb.append(lower);
            }
            wroteAny = true;
        }
        return sb.toString();
    }
}
