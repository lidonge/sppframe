package free.cobol2java.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runtime hooks for converted LINK/START commands.
 */
public final class CicsRuntime {
    public static final int NORMAL = 0;
    public static final int ERROR = 1;
    public static final int NOTFND = 13;
    public static final int INVREQ = 16;
    public static final int ENDFILE = 20;

    private static final List<StartRequest> START_REQUESTS = new ArrayList<>();
    private static final List<ReturnRequest> RETURN_REQUESTS = new ArrayList<>();
    private static final Map<String, Object> COMMON_WORK_AREAS = new ConcurrentHashMap<>();
    private static final Map<String, Object> LOADED_PROGRAMS = new ConcurrentHashMap<>();
    private static final Map<String, ReentrantLock> LOCAL_RESOURCE_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, Queue<Object>> LOCAL_TD_QUEUES = new ConcurrentHashMap<>();
    private static final Map<String, List<Object>> LOCAL_TS_QUEUES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Object>> CURRENT_COMMON_WORK_AREAS =
            ThreadLocal.withInitial(ConcurrentHashMap::new);
    private static final ThreadLocal<CicsStatus> LAST_STATUS = ThreadLocal.withInitial(CicsStatus::ok);
    private static final ThreadLocal<HandleFrame> CURRENT_HANDLE = ThreadLocal.withInitial(HandleFrame::new);
    private static final ThreadLocal<Deque<HandleFrame>> HANDLE_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static volatile DistributedLock distributedLock = new LocalDistributedLock();
    private static volatile CicsQueueService cicsQueueService = new LocalCicsQueueService();

    private CicsRuntime() {
    }

    public static synchronized Response<Void> start(String transid, Object interval, Object from, Integer length,
                                                    Object reqid, Object termid) {
        START_REQUESTS.add(new StartRequest(transid, interval, from, length, reqid, termid));
        return status(null, 0, 0);
    }

    public static Response<Void> link(String program, Object commarea, Integer length) {
        IService service = ServiceManager.getService(program);
        if (service == null) {
            return status(null, 0, 0);
        }
        Method method = findLinkProcedure(service.getClass());
        if (method == null) {
            service.execute(commarea, length);
            return status(null, 0, 0);
        }
        try {
            method.setAccessible(true);
            int parameterCount = method.getParameterCount();
            if (parameterCount == 0) {
                method.invoke(service);
                return status(null, 0, 0);
            }
            if (parameterCount == 1) {
                Object adapted = adaptCommarea(commarea, method.getParameterTypes()[0]);
                method.invoke(service, adapted);
                copyMatchingFields(adapted, commarea);
                return status(null, 0, 0);
            }
            if (parameterCount == 2) {
                Object adapted = adaptCommarea(commarea, method.getParameterTypes()[0]);
                method.invoke(service, adapted, adaptLength(length, method.getParameterTypes()[1]));
                copyMatchingFields(adapted, commarea);
                return status(null, 0, 0);
            }
            service.execute(commarea, length);
            return status(null, 0, 0);
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

    public static Response<Void> enq(Object resource) {
        distributedLock.lock(resourceKey(resource));
        return status(null, 0, 0);
    }

    public static Response<Void> deq(Object resource) {
        distributedLock.unlock(resourceKey(resource));
        return status(null, 0, 0);
    }

    public static DistributedLock getDistributedLock() {
        return distributedLock;
    }

    public static void setDistributedLock(DistributedLock lock) {
        distributedLock = lock == null ? new LocalDistributedLock() : lock;
    }

    public static void resetDistributedLock() {
        distributedLock = new LocalDistributedLock();
        LOCAL_RESOURCE_LOCKS.clear();
    }

    public static Response<Void> writeqTd(Object queue, Object value) {
        cicsQueueService.writeTd(queueName(queue), value);
        return status(null, 0, 0);
    }

    public static Response<Object> readqTd(Object queue) {
        Object value = cicsQueueService.readTd(queueName(queue));
        if (value == null) {
            return status(null, 13, 0);
        }
        return status(value, 0, 0);
    }

    public static Response<Void> writeqTs(Object queue, Object value, Object item) {
        cicsQueueService.writeTs(queueName(queue), value, itemNumber(item));
        return status(null, 0, 0);
    }

    public static Response<Object> readqTs(Object queue, Object item) {
        Object value = cicsQueueService.readTs(queueName(queue), itemNumber(item));
        if (value == null) {
            return status(null, 13, 0);
        }
        return status(value, 0, 0);
    }

    public static Response<Void> deleteqTs(Object queue) {
        cicsQueueService.deleteTs(queueName(queue));
        return status(null, 0, 0);
    }

    public static CicsQueueService getCicsQueueService() {
        return cicsQueueService;
    }

    public static void setCicsQueueService(CicsQueueService queueService) {
        cicsQueueService = queueService == null ? new LocalCicsQueueService() : queueService;
    }

    public static void resetCicsQueueService() {
        cicsQueueService = new LocalCicsQueueService();
        LOCAL_TD_QUEUES.clear();
        LOCAL_TS_QUEUES.clear();
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
        return loadProgramResult(program, type).value();
    }

    public static <T> Response<T> loadProgramResult(String program, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Loaded program type must not be null.");
        }
        String key = loadedProgramKey(program, type);
        Object value = LOADED_PROGRAMS.computeIfAbsent(key, ignored -> newInstance(type));
        return status(type.cast(value), 0, 0);
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

    public static synchronized Response<Void> returnControl(String transid, Object commarea, Integer length) {
        RETURN_REQUESTS.add(new ReturnRequest(transid, commarea, length));
        return returnControl();
    }

    public static synchronized Response<Void> syncpoint() {
        saveCurrentCommonWorkAreas();
        return status(null, 0, 0);
    }

    public static synchronized Response<Void> syncpointRollback() {
        CURRENT_COMMON_WORK_AREAS.remove();
        return status(null, 0, 0);
    }

    public static synchronized void clearReturnRequests() {
        RETURN_REQUESTS.clear();
    }

    public static synchronized ReturnRequest peekLastReturnRequest() {
        return RETURN_REQUESTS.isEmpty() ? null : RETURN_REQUESTS.get(RETURN_REQUESTS.size() - 1);
    }

    public static Response<Void> returnControl() {
        saveCurrentCommonWorkAreas();
        return status(null, 0, 0);
    }

    public static int getResp() {
        return LAST_STATUS.get().resp;
    }

    public static int getResp2() {
        return LAST_STATUS.get().resp2;
    }

    public static ConditionHandler condition(String condition, HandlerAction action) {
        return new ConditionHandler(condition, conditionResp(condition), action);
    }

    public static ConditionHandler condition(int resp, HandlerAction action) {
        return new ConditionHandler(null, resp, action);
    }

    public static void handleCondition(ConditionHandler... handlers) {
        if (handlers == null) {
            return;
        }
        HandleFrame frame = CURRENT_HANDLE.get();
        for (ConditionHandler handler : handlers) {
            if (handler == null || handler.action == null) {
                continue;
            }
            String condition = normalizeCondition(handler.condition);
            if ("ERROR".equals(condition)) {
                frame.errorHandler = handler.action;
                continue;
            }
            int resp = handler.resp;
            if (resp >= 0) {
                frame.conditionHandlers.put(resp, handler.action);
            }
        }
    }

    public static void ignoreCondition(String... conditions) {
        if (conditions == null) {
            return;
        }
        HandleFrame frame = CURRENT_HANDLE.get();
        for (String condition : conditions) {
            String normalized = normalizeCondition(condition);
            if ("ERROR".equals(normalized)) {
                frame.errorHandler = null;
                continue;
            }
            int resp = conditionResp(normalized);
            if (resp >= 0) {
                frame.conditionHandlers.remove(resp);
            }
        }
    }

    public static void handleAbend(HandlerAction action) {
        CURRENT_HANDLE.get().abendHandler = action;
    }

    public static void resetHandleAbend() {
        CURRENT_HANDLE.get().abendHandler = null;
    }

    public static boolean dispatchHandle(Response<?> response) {
        return dispatchHandle(response == null ? NORMAL : response.resp());
    }

    public static boolean dispatchHandle(int resp) {
        if (resp == NORMAL) {
            return false;
        }
        HandleFrame frame = CURRENT_HANDLE.get();
        HandlerAction handler = frame.conditionHandlers.get(resp);
        if (handler == null) {
            handler = frame.errorHandler;
        }
        if (handler == null) {
            return false;
        }
        handler.run();
        return true;
    }

    public static boolean dispatchAbend() {
        HandlerAction handler = CURRENT_HANDLE.get().abendHandler;
        if (handler == null) {
            return false;
        }
        handler.run();
        return true;
    }

    public static void pushHandle() {
        HANDLE_STACK.get().push(CURRENT_HANDLE.get().copy());
    }

    public static void popHandle() {
        Deque<HandleFrame> stack = HANDLE_STACK.get();
        CURRENT_HANDLE.set(stack.isEmpty() ? new HandleFrame() : stack.pop());
    }

    public static void clearHandle() {
        CURRENT_HANDLE.remove();
        HANDLE_STACK.remove();
    }

    static void setStatus(int resp, int resp2) {
        LAST_STATUS.set(new CicsStatus(resp, resp2));
    }

    static <T> Response<T> status(T value, int resp, int resp2) {
        Response<T> response = new Response<>(value, resp, resp2);
        LAST_STATUS.set(new CicsStatus(resp, resp2));
        return response;
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

    private static String resourceKey(Object resource) {
        return resource == null ? "" : String.valueOf(resource).trim();
    }

    private static String queueName(Object queue) {
        return queue == null ? "" : String.valueOf(queue).trim();
    }

    private static Integer itemNumber(Object item) {
        if (item == null) {
            return null;
        }
        if (item instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(item).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int conditionResp(String condition) {
        String normalized = normalizeCondition(condition);
        if (normalized == null) {
            return -1;
        }
        return switch (normalized) {
            case "NORMAL" -> NORMAL;
            case "ERROR" -> ERROR;
            case "RDATT" -> 2;
            case "WRBRK" -> 3;
            case "EOF" -> 4;
            case "EODS" -> 5;
            case "EOC" -> 6;
            case "INBFMH" -> 7;
            case "ENDINPT" -> 8;
            case "NONVAL" -> 9;
            case "NOSTART" -> 10;
            case "TERMIDERR" -> 11;
            case "FILENOTFOUND", "DSIDERR" -> 12;
            case "NOTFND" -> NOTFND;
            case "DUPREC" -> 14;
            case "DUPKEY" -> 15;
            case "INVREQ" -> INVREQ;
            case "IOERR" -> 17;
            case "NOSPACE" -> 18;
            case "NOTOPEN" -> 19;
            case "ENDFILE" -> ENDFILE;
            case "ILLOGIC" -> 21;
            case "LENGERR" -> 22;
            case "QZERO" -> 23;
            case "SIGNAL" -> 24;
            case "QBUSY" -> 25;
            case "ITEMERR" -> 26;
            case "PGMIDERR" -> 27;
            case "TRANSIDERR" -> 28;
            case "ENDDATA" -> 29;
            case "INVTSREQ" -> 30;
            case "EXPIRED" -> 31;
            case "RETPAGE" -> 32;
            case "RTEFAIL" -> 33;
            case "RTESOME" -> 34;
            case "TSIOERR" -> 35;
            case "MAPFAIL" -> 36;
            case "INVERRTERM" -> 37;
            case "INVMPSZ" -> 38;
            case "IGREQID" -> 39;
            case "OVERFLOW" -> 40;
            case "INVLDC" -> 41;
            case "NOSTG" -> 42;
            case "JIDERR" -> 43;
            case "QIDERR" -> 44;
            case "NOJBUFSP" -> 45;
            case "DSSTAT" -> 46;
            case "DISABLED" -> 84;
            default -> -1;
        };
    }

    private static String normalizeCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return null;
        }
        return condition.trim().replace('-', '_').toUpperCase(Locale.ROOT);
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

    public interface DistributedLock {
        void lock(String resource);

        void unlock(String resource);
    }

    public interface CicsQueueService {
        void writeTd(String queue, Object value);

        Object readTd(String queue);

        void writeTs(String queue, Object value, Integer item);

        Object readTs(String queue, Integer item);

        void deleteTs(String queue);
    }

    public interface HandlerAction {
        void run();
    }

    private static final class LocalDistributedLock implements DistributedLock {
        @Override
        public void lock(String resource) {
            LOCAL_RESOURCE_LOCKS.computeIfAbsent(resource, ignored -> new ReentrantLock()).lock();
        }

        @Override
        public void unlock(String resource) {
            ReentrantLock lock = LOCAL_RESOURCE_LOCKS.get(resource);
            if (lock == null) {
                return;
            }
            lock.unlock();
        }
    }

    private static final class LocalCicsQueueService implements CicsQueueService {
        @Override
        public void writeTd(String queue, Object value) {
            LOCAL_TD_QUEUES.computeIfAbsent(queue, ignored -> new ConcurrentLinkedQueue<>()).add(value);
        }

        @Override
        public Object readTd(String queue) {
            Queue<Object> values = LOCAL_TD_QUEUES.get(queue);
            return values == null ? null : values.poll();
        }

        @Override
        public void writeTs(String queue, Object value, Integer item) {
            List<Object> values = LOCAL_TS_QUEUES.computeIfAbsent(queue, ignored -> new ArrayList<>());
            if (item == null || item <= 0 || item > values.size()) {
                values.add(value);
                return;
            }
            values.set(item - 1, value);
        }

        @Override
        public Object readTs(String queue, Integer item) {
            List<Object> values = LOCAL_TS_QUEUES.get(queue);
            if (values == null || values.isEmpty()) {
                return null;
            }
            int index = item == null || item <= 0 ? 0 : item - 1;
            return index >= values.size() ? null : values.get(index);
        }

        @Override
        public void deleteTs(String queue) {
            LOCAL_TS_QUEUES.remove(queue);
        }
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

    private static final class HandleFrame {
        private final Map<Integer, HandlerAction> conditionHandlers = new LinkedHashMap<>();
        private HandlerAction errorHandler;
        private HandlerAction abendHandler;

        private HandleFrame copy() {
            HandleFrame copy = new HandleFrame();
            copy.conditionHandlers.putAll(conditionHandlers);
            copy.errorHandler = errorHandler;
            copy.abendHandler = abendHandler;
            return copy;
        }
    }

    public record ConditionHandler(String condition, int resp, HandlerAction action) {
    }

    public record Response<T>(T value, int resp, int resp2) {
    }

    public record StartRequest(String transid, Object interval, Object from, Integer length, Object reqid,
                               Object termid) {
    }

    public record ReturnRequest(String transid, Object commarea, Integer length) {
    }
}
