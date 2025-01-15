package free.cobol2java.java;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author lidong@date 2024-10-17@version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)  // 注解在运行时可用
public @interface FieldInfo {
    String cobolType() default "9";
    int levelNumber() default 1;
    String description() default ""; // 字段描述

    String usageType() default "";
}