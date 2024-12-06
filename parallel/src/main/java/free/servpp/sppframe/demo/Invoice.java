package free.servpp.sppframe.demo;

import free.servpp.sppframe.IInvoice;

/**
 * @author lidong@date 2024-08-06@version 1.0
 */
public class Invoice implements IInvoice {
    public Customer getBuyer() {
        return new Customer();
    }

    public Goods getGoods() {
        return new Goods();
    }

    public Amount getAmount() {
        return new Amount();
    }

    public Customer getSeller() {
        return new Customer();
    }

    public Account getSellerAccount() {
        return new Account();
    }

    public Account getBuyerAccount() {
        return new Account();
    }
}
