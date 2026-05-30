package free.cobol2java.java.jcl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JclSymbolRuntime {
    private static final Pattern SYMBOL = Pattern.compile("&([A-Za-z0-9_#$@]+)\\.?");
    private static final ThreadLocal<Deque<Map<String, String>>> SYMBOL_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private JclSymbolRuntime() {
    }

    public static SymbolScope push(Map<String, String> symbols) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (symbols != null) {
            for (Map.Entry<String, String> entry : symbols.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    normalized.put(normalize(entry.getKey()), entry.getValue());
                }
            }
        }
        SYMBOL_STACK.get().push(normalized);
        return new SymbolScope();
    }

    public static String resolve(String value) {
        if (value == null || value.indexOf('&') < 0) {
            return value;
        }
        String escaped = value.replace("&&", "\u0000");
        Matcher matcher = SYMBOL.matcher(escaped);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String replacement = lookup(matcher.group(1));
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString().replace('\u0000', '&');
    }

    private static String lookup(String name) {
        String normalized = normalize(name);
        for (Map<String, String> frame : SYMBOL_STACK.get()) {
            String value = frame.get(normalized);
            if (value != null) {
                return value;
            }
        }
        String property = System.getProperty("jcl.symbol." + name);
        if (property == null) {
            property = System.getProperty("jcl.symbol." + normalized);
        }
        if (property != null) {
            return property;
        }
        String envName = "JCL_SYMBOL_" + normalized;
        return System.getenv(envName);
    }

    private static String normalize(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }

    public static final class SymbolScope implements AutoCloseable {
        private boolean closed;

        @Override
        public void close() {
            if (closed) {
                return;
            }
            Deque<Map<String, String>> stack = SYMBOL_STACK.get();
            if (!stack.isEmpty()) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                SYMBOL_STACK.remove();
            }
            closed = true;
        }
    }
}
