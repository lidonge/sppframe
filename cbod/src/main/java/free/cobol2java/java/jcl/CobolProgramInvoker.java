package free.cobol2java.java.jcl;

import free.cobol2java.java.IService;
import free.cobol2java.java.ServiceManager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CobolProgramInvoker {
    private static final String JAVA_SERVICE_CLASS = "JAVA_SERVICE_CLASS";

    private CobolProgramInvoker() {
    }

    public static int invoke(JclStep step) throws Exception {
        if (step == null) {
            throw new IllegalArgumentException("JCL step is required");
        }
        return invoke(resolveServiceName(step), step.getProgram(), step.getParmArray());
    }

    public static int invoke(String programName, String[] args) throws Exception {
        return invoke(programName, programName, args);
    }

    private static int invoke(String serviceName, String programName, String[] args) throws Exception {
        IService service = serviceByClassName(serviceName);
        if (service == null) {
            service = ServiceManager.getService(serviceName);
        }
        if (service == null && programName != null && !programName.equals(serviceName)) {
            service = ServiceManager.getService(programName);
        }
        if (service == null) {
            throw new IllegalStateException("Missing COBOL program service: " + serviceName);
        }
        Object result = service.execute((Object[]) (args == null ? new String[0] : args));
        Integer returnCode = ServiceManager.getReturnCode(service);
        if ((returnCode == null || returnCode == 0) && result instanceof Number number) {
            return number.intValue();
        }
        return returnCode == null ? 0 : returnCode;
    }

    private static String resolveServiceName(JclStep step) {
        String stepValue = param(step.getParameters(), JAVA_SERVICE_CLASS);
        if (stepValue != null && !stepValue.isBlank()) {
            return stepValue;
        }
        for (JclDd dd : step.getDds()) {
            String value = param(dd.getParameters(), JAVA_SERVICE_CLASS);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return step.getProgram();
    }

    private static String param(Map<String, String> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static IService serviceByClassName(String className) throws Exception {
        if (className == null || className.isBlank() || className.indexOf('.') < 0) {
            return null;
        }
        Class<?> serviceClass = null;
        for (ClassLoader classLoader : candidateClassLoaders()) {
            try {
                serviceClass = Class.forName(className, true, classLoader);
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (serviceClass == null || !IService.class.isAssignableFrom(serviceClass)) {
            return null;
        }
        Constructor<?> constructor = serviceClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (IService) constructor.newInstance();
    }

    private static List<ClassLoader> candidateClassLoaders() {
        List<ClassLoader> result = new ArrayList<>();
        addClassLoader(result, Thread.currentThread().getContextClassLoader());
        addClassLoader(result, CobolProgramInvoker.class.getClassLoader());
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream.map(StackWalker.StackFrame::getDeclaringClass)
                        .map(Class::getClassLoader)
                        .peek(loader -> addClassLoader(result, loader))
                        .toList());
        return result;
    }

    private static void addClassLoader(List<ClassLoader> classLoaders, ClassLoader classLoader) {
        if (classLoader != null && !classLoaders.contains(classLoader)) {
            classLoaders.add(classLoader);
        }
    }
}
