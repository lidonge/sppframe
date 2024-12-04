package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-09@version 1.0
 */
public interface ISppException {
    ExceptionType getType() ;

    String getErrorMsg();

    Throwable getException();
}
