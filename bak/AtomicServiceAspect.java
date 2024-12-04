package free.servpp.sppframe;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
@Aspect
public class AtomicServiceAspect {
//    @Before("execution(void free.servpp.sppframe.IAtomicService+.*(..)) && execution(void execute(..))")
//    public void beforeAtomicService(JoinPoint joinPoint){
//        System.out.println("execute atomic service");
//    }

//    @After("execution(void free.servpp.sppframe.IAtomicService.execute(..))")
//    public void afterAtomicService(JoinPoint joinPoint){
//
//    }
//
    @Around("execution(void free.servpp.sppframe.IAtomicService+.*(..)) && execution(void execute(..))")
    public Object aroundAtomicService(ProceedingJoinPoint joinPoint) throws Throwable{
        System.out.println("execute atomic service1");
        return joinPoint.proceed();
    }

    @AfterThrowing(pointcut = "execution(void free.servpp.sppframe.IAtomicService+.*(..)) && execution(void execute(..))", throwing = "error")
    public void afterThrowingAtomicService(JoinPoint joinPoint, Throwable error){

    }

    @AfterReturning(pointcut = "execution(void free.servpp.sppframe.IAtomicService+.*(..)) && execution(void execute(..))", returning = "result")
    public void afterReturningAtomicService(JoinPoint joinPoint, Object result){

    }
}
