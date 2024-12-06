package free.cobol2java.java;


import free.servpp.sppframe.common.IServiceContainer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lidong@date 2024-10-25@version 1.0
 */
@Component
public class ServiceManager {
    private static IServiceContainer springServiceContainer;

    @Autowired
    private IServiceContainer serviceContainer;
    public static <T> T getService(Class<T> serviceClass) {
        return springServiceContainer.getService(serviceClass);
    }

    public static IService getService(String name) {
        return (IService) springServiceContainer.getService(name);
    }
    public static IServiceContainer getServiceContainer(){
        return springServiceContainer;
    }
    @PostConstruct
    private void init() {
        springServiceContainer = serviceContainer;
    }
}
