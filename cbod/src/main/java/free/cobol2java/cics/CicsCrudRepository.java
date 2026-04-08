package free.cobol2java.cics;

import java.util.Optional;

public interface CicsCrudRepository<K, R> {

    void write(K key, R record) throws DuplicateKeyException, CicsDataAccessException;

    Optional<R> read(K key) throws CicsDataAccessException;

    void rewrite(K key, R record) throws RecordNotFoundException, CicsDataAccessException;

    void delete(K key) throws RecordNotFoundException, CicsDataAccessException;
}
