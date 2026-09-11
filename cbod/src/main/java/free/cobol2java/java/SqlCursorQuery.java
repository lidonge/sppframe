package free.cobol2java.java;

import java.util.List;

/** Generated typed query binding; executed synchronously inside the cursor service. */
public interface SqlCursorQuery<R> {
    Class<R> rowType();
    List<R> execute();
}
