package free.cobol2java.java;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Writes operation status into source-bound fields without exposing adapter response types. */
public final class AccessStatusSink {
    private final BiConsumer<Integer, Integer> writer;
    private int status;
    private int detail;

    public AccessStatusSink(BiConsumer<Integer, Integer> writer) {
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    public void publish(int status, int detail) {
        this.status = status;
        this.detail = detail;
        writer.accept(status, detail);
    }

    public int status() { return status; }
    public int detail() { return detail; }
}
