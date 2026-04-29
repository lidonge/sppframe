package free.cobol2java.java.jcl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DfsortRuntime {
    private static final Pattern FIELD_REF = Pattern.compile("(\\d+)\\s*,\\s*(\\d+)(?:\\s*,\\s*([A-Z0-9]+))?");
    private static final Pattern OVERLAY_ITEM = Pattern.compile("(\\d+)\\s*:(.+)", Pattern.CASE_INSENSITIVE);
    private static final List<String> COMMANDS = List.of("SORT", "MERGE", "INCLUDE", "OMIT", "INREC", "OUTREC", "OUTFIL");
    private static final List<String> KEYWORDS = List.of("SORT", "MERGE", "INCLUDE", "OMIT", "INREC", "OUTREC",
            "OUTFIL", "FIELDS", "COND", "BUILD", "OVERLAY", "FNAMES", "FILES", "SAVE");

    private DfsortRuntime() {
    }

    public static int execute(JclStep step) {
        if (step == null) {
            return 8;
        }
        JclDd input = firstDd(step, "SORTIN", "SYSUT1");
        if (input == null) {
            return 8;
        }
        try {
            SortControl control = parseControl(step.dd("SYSIN"));
            List<String> records = readInput(input);
            records = filter(records, control.include, false);
            records = filter(records, control.omit, true);
            records = transform(records, control.inrec);
            sort(records, control.sortKeys);
            records = transform(records, control.outrec);
            if (control.outfils.isEmpty()) {
                JclDd output = firstDd(step, "SORTOUT", "SYSUT2");
                if (output == null) {
                    return 8;
                }
                writeOutput(output, records);
            } else {
                for (OutfilControl outfil : control.outfils) {
                    List<String> outRecords = filter(records, outfil.include, false);
                    outRecords = filter(outRecords, outfil.omit, true);
                    outRecords = transform(outRecords, outfil.outrec);
                    for (String ddName : outfil.ddNames) {
                        JclDd output = step.dd(ddName);
                        if (output == null) {
                            return 8;
                        }
                        writeOutput(output, outRecords);
                    }
                }
            }
            return 0;
        } catch (IllegalArgumentException e) {
            return 8;
        } catch (IOException e) {
            return 12;
        }
    }

    private static List<String> readInput(JclDd dd) throws IOException {
        if (dd == null || isDummy(dd)) {
            return new ArrayList<>();
        }
        if (!dd.getInlineData().isEmpty()) {
            return new ArrayList<>(dd.getInlineData());
        }
        Path path = dataSetPath(dd);
        if (path == null) {
            throw new IOException("Missing DFSORT input data set");
        }
        if (!Files.exists(path)) {
            throw new IOException("DFSORT input data set not found");
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    private static void writeOutput(JclDd dd, List<String> records) throws IOException {
        if (dd == null || isDummy(dd)) {
            return;
        }
        Path path = dataSetPath(dd);
        if (path == null) {
            throw new IOException("Missing DFSORT output data set");
        }
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String record : records) {
            out.write(record.getBytes(StandardCharsets.UTF_8));
            out.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        }
        if (isModDisposition(dd)) {
            Files.write(path, out.toByteArray(), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.write(path, out.toByteArray(), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static List<String> filter(List<String> records, Condition condition, boolean dropMatches) {
        if (condition == null) {
            return records;
        }
        List<String> result = new ArrayList<>();
        for (String record : records) {
            boolean matches = condition.test(record);
            if (dropMatches != matches) {
                result.add(record);
            }
        }
        return result;
    }

    private static List<String> transform(List<String> records, RecordTransform transform) {
        if (transform == null) {
            return records;
        }
        List<String> result = new ArrayList<>(records.size());
        for (String record : records) {
            result.add(transform.apply(record));
        }
        return result;
    }

    private static void sort(List<String> records, List<SortKey> keys) {
        if (keys.isEmpty()) {
            return;
        }
        records.sort((left, right) -> {
            for (SortKey key : keys) {
                int compared = compareKey(key, left, right);
                if (compared != 0) {
                    return key.descending ? -compared : compared;
                }
            }
            return 0;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareKey(SortKey key, String left, String right) {
        Comparable leftValue = comparableValue(slice(left, key.position, key.length), key.type);
        Comparable rightValue = comparableValue(slice(right, key.position, key.length), key.type);
        return leftValue.compareTo(rightValue);
    }

    private static Comparable<?> comparableValue(String value, String type) {
        String normalizedType = type == null ? "CH" : type.toUpperCase(Locale.ROOT);
        if (normalizedType.equals("ZD") || normalizedType.equals("PD") || normalizedType.equals("BI")
                || normalizedType.equals("FI") || normalizedType.equals("FS") || normalizedType.equals("UFF")
                || normalizedType.equals("SFF")) {
            return parseNumber(value);
        }
        return value;
    }

    private static BigDecimal parseNumber(String value) {
        String normalized = value.trim();
        if (normalized.endsWith("-") || normalized.endsWith("+")) {
            normalized = normalized.substring(normalized.length() - 1) + normalized.substring(0, normalized.length() - 1);
        }
        normalized = normalized.replaceAll("[^0-9+\\-.]", "");
        if (normalized.isBlank() || normalized.equals("+") || normalized.equals("-") || normalized.equals(".")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(normalized);
    }

    private static SortControl parseControl(JclDd sysin) throws IOException {
        SortControl control = new SortControl();
        for (String statement : controlStatements(sysin)) {
            String upper = statement.toUpperCase(Locale.ROOT);
            if (startsWithCommand(upper, "SORT") || startsWithCommand(upper, "MERGE")) {
                control.sortKeys.clear();
                control.sortKeys.addAll(parseSortKeys(valueAfterKeyword(statement, "FIELDS")));
            } else if (startsWithCommand(upper, "INCLUDE")) {
                control.include = parseCondition(conditionValue(statement, "INCLUDE"));
            } else if (startsWithCommand(upper, "OMIT")) {
                control.omit = parseCondition(conditionValue(statement, "OMIT"));
            } else if (startsWithCommand(upper, "INREC")) {
                control.inrec = parseTransform(statement);
            } else if (startsWithCommand(upper, "OUTREC")) {
                control.outrec = parseTransform(statement);
            } else if (startsWithCommand(upper, "OUTFIL")) {
                control.outfils.add(parseOutfil(statement));
            }
        }
        return control;
    }

    private static List<String> controlStatements(JclDd sysin) throws IOException {
        if (sysin == null || isDummy(sysin)) {
            return List.of();
        }
        List<String> lines;
        if (!sysin.getInlineData().isEmpty()) {
            lines = sysin.getInlineData();
        } else {
            Path path = dataSetPath(sysin);
            if (path == null) {
                return List.of();
            }
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isBlank()) {
                continue;
            }
            String upper = line.toUpperCase(Locale.ROOT);
            boolean startsCommand = COMMANDS.stream().anyMatch(command -> startsWithCommand(upper, command));
            if (startsCommand && current.length() > 0 && !requiresContinuation(current)) {
                statements.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(line);
        }
        if (current.length() > 0) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private static boolean startsWithCommand(String upperStatement, String command) {
        if (!upperStatement.startsWith(command)) {
            return false;
        }
        int length = command.length();
        return upperStatement.length() == length
                || Character.isWhitespace(upperStatement.charAt(length))
                || upperStatement.charAt(length) == '=';
    }

    private static boolean requiresContinuation(StringBuilder statement) {
        String value = statement.toString().trim();
        if (value.endsWith(",")) {
            return true;
        }
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'') {
                quoted = !quoted;
            } else if (!quoted && ch == '(') {
                depth++;
            } else if (!quoted && ch == ')') {
                depth--;
            }
        }
        return quoted || depth > 0;
    }

    private static String stripComment(String line) {
        int comment = indexOfTopLevel(line, '*');
        if (comment == 0) {
            return "";
        }
        return line;
    }

    private static List<SortKey> parseSortKeys(String value) {
        if (value == null) {
            return List.of();
        }
        String fields = unwrap(value.trim());
        if (fields.equalsIgnoreCase("COPY")) {
            return List.of();
        }
        List<String> parts = splitTopLevel(fields, ',');
        List<SortKey> keys = new ArrayList<>();
        for (int i = 0; i + 1 < parts.size();) {
            int position = parsePositiveInt(parts.get(i++));
            int length = parsePositiveInt(parts.get(i++));
            String type = "CH";
            String order = "A";
            if (i < parts.size() && !isOrder(parts.get(i))) {
                type = parts.get(i++).trim();
            }
            if (i < parts.size() && isOrder(parts.get(i))) {
                order = parts.get(i++).trim();
            }
            keys.add(new SortKey(position, length, type, order.equalsIgnoreCase("D")));
        }
        return keys;
    }

    private static Condition parseCondition(String value) {
        if (value == null) {
            return null;
        }
        String expression = unwrap(value.trim());
        if (expression.equalsIgnoreCase("ALL")) {
            return record -> true;
        }
        if (expression.equalsIgnoreCase("NONE")) {
            return record -> false;
        }
        List<String> orParts = splitByOperator(expression, "OR");
        if (orParts.size() > 1) {
            List<Condition> conditions = orParts.stream().map(DfsortRuntime::parseCondition).toList();
            return record -> conditions.stream().anyMatch(condition -> condition.test(record));
        }
        List<String> andParts = splitByOperator(expression, "AND");
        if (andParts.size() > 1) {
            List<Condition> conditions = andParts.stream().map(DfsortRuntime::parseCondition).toList();
            return record -> conditions.stream().allMatch(condition -> condition.test(record));
        }
        List<String> parts = splitTopLevel(unwrap(expression), ',');
        if (parts.size() < 5) {
            throw new IllegalArgumentException("Unsupported DFSORT condition: " + value);
        }
        FieldValue left = fieldValue(parts.get(0), parts.get(1), parts.get(2));
        String operator = parts.get(3).trim().toUpperCase(Locale.ROOT);
        ValueSource right = valueSource(parts.subList(4, parts.size()));
        return record -> compareCondition(left.value(record), right.value(record), left.type, operator);
    }

    private static boolean compareCondition(String left, String right, String type, String operator) {
        int compared;
        if (isNumericType(type)) {
            compared = parseNumber(left).compareTo(parseNumber(right));
        } else {
            int width = Math.max(left.length(), right.length());
            left = rightPad(left, width);
            right = rightPad(right, width);
            compared = left.compareTo(right);
        }
        return switch (operator) {
            case "EQ", "=" -> compared == 0;
            case "NE", "NQ", "!=" -> compared != 0;
            case "GT", ">" -> compared > 0;
            case "GE", ">=" -> compared >= 0;
            case "LT", "<" -> compared < 0;
            case "LE", "<=" -> compared <= 0;
            default -> throw new IllegalArgumentException("Unsupported DFSORT condition operator: " + operator);
        };
    }

    private static RecordTransform parseTransform(String statement) {
        String build = firstValueAfterKeyword(statement, "BUILD", "FIELDS");
        if (build != null) {
            List<BuildItem> items = parseBuildItems(build);
            return record -> {
                StringBuilder out = new StringBuilder();
                for (BuildItem item : items) {
                    out.append(item.value(record));
                }
                return out.toString();
            };
        }
        String overlay = valueAfterKeyword(statement, "OVERLAY");
        if (overlay != null) {
            List<OverlayItem> items = parseOverlayItems(overlay);
            return record -> applyOverlay(record, items);
        }
        return null;
    }

    private static OutfilControl parseOutfil(String statement) {
        OutfilControl outfil = new OutfilControl();
        String fnames = firstValueAfterKeyword(statement, "FNAMES", "FILES");
        if (fnames != null) {
            for (String ddName : splitTopLevel(unwrap(fnames), ',')) {
                outfil.ddNames.add(unquote(ddName.trim()));
            }
        }
        if (outfil.ddNames.isEmpty()) {
            outfil.ddNames.add("SORTOUT");
        }
        String include = valueAfterKeyword(statement, "INCLUDE");
        if (include != null) {
            String cond = valueAfterKeyword(include, "COND");
            outfil.include = parseCondition(cond == null ? include : cond);
        }
        String omit = valueAfterKeyword(statement, "OMIT");
        if (omit != null) {
            String cond = valueAfterKeyword(omit, "COND");
            outfil.omit = parseCondition(cond == null ? omit : cond);
        }
        String outrec = valueAfterKeyword(statement, "OUTREC");
        if (outrec != null) {
            outfil.outrec = parseOutfilTransform(outrec);
        } else {
            outfil.outrec = parseTransform(statement);
        }
        return outfil;
    }

    private static RecordTransform parseOutfilTransform(String value) {
        String unwrapped = unwrap(value);
        String upper = unwrapped.toUpperCase(Locale.ROOT);
        if (upper.startsWith("BUILD=") || upper.startsWith("FIELDS=") || upper.startsWith("OVERLAY=")) {
            return parseTransform("OUTREC " + unwrapped);
        }
        List<BuildItem> items = parseBuildItems(unwrapped);
        return record -> {
            StringBuilder out = new StringBuilder();
            for (BuildItem item : items) {
                out.append(item.value(record));
            }
            return out.toString();
        };
    }

    private static List<BuildItem> parseBuildItems(String value) {
        List<BuildItem> items = new ArrayList<>();
        List<String> parts = splitTopLevel(unwrap(value.trim()), ',');
        for (int i = 0; i < parts.size();) {
            String current = parts.get(i).trim();
            if (current.isBlank()) {
                i++;
                continue;
            }
            if (isLiteral(current)) {
                String literal = literalValue(current);
                items.add(record -> literal);
                i++;
                continue;
            }
            if (isInteger(current) && i + 1 < parts.size() && isInteger(parts.get(i + 1))) {
                int position = parsePositiveInt(current);
                int length = parsePositiveInt(parts.get(i + 1));
                items.add(record -> slice(record, position, length));
                i += 2;
                if (i < parts.size() && isDataType(parts.get(i))) {
                    i++;
                }
                continue;
            }
            Matcher matcher = FIELD_REF.matcher(current);
            if (matcher.matches()) {
                int position = parsePositiveInt(matcher.group(1));
                int length = parsePositiveInt(matcher.group(2));
                items.add(record -> slice(record, position, length));
                i++;
                continue;
            }
            if (isInteger(current)) {
                int position = parsePositiveInt(current);
                items.add(record -> position <= record.length() ? record.substring(position - 1) : "");
                i++;
                continue;
            }
            throw new IllegalArgumentException("Unsupported DFSORT BUILD item: " + current);
        }
        return items;
    }

    private static List<OverlayItem> parseOverlayItems(String value) {
        List<OverlayItem> items = new ArrayList<>();
        for (String item : overlayItems(unwrap(value.trim()))) {
            Matcher matcher = OVERLAY_ITEM.matcher(item);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Unsupported DFSORT OVERLAY item: " + item);
            }
            int position = parsePositiveInt(matcher.group(1));
            List<BuildItem> source = parseBuildItems(matcher.group(2));
            items.add(new OverlayItem(position, record -> {
                StringBuilder valueBuilder = new StringBuilder();
                for (BuildItem buildItem : source) {
                    valueBuilder.append(buildItem.value(record));
                }
                return valueBuilder.toString();
            }));
        }
        return items;
    }

    private static List<String> overlayItems(String value) {
        List<String> parts = splitTopLevel(value, ',');
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            boolean startsOverlayItem = trimmed.matches("\\d+\\s*:.*");
            if (startsOverlayItem && current.length() > 0) {
                items.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(',');
            }
            current.append(trimmed);
        }
        if (current.length() > 0) {
            items.add(current.toString().trim());
        }
        return items;
    }

    private static String applyOverlay(String record, List<OverlayItem> items) {
        StringBuilder out = new StringBuilder(record);
        for (OverlayItem item : items) {
            while (out.length() < item.position - 1) {
                out.append(' ');
            }
            String value = item.source.value(record);
            int start = item.position - 1;
            int end = Math.min(out.length(), start + value.length());
            out.replace(start, end, value);
            if (out.length() < start + value.length()) {
                out.append(value.substring(out.length() - start));
            }
        }
        return out.toString();
    }

    private static ValueSource valueSource(List<String> parts) {
        if (parts.size() >= 3 && isInteger(parts.get(0)) && isInteger(parts.get(1))) {
            return fieldValue(parts.get(0), parts.get(1), parts.get(2))::value;
        }
        String value = String.join(",", parts).trim();
        if (isLiteral(value)) {
            String literal = literalValue(value);
            return record -> literal;
        }
        return record -> unquote(value);
    }

    private static String conditionValue(String statement, String command) {
        String cond = valueAfterKeyword(statement, "COND");
        if (cond != null) {
            return cond;
        }
        return valueAfterCommand(statement, command);
    }

    private static String valueAfterCommand(String statement, String command) {
        String trimmed = statement.trim();
        String normalizedCommand = command.toUpperCase(Locale.ROOT);
        if (!trimmed.toUpperCase(Locale.ROOT).startsWith(normalizedCommand)) {
            return null;
        }
        String value = trimmed.substring(normalizedCommand.length()).trim();
        if (value.startsWith("=")) {
            value = value.substring(1).trim();
        }
        return value.isBlank() ? null : trimTrailingComma(value);
    }

    private static FieldValue fieldValue(String position, String length, String type) {
        int pos = parsePositiveInt(position);
        int len = parsePositiveInt(length);
        String fieldType = type.trim();
        return new FieldValue(fieldType, record -> slice(record, pos, len));
    }

    private static String slice(String record, int position, int length) {
        if (position < 1 || length < 0) {
            throw new IllegalArgumentException("Invalid DFSORT field reference");
        }
        StringBuilder value = new StringBuilder(length);
        int start = position - 1;
        for (int i = 0; i < length; i++) {
            int index = start + i;
            value.append(index < record.length() ? record.charAt(index) : ' ');
        }
        return value.toString();
    }

    private static String rightPad(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        StringBuilder padded = new StringBuilder(width);
        padded.append(value);
        while (padded.length() < width) {
            padded.append(' ');
        }
        return padded.toString();
    }

    private static String valueAfterKeyword(String statement, String keyword) {
        String upper = statement.toUpperCase(Locale.ROOT);
        String normalizedKeyword = keyword.toUpperCase(Locale.ROOT);
        int index = upper.indexOf(normalizedKeyword + "=");
        if (index < 0) {
            return null;
        }
        int valueStart = index + normalizedKeyword.length() + 1;
        int valueEnd = statement.length();
        for (String nextKeyword : KEYWORDS) {
            if (nextKeyword.equalsIgnoreCase(keyword)) {
                continue;
            }
            int next = indexOfTopLevelKeyword(statement, nextKeyword + "=", valueStart);
            if (next >= 0 && next < valueEnd) {
                valueEnd = next;
            }
        }
        return trimTrailingComma(statement.substring(valueStart, valueEnd).trim());
    }

    private static String firstValueAfterKeyword(String statement, String... keywords) {
        for (String keyword : keywords) {
            String value = valueAfterKeyword(statement, keyword);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static int indexOfTopLevelKeyword(String value, String keyword, int fromIndex) {
        String upper = value.toUpperCase(Locale.ROOT);
        String needle = keyword.toUpperCase(Locale.ROOT);
        int depth = 0;
        boolean quoted = false;
        for (int i = fromIndex; i <= value.length() - needle.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'') {
                quoted = !quoted;
            } else if (!quoted && ch == '(') {
                depth++;
            } else if (!quoted && ch == ')') {
                depth--;
            }
            if (!quoted && depth == 0 && upper.startsWith(needle, i)) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> splitByOperator(String expression, String operator) {
        List<String> result = new ArrayList<>();
        String upper = expression.toUpperCase(Locale.ROOT);
        int depth = 0;
        boolean quoted = false;
        int start = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '\'') {
                quoted = !quoted;
            } else if (!quoted && ch == '(') {
                depth++;
            } else if (!quoted && ch == ')') {
                depth--;
            }
            if (!quoted && depth == 0 && isOperatorAt(upper, operator, i)) {
                result.add(trimDelimiter(expression.substring(start, i)));
                i += operator.length() - 1;
                start = i + 1;
            }
        }
        if (start == 0) {
            return List.of(expression);
        }
        result.add(trimDelimiter(expression.substring(start)));
        return result;
    }

    private static String trimDelimiter(String value) {
        String trimmed = value.trim();
        while (trimmed.startsWith(",")) {
            trimmed = trimmed.substring(1).trim();
        }
        while (trimmed.endsWith(",")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static boolean isOperatorAt(String upper, String operator, int index) {
        if (!upper.startsWith(operator, index)) {
            return false;
        }
        int before = index - 1;
        int after = index + operator.length();
        return (before < 0 || !Character.isLetterOrDigit(upper.charAt(before)))
                && (after >= upper.length() || !Character.isLetterOrDigit(upper.charAt(after)));
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean quoted = false;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'') {
                quoted = !quoted;
            } else if (!quoted && ch == '(') {
                depth++;
            } else if (!quoted && ch == ')') {
                depth--;
            } else if (!quoted && depth == 0 && ch == delimiter) {
                parts.add(value.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(value.substring(start).trim());
        return parts;
    }

    private static String unwrap(String value) {
        String trimmed = value.trim();
        while (trimmed.length() >= 2 && trimmed.startsWith("(") && trimmed.endsWith(")")
                && balanced(trimmed.substring(1, trimmed.length() - 1))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static boolean balanced(String value) {
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'') {
                quoted = !quoted;
            } else if (!quoted && ch == '(') {
                depth++;
            } else if (!quoted && ch == ')') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0 && !quoted;
    }

    private static int indexOfTopLevel(String value, char target) {
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'') {
                quoted = !quoted;
            } else if (!quoted && ch == '(') {
                depth++;
            } else if (!quoted && ch == ')') {
                depth--;
            } else if (!quoted && depth == 0 && ch == target) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isLiteral(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        return (upper.startsWith("C'") || upper.startsWith("X'")) && value.endsWith("'");
    }

    private static String literalValue(String value) {
        String trimmed = value.trim();
        String body = trimmed.substring(2, trimmed.length() - 1);
        if (trimmed.regionMatches(true, 0, "X'", 0, 2)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 0; i + 1 < body.length(); i += 2) {
                out.write(Integer.parseInt(body.substring(i, i + 2), 16));
            }
            return out.toString(StandardCharsets.UTF_8);
        }
        return body;
    }

    private static String trimTrailingComma(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith(",")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static boolean isOrder(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("A") || normalized.equals("D") || normalized.equals("E");
    }

    private static boolean isDataType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("CH") || normalized.equals("ZD") || normalized.equals("PD") || normalized.equals("BI")
                || normalized.equals("FI") || normalized.equals("FS") || normalized.equals("UFF")
                || normalized.equals("SFF");
    }

    private static boolean isInteger(String value) {
        return value.trim().matches("\\d+");
    }

    private static int parsePositiveInt(String value) {
        int parsed = Integer.parseInt(value.trim());
        if (parsed < 1) {
            throw new IllegalArgumentException("DFSORT positions are 1-based");
        }
        return parsed;
    }

    private static boolean isNumericType(String type) {
        String normalized = type.toUpperCase(Locale.ROOT);
        return normalized.equals("ZD") || normalized.equals("PD") || normalized.equals("BI")
                || normalized.equals("FI") || normalized.equals("FS") || normalized.equals("UFF")
                || normalized.equals("SFF");
    }

    private static JclDd firstDd(JclStep step, String... names) {
        for (String name : names) {
            JclDd dd = step.dd(name);
            if (dd != null) {
                return dd;
            }
        }
        return null;
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
        return disp != null && disp.toUpperCase(Locale.ROOT).contains("MOD");
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

    private record SortKey(int position, int length, String type, boolean descending) {
    }

    private record FieldValue(String type, ValueSource source) {
        private String value(String record) {
            return source.value(record);
        }
    }

    private record OverlayItem(int position, BuildItem source) {
    }

    private static final class SortControl {
        private final List<SortKey> sortKeys = new ArrayList<>();
        private final List<OutfilControl> outfils = new ArrayList<>();
        private Condition include;
        private Condition omit;
        private RecordTransform inrec;
        private RecordTransform outrec;
    }

    private static final class OutfilControl {
        private final List<String> ddNames = new ArrayList<>();
        private Condition include;
        private Condition omit;
        private RecordTransform outrec;
    }

    private interface Condition extends Predicate<String> {
    }

    private interface RecordTransform {
        String apply(String record);
    }

    private interface BuildItem {
        String value(String record);
    }

    private interface ValueSource {
        String value(String record);
    }
}
