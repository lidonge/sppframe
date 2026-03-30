package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-24@version 1.0
 * Metadata annotation describing COBOL array characteristics on generated fields.
 */
public @interface ArrayInfo {
    /**
     * Returns the maximum logical length of the array.
     */
    int maxLength() default Integer.MAX_VALUE;

    /**
     * Returns the field name that controls the effective length, when present.
     */
    String dependOn() default "";

    /**
     * Returns the comma-separated sort key fields for SEARCH ALL style access.
     */
    String sortFields() default "";

    /**
     * Returns the declared sort order metadata.
     */
    String order() default "";

    /**
     * Returns the COBOL name or marker identifying the array end element.
     */
    String end();
}
