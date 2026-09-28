package free.cobol2java.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Dispatches a runtime COBOL program name using its generated transaction contract. */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class DynamicCobolCallAspect {
    @Around("execution(* free.cobol2java.java.DynamicCobolCallDispatcher.dispatch(..))"
            + " && @annotation(free.cobol2java.java.CobolCallDispatch)")
    public Object dispatch(ProceedingJoinPoint call) {
        Object[] invocation = call.getArgs();
        if (invocation.length != 3 || !(invocation[0] instanceof String sourcePosition)
                || !(invocation[1] instanceof String programName)
                || !(invocation[2] instanceof Object[] parameters))
            throw new IllegalStateException("Invalid dynamic COBOL CALL dispatch arguments");
        ServiceManager.ResolvedCobolProgram target = ServiceManager.resolveDynamicProgram(programName);
        CobolProgramContract contract = target.type().getAnnotation(CobolProgramContract.class);
        if (contract == null || contract.schema() != 9
                || !contract.programId().equalsIgnoreCase(programName.trim()))
            throw new IllegalStateException("Dynamic CALL transaction metadata mismatch at "
                    + sourcePosition + " target " + programName);
        return switch (contract.effect()) {
            case NONE -> target.service().execute(parameters);
            case RESOURCE_ONLY -> {
                requireActive(sourcePosition);
                yield target.service().execute(parameters);
            }
            case COMPLETES_UOW -> {
                requireActive(sourcePosition);
                invokeParticipation(target.service(), parameters, sourcePosition);
                throw new IllegalStateException("Dynamic CALL completion entry returned at "
                        + sourcePosition);
            }
            case UNSUPPORTED -> throw new IllegalStateException(
                    "Dynamic CALL target has no participating transaction contract at "
                            + sourcePosition + " target " + programName);
        };
    }

    private static void requireActive(String sourcePosition) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Dynamic CALL has no caller-owned UOW at "
                    + sourcePosition);
    }

    private static void invokeParticipation(IService service, Object[] parameters,
            String sourcePosition) {
        Method method = null;
        for (Method candidate : service.getClass().getMethods()) {
            if (!candidate.getName().equals("procedureParticipating")
                    || candidate.getParameterCount() != parameters.length) continue;
            boolean matches = true;
            Class<?>[] types = candidate.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];
                if (type.isPrimitive()) {
                    if (type == boolean.class) type = Boolean.class;
                    else if (type == byte.class) type = Byte.class;
                    else if (type == short.class) type = Short.class;
                    else if (type == int.class) type = Integer.class;
                    else if (type == long.class) type = Long.class;
                    else if (type == float.class) type = Float.class;
                    else if (type == double.class) type = Double.class;
                    else if (type == char.class) type = Character.class;
                }
                if ((parameters[i] == null && types[i].isPrimitive())
                        || (parameters[i] != null && !type.isInstance(parameters[i]))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                method = candidate;
                break;
            }
        }
        if (method == null) throw new ServiceUnavailableException(
                "Dynamic CALL participation entry is unavailable at " + sourcePosition);
        try {
            method.invoke(service, parameters);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause() == null ? failure : failure.getCause();
            if (cause instanceof CobolProgramTransactionExit completed) throw completed;
            if (cause instanceof CobolRollbackSignal rollback) throw rollback;
            if (cause instanceof Error error) throw error;
            throw new ServiceInvocationException(
                    "Dynamic CALL participation failed at " + sourcePosition, cause);
        } catch (ReflectiveOperationException | IllegalArgumentException failure) {
            throw new ServiceInvocationException(
                    "Dynamic CALL participation failed at " + sourcePosition, failure);
        }
    }
}
