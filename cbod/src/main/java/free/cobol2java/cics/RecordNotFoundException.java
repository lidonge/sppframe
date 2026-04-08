package free.cobol2java.cics;

public class RecordNotFoundException extends CicsDataAccessException {
    public RecordNotFoundException(String message) {
        super(message);
    }
}