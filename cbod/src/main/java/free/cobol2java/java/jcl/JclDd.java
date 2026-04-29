package free.cobol2java.java.jcl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JclDd {
    private final String name;
    private final Map<String, String> parameters = new LinkedHashMap<>();
    private final List<String> inlineData = new ArrayList<>();

    private JclDd(String name) {
        this.name = name;
    }

    public static JclDd named(String name) {
        return new JclDd(name);
    }

    public JclDd param(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    public JclDd data(String line) {
        inlineData.add(line);
        return this;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getParameters() {
        return Map.copyOf(parameters);
    }

    public List<String> getInlineData() {
        return List.copyOf(inlineData);
    }
}
