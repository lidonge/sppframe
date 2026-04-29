package free.cobol2java.java.jcl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IdcamsRuntime {
    private static final Pattern NAME_VALUE = Pattern.compile("\\bNAME\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPRO_FILES = Pattern.compile(
            "\\bINFILE\\s*\\(([^)]+)\\).*\\bOUTFILE\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);

    private IdcamsRuntime() {
    }

    public static int execute(JclStep step) {
        if (step == null) {
            return JclReturnCodes.ERROR;
        }
        try {
            VsamStorageCatalog catalog = VsamStorageCatalog.from(step);
            for (String command : controlStatements(step.dd("SYSIN"))) {
                executeCommand(step, catalog, command);
            }
            return JclReturnCodes.OK;
        } catch (IllegalArgumentException e) {
            return JclReturnCodes.ERROR;
        } catch (IOException e) {
            return JclReturnCodes.SEVERE_ERROR;
        }
    }

    private static void executeCommand(JclStep step, VsamStorageCatalog catalog, String command) throws IOException {
        String upper = command.toUpperCase(Locale.ROOT);
        if (upper.startsWith("DEFINE ") && upper.contains(" GDG")) {
            String name = nameValue(command);
            if (name == null) {
                throw new IllegalArgumentException("Missing IDCAMS DEFINE GDG NAME");
            }
            JclDatasetRuntime.defineGdg(name);
            return;
        }
        if (upper.startsWith("DEFINE ") && upper.contains(" CLUSTER")) {
            defineCluster(catalog, command);
            return;
        }
        if (upper.startsWith("DELETE ")) {
            String datasetName = firstOperand(command, "DELETE");
            if (!catalog.isDatabaseBacked(datasetName)) {
                JclDatasetRuntime.deleteDataSet(datasetName);
            }
            return;
        }
        if (upper.startsWith("LISTCAT")) {
            String name = nameValue(command);
            if (catalog.isDatabaseBacked(name)) {
                return;
            }
            if (name != null && !JclDatasetRuntime.exists(name)) {
                throw new IOException("IDCAMS LISTCAT data set not found: " + name);
            }
            return;
        }
        if (upper.startsWith("ALTER ")) {
            return;
        }
        if (upper.startsWith("REPRO ")) {
            repro(step, catalog, command);
        }
    }

    private static void defineCluster(VsamStorageCatalog catalog, String command) throws IOException {
        String name = nameValue(command);
        if (name == null) {
            throw new IllegalArgumentException("Missing IDCAMS DEFINE CLUSTER NAME");
        }
        if (catalog.isDatabaseBacked(name)) {
            return;
        }
        JclDatasetRuntime.resolveDataSetName(name);
    }

    private static void repro(JclStep step, VsamStorageCatalog catalog, String command) throws IOException {
        Matcher matcher = REPRO_FILES.matcher(command);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unsupported IDCAMS REPRO command: " + command);
        }
        JclDd input = step.dd(matcher.group(1).trim());
        JclDd output = step.dd(matcher.group(2).trim());
        if (input == null || output == null) {
            throw new IllegalArgumentException("Missing IDCAMS REPRO DD: " + command);
        }
        String outputDsn = JclDatasetRuntime.parameter(output, "DSN");
        if (catalog.isDatabaseBacked(outputDsn)) {
            return;
        }
        JclDatasetRuntime.writeBytes(output, JclDatasetRuntime.readBytes(input));
    }

    private static List<String> controlStatements(JclDd sysin) throws IOException {
        if (sysin == null || JclDatasetRuntime.isDummy(sysin)) {
            return List.of();
        }
        List<String> lines;
        if (sysin.getInlineData().isEmpty()) {
            java.nio.file.Path path = JclDatasetRuntime.dataSetPath(sysin);
            if (path == null) {
                return List.of();
            }
            lines = java.nio.file.Files.readAllLines(path);
        } else {
            lines = sysin.getInlineData();
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isBlank()) {
                continue;
            }
            if (startsCommand(line) && current.length() > 0 && !current.toString().trim().endsWith("-")) {
                statements.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(line.endsWith("-") ? line.substring(0, line.length() - 1).trim() : line);
        }
        if (current.length() > 0) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private static boolean startsCommand(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        return upper.startsWith("DEFINE ")
                || upper.startsWith("DELETE ")
                || upper.startsWith("LISTCAT")
                || upper.startsWith("ALTER ")
                || upper.startsWith("REPRO ");
    }

    private static String stripComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("/*") || trimmed.startsWith("*") ? "" : line;
    }

    private static String nameValue(String command) {
        Matcher matcher = NAME_VALUE.matcher(command);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String firstOperand(String command, String verb) {
        String value = command.substring(verb.length()).trim();
        int blank = value.indexOf(' ');
        if (blank >= 0) {
            value = value.substring(0, blank);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing IDCAMS " + verb + " operand");
        }
        return value;
    }
}
