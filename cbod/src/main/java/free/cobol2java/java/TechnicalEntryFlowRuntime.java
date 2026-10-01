package free.cobol2java.java;

import java.util.List;
import java.util.Objects;

/** Executes source-ordered adapter and business stages on one generated invocation state. */
public final class TechnicalEntryFlowRuntime {
    private TechnicalEntryFlowRuntime() { }

    public sealed interface Step permits Business, Adapter {
        void run();
    }

    public record Business(String id, Runnable action) implements Step {
        public Business {
            requireId(id);
            Objects.requireNonNull(action, "business stage action");
        }

        @Override public void run() {
            action.run();
        }
    }

    public record Adapter(String id, Runnable before, List<Step> steps, Runnable after)
            implements Step {
        public Adapter {
            requireId(id);
            steps = List.copyOf(Objects.requireNonNull(steps, "adapter continuation"));
            if (steps.isEmpty() || (before == null && after == null)) {
                throw new IllegalArgumentException("Adapter requires a continuation and source code: " + id);
            }
        }

        @Override public void run() {
            if (before != null) before.run();
            execute(steps);
            if (after != null) after.run();
        }
    }

    public static void execute(List<Step> steps) {
        for (Step step : List.copyOf(Objects.requireNonNull(steps, "entry flow steps"))) {
            Objects.requireNonNull(step, "entry flow step").run();
        }
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Entry flow stage id is missing");
        }
    }
}
