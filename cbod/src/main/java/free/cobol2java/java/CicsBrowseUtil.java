package free.cobol2java.java;

import free.cobol2java.cics.CicsCrudRepository;
import free.cobol2java.cics.CicsDataAccessException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal browse runtime for generated STARTBR/READNEXT/READPREV/ENDBR code.
 */
public final class CicsBrowseUtil {
    private static final Map<String, BrowseState> BROWSE_STATES = new ConcurrentHashMap<>();

    private CicsBrowseUtil() {
    }

    public static void startBrowse(CicsCrudRepository<Object, Object> repository, Object ridfld, boolean gteq) {
        startBrowse(repository, ridfld, gteq, !gteq, false, null);
    }

    public static void startBrowse(CicsCrudRepository<Object, Object> repository, Object ridfld,
                                   boolean gteq, boolean equal, boolean generic, Integer keyLength) {
        if (repository == null) {
            CicsUtil.setStatus(16, 1);
            return;
        }
        List<Object> keys = snapshotKeys(repository);
        Object normalizedKey = normalizeKey(ridfld);
        int position = resolveStartPosition(keys, normalizedKey, gteq, equal, generic, keyLength);
        String browseKey = browseKey(repository);
        if (position < 0) {
            BROWSE_STATES.remove(browseKey);
            CicsUtil.setStatus(13, 1);
            return;
        }
        BROWSE_STATES.put(browseKey, new BrowseState(position));
        CicsUtil.setStatus(0, 0);
    }

    public static <T> void readNext(CicsCrudRepository<Object, Object> repository, T into,
                                    Object ridfld, Integer length) {
        if (repository == null) {
            CicsUtil.setStatus(16, 1);
            return;
        }
        BrowseState state = BROWSE_STATES.get(browseKey(repository));
        if (state == null) {
            CicsUtil.setStatus(16, 2);
            return;
        }
        List<Object> keys = snapshotKeys(repository);
        if (state.nextIndex < 0 || state.nextIndex >= keys.size()) {
            CicsUtil.setStatus(20, 1);
            return;
        }
        Object key = keys.get(state.nextIndex);
        state.nextIndex++;
        state.currentKey = key;
        read(repository, into, key);
    }

    public static <T> void readPrev(CicsCrudRepository<Object, Object> repository, T into,
                                    Object ridfld, Integer length) {
        if (repository == null) {
            CicsUtil.setStatus(16, 1);
            return;
        }
        BrowseState state = BROWSE_STATES.get(browseKey(repository));
        if (state == null) {
            CicsUtil.setStatus(16, 2);
            return;
        }
        List<Object> keys = snapshotKeys(repository);
        int targetIndex = state.nextIndex - 1;
        if (targetIndex < 0 || targetIndex >= keys.size()) {
            CicsUtil.setStatus(20, 1);
            return;
        }
        Object key = keys.get(targetIndex);
        state.nextIndex = targetIndex;
        state.currentKey = key;
        read(repository, into, key);
    }

    public static Object currentKey(CicsCrudRepository<Object, Object> repository) {
        BrowseState state = repository == null ? null : BROWSE_STATES.get(browseKey(repository));
        return state == null ? null : state.currentKey;
    }

    public static void endBrowse(CicsCrudRepository<Object, Object> repository) {
        if (repository != null) {
            BROWSE_STATES.remove(browseKey(repository));
        }
        CicsUtil.setStatus(0, 0);
    }

    private static List<Object> snapshotKeys(CicsCrudRepository<Object, Object> repository) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> keys = new ArrayList<>((List<Object>) repository.getClass()
                    .getMethod("browseKeys")
                    .invoke(repository));
            keys.sort(KEY_COMPARATOR);
            return keys;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static <T> void read(CicsCrudRepository<Object, Object> repository, T into, Object key) {
        try {
            Optional<Object> stored = repository.read(key);
            if (stored == null || stored.isEmpty()) {
                CicsUtil.setStatus(13, 1);
                return;
            }
            if (into != null) {
                Util.copy(stored.get(), into);
            }
            CicsUtil.setStatus(0, 0);
        } catch (CicsDataAccessException e) {
            CicsUtil.setStatus(16, 9);
        }
    }

    private static String browseKey(CicsCrudRepository<Object, Object> repository) {
        return repository.getClass().getName();
    }

    private static int resolveStartPosition(List<Object> keys, Object key, boolean gteq, boolean equal,
                                            boolean generic, Integer keyLength) {
        if (keys.isEmpty()) {
            return key == null ? 0 : (gteq ? 0 : -1);
        }
        if (key == null) {
            return 0;
        }
        String matchKey = projectKey(key, generic, keyLength);
        for (int i = 0; i < keys.size(); i++) {
            Object candidate = keys.get(i);
            String projectedCandidate = projectKey(candidate, generic, keyLength);
            int comparison = compareKeys(projectedCandidate, matchKey);
            if (comparison == 0) {
                if (equal || !gteq) {
                    return i;
                }
                return i;
            }
            if (comparison > 0) {
                return gteq ? i : -1;
            }
        }
        return gteq ? keys.size() : -1;
    }

    private static String projectKey(Object key, boolean generic, Integer keyLength) {
        String value = key == null ? null : String.valueOf(key);
        if (value == null) {
            return null;
        }
        if (!generic) {
            return value;
        }
        if (keyLength == null || keyLength < 0) {
            return value;
        }
        return value.length() <= keyLength ? value : value.substring(0, keyLength);
    }

    private static Object normalizeKey(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return value;
        }
        return Objects.toString(value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareKeys(Object left, Object right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        if (left instanceof Comparable comparable && left.getClass().isInstance(right)) {
            return comparable.compareTo(right);
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private static final Comparator<Object> KEY_COMPARATOR = CicsBrowseUtil::compareKeys;

    private static final class BrowseState {
        private int nextIndex;
        private Object currentKey;

        private BrowseState(int nextIndex) {
            this.nextIndex = nextIndex;
        }
    }
}
