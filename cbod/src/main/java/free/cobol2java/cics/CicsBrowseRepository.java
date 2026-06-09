package free.cobol2java.cics;

import java.util.List;

public interface CicsBrowseRepository<K, R> extends CicsCrudRepository<K, R> {

    List<K> browseKeys(Object ridfld, boolean gteq, boolean equal, boolean generic, Integer keyLength)
            throws CicsDataAccessException;
}
