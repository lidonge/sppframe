package free.cobol2java.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal browse runtime for generated STARTBR/READNEXT/READPREV/ENDBR code.
 *
 * <p>It reuses the in-memory VSAM state already maintained by {@link CicsUtil}
 * so browse flows can be validated without changing the broader CRUD runtime.</p>
 */
public final class CicsBrowseUtil {
    private static final Map<String, BrowseState> BROWSE_STATES = new ConcurrentHashMap<>();

    private CicsBrowseUtil() {
    }

    public static CicsUtil.Response<Void> startBrowse(String file, Object ridfld, boolean gteq) {
        return startBrowse(file, ridfld, gteq, !gteq, false, null);
    }

    public static CicsUtil.Response<Void> startBrowse(String file, Object ridfld, boolean gteq, boolean equal,
                                                      boolean generic, Integer keyLength) {
        if (file == null || file.isBlank()) {
            return response(16, 1, null);
        }
        List<Object> keys = snapshotKeys(file);
        Object normalizedKey = normalizeKey(ridfld);
        int position = resolveStartPosition(keys, normalizedKey, gteq, equal, generic, keyLength);
        if (position < 0) {
            BROWSE_STATES.remove(file);
            return response(13, 1, null);
        }
        BROWSE_STATES.put(file, new BrowseState(position));
        return response(0, 0, null);
    }

    public static <T> CicsUtil.Response<T> readNext(String file, T into, Object ridfld, Integer length) {
        BrowseState state = BROWSE_STATES.get(file);
        if (state == null) {
            return response(16, 2, into);
        }
        List<Object> keys = snapshotKeys(file);
        if (state.nextIndex < 0 || state.nextIndex >= keys.size()) {
            return response(20, 1, into);
        }
        Object key = keys.get(state.nextIndex);
        state.nextIndex++;
        state.currentKey = key;
        return CicsUtil.read(file, into, key, length);
    }

    public static <T> CicsUtil.Response<T> readPrev(String file, T into, Object ridfld, Integer length) {
        BrowseState state = BROWSE_STATES.get(file);
        if (state == null) {
            return response(16, 2, into);
        }
        List<Object> keys = snapshotKeys(file);
        int targetIndex = state.nextIndex - 1;
        if (targetIndex < 0 || targetIndex >= keys.size()) {
            return response(20, 1, into);
        }
        Object key = keys.get(targetIndex);
        state.nextIndex = targetIndex;
        state.currentKey = key;
        return CicsUtil.read(file, into, key, length);
    }

    public static Object currentKey(String file) {
        BrowseState state = BROWSE_STATES.get(file);
        return state == null ? null : state.currentKey;
    }

    public static CicsUtil.Response<Void> endBrowse(String file) {
        if (file != null) {
            BROWSE_STATES.remove(file);
        }
        return response(0, 0, null);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> snapshotKeys(String file) {
        try {
            Field field = CicsUtil.class.getDeclaredField("VSAM_FILES");
            field.setAccessible(true);
            Map<String, Map<Object, Object>> files = (Map<String, Map<Object, Object>>) field.get(null);
            Map<Object, Object> store = files.get(file);
            if (store == null || store.isEmpty()) {
                return List.of();
            }
            List<Object> keys = new ArrayList<>(store.keySet());
            keys.sort(KEY_COMPARATOR);
            return keys;
        } catch (Exception e) {
            return List.of();
        }
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

    private static <T> CicsUtil.Response<T> response(int resp, int resp2, T payload) {
        try {
            Constructor<CicsUtil.Response> constructor = CicsUtil.Response.class
                    .getDeclaredConstructor(int.class, int.class, Object.class);
            constructor.setAccessible(true);
            @SuppressWarnings("unchecked")
            CicsUtil.Response<T> result = constructor.newInstance(resp, resp2, payload);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create CICS browse response", e);
        }
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
