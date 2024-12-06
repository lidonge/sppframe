package free.servpp.sppframe.impls;

import free.servpp.sppframe.common.IExecutor;
import free.servpp.sppframe.IInvoice;
import free.servpp.sppframe.IScenario;
import free.servpp.sppframe.common.ISppContext;

/**
 * @author lidong@date 2024-12-03@version 1.0
 */
public abstract class Scenario<T extends IInvoice> implements IScenario<T> {
    @Override
    public void execTransaction(String name, IExecutor transaction) {
        transaction.execute();
    }

    @Override
    public void execSerial(String name, IExecutor serial) {
        serial.execute();
    }

    @Override
    public void execParallel(String name, IExecutor parallel) {
        parallel.execute();
    }
}
