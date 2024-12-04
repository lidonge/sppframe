package free.servpp.sppframe.common;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
public interface IAtomicBefore {
    default boolean process(ISppContext context, Object[] args, ProceedingJoinPoint joinPoint){
        //TODO
        return false;
    }
}
