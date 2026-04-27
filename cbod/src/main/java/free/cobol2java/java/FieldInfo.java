package free.cobol2java.java;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author lidong@date 2024-10-17@version 1.0
 * Metadata annotation describing a generated field from COBOL source.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldInfo {
    /**
     * Returns the COBOL picture category or basic type code.
     */
    String cobolType() default "9";

    /**
     * Returns the COBOL level number for the field.
     */
    int levelNumber() default 1;

    /**
     * Returns the optional field description captured during translation.
     */
    String description() default "";

    /**
     * Returns the COBOL USAGE clause value when one was present.
     */
    String usageType() default "";

    int precision() default -1;

    int scale() default 0;

}
