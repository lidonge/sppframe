package free.cobol2java.java.jcl;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProcRuntime {
    private static final String JAVA_PROC_CLASS = "JAVA_PROC_CLASS";

    private ProcRuntime() {
    }

    public static int execute(JclStep step) throws Exception {
        if (step == null) {
            throw new IllegalArgumentException("JCL step is required");
        }
        String proc = step.getProc();
        if (proc == null || proc.isBlank()) {
            if (step.getProgram() == null && !step.getDds().isEmpty()) {
                return JclReturnCodes.OK;
            }
            throw new IllegalStateException("Missing JCL PROC name for step " + step.getName());
        }
        if (skippedProcs().contains(proc.toUpperCase(Locale.ROOT))) {
            System.out.println("Skipping JCL PROC " + proc + " for step " + step.getName());
            return JclReturnCodes.OK;
        }
        String procClassName = param(step.getParameters(), JAVA_PROC_CLASS);
        if (procClassName != null && !procClassName.isBlank()) {
            AbstractJclJob procJob = procJob(procClassName);
            if (procJob != null) {
                try (JclSymbolRuntime.SymbolScope ignored = JclSymbolRuntime.push(step.getParameters())) {
                    return procJob.run();
                }
            }
        }
        throw new UnsupportedOperationException("JCL PROC is not generated or registered: " + proc
                + ". Configure -Djcl.proc.skip=" + proc + " only when this PROC is intentionally bypassed.");
    }

    private static AbstractJclJob procJob(String className) throws Exception {
        Class<?> jobClass = null;
        for (ClassLoader classLoader : candidateClassLoaders()) {
            try {
                jobClass = Class.forName(className, true, classLoader);
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (jobClass == null || !AbstractJclJob.class.isAssignableFrom(jobClass)) {
            return null;
        }
        return (AbstractJclJob) jobClass.getDeclaredConstructor().newInstance();
    }

    private static ClassLoader[] candidateClassLoaders() {
        return new ClassLoader[]{
                Thread.currentThread().getContextClassLoader(),
                ProcRuntime.class.getClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
    }

    private static String param(java.util.Map<String, String> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }
        for (java.util.Map.Entry<String, String> entry : parameters.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Set<String> skippedProcs() {
        String value = System.getProperty("jcl.proc.skip");
        if (value == null || value.isBlank()) {
            value = System.getenv("JCL_PROC_SKIP");
        }
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(item -> item.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
