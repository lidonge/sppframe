package free.servpp.sppframe;

import free.servpp.sppframe.common.IExecutor;
import free.servpp.sppframe.common.ISppContext;

/**
 * @author lidong@date 2024-08-06@version 1.0
 */
public interface IScenario<T extends IInvoice>{
    static final boolean IGNORE_EXCEPTION = true;
    void service(T invoice);
    void execTransaction(String name, IExecutor transaction);

    void execSerial(String name, IExecutor serial);

    void execParallel(String name, IExecutor parallel);
}
