package free.cobol2java.java.jcl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class IebgenerRuntime {
    private IebgenerRuntime() {
    }

    public static int execute(JclStep step) {
        if (step == null) {
            return 8;
        }
        if (!hasSupportedSysin(step.dd("SYSIN"))) {
            return 8;
        }
        JclDd input = step.dd("SYSUT1");
        JclDd output = step.dd("SYSUT2");
        if (input == null || output == null) {
            return 8;
        }
        try {
            byte[] content = readSysut1(input);
            writeSysut2(output, content);
            return 0;
        } catch (IOException e) {
            return 12;
        }
    }

    private static boolean hasSupportedSysin(JclDd sysin) {
        if (sysin == null || isDummy(sysin) || sysin.getInlineData().isEmpty()) {
            return true;
        }
        return sysin.getInlineData().stream().allMatch(line -> line == null || line.isBlank());
    }

    private static byte[] readSysut1(JclDd input) throws IOException {
        if (isDummy(input)) {
            return new byte[0];
        }
        List<String> inlineData = input.getInlineData();
        if (!inlineData.isEmpty()) {
            return String.join(System.lineSeparator(), inlineData)
                    .concat(System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8);
        }
        Path inputPath = dataSetPath(input);
        if (inputPath == null) {
            throw new IOException("Missing SYSUT1 input data set");
        }
        return Files.readAllBytes(inputPath);
    }

    private static void writeSysut2(JclDd output, byte[] content) throws IOException {
        if (isDummy(output)) {
            return;
        }
        Path outputPath = dataSetPath(output);
        if (outputPath == null) {
            throw new IOException("Missing SYSUT2 output data set");
        }
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (isModDisposition(output)) {
            Files.write(outputPath, content, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.write(outputPath, content, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static Path dataSetPath(JclDd dd) {
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
        return resolvePath(unquote(dsn.trim()));
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

    private static boolean isDummy(JclDd dd) {
        return dd.getParameters().keySet().stream().anyMatch("DUMMY"::equalsIgnoreCase);
    }

    private static boolean isModDisposition(JclDd dd) {
        String disp = parameter(dd, "DISP");
        return disp != null && disp.toUpperCase(java.util.Locale.ROOT).contains("MOD");
    }

    private static String parameter(JclDd dd, String key) {
        for (Map.Entry<String, String> entry : dd.getParameters().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
