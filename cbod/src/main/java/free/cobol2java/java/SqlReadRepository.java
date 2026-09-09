package free.cobol2java.java;

import java.util.List;
import java.util.Optional;

/** Existing generated repository operations used by the default SQL read binding. */
public interface SqlReadRepository<R> {
    Optional<R> selectByKey(R key);
    List<R> selectAll();
}
