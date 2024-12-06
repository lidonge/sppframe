package free.servpp.sppframe.spring;

/**
 * @author lidong@date 2024-12-04@version 1.0
 */
import free.servpp.sppframe.common.IServiceContainer;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class SpringServiceContainer implements IServiceContainer {

    private final ApplicationContext applicationContext;

    // Constructor to initialize ClassPathXmlApplicationContext
    public SpringServiceContainer() {
        this.applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return applicationContext.getBean(serviceClass);
    }

    @Override
    public Object getService(String serviceName) {
        return applicationContext.getBean(serviceName);
    }

    @Override
    public String getServiceName(Class<?> clazz) {
        return clazz.getSimpleName();
    }
}