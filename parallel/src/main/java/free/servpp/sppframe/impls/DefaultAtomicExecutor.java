package free.servpp.sppframe.impls;

import free.servpp.sppframe.common.IAtomicExecutor;
import free.servpp.sppframe.IAtomicService;
import free.servpp.sppframe.common.ISppContext;

/**
 * @author lidong@date 2024-08-09@version 1.0
 */
public class DefaultAtomicExecutor implements IAtomicExecutor {
    @Override
    public Class<? extends IAtomicService> getPolymorphicClass(ISppContext context, String serviceName){
        return null;
    }

    @Override
    public String getExecuteFunctionName() {
        return "execute";
    }
}
