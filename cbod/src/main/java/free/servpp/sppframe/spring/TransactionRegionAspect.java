package free.servpp.sppframe.spring;

import free.cobol2java.java.TransactionRegionResult;
import free.cobol2java.java.Transactional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Interprets source completion exits on the existing service proxy. */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TransactionRegionAspect {
    private final ObjectProvider<PlatformTransactionManager> managers;
    private static final ThreadLocal<Active> CURRENT = new ThreadLocal<>();

    public TransactionRegionAspect(ObjectProvider<PlatformTransactionManager> managers) {
        this.managers = managers;
    }

    @Around("@annotation(free.cobol2java.java.Transactional)")
    public Object aroundRegion(ProceedingJoinPoint call) throws Throwable {
        Transactional region = ((MethodSignature) call.getSignature()).getMethod()
                .getAnnotation(Transactional.class);
        if (region == null)
            throw new IllegalStateException("Transactional region annotation is not on the target method");
        if (!region.resultDriven()) return call.proceed();
        Object[] args = call.getArgs();
        if (args.length != 1 || args[0] == null)
            throw new IllegalStateException("A result-driven region needs one typed state argument");
        Object state = args[0];
        Active active = CURRENT.get();
        if (active == null) {
            if (TransactionSynchronizationManager.isActualTransactionActive())
                throw new IllegalStateException("Source completion of an externally owned transaction is unsupported");
            PlatformTransactionManager manager = managers.getIfAvailable();
            if (manager == null)
                throw new IllegalStateException("No transaction manager for result-driven COBOL region");
            TransactionStatus status = manager.getTransaction(new DefaultTransactionDefinition());
            if (!status.isNewTransaction())
                throw new IllegalStateException("Result-driven region did not obtain its own transaction");
            active = new Active(state, manager, status);
            CURRENT.set(active);
        } else if (active.state != state) {
            throw new IllegalStateException("A second COBOL state cannot join the pending transaction region");
        }

        Object value;
        try {
            value = call.proceed();
        } catch (Throwable failure) {
            rollbackAfterFailure(active, failure);
            throw failure;
        }
        if (!(value instanceof TransactionRegionResult<?, ?, ?> result) || result.state() != state
                || result.completion() != TransactionRegionResult.CompletionStatus.PENDING) {
            var failure = new IllegalStateException("Region did not return its pending typed source exit");
            rollbackAfterFailure(active, failure);
            throw failure;
        }
        return switch (result.request()) {
            case NONE -> result.observed(TransactionRegionResult.CompletionStatus.UNCHANGED, null);
            case COMMIT -> {
                CURRENT.remove();
                active.manager.commit(active.status);
                yield result.observed(TransactionRegionResult.CompletionStatus.COMMITTED, null);
            }
            case ROLLBACK -> {
                CURRENT.remove();
                active.manager.rollback(active.status);
                yield result.observed(TransactionRegionResult.CompletionStatus.ROLLED_BACK, null);
            }
        };
    }

    /** Explicit boundary cleanup; never silently commit a pending source work unit. */
    public static void abortIncomplete(Object state) {
        Active active = CURRENT.get();
        if (active == null) return;
        if (active.state != state)
            throw new IllegalStateException("Pending transaction belongs to a different COBOL state");
        CURRENT.remove();
        active.manager.rollback(active.status);
    }

    private void rollbackAfterFailure(Active active, Throwable failure) {
        CURRENT.remove();
        try {
            active.manager.rollback(active.status);
        } catch (Throwable rollbackFailure) {
            if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
        }
    }

    private record Active(Object state, PlatformTransactionManager manager, TransactionStatus status) {}
}
