package free.cobol2java.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime hooks for converted LINK/START commands.
 */
public final class CicsRuntime {
    private static final List<StartRequest> START_REQUESTS = new ArrayList<>();
    private static final List<ReturnRequest> RETURN_REQUESTS = new ArrayList<>();

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

    public static synchronized void clearStartedTransactions() {
        START_REQUESTS.clear();
    }

    public static synchronized StartRequest peekLastStartRequest() {
        return START_REQUESTS.isEmpty() ? null : START_REQUESTS.get(START_REQUESTS.size() - 1);
    }

    public static synchronized void returnControl(String transid, Object commarea, Integer length) {
        RETURN_REQUESTS.add(new ReturnRequest(transid, commarea, length));
        CicsUtil.returnControl();
    }

    public static synchronized void syncpoint() {
        CicsUtil.returnControl();
    }

    public static synchronized void syncpointRollback() {
        CicsUtil.returnControl();
    }

    public static synchronized void clearReturnRequests() {
        RETURN_REQUESTS.clear();
    }

    public static synchronized ReturnRequest peekLastReturnRequest() {
        return RETURN_REQUESTS.isEmpty() ? null : RETURN_REQUESTS.get(RETURN_REQUESTS.size() - 1);
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

    public record StartRequest(String transid, Object interval, Object from, Integer length, Object reqid,
                               Object termid) {
    }

    public record ReturnRequest(String transid, Object commarea, Integer length) {
    }
}
