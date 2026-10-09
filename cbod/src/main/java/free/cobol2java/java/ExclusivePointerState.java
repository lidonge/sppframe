package free.cobol2java.java;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/** One ordinary payload for mutually exclusive POINTER and integral COMP roles. */
public final class ExclusivePointerState {
    private Object value;
    private boolean integerRole;
    private final int pointerWidth;
    private final int integerWidth;
    private final String identity;
    private final String sourceFile;
    private final int sourceLine;
    private final boolean integerBase;
    private String retainedTail;

    public ExclusivePointerState(int pointerWidth, int integerWidth, String identity) {
        this(pointerWidth, integerWidth, identity, "<source not supplied>", 0);
    }

    public ExclusivePointerState(int pointerWidth, int integerWidth, String identity,
                                 String sourceFile, int sourceLine) {
        this(pointerWidth, integerWidth, identity, sourceFile, sourceLine, false);
    }

    public ExclusivePointerState(int pointerWidth, int integerWidth, String identity,
                                 String sourceFile, int sourceLine, boolean integerBase) {
        if (pointerWidth < integerWidth || integerWidth != Integer.BYTES)
            throw new IllegalArgumentException("Unsupported exclusive pointer word extent");
        this.pointerWidth = pointerWidth;
        this.integerWidth = integerWidth;
        this.identity = Objects.requireNonNull(identity);
        this.sourceFile = Objects.requireNonNull(sourceFile);
        this.sourceLine = sourceLine;
        this.integerBase = integerBase;
        retainedTail = "\0".repeat(pointerWidth - integerWidth);
    }

    public Object getPointer() {
        if (integerRole) {
            if ((Integer) value != 0 || retainedTail.chars().anyMatch(character -> character != 0))
                throw addressError("INTEGER_TO_POINTER");
            return null;
        }
        return value;
    }

    public void setPointer(Object pointer) {
        value = pointer;
        integerRole = false;
        if (pointer == null) retainedTail = "\0".repeat(pointerWidth - integerWidth);
    }

    public Integer getInteger() {
        if (!integerRole && value != null) throw addressError("POINTER_TO_INTEGER");
        return value == null ? 0 : (Integer) value;
    }

    public void setInteger(Integer number) {
        if (integerBase && !integerRole && value != null && pointerWidth > integerWidth)
            throw addressError("PARTIAL_WRITE_TO_POINTER");
        value = number == null ? 0 : number;
        integerRole = true;
    }

    /** Group INITIALIZE excludes the POINTER base and its REDEFINES view. */
    public void initializeGroup() { if (integerBase) setInteger(0); }

    private UnsupportedAbsoluteAddressException addressError(String operation) {
        return new UnsupportedAbsoluteAddressException(identity, operation, sourceFile, sourceLine);
    }

    /** Portable group bytes exist for numeric or null roles; a Java reference has no native address encoding. */
    public String groupState() {
        if (!integerRole && value != null)
            throw addressError("POINTER_TO_BYTES");
        byte[] bytes = new byte[pointerWidth];
        ByteBuffer.wrap(bytes).putInt(value == null ? 0 : (Integer) value);
        byte[] tail = CobolUtf8Codec.encodeState(retainedTail);
        System.arraycopy(tail, 0, bytes, integerWidth, tail.length);
        return CobolUtf8Codec.decodeState(bytes);
    }

    public void setGroupState(String state) {
        byte[] bytes = CobolUtf8Codec.encodeState(state);
        if (bytes.length != pointerWidth)
            throw new IllegalArgumentException("Exclusive pointer group extent differs: " + identity);
        value = ByteBuffer.wrap(bytes).getInt();
        integerRole = true;
        retainedTail = CobolUtf8Codec.decodeState(Arrays.copyOfRange(bytes, integerWidth, bytes.length));
        if ((Integer) value == 0 && retainedTail.chars().allMatch(character -> character == 0)) {
            value = null;
            integerRole = false;
        }
    }

    public int readFromGroup(String group, int characterOffset) {
        byte[] bytes = CobolUtf8Codec.encodeState(group.substring(characterOffset));
        Objects.checkFromIndexSize(0, pointerWidth, bytes.length);
        String state = CobolUtf8Codec.decodeState(Arrays.copyOf(bytes, pointerWidth));
        setGroupState(state);
        return state.length();
    }
}
