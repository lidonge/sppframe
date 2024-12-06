package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-06@version 1.0
 */
public interface ISppContext extends IServiceContext{
    static final ThreadLocal<ISppContext> threadLocalContext = ThreadLocal.withInitial(SppContext::new);
    static ISppContext getSppContext(){
        return threadLocalContext.get();
    }
    static void setThreadLocalContext(ISppContext context){
        threadLocalContext.set(context);
    }
    IServiceAspectFactory getServiceAspectFactory();

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

    void setServiceContainer(IServiceContainer serviceContainer);

    void setServiceAspectFactory(IServiceAspectFactory serviceAspectFactory);

    void setRunBlockExceptionHandler(IRunBlockExceptionHandler iRunBlockExceptionHandler);

    void setTransactionExceptionHandler(ITransactionExceptionHandler iTransactionExceptionHandler);
}
