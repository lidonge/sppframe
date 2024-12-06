package free.servpp.sppframe.spring.test;


import free.cobol2java.java.IService;
import free.cobol2java.java.ServiceManager;
import free.servpp.sppframe.common.*;
import free.servpp.sppframe.spring.CBODServiceAspectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author lidong@date 2024-12-04@version 1.0
 */
@RestController
public class TestController {

    @GetMapping("/test/{id}")
    public String test(@PathVariable("id") String id) {
        ISppContext sppContext = ISppContext.getSppContext();
        sppContext.setServiceContainer(sppContext.getServiceContainer());
        sppContext.setServiceAspectFactory(new CBODServiceAspectFactory());
        sppContext.enterParallelBlock(new Block(IBlock.BlockType.Serial, "test"));

//        MyService myService = ServiceManager.getService(MyService.class);
//        myService.procedure("test");
//
//        IService myNameService = ServiceManager.getService("myNameService");
//        myNameService.execute("test");

        if(id != null) {
            IService dynService = ServiceManager.getService(id);
            if (dynService != null)
                dynService.execute();
        }
        sppContext.exitSerialBlock();
        return "Test finished!";
    }
}