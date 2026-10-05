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
    private String retainedTail;

    public ExclusivePointerState(int pointerWidth, int integerWidth, String identity) {
        if (pointerWidth < integerWidth || integerWidth != Integer.BYTES)
            throw new IllegalArgumentException("Unsupported exclusive pointer word extent");
        this.pointerWidth = pointerWidth;
        this.integerWidth = integerWidth;
        this.identity = Objects.requireNonNull(identity);
        retainedTail = "\0".repeat(pointerWidth - integerWidth);
    }

    public Object getPointer() {
        if (integerRole) warn();
        return value;
    }

    public void setPointer(Object pointer) {
        value = pointer;
        integerRole = false;
        if (pointer == null) retainedTail = "\0".repeat(pointerWidth - integerWidth);
    }

    public Integer getInteger() {
        if (!integerRole && value != null) warn();
        return value == null ? 0 : (Integer) value;
    }

    public void setInteger(Integer number) {
        value = number == null ? 0 : number;
        integerRole = true;
    }

    /** Group INITIALIZE excludes the POINTER base and its REDEFINES view. */
    public void initializeGroup() { }

    private void warn() {
        System.getLogger(ExclusivePointerState.class.getName()).log(System.Logger.Level.WARNING,
                "Non-exclusive pointer/integer use: " + identity);
    }

    /** Portable group bytes exist for numeric or null roles; a Java reference has no native address encoding. */
    public String groupState() {
        if (!integerRole && value != null)
            throw new IllegalStateException("Native pointer encoding requires an explicit ABI: " + identity);
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
