package free.cobol2java.java;

public interface SqlDeleteRepository<R> {
    int deleteByKey(R key);
}
