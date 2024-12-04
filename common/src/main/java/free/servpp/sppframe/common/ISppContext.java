package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-06@version 1.0
 */
public interface ISppContext {
    IServiceAspectFactory getServiceAspectFactory();

    IServiceContainer getServiceContainer();

    void enterSerialBlock(IBlock block);

    void exitSerialBlock();

    void enterParallelBlock(IBlock block);

    void exitParallelBlock();

    boolean isInSerialBlock();

    boolean canExecute(String serviceName);

    IRunBlockExceptionHandler getRunBlockExceptionHandler();

    void enterTransactionBlock(IBlock block);

    void exitTransactionBlock();

    ITransactionExceptionHandler getTransactionExceptionHandler();

    void addException(ISppException error);
}
