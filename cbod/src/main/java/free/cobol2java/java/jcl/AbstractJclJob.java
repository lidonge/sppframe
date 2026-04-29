package free.cobol2java.java.jcl;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractJclJob {
    private final Map<String, Integer> stepReturnCodes = new LinkedHashMap<>();
    private int jobRc;

    public abstract int run() throws Exception;

    protected int runJob(String jobName, JclStepAction action) throws Exception {
        stepReturnCodes.clear();
        jobRc = 0;
        return action.run();
    }

    protected int runStep(JclStep step, JclStepAction action) throws Exception {
        if (shouldSkip(step)) {
            stepReturnCodes.put(step.getName(), 0);
            return 0;
        }
        int rc = action.run();
        stepReturnCodes.put(step.getName(), rc);
        jobRc = Math.max(jobRc, rc);
        return rc;
    }

    protected boolean shouldSkip(JclStep step) {
        JclCond cond = step.getCond();
        return cond != null && cond.matches(stepReturnCodes, jobRc);
    }

    public int getJobRc() {
        return jobRc;
    }

    public Map<String, Integer> getStepReturnCodes() {
        return Map.copyOf(stepReturnCodes);
    }
}
