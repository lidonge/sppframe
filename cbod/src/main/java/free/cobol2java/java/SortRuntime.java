package free.cobol2java.java;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Small runtime holder for COBOL SORT input procedure RELEASE records.
 */
public final class SortRuntime {
    private static final Map<Object, Map<String, List<Object>>> SORT_RECORDS = new WeakHashMap<>();

    private SortRuntime() {
    }

    public static synchronized void begin(Object owner, String sortFileName) {
        recordsByOwner(owner).put(sortFileName, new ArrayList<>());
    }

    public static synchronized void release(Object owner, String sortFileName, Object record) {
        records(owner, sortFileName).add(cloneRecord(record));
    }

    public static synchronized void sort(Object owner, String sortFileName) {
        records(owner, sortFileName);
    }

    public static synchronized void give(Object sortOwner, Object fileOwner, String sortFileName, String outputFileName) {
        records(sortOwner, sortFileName);
        FileCtl.setFileStatusByFileName(fileOwner, outputFileName, "00");
    }

    public static synchronized List<Object> records(Object owner, String sortFileName) {
        return recordsByOwner(owner).computeIfAbsent(sortFileName, key -> new ArrayList<>());
    }

    private static Map<String, List<Object>> recordsByOwner(Object owner) {
        Object key = owner == null ? SortRuntime.class : owner;
        return SORT_RECORDS.computeIfAbsent(key, ownerKey -> new LinkedHashMap<>());
    }

    private static Object cloneRecord(Object record) {
        if (record == null) {
            return null;
        }
        try {
            Object target = record.getClass().getDeclaredConstructor().newInstance();
            return Util.copy(record, target);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot clone SORT release record: " + record.getClass().getName(), e);
        }
    }
}
