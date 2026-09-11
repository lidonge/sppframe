package free.cobol2java.java;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A live address into ordinary character state. Addresses carry identity and an offset,
 * not a copied value; the receiving view supplies its width on every read/write.
 */
public final class CobolCharacterAddress {
    public static final class Space {
        private final int extent;
        private final Supplier<String> getter;
        private final Consumer<String> setter;
        private final boolean utf8;
        private final ConcurrentHashMap<Integer, CobolCharacterAddress> addresses = new ConcurrentHashMap<>();

        public Space(int extent, Supplier<String> getter, Consumer<String> setter) {
            this(extent, getter, setter, false);
        }

        /** Callbacks carry lossless Codec state, not decoded public field values. */
        public static Space utf8(int extent, Supplier<String> rawGetter, Consumer<String> rawSetter) {
            return new Space(extent, rawGetter, rawSetter, true);
        }

        private Space(int extent, Supplier<String> getter, Consumer<String> setter, boolean utf8) {
            if (extent < 1) throw new IllegalArgumentException("Character address space requires a positive extent");
            this.extent = extent;
            this.getter = Objects.requireNonNull(getter);
            this.setter = Objects.requireNonNull(setter);
            this.utf8 = utf8;
        }

        public CobolCharacterAddress at(int offset) {
            if (offset < 0 || offset > extent) throw new IndexOutOfBoundsException("Character address offset: " + offset);
            return addresses.computeIfAbsent(offset, key -> new CobolCharacterAddress(this, key));
        }

        private String current() {
            String value = Objects.requireNonNull(getter.get(), "Character address state");
            int width = utf8 ? CobolUtf8Codec.encodeState(value).length : value.length();
            if (width != extent) throw new IllegalStateException("Character address extent changed");
            return value;
        }
    }

    private final Space space;
    private final int offset;

    private CobolCharacterAddress(Space space, int offset) {
        this.space = space;
        this.offset = offset;
    }

    public int offset() { return offset; }

    public String read(int width) {
        checkWidth(width);
        String current = space.current();
        return space.utf8 ? CobolUtf8Codec.read(current, offset, width)
                : current.substring(offset, offset + width);
    }

    public void write(String value, int width) {
        checkWidth(width);
        String current = space.current();
        if (space.utf8) {
            space.setter.accept(CobolUtf8Codec.write(current, value, offset, width));
            return;
        }
        space.setter.accept(current.substring(0, offset) + CobolString.fixed(value, width)
                + current.substring(offset + width));
    }

    private void checkWidth(int width) {
        if (width < 1 || width > space.extent - offset) {
            throw new IndexOutOfBoundsException("Character address receiving width: " + width);
        }
    }
}
