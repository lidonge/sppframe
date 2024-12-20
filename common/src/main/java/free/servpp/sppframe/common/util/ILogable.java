package free.servpp.sppframe.common.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lidong@date 2023-10-25@version 1.0
 */
public interface ILogable {
    default Logger getLogger(){
        return LoggerFactory.getLogger(this.getClass());
    }
    default void info(String template, ILogRunner runner){
        Logger logger = getLogger();
        if(logger.isInfoEnabled())
            logger.info(template,runner.getParams());
    }
}
