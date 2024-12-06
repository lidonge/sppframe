package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-25@version 1.0
 */
public interface IService {

    /**
     * Execute a method named "procedure" with dynamically provided parameters.
     * This uses reflection to find and invoke the method at runtime.
     *
     * @param parameters The parameters to pass to the "procedure" method.
     * @return The result of the "procedure" method, or null if no matching method is found or an exception occurs.
     */
    default Object execute(Object... parameters) {
        try {
            // Initialize a variable to hold the matched method
            java.lang.reflect.Method method = null;

            // Iterate over all declared methods in the implementing class
            for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                // Match method by name and parameter count
                if (m.getName().equals("procedure") && m.getParameterCount() == parameters.length) {
                    method = m;
                    break;
                }
            }

            // If no matching method is found, throw an exception
            if (method == null) {
                throw new NoSuchMethodException("No matching procedure method found.");
            }

            // Make the method accessible (useful for private or protected methods)
            method.setAccessible(true);

            // Invoke the method dynamically with the provided parameters
            return method.invoke(this, parameters);

        } catch (Exception e) {
            // Catch and handle reflection-related exceptions
            e.printStackTrace();
            return null;
        }
    }
}
