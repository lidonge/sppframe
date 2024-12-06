package free.servpp.sppframe.demo;

import free.servpp.sppframe.common.IRunBlockExceptionHandler;
import free.servpp.sppframe.common.ISppContext;
import free.servpp.sppframe.common.ITransactionExceptionHandler;
import free.servpp.sppframe.impls.ServiceAspect;
import free.servpp.sppframe.impls.ServiceAspectFactory;
import free.servpp.sppframe.impls.ServiceContainer;
import free.servpp.sppframe.common.SppContext;
import org.aspectj.lang.Aspects;

/**
 * @author lidong@date 2024-08-07@version 1.0
 */
public class Main {
    public static void main(String[] args) {
        Aspects.aspectOf(ServiceAspect.class);
//        new SppFrameAspect();
        MakeInvoice makeInvoice = new MakeInvoice();

        ISppContext context = ISppContext.getSppContext();
        context.setServiceContainer(new ServiceContainer<>());
        context.setServiceAspectFactory(new ServiceAspectFactory());
        context.setRunBlockExceptionHandler(new IRunBlockExceptionHandler() {});
        context.setTransactionExceptionHandler(new ITransactionExceptionHandler() {});
        makeInvoice.service(new Invoice());
        System.out.println();
    }
}
