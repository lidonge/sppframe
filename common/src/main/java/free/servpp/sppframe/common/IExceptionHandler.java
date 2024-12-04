package free.servpp.sppframe.common;

/**
 * @author lidong@date 2024-08-09@version 1.0
 */
public interface IExceptionHandler {
    default void process(ISppContext context, Object[] args, Throwable t){
        process(context,args,new ISppException(){

            @Override
            public ExceptionType getType() {
                return ExceptionType.ApplicationException;
            }

            @Override
            public String getErrorMsg() {
                return null;
            }

            @Override
            public Throwable getException() {
                return t;
            }
        });
    }

    default void process(ISppContext context, Object[] args, ISppException error){
        if(error != null)
            context.addException(error);
    }
}
