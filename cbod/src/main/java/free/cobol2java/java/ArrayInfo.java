package free.cobol2java.java;

/**
 * @author lidong@date 2024-10-24@version 1.0
 */
public @interface ArrayInfo {
    int maxLength() default Integer.MAX_VALUE;

    String dependOn() default "";

    String sortFields() default "";

    String order() default "";

    String end();
}
