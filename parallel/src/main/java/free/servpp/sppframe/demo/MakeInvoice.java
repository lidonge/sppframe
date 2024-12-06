package free.servpp.sppframe.demo;

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
    public void service(Invoice invoice) {
        execSerial("query" , () -> {
            queryAccount.execute(invoice.getBuyer(), invoice.getBuyerAccount());
            execParallel("check", () -> {
                calculateAmount.execute(invoice.getGoods(), invoice.getAmount());
                checkStock.execute(invoice.getGoods().getStock());
            });
        });
        execTransaction("update" ,() -> {
            decAccount.execute(invoice.getBuyer(), invoice.getAmount());
            incAccount.execute(invoice.getSeller(), invoice.getAmount());
            decStock.execute(invoice.getGoods().getStock(), invoice.getAmount());
        });
    }
}
