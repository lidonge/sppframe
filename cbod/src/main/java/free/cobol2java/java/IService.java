package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-25@version 1.0
 * Common service contract for generated runtime services that expose a procedure entry point.
 */
public interface IService {

    /**
     * Executes a method named {@code procedure} with dynamically provided parameters.
     * Reflection is used to find the overload with the same parameter count.
     *
     * @param parameters parameters forwarded to the procedure method
     * @return the invoked result, or null when no compatible method is found or invocation fails
     */
    default Object execute(Object... parameters) {
        try {
            java.lang.reflect.Method method = null;

            for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                if (m.getName().equals("procedure") && m.getParameterCount() == parameters.length) {
                    method = m;
                    break;
                }
            }

            if (method == null) {
                throw new NoSuchMethodException("No matching procedure method found.");
            }

            method.setAccessible(true);

            return method.invoke(this, parameters);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
