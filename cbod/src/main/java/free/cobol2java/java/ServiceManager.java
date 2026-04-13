package free.cobol2java.java;


import free.servpp.sppframe.common.IServiceContainer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lidong@date 2024-10-25@version 1.0
 */
@Component
public class ServiceManager {
    private static final Map<Class<?>, IService> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> RETURN_CODES = new ConcurrentHashMap<>();
    private static IServiceContainer springServiceContainer;

    @Autowired
    private IServiceContainer serviceContainer;

    public static <T> T getService(Class<T> serviceClass) {
        if (springServiceContainer != null) {
            T service = springServiceContainer.getService(serviceClass);
            if (service != null) {
                return service;
            }
        }
        if (serviceClass == null || !IService.class.isAssignableFrom(serviceClass)) {
            return null;
        }
        return serviceClass.cast(CLASS_CACHE.computeIfAbsent(serviceClass, ServiceManager::createReflectiveService));
    }

    public static IService getService(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (springServiceContainer != null) {
            Object service = springServiceContainer.getService(name);
            if (service instanceof IService iService) {
                return iService;
            }
        }
        for (String candidate : buildCandidateClassNames(name)) {
            try {
                return getService((Class<? extends IService>) Class.forName(candidate));
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    public static IServiceContainer getServiceContainer(){
        return springServiceContainer;
    }

    public static Integer getReturnCode(Object owner) {
        return RETURN_CODES.getOrDefault(returnCodeKey(owner), 0);
    }

    public static Integer setReturnCode(Object owner, Object value) {
        Integer code = Util.copyCastToInteger(value);
        RETURN_CODES.put(returnCodeKey(owner), code == null ? 0 : code);
        return code;
    }

    @PostConstruct
    private void init() {
        springServiceContainer = serviceContainer;
    }

    private static IService createReflectiveService(Class<?> targetClass) {
        return new IService() {
            @Override
            public Object execute(Object... parameters) {
                try {
                    Object target = targetClass.getDeclaredConstructor().newInstance();
                    Method method = findProcedureMethod(targetClass, parameters);
                    if (method == null) {
                        return null;
                    }
                    method.setAccessible(true);
                    Object[] args = adaptArguments(method, parameters);
                    return method.invoke(target, args);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to invoke service " + targetClass.getName(), e);
                }
            }
        };
    }

    private static Method findProcedureMethod(Class<?> targetClass, Object[] parameters) {
        Method[] methods = targetClass.getMethods();
        Method fallback = null;
        int argCount = parameters == null ? 0 : parameters.length;
        for (Method method : methods) {
            if (!"procedure".equals(method.getName())) {
                continue;
            }
            if (method.isVarArgs()) {
                return method;
            }
            if (method.getParameterCount() == argCount && parametersMatch(method.getParameterTypes(), parameters)) {
                return method;
            }
            if (fallback == null && method.getParameterCount() == 0 && argCount == 0) {
                fallback = method;
            }
        }
        return fallback;
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] parameters) {
        if (parameterTypes == null) {
            return parameters == null || parameters.length == 0;
        }
        if (parameters == null) {
            return parameterTypes.length == 0;
        }
        if (parameterTypes.length != parameters.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = parameters[i];
            if (arg == null) {
                continue;
            }
            Class<?> expected = wrap(parameterTypes[i]);
            if (!expected.isInstance(arg)) {
                return false;
            }
        }
        return true;
    }

    private static Object[] adaptArguments(Method method, Object[] parameters) {
        if (!method.isVarArgs()) {
            return parameters == null ? new Object[0] : parameters;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        int fixedCount = parameterTypes.length - 1;
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < fixedCount; i++) {
            args[i] = parameters != null && i < parameters.length ? parameters[i] : null;
        }
        Class<?> componentType = parameterTypes[parameterTypes.length - 1].getComponentType();
        int varArgLength = parameters == null ? 0 : Math.max(0, parameters.length - fixedCount);
        Object varArgArray = Array.newInstance(componentType, varArgLength);
        for (int i = 0; i < varArgLength; i++) {
            Array.set(varArgArray, i, parameters[fixedCount + i]);
        }
        args[args.length - 1] = varArgArray;
        return args;
    }

    private static Class<?> wrap(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return type == null ? Object.class : type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private static List<String> buildCandidateClassNames(String programName) {
        List<String> candidates = new ArrayList<>();
        String trimmed = programName.trim();
        candidates.add(trimmed);
        String simple = normalizeProgramSimpleName(trimmed);
        if (!simple.equals(trimmed)) {
            candidates.add(simple);
        }
        String callerPackage = callerPackage();
        if (callerPackage != null) {
            candidates.add(callerPackage + "." + simple);
            candidates.add(callerPackage + ".external." + simple);
        }
        return candidates;
    }

    private static String normalizeProgramSimpleName(String programName) {
        String cleaned = programName.replace("'", "").replace("\"", "").trim();
        if (cleaned.isBlank()) {
            return cleaned;
        }
        if (cleaned.indexOf('.') != -1) {
            return cleaned;
        }
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String callerPackage() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream
                        .map(StackWalker.StackFrame::getDeclaringClass)
                        .map(Class::getPackageName)
                        .filter(name -> name != null && !name.isBlank() && !name.equals(ServiceManager.class.getPackageName()))
                        .findFirst()
                        .orElse(null));
    }

    private static String returnCodeKey(Object owner) {
        return owner == null ? "<null>" : owner.getClass().getName() + "@" + System.identityHashCode(owner);
    }
}
