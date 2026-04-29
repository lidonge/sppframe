package free.cobol2java.java.jcl;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class VsamStorageCatalog {
    private static final String PREFIX = "VSAM_CATALOG.";

    private final Map<String, Entry> entries;

    private VsamStorageCatalog(Map<String, Entry> entries) {
        this.entries = entries;
    }

    public static VsamStorageCatalog from(JclStep step) {
        if (step == null) {
            return new VsamStorageCatalog(Map.of());
        }
        Map<String, String> params = step.getParameters();
        int count = parseInt(params.get(PREFIX + "COUNT"));
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String itemPrefix = PREFIX + i + ".";
            String dataset = params.get(itemPrefix + "DATASET");
            if (dataset == null || dataset.isBlank()) {
                continue;
            }
            Entry entry = new Entry(
                    dataset,
                    params.get(itemPrefix + "ORGANIZATION"),
                    params.get(itemPrefix + "BACKEND"),
                    params.get(itemPrefix + "TABLE"),
                    params.get(itemPrefix + "KEY_LENGTH"),
                    params.get(itemPrefix + "KEY_OFFSET"),
                    params.get(itemPrefix + "RECORD_MIN_LENGTH"),
                    params.get(itemPrefix + "RECORD_MAX_LENGTH"),
                    Boolean.parseBoolean(params.getOrDefault(itemPrefix + "REQUIRES_MAPPING", "false")));
            entries.put(normalize(dataset), entry);
        }
        return new VsamStorageCatalog(entries);
    }

    public Optional<Entry> find(String datasetName) {
        if (datasetName == null || datasetName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(normalize(JclDatasetRuntime.unquote(datasetName.trim()))));
    }

    public boolean isDatabaseBacked(String datasetName) {
        return find(datasetName)
                .map(entry -> "DATABASE".equalsIgnoreCase(entry.backend()))
                .orElse(false);
    }

    public Map<String, Entry> entries() {
        return Map.copyOf(entries);
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    public record Entry(String dataset,
                        String organization,
                        String backend,
                        String table,
                        String keyLength,
                        String keyOffset,
                        String recordMinLength,
                        String recordMaxLength,
                        boolean requiresMapping) {
    }
}
