package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-09@version 1.0
 */
public interface IServiceAspectFactory {
    IAtomicBefore getAtomicBefore(String serviceName);

    IAtomicExceptionHandler getAtomicExceptionHandler(String serviceName);

    IAtomicAfter getAtomicAfter(String serviceName);

    IAtomicExecutor getAtomicExecutor(String serviceName);
}
