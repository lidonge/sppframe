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
            if (parametersMatch(candidate.getParameterTypes(), actualParameters)) {
                method = candidate;
                break;
            }
        }
        if (method == null) {
            throw new ServiceUnavailableException("No compatible procedure method on " + getClass().getName());
        }
        try {
            method.setAccessible(true);
            return method.invoke(this, actualParameters);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof CobolRollbackSignal signal) throw signal;
            if (cause instanceof CobolProgramTransactionExit exit) throw exit;
            if (cause instanceof Error error) throw error;
            throw new ServiceInvocationException("Service invocation failed: " + getClass().getName(), cause);
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            throw new ServiceInvocationException("Service invocation failed: " + getClass().getName(), e);
        }
    }

    /**
     * Executes a method named {@code procedure} with dynamically provided parameters.
     * Reflection is used to find the overload with the same parameter count.
     *
     * @param parameters parameters forwarded to the procedure method
     * @return the invoked result
     * @throws ServiceInvocationException when the entry point cannot be invoked or fails
     */
    default Object execute(Object... parameters) {
        try {
            Object[] actualParameters = parameters == null ? new Object[0] : parameters;
            java.lang.reflect.Method method = null;

            for (java.lang.reflect.Method m : procedureMethods(this.getClass())) {
                if (m.getName().equals("procedure")
                        && !m.isVarArgs()
                        && m.getParameterCount() == actualParameters.length
                        && parametersMatch(m.getParameterTypes(), actualParameters)) {
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
                throw new ServiceUnavailableException("No compatible procedure method on "
                        + getClass().getName());
            }

            method.setAccessible(true);

            if (method.isVarArgs()
                    || (method.getParameterCount() == 1 && method.getParameterTypes()[0].isArray())) {
                return method.invoke(this, new Object[]{actualParameters});
            }
            return method.invoke(this, actualParameters);

        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof CobolRollbackSignal signal) throw signal;
            if (cause instanceof CobolProgramTransactionExit exit) throw exit;
            if (cause instanceof Error error) throw error;
            throw new ServiceInvocationException("Service invocation failed: " + getClass().getName(), cause);
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            throw new ServiceInvocationException("Service invocation failed: " + getClass().getName(), e);
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

    private static boolean parametersMatch(Class<?>[] types, Object[] values) {
        if (types.length != values.length) return false;
        for (int index = 0; index < types.length; index++) {
            Class<?> type = types[index];
            Object value = values[index];
            if (value == null) {
                if (type.isPrimitive()) return false;
                continue;
            }
            if (type.isPrimitive()) {
                if (type == boolean.class) type = Boolean.class;
                else if (type == byte.class) type = Byte.class;
                else if (type == short.class) type = Short.class;
                else if (type == int.class) type = Integer.class;
                else if (type == long.class) type = Long.class;
                else if (type == float.class) type = Float.class;
                else if (type == double.class) type = Double.class;
                else if (type == char.class) type = Character.class;
            }
            if (!type.isInstance(value)) return false;
        }
        return true;
    }
}
