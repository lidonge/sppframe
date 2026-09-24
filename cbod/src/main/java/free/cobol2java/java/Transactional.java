package free.cobol2java.java;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

/** Framework-owned composed annotation for generated transactional methods. */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@org.springframework.transaction.annotation.Transactional
public @interface Transactional {
    /** Result-directed source completion, interpreted by the runtime region aspect. */
    boolean resultDriven() default false;

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class,
            attribute = "transactionManager")
    String transactionManager() default "";

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class, attribute = "label")
    String[] label() default {};

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class,
            attribute = "propagation")
    org.springframework.transaction.annotation.Propagation propagation()
            default org.springframework.transaction.annotation.Propagation.REQUIRED;

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class, attribute = "isolation")
    org.springframework.transaction.annotation.Isolation isolation()
            default org.springframework.transaction.annotation.Isolation.DEFAULT;

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class, attribute = "timeout")
    int timeout() default org.springframework.transaction.TransactionDefinition.TIMEOUT_DEFAULT;

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class,
            attribute = "timeoutString")
    String timeoutString() default "";

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class, attribute = "rollbackFor")
    Class<? extends Throwable>[] rollbackFor() default {};

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class,
            attribute = "rollbackForClassName")
    String[] rollbackForClassName() default {};

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class,
            attribute = "noRollbackFor")
    Class<? extends Throwable>[] noRollbackFor() default {};

    @AliasFor(annotation = org.springframework.transaction.annotation.Transactional.class,
            attribute = "noRollbackForClassName")
    String[] noRollbackForClassName() default {};
}
