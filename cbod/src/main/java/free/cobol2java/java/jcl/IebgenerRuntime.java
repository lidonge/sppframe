package free.cobol2java.java.jcl;

import java.io.IOException;

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
            byte[] content = JclDatasetRuntime.readBytes(input);
            JclDatasetRuntime.writeBytes(output, content);
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

    private static boolean isDummy(JclDd dd) {
        return JclDatasetRuntime.isDummy(dd);
    }
}
