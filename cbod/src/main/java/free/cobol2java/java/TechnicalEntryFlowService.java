package free.cobol2java.java;

/** Generated entry invoked by the execute proxy for one shared COBOL call state. */
public interface TechnicalEntryFlowService extends IService {
    default Object executeTechnicalEntry(Object... parameters) {
        return IService.super.execute(parameters);
    }
}
