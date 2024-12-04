package free.servpp.sppframe.impls;

import free.servpp.sppframe.IAtomicService;
import free.servpp.sppframe.common.IServiceContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lidong@date 2024-12-04@version 1.0
 */
public class ServiceContainer<T extends IAtomicService> implements IServiceContainer {
    private Map<Class<T>, IAtomicService> serviceMap = new HashMap<>();
    private Map<Class<T>, String> serviceNames = new HashMap<>();
    private Map<String, Class<T>> serviceClasses = new HashMap<>();
    @Override
    public <T> T getService(Class<T> serviceClass) {
        T ret = (T) serviceMap.get(serviceClass);
        if(ret == null){
            ret = (T) ServiceContainerTool.getServiceName(serviceClass);
        }
        return ret;
    }

    @Override
    public Object getService(String serviceName) {
        return getService(serviceClasses.get(serviceName));
    }

    @Override
    public String getServiceName(Class<?> clazz) {
        return serviceNames.get(clazz);
    }
}
