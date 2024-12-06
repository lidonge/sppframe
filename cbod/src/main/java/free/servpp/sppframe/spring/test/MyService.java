package free.servpp.sppframe.spring.test;
import free.servpp.sppframe.common.ISppContext;
import org.springframework.stereotype.Service;

/**
 * @author lidong@date 2024-12-04@version 1.0
 */

public class MyService{
    public void procedure(Object... params) {
        System.out.println("MyService is processing.");
    }
}