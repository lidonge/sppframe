package free.cobol2java.java;

import org.springframework.stereotype.Component;

/** Concrete proxy entry: the aspect supplies the invocation. */
@Component
public class DynamicCobolCallDispatcher {
    @CobolCallDispatch
    public Object dispatch(String sourcePosition, String programName, Object[] parameters) {
        throw new IllegalStateException("Dynamic COBOL CALL interceptor was bypassed at "
                + sourcePosition);
    }
}
