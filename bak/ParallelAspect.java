package free.servpp.sppframe;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
@Aspect
public class ParallelAspect {
//    @Before("execution(void free.servpp.sppframe.IScenario.execParallel(free.servpp.sppframe.ISppContext,..))")
//    public void beforeParallel(JoinPoint joinPoint){
//
//    }
//
//    @After("execution(void free.servpp.sppframe.IScenario.execParallel(free.servpp.sppframe.ISppContext,..))")
//    public void afterParallel(JoinPoint joinPoint){
//
//    }

    @Around("execution(void free.servpp.sppframe.IScenario.execParallel(free.servpp.sppframe.ISppContext,..))")
    public Object aroundParallel(ProceedingJoinPoint joinPoint) throws Throwable{
        return joinPoint.proceed();
    }

    @AfterThrowing(pointcut ="execution(void free.servpp.sppframe.IScenario.execParallel(free.servpp.sppframe.ISppContext,..))", throwing = "error")
    public void afterThrowingParallel(JoinPoint joinPoint, Throwable error){

    }

    @AfterReturning(pointcut = "execution(void free.servpp.sppframe.IScenario.execParallel(free.servpp.sppframe.ISppContext,..))", returning = "result")
    public void afterReturningParallel(JoinPoint joinPoint, Object result){

    }
}
