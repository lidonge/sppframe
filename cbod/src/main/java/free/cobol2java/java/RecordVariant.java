package free.cobol2java.java;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/** One active ordinary record; source record switching uses explicit byte Codecs. */
public final class RecordVariant<T> {
    public interface Codec<R> {
        int width();
        byte[] encode(R value);
        void decode(R value, byte[] bytes);
    }

    private final int width;
    private T value;
    private Codec<T> codec;
    private String tail;

    public RecordVariant(int width) {
        if (width < 1) throw new IllegalArgumentException("Record workspace must have a positive extent");
        this.width = width;
        tail = " ".repeat(width);
    }

    public String state() {
        if (value == null) return tail;
        byte[] prefix = Objects.requireNonNull(codec.encode(value));
        if (prefix.length != codec.width()) throw new IllegalStateException("Record Codec changed its extent");
        return CobolUtf8Codec.decodeState(prefix) + tail;
    }

    /** A full raw write postpones selection until the source accesses a declared record view. */
    public void state(String state) {
        byte[] incoming = CobolUtf8Codec.encodeState(Objects.requireNonNull(state));
        byte[] bytes = new byte[width];
        Arrays.fill(bytes, (byte) 32);
        System.arraycopy(incoming, 0, bytes, 0, Math.min(incoming.length, bytes.length));
        value = null;
        codec = null;
        tail = CobolUtf8Codec.decodeState(bytes);
    }

    public <R extends T> R select(Class<R> type, Codec<R> target, Supplier<R> factory) {
        requireCodec(target);
        if (value != null && type.isInstance(value)) {
            if (codec.width() != target.width()) throw new IllegalStateException("Record type has conflicting Codec extents");
            return type.cast(value);
        }
        return bind(type, target, factory, CobolUtf8Codec.encodeState(state()));
    }

    /** A record MOVE overwrites its declared extent and leaves the rest of the workspace intact. */
    public <R extends T> void receive(Class<R> type, Codec<R> target, Supplier<R> factory, String incoming) {
        requireCodec(target);
        byte[] bytes = CobolUtf8Codec.encodeState(state());
        Arrays.fill(bytes, 0, target.width(), (byte) 32);
        byte[] source = CobolUtf8Codec.encodeState(Objects.requireNonNull(incoming));
        System.arraycopy(source, 0, bytes, 0, Math.min(source.length, target.width()));
        bind(type, target, factory, bytes);
    }

    public void write(int start, int length, String incoming) {
        Objects.checkFromIndexSize(start - 1, length, width);
        byte[] bytes = CobolUtf8Codec.encodeState(state());
        Arrays.fill(bytes, start - 1, start - 1 + length, (byte) 32);
        byte[] source = CobolUtf8Codec.encodeState(Objects.requireNonNull(incoming));
        System.arraycopy(source, 0, bytes, start - 1, Math.min(source.length, length));
        state(CobolUtf8Codec.decodeState(bytes));
    }

    private void requireCodec(Codec<?> target) {
        Objects.requireNonNull(target);
        if (target.width() < 1 || target.width() > width)
            throw new IllegalArgumentException("Record Codec exceeds its source workspace");
    }

    @SuppressWarnings("unchecked")
    private <R extends T> R bind(Class<R> type, Codec<R> target, Supplier<R> factory, byte[] bytes) {
        R next = type.cast(Objects.requireNonNull(factory.get()));
        target.decode(next, Arrays.copyOf(bytes, target.width()));
        value = next;
        codec = (Codec<T>) (Codec<?>) target;
        tail = CobolUtf8Codec.decodeState(Arrays.copyOfRange(bytes, target.width(), width));
        return next;
    }
}
