package free.servpp.sppframe.spring.test;

import free.cobol2java.java.IService;
import org.springframework.stereotype.Service;

/**
 * @author lidong@date 2024-12-05@version 1.0
 */
public class MyNameService implements IService {
    public Object procedure(String test) {
        System.out.println("Execute MyNameService");
        return test;
    }
}
