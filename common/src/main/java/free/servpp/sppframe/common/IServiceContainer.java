package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-12-02@version 1.0
 */
public interface IServiceContainer {
    <T> T getService(Class<T> serviceClass);

    Object getService(String serviceName);
    String getServiceName(Class<?> clazz);
}
