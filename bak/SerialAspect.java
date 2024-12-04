package free.servpp.sppframe;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
@Aspect
public class SerialAspect {
//    @Before("execution(void free.servpp.sppframe.IScenario.execSerial(free.servpp.sppframe.ISppContext,..))")
//    public void beforeSerial(JoinPoint joinPoint){
//
//    }
//
//    @After("execution(void free.servpp.sppframe.IScenario.execSerial(free.servpp.sppframe.ISppContext,..))")
//    public void afterSerial(JoinPoint joinPoint){
//
//    }

    @Around("execution(void free.servpp.sppframe.IScenario.execSerial(free.servpp.sppframe.ISppContext,..))")
    public Object aroundSerial(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
    @AfterThrowing(pointcut ="execution(void free.servpp.sppframe.IScenario.execSerial(free.servpp.sppframe.ISppContext,..))", throwing = "error")
    public void afterThrowingSerial(JoinPoint joinPoint, Throwable error){

    }

    @AfterReturning(pointcut = "execution(void free.servpp.sppframe.IScenario.execSerial(free.servpp.sppframe.ISppContext,..))", returning = "result")
    public void afterReturningSerial(JoinPoint joinPoint, Object result){

    }

}
