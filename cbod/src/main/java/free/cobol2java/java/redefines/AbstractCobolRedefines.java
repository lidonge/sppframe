package free.cobol2java.java.redefines;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * {@link ICobolRedefines} 的抽象基类。
 * <p>
 * 该类封装了共享字节数组、起始偏移、字段长度等通用逻辑，
 * 具体类型的编码与解码由子类实现。
 * </p>
 *
 * @param <T> 字段对应的 Java 类型
 */
public abstract class AbstractCobolRedefines<T> implements ICobolRedefines<T> {

    protected final byte[] storage;
    protected int start;
    protected int length;
    protected final Charset charset;

    protected AbstractCobolRedefines(int totalSize) {
        this(new byte[totalSize], 0, totalSize, StandardCharsets.UTF_8);
    }

    protected AbstractCobolRedefines(byte[] storage) {
        this(storage, 0, storage.length, StandardCharsets.UTF_8);
    }

    protected AbstractCobolRedefines(byte[] storage, int start, int length) {
        this(storage, start, length, StandardCharsets.UTF_8);
    }

    protected AbstractCobolRedefines(byte[] storage, int start, int length, Charset charset) {
        this.storage = Objects.requireNonNull(storage, "storage must not be null");
        this.charset = Objects.requireNonNull(charset, "charset must not be null");
        validateRange(start, length);
        this.start = start;
        this.length = length;
    }

    @Override
    public void setRedefines(int start, int length) {
        validateRange(start, length);
        this.start = start;
        this.length = length;
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOfRange(storage, start, start + length);
    }

    protected String readTrimmedString() {
        return new String(storage, start, length, charset).trim();
    }

    protected String readRawString() {
        return new String(storage, start, length, charset);
    }

    @Override
    public String toString() {
        return readRawString();
    }

    protected void writeString(String value) {
        String actual = value == null ? "" : value;
        writeBytes(actual.getBytes(charset));
    }

    protected void writeBytes(byte[] src) {
        Arrays.fill(storage, start, start + length, (byte) ' ');
        if (src != null) {
            System.arraycopy(src, 0, storage, start, Math.min(src.length, length));
        }
    }

    public void set(String value) {
        writeString(value);
    }

    @Override
    public void set(ICobolRedefines<?> value) {
        writeBytes(value == null ? null : value.getBytes());
    }

    protected void validateRange(int start, int length) {
        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        if (start + length > storage.length) {
            throw new IllegalArgumentException(
                    "redefines range exceeds storage size: start=" + start + ", length=" + length +
                            ", storage=" + storage.length);
        }
    }
}
