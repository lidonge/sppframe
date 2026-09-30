package free.cobol2java.cics;

import java.util.Optional;

public interface CicsCrudRepository<K, R> {

    void write(K key, R record) throws DuplicateKeyException, CicsDataAccessException;

    Optional<R> read(K key) throws CicsDataAccessException;

    /** Read under the caller's active transaction; repositories without locking support fail closed. */
    default Optional<R> readForUpdate(K key) throws CicsDataAccessException {
        throw new CicsDataAccessException("Repository does not support transactional read-for-update.");
    }

    void rewrite(K key, R record) throws RecordNotFoundException, CicsDataAccessException;

    void delete(K key) throws RecordNotFoundException, CicsDataAccessException;
}
