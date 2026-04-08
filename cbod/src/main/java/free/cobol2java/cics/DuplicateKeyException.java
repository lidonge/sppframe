package free.cobol2java.cics;
public class DuplicateKeyException extends CicsDataAccessException {
    public DuplicateKeyException(String message) {
        super(message);
    }
}