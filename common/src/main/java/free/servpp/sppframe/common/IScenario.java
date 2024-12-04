package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-06@version 1.0
 */
public interface IScenario<T extends IInvoice>{
    static final boolean IGNORE_EXCEPTION = true;
    void service(ISppContext context, T invoice);
    void execTransaction(ISppContext context, String name, IExecutor transaction);

    void execSerial(ISppContext context, String name, IExecutor serial);

    void execParallel(ISppContext context, String name, IExecutor parallel);
}
