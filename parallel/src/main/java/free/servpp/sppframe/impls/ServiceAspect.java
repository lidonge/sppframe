package free.servpp.sppframe.impls;

import free.servpp.sppframe.common.IServiceAspect;
import free.servpp.sppframe.common.ISppException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * @author lidong@date 2024-08-07@version 1.0
 */
@Aspect
public class ServiceAspect implements IServiceAspect {
//    @Before("execution(* *(..))")
//    public void test(JoinPoint joinPoint){
//        System.out.println("Aspect：" + joinPoint.getSignature().getName());
//    }
    @Around("execution(free.servpp.sppframe.common.ISppException free.servpp.sppframe.IAtomicService+.*(..)) && execution(free.servpp.sppframe.common.ISppException execute(..))")
    public ISppException aroundAtomicService1(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundAtomicService(joinPoint);
    }

    @Around("execution(void free.servpp.sppframe.IScenario.execSerial( ..))")
    public void aroundSerial1(ProceedingJoinPoint joinPoint) throws Throwable {
        aroundSerial(joinPoint);
    }

    @Around("execution(void free.servpp.sppframe.IScenario.execParallel(..))")
    public void aroundParallel1(ProceedingJoinPoint joinPoint) throws Throwable {
        aroundParallel(joinPoint);
    }

    @Around("execution(void free.servpp.sppframe.IScenario.execTransaction(..))")
    public void aroundTransaction1(ProceedingJoinPoint joinPoint) throws Throwable {
        aroundTransaction(joinPoint);
    }
}
