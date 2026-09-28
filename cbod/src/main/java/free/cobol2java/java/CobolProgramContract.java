package free.cobol2java.java;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Runtime projection of the source-derived program transaction sidecar. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CobolProgramContract {
    int schema();
    String programId();
    Effect effect();

    enum Effect { NONE, RESOURCE_ONLY, COMPLETES_UOW, UNSUPPORTED }
}
