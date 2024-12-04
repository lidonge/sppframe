package free.servpp.sppframe.demo;

import free.servpp.sppframe.IAtomicService;
import free.servpp.sppframe.common.ISppContext;
import free.servpp.sppframe.common.ISppException;

/**
 * @author lidong@date 2024-08-06@version 1.0
 */
public interface IIncAccount extends IAtomicService {
    default ISppException execute(ISppContext c, Customer seller, Amount amount) {
        return null;
    }
}
