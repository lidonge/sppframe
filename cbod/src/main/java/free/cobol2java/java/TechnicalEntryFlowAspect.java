package free.cobol2java.java;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/** Routes configured IService.execute calls through the generated stage entry. */
@Aspect
@Component
public final class TechnicalEntryFlowAspect {
    @Around("execution(* free.cobol2java.java.IService+.execute(..))")
    public Object dispatch(ProceedingJoinPoint call) throws Throwable {
        Object target = call.getTarget();
        if (target == null || AnnotationUtils.findAnnotation(target.getClass(),
                TechnicalEntryFlow.class) == null) {
            return call.proceed();
        }
        if (!(target instanceof TechnicalEntryFlowService entry)) {
            throw new IllegalStateException("Annotated COBOL program lacks its generated entry flow");
        }
        Object[] invocation = call.getArgs();
        if (invocation.length != 1
                || (invocation[0] != null && !(invocation[0] instanceof Object[]))) {
            throw new IllegalArgumentException("Invalid IService.execute entry flow arguments");
        }
        Object[] parameters = invocation[0] == null ? new Object[0] : (Object[]) invocation[0];
        try {
            return entry.executeTechnicalEntry(parameters);
        } catch (CobolRollbackSignal | CobolProgramTransactionExit signal) {
            throw signal;
        } catch (Error failure) {
            throw failure;
        } catch (ServiceInvocationException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new ServiceInvocationException(
                    "Service invocation failed: " + target.getClass().getName(), failure);
        }
    }
}
