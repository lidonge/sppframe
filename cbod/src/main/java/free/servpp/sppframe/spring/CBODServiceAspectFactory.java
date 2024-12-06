package free.servpp.sppframe.spring;

import free.servpp.sppframe.common.*;

/**
 * @author lidong@date 2024-12-05@version 1.0
 */
public class CBODServiceAspectFactory implements IServiceAspectFactory {
    @Override
    public IAtomicBefore getAtomicBefore(String serviceName) {
        return null;
    }

    @Override
    public IAtomicExceptionHandler getAtomicExceptionHandler(String serviceName) {
        return null;
    }

    @Override
    public IAtomicAfter getAtomicAfter(String serviceName) {
        return null;
    }

    @Override
    public IAtomicExecutor getAtomicExecutor(String serviceName) {
        return new IAtomicExecutor() {
            @Override
            public Class getPolymorphicClass(ISppContext context, String serviceName) {
                return null;
            }

            @Override
            public String getExecuteFunctionName() {
                return "procedure";
            }
        };
    }
}
