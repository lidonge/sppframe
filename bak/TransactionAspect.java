package free.servpp.sppframe;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
@Aspect
public class TransactionAspect {
//    @Before("execution(void free.servpp.sppframe.IScenario.execTransaction(free.servpp.sppframe.ISppContext,..))")
//    public void beforeTransaction(JoinPoint joinPoint){
//
//    }
//
//    @After("execution(void free.servpp.sppframe.IScenario.execTransaction(free.servpp.sppframe.ISppContext,..))")
//    public void afterTransaction(JoinPoint joinPoint){
//
//    }

    @Around("execution(void free.servpp.sppframe.IScenario.execTransaction(free.servpp.sppframe.ISppContext,..))")
    public Object aroundTransaction(ProceedingJoinPoint joinPoint) throws Throwable{
        return joinPoint.proceed();
    }

    @AfterThrowing(pointcut = "execution(void free.servpp.sppframe.IScenario.execTransaction(free.servpp.sppframe.ISppContext,..))", throwing = "error")
    public void afterThrowingTransaction(JoinPoint joinPoint, Throwable error){

    }

    @AfterReturning(pointcut = "execution(void free.servpp.sppframe.IScenario.execTransaction(free.servpp.sppframe.ISppContext,..))", returning = "result")
    public void afterReturningTransaction(JoinPoint joinPoint, Object result){

    }
}
