package free.servpp.sppframe.demo;

import free.servpp.sppframe.common.IExecutor;
import free.servpp.sppframe.common.IScenario;
import free.servpp.sppframe.common.ISppContext;
import free.servpp.sppframe.impls.Scenario;

/**
 * @author lidong@date 2024-08-06@version 1.0
 */
public class MakeInvoice extends Scenario<Invoice>{
    protected IQueryAccount queryAccount = new IQueryAccount(){};
    protected ICalculateAmount calculateAmount = new ICalculateAmount(){};
    protected ICheckStock checkStock = new ICheckStock(){};
    protected IDecAccount decAccount = new IDecAccount(){};
    protected IIncAccount incAccount = new IIncAccount(){};
    protected IDecStock decStock = new IDecStock(){};

    @Override
    public void service(ISppContext context, Invoice invoice) {
        execSerial(context,"query" , (c) -> {
            queryAccount.execute(c,invoice.getBuyer(), invoice.getBuyerAccount());
            execParallel(context, "check", (c1) -> {
                calculateAmount.execute(c1,invoice.getGoods(), invoice.getAmount());
                checkStock.execute(c1,invoice.getGoods().getStock());
            });
        });
        execTransaction(context,"update" ,(c) -> {
            decAccount.execute(c,invoice.getBuyer(), invoice.getAmount());
            incAccount.execute(c,invoice.getSeller(), invoice.getAmount());
            decStock.execute(c,invoice.getGoods().getStock(), invoice.getAmount());
        });
    }
}
