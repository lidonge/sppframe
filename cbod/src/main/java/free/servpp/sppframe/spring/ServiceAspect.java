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
    @Around("execution(free.cobol2java.java.CicsCommandStatus free.cobol2java.java.CicsTransactionService+.*(..))")
    public Object aroundCicsTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundValueService(joinPoint);
    }

    @Around("execution(free.cobol2java.java.SqlCursorStatus free.cobol2java.java.SqlCursorService+.*(..)) || execution(free.cobol2java.java.SqlReadResult free.cobol2java.java.SqlCursorService+.*(..))")
    public Object aroundSqlCursor(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundValueService(joinPoint);
    }

    @Around("execution(free.cobol2java.java.SqlWriteResult free.cobol2java.java.SqlWriteService+.*(..))")
    public Object aroundSqlWrite(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundValueService(joinPoint);
    }

    @Around("execution(free.cobol2java.java.SqlReadResult free.cobol2java.java.SqlReadService+.read(..))")
    public Object aroundSqlRead(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundValueService(joinPoint);
    }

//    @Before("execution(* *(..))")
//    public void test(JoinPoint joinPoint){
//        System.out.println("Aspect：" + joinPoint.getSignature().getName());
//    }
    @Around("execution(void free.cobol2java.java.IService+.*(..)) && execution(void procedure(..))")
    public ISppException aroundAtomicService1(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundAtomicService(joinPoint);
    }
}
