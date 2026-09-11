package free.cobol2java.java;

public interface SqlUpdateRepository<R> {
    int update(R row);
}
