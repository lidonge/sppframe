package free.cobol2java.java.jcl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JclDatasetRuntime {
    private static final Pattern GDG_REFERENCE = Pattern.compile("^(.+)\\(([+-]?\\d+)\\)$");
    private static final Pattern GDG_GENERATION = Pattern.compile("^G(\\d{4})V(\\d{2})$");
    private static final ThreadLocal<JobContext> CURRENT_JOB = new ThreadLocal<>();

    private JclDatasetRuntime() {
    }

    public static void beginJob(String jobName) {
        CURRENT_JOB.set(new JobContext(jobName));
    }

    public static void endJob() {
        CURRENT_JOB.remove();
    }

    public static Path dataSetPath(JclDd dd) {
        if (dd == null || isDummy(dd)) {
            return null;
        }
        String dsn = parameter(dd, "DSN");
        if (dsn == null) {
            dsn = parameter(dd, "PATH");
        }
        if (dsn == null) {
            dsn = parameter(dd, "FILE");
        }
        if (dsn == null || dsn.isBlank()) {
            return null;
        }
        return resolveDataSetName(unquote(dsn.trim()));
    }

    public static Path resolveDataSetName(String dsn) {
        Matcher matcher = GDG_REFERENCE.matcher(dsn);
        if (matcher.matches()) {
            return resolveGdg(matcher.group(1), Integer.parseInt(matcher.group(2)));
        }
        return resolvePath(dsn);
    }

    public static byte[] readBytes(JclDd dd) throws IOException {
        if (dd == null || isDummy(dd)) {
            return new byte[0];
        }
        List<String> inlineData = dd.getInlineData();
        if (!inlineData.isEmpty()) {
            return String.join(System.lineSeparator(), inlineData)
                    .concat(System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8);
        }
        Path path = dataSetPath(dd);
        if (path == null) {
            throw new IOException("Missing JCL input data set");
        }
        return Files.readAllBytes(path);
    }

    public static List<String> readLines(JclDd dd) throws IOException {
        if (dd == null || isDummy(dd)) {
            return new ArrayList<>();
        }
        if (!dd.getInlineData().isEmpty()) {
            return new ArrayList<>(dd.getInlineData());
        }
        Path path = dataSetPath(dd);
        if (path == null) {
            throw new IOException("Missing JCL input data set");
        }
        if (!Files.exists(path)) {
            throw new IOException("JCL input data set not found");
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    public static void writeBytes(JclDd dd, byte[] content) throws IOException {
        if (dd == null || isDummy(dd)) {
            return;
        }
        Path path = dataSetPath(dd);
        if (path == null) {
            throw new IOException("Missing JCL output data set");
        }
        createParent(path);
        if (isModDisposition(dd)) {
            Files.write(path, content, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.write(path, content, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    public static void writeLines(JclDd dd, List<String> records) throws IOException {
        if (dd == null || isDummy(dd)) {
            return;
        }
        Path path = dataSetPath(dd);
        if (path == null) {
            throw new IOException("Missing JCL output data set");
        }
        createParent(path);
        StringBuilder out = new StringBuilder();
        for (String record : records) {
            out.append(record).append(System.lineSeparator());
        }
        writeBytes(dd, out.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void defineGdg(String baseName) throws IOException {
        if (baseName == null || baseName.isBlank()) {
            throw new IOException("Missing GDG base name");
        }
        Files.createDirectories(resolvePath(unquote(baseName.trim())));
    }

    public static void deleteDataSet(String dsn) throws IOException {
        Path path = resolveDataSetName(unquote(dsn.trim()));
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
                List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
                for (Path current : paths) {
                    Files.deleteIfExists(current);
                }
            }
        } else {
            Files.deleteIfExists(path);
        }
    }

    public static boolean exists(String dsn) {
        return Files.exists(resolveDataSetName(unquote(dsn.trim())));
    }

    public static boolean isDummy(JclDd dd) {
        return dd.getParameters().keySet().stream().anyMatch("DUMMY"::equalsIgnoreCase);
    }

    public static boolean isModDisposition(JclDd dd) {
        String disp = parameter(dd, "DISP");
        return disp != null && disp.toUpperCase(Locale.ROOT).contains("MOD");
    }

    public static String parameter(JclDd dd, String key) {
        for (Map.Entry<String, String> entry : dd.getParameters().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Path resolveGdg(String baseName, int relativeGeneration) {
        String normalizedBase = unquote(baseName.trim());
        JobContext context = CURRENT_JOB.get();
        if (context == null) {
            context = new JobContext("");
            CURRENT_JOB.set(context);
        }
        String key = normalizedBase + "(" + (relativeGeneration >= 0 ? "+" : "") + relativeGeneration + ")";
        if (relativeGeneration > 0) {
            int baseGeneration = context.baseGenerations.computeIfAbsent(normalizedBase, JclDatasetRuntime::currentGeneration);
            return context.allocatedGenerations.computeIfAbsent(key,
                    ignored -> gdgGenerationPath(normalizedBase, baseGeneration + relativeGeneration));
        }
        Path allocated = context.allocatedGenerations.get(key);
        if (allocated != null) {
            return allocated;
        }
        return gdgGenerationPath(normalizedBase, currentGeneration(normalizedBase) + relativeGeneration);
    }

    private static int currentGeneration(String baseName) {
        Path basePath = resolvePath(baseName);
        if (!Files.isDirectory(basePath)) {
            return 0;
        }
        try (var stream = Files.list(basePath)) {
            return stream
                    .map(path -> GDG_GENERATION.matcher(path.getFileName().toString()))
                    .filter(Matcher::matches)
                    .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                    .max()
                    .orElse(0);
        } catch (IOException e) {
            return 0;
        }
    }

    private static Path gdgGenerationPath(String baseName, int generation) {
        int normalizedGeneration = Math.max(0, generation);
        return resolvePath(baseName).resolve(String.format(Locale.ROOT, "G%04dV00", normalizedGeneration));
    }

    private static Path resolvePath(String value) {
        Path direct = Path.of(value);
        if (direct.isAbsolute() || value.contains("/") || value.contains("\\")) {
            return direct;
        }
        String root = System.getProperty("jcl.dataset.root");
        if (root == null || root.isBlank()) {
            root = System.getenv("JCL_DATASET_ROOT");
        }
        if (root == null || root.isBlank()) {
            return direct;
        }
        return Path.of(root).resolve(toRelativeDataSetPath(value));
    }

    private static String toRelativeDataSetPath(String dsn) {
        String value = dsn;
        int open = value.lastIndexOf('(');
        int close = value.indexOf(')', open + 1);
        if (open >= 0 && close > open) {
            String base = value.substring(0, open);
            String member = value.substring(open + 1, close);
            return base.replace('.', java.io.File.separatorChar)
                    + java.io.File.separator + member;
        }
        return value.replace('.', java.io.File.separatorChar);
    }

    private static void createParent(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static final class JobContext {
        private final String jobName;
        private final Map<String, Path> allocatedGenerations = new HashMap<>();
        private final Map<String, Integer> baseGenerations = new HashMap<>();

        private JobContext(String jobName) {
            this.jobName = jobName;
        }
    }
}
