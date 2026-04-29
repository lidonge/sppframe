package free.cobol2java.java.jcl;

import java.util.ArrayList;
import java.util.List;

public class JclStep {
    private final String name;
    private String program;
    private String proc;
    private String parm;
    private JclCond cond;
    private final List<JclDd> dds = new ArrayList<>();

    private JclStep(String name) {
        this.name = name;
    }

    public static JclStep named(String name) {
        return new JclStep(name);
    }

    public JclStep program(String program) {
        this.program = program;
        return this;
    }

    public JclStep proc(String proc) {
        this.proc = proc;
        return this;
    }

    public JclStep parm(String parm) {
        this.parm = parm;
        return this;
    }

    public JclStep cond(String cond) {
        this.cond = JclCond.parse(cond);
        return this;
    }

    public JclStep dd(JclDd dd) {
        dds.add(dd);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getProgram() {
        return program;
    }

    public String getProc() {
        return proc;
    }

    public String getParm() {
        return parm;
    }

    public String[] getParmArray() {
        return parm == null || parm.isBlank() ? new String[0] : new String[]{parm};
    }

    public JclCond getCond() {
        return cond;
    }

    public List<JclDd> getDds() {
        return List.copyOf(dds);
    }

    public JclDd dd(String name) {
        return dds.stream().filter(dd -> dd.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
