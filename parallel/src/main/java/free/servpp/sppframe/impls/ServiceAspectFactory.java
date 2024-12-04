package free.servpp.sppframe.impls;

import free.servpp.sppframe.common.*;

import java.util.HashMap;
import java.util.Map;

public class ServiceAspectFactory implements IServiceAspectFactory {
    private Map<String, IAtomicBefore> atomicBeforeHashMap = new HashMap<String, IAtomicBefore>();
    private Map<String, IAtomicExceptionHandler> atomicExceptionHandlerHashMap = new HashMap<String, IAtomicExceptionHandler>();
    private Map<String, IAtomicAfter> atomicAfterMap = new HashMap<String, IAtomicAfter>();
    private Map<String, IAtomicExecutor> atomicExecutorMap = new HashMap<String, IAtomicExecutor>();

    public ServiceAspectFactory() {
    }

    @Override
    public IAtomicBefore getAtomicBefore(String serviceName) {
        return atomicBeforeHashMap.get(serviceName);
    }

    @Override
    public IAtomicExceptionHandler getAtomicExceptionHandler(String serviceName) {
        return atomicExceptionHandlerHashMap.get(serviceName);
    }

    @Override
    public IAtomicAfter getAtomicAfter(String serviceName) {
        return atomicAfterMap.get(serviceName);
    }

    @Override
    public IAtomicExecutor getAtomicExecutor(String serviceName) {
        IAtomicExecutor ret =  atomicExecutorMap.get(serviceName);
        if(ret == null)
            ret = new DefaultAtomicExecutor();
        return ret;
    }
}