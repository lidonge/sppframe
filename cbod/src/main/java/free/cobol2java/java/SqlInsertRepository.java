package free.cobol2java.java;

public interface SqlInsertRepository<R> {
    int insert(R row);
}
