package free.servpp.sppframe.demo;

import free.servpp.sppframe.common.IRunBlockExceptionHandler;
import free.servpp.sppframe.common.ITransactionExceptionHandler;
import free.servpp.sppframe.impls.ServiceAspect;
import free.servpp.sppframe.impls.ServiceContainer;
import free.servpp.sppframe.impls.SppContext;
import org.aspectj.lang.Aspects;

/**
 * @author lidong@date 2024-08-07@version 1.0
 */
public class Main {
    public static void main(String[] args) {
        Aspects.aspectOf(ServiceAspect.class);
//        new SppFrameAspect();
        MakeInvoice makeInvoice = new MakeInvoice();

        SppContext context = new SppContext();
        ServiceContainer serviceContainer = new ServiceContainer();
        context.setServiceContainer(serviceContainer);
        context.setRunBlockExceptionHandler(new IRunBlockExceptionHandler() {});
        context.setTransactionExceptionHandler(new ITransactionExceptionHandler() {});
        makeInvoice.service(context, new Invoice());
        System.out.println();
    }
}
