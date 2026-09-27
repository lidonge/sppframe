package free.cobol2java.java;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

/** SppFrame repository marker; the default runtime maps it to Spring. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@org.springframework.stereotype.Repository
public @interface Repository {
    @AliasFor(annotation = org.springframework.stereotype.Repository.class, attribute = "value")
    String value() default "";
}
