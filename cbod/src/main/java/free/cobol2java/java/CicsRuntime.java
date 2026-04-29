package free.cobol2java.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime hooks for converted LINK/START commands.
 */
public final class CicsRuntime {
    private static final List<StartRequest> START_REQUESTS = new ArrayList<>();
    private static final List<ReturnRequest> RETURN_REQUESTS = new ArrayList<>();
    private static final Map<String, Object> COMMON_WORK_AREAS = new ConcurrentHashMap<>();
    private static final Map<String, Object> LOADED_PROGRAMS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Object>> CURRENT_COMMON_WORK_AREAS =
            ThreadLocal.withInitial(ConcurrentHashMap::new);
    private static final ThreadLocal<CicsStatus> LAST_STATUS = ThreadLocal.withInitial(CicsStatus::ok);

    private CicsRuntime() {
    }

    public static synchronized void start(String transid, Object interval, Object from, Integer length,
                                          Object reqid, Object termid) {
        START_REQUESTS.add(new StartRequest(transid, interval, from, length, reqid, termid));
    }

    public static void link(String program, Object commarea, Integer length) {
        IService service = ServiceManager.getService(program);
        if (service == null) {
            return;
        }
        Method method = findLinkProcedure(service.getClass());
        if (method == null) {
            service.execute(commarea, length);
            return;
        }
        try {
            method.setAccessible(true);
            int parameterCount = method.getParameterCount();
            if (parameterCount == 0) {
                method.invoke(service);
                return;
            }
            if (parameterCount == 1) {
                Object adapted = adaptCommarea(commarea, method.getParameterTypes()[0]);
                method.invoke(service, adapted);
                copyMatchingFields(adapted, commarea);
                return;
            }
            if (parameterCount == 2) {
                Object adapted = adaptCommarea(commarea, method.getParameterTypes()[0]);
                method.invoke(service, adapted, adaptLength(length, method.getParameterTypes()[1]));
                copyMatchingFields(adapted, commarea);
                return;
            }
            service.execute(commarea, length);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute LINK program " + program, e);
        }
    }

    public static <T> T getCommonWorkArea(String name, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Common work area type must not be null.");
        }
        String key = commonWorkAreaKey(name, type);
        Object value = COMMON_WORK_AREAS.computeIfAbsent(key, ignored -> newInstance(type));
        CURRENT_COMMON_WORK_AREAS.get().put(key, value);
        return type.cast(value);
    }

    public static void saveCommonWorkArea(String name, Object value) {
        if (value == null) {
            return;
        }
        String key = commonWorkAreaKey(name, value.getClass());
        COMMON_WORK_AREAS.put(key, value);
        CURRENT_COMMON_WORK_AREAS.get().put(key, value);
    }

    public static void saveCurrentCommonWorkAreas() {
        COMMON_WORK_AREAS.putAll(CURRENT_COMMON_WORK_AREAS.get());
    }

    public static <T> T loadProgram(String program, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Loaded program type must not be null.");
        }
        String key = loadedProgramKey(program, type);
        Object value = LOADED_PROGRAMS.computeIfAbsent(key, ignored -> newInstance(type));
        setStatus(0, 0);
        return type.cast(value);
    }

    public static void registerLoadedProgram(String program, Object value) {
        if (value == null) {
            return;
        }
        LOADED_PROGRAMS.put(loadedProgramKey(program, value.getClass()), value);
    }

    public static Map<String, Object> getLoadedProgramStore() {
        return LOADED_PROGRAMS;
    }

    public static void clearLoadedPrograms() {
        LOADED_PROGRAMS.clear();
    }

    public static Map<String, Object> getCommonWorkAreaStore() {
        return COMMON_WORK_AREAS;
    }

    public static void clearCommonWorkAreas() {
        COMMON_WORK_AREAS.clear();
        CURRENT_COMMON_WORK_AREAS.remove();
    }

    public static synchronized void clearStartedTransactions() {
        START_REQUESTS.clear();
    }

    public static synchronized StartRequest peekLastStartRequest() {
        return START_REQUESTS.isEmpty() ? null : START_REQUESTS.get(START_REQUESTS.size() - 1);
    }

    public static synchronized void returnControl(String transid, Object commarea, Integer length) {
        RETURN_REQUESTS.add(new ReturnRequest(transid, commarea, length));
        returnControl();
    }

    public static synchronized void syncpoint() {
        saveCurrentCommonWorkAreas();
        setStatus(0, 0);
    }

    public static synchronized void syncpointRollback() {
        CURRENT_COMMON_WORK_AREAS.remove();
        setStatus(0, 0);
    }

    public static synchronized void clearReturnRequests() {
        RETURN_REQUESTS.clear();
    }

    public static synchronized ReturnRequest peekLastReturnRequest() {
        return RETURN_REQUESTS.isEmpty() ? null : RETURN_REQUESTS.get(RETURN_REQUESTS.size() - 1);
    }

    public static void returnControl() {
        saveCurrentCommonWorkAreas();
        setStatus(0, 0);
    }

    public static int getResp() {
        return LAST_STATUS.get().resp;
    }

    public static int getResp2() {
        return LAST_STATUS.get().resp2;
    }

    static void setStatus(int resp, int resp2) {
        LAST_STATUS.set(new CicsStatus(resp, resp2));
    }

    private static Method findLinkProcedure(Class<?> serviceClass) {
        Method[] methods = serviceClass.getDeclaredMethods();
        Method oneArg = null;
        Method twoArg = null;
        Method zeroArg = null;
        for (Method method : methods) {
            if (!"procedure".equals(method.getName())) {
                continue;
            }
            if (method.getParameterCount() == 1) {
                oneArg = method;
            } else if (method.getParameterCount() == 2) {
                twoArg = method;
            } else if (method.getParameterCount() == 0) {
                zeroArg = method;
            }
        }
        return oneArg != null ? oneArg : (twoArg != null ? twoArg : zeroArg);
    }

    private static Object adaptCommarea(Object source, Class<?> targetType) throws Exception {
        if (source == null || targetType == null || targetType.isInstance(source)) {
            return source;
        }
        Object target = targetType.getDeclaredConstructor().newInstance();
        copyMatchingFields(source, target);
        return target;
    }

    private static String commonWorkAreaKey(String name, Class<?> type) {
        String actualName = name == null || name.isBlank() ? "CWA" : name.trim();
        return actualName + ":" + type.getName();
    }

    private static String loadedProgramKey(String program, Class<?> type) {
        String actualProgram = program == null || program.isBlank() ? "" : program.trim();
        return actualProgram + ":" + type.getName();
    }

    private static Object newInstance(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create CICS common work area " + type.getName(), e);
        }
    }

    private static Object adaptLength(Integer length, Class<?> targetType) {
        if (length == null || targetType == null || targetType.isInstance(length)) {
            return length;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return length;
        }
        if (targetType == long.class || targetType == Long.class) {
            return length.longValue();
        }
        return length;
    }

    private static void copyMatchingFields(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        for (Field sourceField : source.getClass().getDeclaredFields()) {
            try {
                Field targetField = target.getClass().getDeclaredField(sourceField.getName());
                sourceField.setAccessible(true);
                targetField.setAccessible(true);
                targetField.set(target, sourceField.get(source));
            } catch (Exception ignored) {
            }
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

    private record CicsStatus(int resp, int resp2) {
        private static CicsStatus ok() {
            return new CicsStatus(0, 0);
        }
    }

    public record StartRequest(String transid, Object interval, Object from, Integer length, Object reqid,
                               Object termid) {
    }

    public record ReturnRequest(String transid, Object commarea, Integer length) {
    }
}
