package free.cobol2java.java;

import free.cobol2java.cics.CicsCrudRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Pending source READ UPDATE identities, owned by the existing transaction. */
final class CicsUpdateRecords implements TransactionSynchronization {
    private static final Object RESOURCE_KEY = new Object();
    private final Map<String, Record> records = new HashMap<>();

    record Record(CicsCrudRepository<Object, Object> repository, Object key) {}

    static CicsUpdateRecords current() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) return null;
        var current = (CicsUpdateRecords) TransactionSynchronizationManager.getResource(RESOURCE_KEY);
        if (current == null) {
            current = new CicsUpdateRecords();
            TransactionSynchronizationManager.bindResource(RESOURCE_KEY, current);
            TransactionSynchronizationManager.registerSynchronization(current);
        }
        return current;
    }

    void forget(String file) { records.remove(file); }

    void remember(String file, CicsCrudRepository<Object, Object> repository, Object key) {
        records.put(file, new Record(repository, key));
    }

    Record get(String file) { return records.get(file); }

    @Override public void suspend() {
        TransactionSynchronizationManager.unbindResource(RESOURCE_KEY);
    }

    @Override public void resume() {
        TransactionSynchronizationManager.bindResource(RESOURCE_KEY, this);
    }

    @Override public void afterCompletion(int status) {
        records.clear();
        if (TransactionSynchronizationManager.getResource(RESOURCE_KEY) == this)
            TransactionSynchronizationManager.unbindResource(RESOURCE_KEY);
    }
}
