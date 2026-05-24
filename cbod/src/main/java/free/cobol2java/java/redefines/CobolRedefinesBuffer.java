package free.cobol2java.java.redefines;

import java.util.Arrays;

/**
 * 可增长的共享字节存储，被同一个 REDEFINES 区域上的多个视图共用。
 * <p>
 * COBOL 里一个 REDEFINES 项可能比被重定义字段更大（在大型机上这是合法的：
 * 该区域只是更大 commarea 的一个窗口）。用一个可增长的缓冲区持有底层数组，
 * 任何视图需要更大的范围时就地扩容，所有引用同一缓冲区的视图都能看到扩容后的数组，
 * 从而既不会越界崩溃、又保持别名（重叠）语义。
 * </p>
 */
public final class CobolRedefinesBuffer {

    private byte[] bytes;

    public CobolRedefinesBuffer(int size) {
        this.bytes = new byte[Math.max(0, size)];
    }

    public CobolRedefinesBuffer(byte[] bytes) {
        this.bytes = bytes == null ? new byte[0] : bytes;
    }

    /** 当前底层数组（扩容后引用会变化，调用方不应缓存）。 */
    public byte[] bytes() {
        return bytes;
    }

    public int size() {
        return bytes.length;
    }

    /** 确保至少能容纳 {@code minSize} 个字节，必要时扩容并保留已有数据。 */
    public void ensureCapacity(int minSize) {
        if (minSize > bytes.length) {
            bytes = Arrays.copyOf(bytes, minSize);
        }
    }
}
