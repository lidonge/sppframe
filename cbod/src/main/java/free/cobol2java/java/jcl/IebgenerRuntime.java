package free.cobol2java.java.jcl;

import java.io.IOException;

public final class IebgenerRuntime {
    private IebgenerRuntime() {
    }

    public static int execute(JclStep step) {
        if (step == null) {
            return JclReturnCodes.ERROR;
        }
        if (!hasSupportedSysin(step.dd("SYSIN"))) {
            return JclReturnCodes.ERROR;
        }
        JclDd input = step.dd("SYSUT1");
        JclDd output = step.dd("SYSUT2");
        if (input == null || output == null) {
            return JclReturnCodes.ERROR;
        }
        try {
            byte[] content = JclDatasetRuntime.readBytes(input);
            JclDatasetRuntime.writeBytes(output, content);
            return JclReturnCodes.OK;
        } catch (IOException e) {
            return JclReturnCodes.SEVERE_ERROR;
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
