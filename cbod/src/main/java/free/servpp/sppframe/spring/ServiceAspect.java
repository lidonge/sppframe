package free.servpp.sppframe.spring;

import free.servpp.sppframe.common.IServiceAspect;
import free.servpp.sppframe.common.ISppException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author lidong@date 2024-08-07@version 1.0
 */
@Aspect
@Component
public class ServiceAspect implements IServiceAspect {
//    @Before("execution(* *(..))")
//    public void test(JoinPoint joinPoint){
//        System.out.println("Aspect：" + joinPoint.getSignature().getName());
//    }
    @Around("execution(void free.cobol2java.java.IService+.*(..)) && execution(void procedure(..))")
    public ISppException aroundAtomicService1(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundAtomicService(joinPoint);
    }
}
