package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-12-02@version 1.0
 */
public interface IServiceContainer {
    <T> T getService(Class<T> serviceClass);

    Object getService(String serviceName);

    /** Retrieve a container bean by its exact bean name, whether or not it is an IService. */
    default Object getBean(String beanName) {
        throw new UnsupportedOperationException("This service container does not support bean lookup by name");
    }

    String getServiceName(Class<?> clazz);
}
