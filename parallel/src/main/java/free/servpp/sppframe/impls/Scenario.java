package free.servpp.sppframe.impls;

import free.servpp.sppframe.common.IExecutor;
import free.servpp.sppframe.common.IInvoice;
import free.servpp.sppframe.common.IScenario;
import free.servpp.sppframe.common.ISppContext;
import free.servpp.sppframe.demo.Invoice;

/**
 * @author lidong@date 2024-12-03@version 1.0
 */
public abstract class Scenario<T extends IInvoice> implements IScenario<T> {
    @Override
    public void execTransaction(ISppContext context, String name, IExecutor transaction) {
        transaction.execute(context);
    }

    @Override
    public void execSerial(ISppContext context, String name, IExecutor serial) {
        serial.execute(context);
    }

    @Override
    public void execParallel(ISppContext context, String name, IExecutor parallel) {
        parallel.execute(context);
    }
}
