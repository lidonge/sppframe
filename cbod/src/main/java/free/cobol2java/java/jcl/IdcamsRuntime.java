package free.cobol2java.java.jcl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        VsamStorageCatalog catalog = VsamStorageCatalog.from(step);
        ConditionCodes codes = new ConditionCodes();
        List<String> commands;
        try {
            commands = controlStatements(step.dd("SYSIN"));
        } catch (IOException e) {
            return JclReturnCodes.SEVERE_ERROR;
        }
        for (String command : commands) {
            CommandResult result = executeCommand(step, catalog, command, codes);
            if (!result.updateLastCc) {
                continue;
            }
            codes.lastCc = result.returnCode;
            codes.maxCc = Math.max(codes.maxCc, result.returnCode);
        }
        return codes.maxCc;
    }

    private static CommandResult executeCommand(JclStep step,
                                                VsamStorageCatalog catalog,
                                                String command,
                                                ConditionCodes codes) {
        try {
            return executeCommandChecked(step, catalog, command, codes);
        } catch (IllegalArgumentException e) {
            return CommandResult.returnCode(JclReturnCodes.ERROR);
        } catch (IOException e) {
            return CommandResult.returnCode(JclReturnCodes.SEVERE_ERROR);
        }
    }

    private static CommandResult executeCommandChecked(JclStep step,
                                                       VsamStorageCatalog catalog,
                                                       String command,
                                                       ConditionCodes codes) throws IOException {
        String upper = command.toUpperCase(Locale.ROOT);
        if (upper.startsWith("IF ")) {
            return executeIf(step, catalog, command, codes);
        }
        if (upper.startsWith("SET ")) {
            return executeSet(command, codes);
        }
        if (upper.startsWith("DEFINE ") && upper.contains(" GDG")) {
            String name = nameValue(command);
            if (name == null) {
                throw new IllegalArgumentException("Missing IDCAMS DEFINE GDG NAME");
            }
            JclDatasetRuntime.defineGdg(name);
            return CommandResult.returnCode(JclReturnCodes.OK);
        }
        if (upper.startsWith("DEFINE ") && upper.contains(" CLUSTER")) {
            defineCluster(catalog, command);
            return CommandResult.returnCode(JclReturnCodes.OK);
        }
        if (upper.startsWith("DELETE ")) {
            String datasetName = firstOperand(command, "DELETE");
            if (!catalog.isDatabaseBacked(datasetName)) {
                JclDatasetRuntime.deleteDataSet(datasetName);
            }
            return CommandResult.returnCode(JclReturnCodes.OK);
        }
        if (upper.startsWith("LISTCAT")) {
            String name = nameValue(command);
            if (catalog.isDatabaseBacked(name)) {
                return CommandResult.returnCode(JclReturnCodes.OK);
            }
            if (name != null && !JclDatasetRuntime.exists(name)) {
                throw new IOException("IDCAMS LISTCAT data set not found: " + name);
            }
            return CommandResult.returnCode(JclReturnCodes.OK);
        }
        if (upper.startsWith("ALTER ")) {
            return CommandResult.returnCode(JclReturnCodes.OK);
        }
        if (upper.startsWith("REPRO ")) {
            repro(step, catalog, command);
            return CommandResult.returnCode(JclReturnCodes.OK);
        }
        return CommandResult.returnCode(JclReturnCodes.OK);
    }

    private static CommandResult executeIf(JclStep step,
                                           VsamStorageCatalog catalog,
                                           String command,
                                           ConditionCodes codes) throws IOException {
        int thenIndex = indexOfThen(command);
        if (thenIndex < 0) {
            return CommandResult.noUpdate();
        }
        String condition = command.substring(2, thenIndex).trim();
        String thenCommand = command.substring(thenIndex + 4).trim();
        if (thenCommand.isBlank()) {
            return CommandResult.noUpdate();
        }
        JclIfCondition parsed = JclIfCondition.parse(condition);
        if (parsed == null || !parsed.matches(Map.of(), codes.maxCc, codes.lastCc)) {
            return CommandResult.noUpdate();
        }
        return executeCommandChecked(step, catalog, thenCommand, codes);
    }

    private static CommandResult executeSet(String command, ConditionCodes codes) {
        Matcher matcher = Pattern.compile("\\b(MAXCC|LASTCC)\\s*=\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(command);
        if (!matcher.find()) {
            return CommandResult.returnCode(JclReturnCodes.OK);
        }
        int value = Integer.parseInt(matcher.group(2));
        if ("MAXCC".equalsIgnoreCase(matcher.group(1))) {
            codes.maxCc = value;
            return CommandResult.noUpdate();
        }
        codes.lastCc = value;
        codes.maxCc = Math.max(codes.maxCc, value);
        return CommandResult.noUpdate();
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
                || upper.startsWith("REPRO ")
                || upper.startsWith("SET ")
                || upper.startsWith("IF ");
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

    private static int indexOfThen(String command) {
        Matcher matcher = Pattern.compile("\\bTHEN\\b", Pattern.CASE_INSENSITIVE).matcher(command);
        return matcher.find() ? matcher.start() : -1;
    }

    private static final class ConditionCodes {
        private int maxCc = JclReturnCodes.OK;
        private int lastCc = JclReturnCodes.OK;
    }

    private record CommandResult(int returnCode, boolean updateLastCc) {
        private static CommandResult returnCode(int returnCode) {
            return new CommandResult(returnCode, true);
        }

        private static CommandResult noUpdate() {
            return new CommandResult(JclReturnCodes.OK, false);
        }
    }
}
