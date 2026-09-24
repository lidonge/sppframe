package free.cobol2java.java;

import java.util.HashMap;
import java.util.Map;

/** Browse cursor state owned by one generated program instance. */
public final class CicsBrowseContext {
    private final Map<String, CicsBrowseUtil.BrowseState> states = new HashMap<>();

    CicsBrowseUtil.BrowseState state(String resource) {
        return states.get(resource);
    }

    void state(String resource, CicsBrowseUtil.BrowseState state) {
        if (state == null) states.remove(resource);
        else states.put(resource, state);
    }
}
