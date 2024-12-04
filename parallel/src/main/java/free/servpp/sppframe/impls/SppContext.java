package free.servpp.sppframe.impls;

import free.servpp.sppframe.common.*;

import java.util.*;

/**
 * @author lidong@date 2024-08-07@version 1.0
 */
public class SppContext implements ISppContext {
    private IServiceAspectFactory serviceAspectFactory = new ServiceAspectFactory();
    private IServiceContainer serviceContainer;
    private Stack<IBlock> blocks = new Stack<>();
    private List<ISppException> exceptionList = new ArrayList<>();

    private IRunBlockExceptionHandler runBlockExceptionHandler;
    private ITransactionExceptionHandler transactionExceptionHandler;

    public void setRunBlockExceptionHandler(IRunBlockExceptionHandler runBlockExceptionHandler) {
        this.runBlockExceptionHandler = runBlockExceptionHandler;
    }

    public void setTransactionExceptionHandler(ITransactionExceptionHandler transactionExceptionHandler) {
        this.transactionExceptionHandler = transactionExceptionHandler;
    }

    @Override
    public IServiceAspectFactory getServiceAspectFactory() {
        return serviceAspectFactory;
    }

    @Override
    public IServiceContainer getServiceContainer() {
        return serviceContainer;
    }

    public void setServiceContainer(IServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
    }

    @Override
    public void enterSerialBlock(IBlock block) {
        blocks.push(block);
    }

    @Override
    public void exitSerialBlock() {
        blocks.pop();
    }

    @Override
    public void enterParallelBlock(IBlock block) {
        blocks.push(block);
    }

    @Override
    public void exitParallelBlock() {
        blocks.pop();
    }

    @Override
    public boolean isInSerialBlock() {
        return blocks.peek().getType() == IBlock.BlockType.Serial;
    }

    @Override
    public void enterTransactionBlock(IBlock block) {
        blocks.push(block);
    }

    @Override
    public void exitTransactionBlock() {
        blocks.pop();
    }

    @Override
    public ITransactionExceptionHandler getTransactionExceptionHandler() {
        return transactionExceptionHandler;
    }

    @Override
    public IRunBlockExceptionHandler getRunBlockExceptionHandler() {
        return runBlockExceptionHandler;
    }

    @Override
    public void addException(ISppException error) {
        exceptionList.add(error);
    }


    @Override
    public boolean canExecute(String serviceName) {
        return exceptionList.isEmpty();
    }
}
