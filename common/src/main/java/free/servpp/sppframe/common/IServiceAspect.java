package free.servpp.sppframe.common;

import free.servpp.sppframe.common.util.ILogable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;

/**
 * @author lidong@date 2024-12-03@version 1.0
 */
public interface IServiceAspect extends ILogable {
    /** Preserve the operation value and propagate failures to the caller's status handling. */
    default Object aroundValueService(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        ISppContext context = ISppContext.getSppContext();
        String serviceName = context.getServiceContainer().getServiceName(joinPoint.getTarget().getClass());
        IServiceAspectFactory factory = context.getServiceAspectFactory();
        Throwable failure = null;
        try {
            if (context.isInSerialBlock() && !context.canExecute(serviceName)) {
                throw new IllegalStateException("Value service cannot be skipped: " + serviceName);
            }
            IAtomicBefore before = factory.getAtomicBefore(serviceName);
            if (before != null && before.process(context, args, joinPoint)) {
                throw new IllegalStateException("Value service requires a result: " + serviceName);
            }
            return factory.getAtomicExecutor(serviceName).processValue(context, serviceName, args, joinPoint);
        } catch (Throwable thrown) {
            failure = thrown;
            IAtomicExceptionHandler handler = factory.getAtomicExceptionHandler(serviceName);
            if (handler != null) {
                try {
                    handler.process(context, args, thrown);
                } catch (Throwable handlerFailure) {
                    if (handlerFailure != thrown) thrown.addSuppressed(handlerFailure);
                }
            }
            throw thrown;
        } finally {
            IAtomicAfter after = factory.getAtomicAfter(serviceName);
            if (after != null) {
                try {
                    after.process(context, args, failure, null);
                } catch (Throwable afterFailure) {
                    if (failure == null) throw afterFailure;
                    if (afterFailure != failure) failure.addSuppressed(afterFailure);
                }
            }
        }
    }

    default ISppException aroundAtomicService(ProceedingJoinPoint joinPoint) throws Throwable{
        Object[] args = joinPoint.getArgs();
        ISppContext context = ISppContext.getSppContext();
        Class<?> aClass = joinPoint.getTarget().getClass();
        String serviceName = context.getServiceContainer().getServiceName(aClass);
        Logger logger = getLogger();
        logger.info("Aspect aroundAtomicService:{}" , serviceName);
        if (context.isInSerialBlock() && !context.canExecute(serviceName)) {
            return null;
        }

        Throwable throwable = null;
        ISppException error = null;
        IServiceAspectFactory factory = context.getServiceAspectFactory();
        try {
            IAtomicBefore before = factory.getAtomicBefore(serviceName);
            logger.info("Before execute service: {}", serviceName);
            boolean exit = before == null ? false : before.process(context,args, joinPoint);
            if (!exit) {
                //for polymorphic
                IAtomicExecutor executor = factory.getAtomicExecutor(serviceName);
                logger.info("Executing service: {}", serviceName);
                error = executor.process(context, serviceName, args, joinPoint);
                if (error != null) {
                    IAtomicExceptionHandler exception = factory.getAtomicExceptionHandler(serviceName);
                    logger.info("Service Error processing: {}, error: {}", serviceName, error);
                    exception.process(context,args, error);
                }
            }
        } catch (Throwable t) {
            IAtomicExceptionHandler exception = factory.getAtomicExceptionHandler(serviceName);
            logger.info("Service Exception processing: {}, exception: {}", serviceName, t);
            exception.process(context, args, t);
        } finally {
            IAtomicAfter after = factory.getAtomicAfter(serviceName);
            if(after != null) {
                logger.info("After Service: {}, error: {}", serviceName, error);
                after.process(context, args, throwable, error);
            }
        }
        return error;
    }

    default void aroundSerial(ProceedingJoinPoint joinPoint) throws Throwable{
        Object[] args = joinPoint.getArgs();
        ISppContext context = ISppContext.getSppContext();
        String name = (String) args[0];
        getLogger().info("Aspect aroundSerial:{}",name);
        context.enterSerialBlock(new Block(IBlock.BlockType.Serial,name));
        try {
            joinPoint.proceed();
        } catch (Throwable t) {
            IRunBlockExceptionHandler exception = context.getRunBlockExceptionHandler();
            exception.process(context,args, t);
        } finally {
            IRunBlockExceptionHandler exception = context.getRunBlockExceptionHandler();
            exception.process(context, args, (ISppException) null);
            context.exitSerialBlock();
        }
    }

    default void aroundParallel(ProceedingJoinPoint joinPoint) throws Throwable{
        Object[] args = joinPoint.getArgs();
        ISppContext context = ISppContext.getSppContext();
        String name = (String) args[0];
        getLogger().info("Aspect aroundParallel:{}",name);
        context.enterParallelBlock(new Block(IBlock.BlockType.Parallel,name));
        try {
            joinPoint.proceed();
        } catch (Throwable t) {
            IRunBlockExceptionHandler exception = context.getRunBlockExceptionHandler();
            exception.process(context, args, t);
        } finally {
            IRunBlockExceptionHandler exception = context.getRunBlockExceptionHandler();
            exception.process(context, args, (ISppException) null);
            context.exitParallelBlock();
        }
    }

    default void aroundTransaction(ProceedingJoinPoint joinPoint) throws Throwable{
        Object[] args = joinPoint.getArgs();
        ISppContext context = ISppContext.getSppContext();
        String name = (String) args[0];
        getLogger().info("Aspect aroundTransaction:{}",name);
        context.enterTransactionBlock(new Block(IBlock.BlockType.Transaction,name));
        try {
            joinPoint.proceed();
        } catch (Throwable t) {
            ITransactionExceptionHandler exception = context.getTransactionExceptionHandler();
            exception.process(context,args, t);
        } finally {
            ITransactionExceptionHandler exception = context.getTransactionExceptionHandler();
            exception.process(context,args, (ISppException) null);
            context.exitTransactionBlock();
        }
    }
}
