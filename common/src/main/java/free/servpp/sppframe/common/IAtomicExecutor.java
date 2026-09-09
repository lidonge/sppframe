package free.servpp.sppframe.common;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * @author lidong@date 2024-08-08@version 1.0
 */
public interface IAtomicExecutor {
    /** Value-returning operation; Object is confined to the reflective AOP boundary. */
    default Object processValue(ISppContext context, String serviceName, Object[] args,
                                ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> implementation = getPolymorphicClass(context, serviceName);
        if (implementation == null) {
            return joinPoint.proceed();
        }
        var signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
        Method method = implementation.getMethod(signature.getName(), signature.getParameterTypes());
        Object receiver = context.getServiceContainer().getService(implementation);
        if (!implementation.isInstance(receiver)) {
            throw new IllegalStateException("Missing service implementation: " + implementation.getName());
        }
        try {
            return method.invoke(receiver, args);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    default ISppException process(ISppContext context, String serviceName, Object[] args, ProceedingJoinPoint joinPoint) throws Throwable {
        ISppException ret = null;
        Class dynClass = getPolymorphicClass(context, serviceName);
        if(dynClass != null) {
            Method method = null;
            //FIXME should prepare method
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
