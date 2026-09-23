package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-25@version 1.0
 * Common service contract for generated runtime services that expose a procedure entry point.
 */
public interface IService {

    /** Invokes the generated business entry point and preserves invocation failures. */
    default Object invoke(Object... parameters) {
        Object[] actualParameters = parameters == null ? new Object[0] : parameters;
        java.lang.reflect.Method method = null;
        for (java.lang.reflect.Method candidate : procedureMethods(this.getClass())) {
            if (!candidate.getName().equals("procedure") || candidate.isVarArgs()
                    || candidate.getParameterCount() != actualParameters.length) {
                continue;
            }
            Class<?>[] parameterTypes = candidate.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (actualParameters[i] != null && !parameterTypes[i].isInstance(actualParameters[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                method = candidate;
                break;
            }
        }
        if (method == null) {
            throw new ServiceInvocationException("No compatible procedure method on " + getClass().getName());
        }
        try {
            method.setAccessible(true);
            return method.invoke(this, actualParameters);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new ServiceInvocationException("Service invocation failed: " + getClass().getName(), cause);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new ServiceInvocationException("Service invocation failed: " + getClass().getName(), e);
        }
    }

    /**
     * Executes a method named {@code procedure} with dynamically provided parameters.
     * Reflection is used to find the overload with the same parameter count.
     *
     * @param parameters parameters forwarded to the procedure method
     * @return the invoked result, or null when no compatible method is found or invocation fails
     */
    default Object execute(Object... parameters) {
        try {
            Object[] actualParameters = parameters == null ? new Object[0] : parameters;
            java.lang.reflect.Method method = null;

            for (java.lang.reflect.Method m : procedureMethods(this.getClass())) {
                if (m.getName().equals("procedure")
                        && !m.isVarArgs()
                        && m.getParameterCount() == actualParameters.length) {
                    method = m;
                    break;
                }
            }
            if (method == null) {
                for (java.lang.reflect.Method m : procedureMethods(this.getClass())) {
                    if (m.getName().equals("procedure")
                            && m.getParameterCount() == 1
                            && m.getParameterTypes()[0].isArray()) {
                        method = m;
                        break;
                    }
                }
            }

            if (method == null) {
                throw new NoSuchMethodException("No matching procedure method found.");
            }

            method.setAccessible(true);

            if (method.isVarArgs()
                    || (method.getParameterCount() == 1 && method.getParameterTypes()[0].isArray())) {
                return method.invoke(this, new Object[]{actualParameters});
            }
            return method.invoke(this, actualParameters);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static java.util.List<java.lang.reflect.Method> procedureMethods(Class<?> type) {
        java.util.List<java.lang.reflect.Method> methods = new java.util.ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (java.lang.reflect.Method method : current.getDeclaredMethods()) {
                methods.add(method);
            }
        }
        return methods;
    }
}
