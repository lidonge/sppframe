package free.servpp.sppframe.common;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
public interface IAtomicExecutor {
    default ISppException process(ISppContext context, String serviceName, Object[] args, ProceedingJoinPoint joinPoint) throws Throwable {
        ISppException ret = null;
        Class dynClass = getPolymorphicClass(context, serviceName);
        if(dynClass != null) {
            Method method = null;
            for (Method m : dynClass.getDeclaredMethods()) {
                m.setAccessible(true);
                if (!getExecuteFunctionName().equals(m.getName()))
                    continue;
                method = m;
                break;
            }
            ret = (ISppException) method.invoke(args);
        }else
            ret = (ISppException) joinPoint.proceed();
        return ret;
    }

    //TODO for asyn call
    Class getPolymorphicClass(ISppContext context, String serviceName);

    String getExecuteFunctionName();
}
