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
        private final ConcurrentHashMap<Integer, CobolCharacterAddress> addresses = new ConcurrentHashMap<>();

        public Space(int extent, Supplier<String> getter, Consumer<String> setter) {
            if (extent < 1) throw new IllegalArgumentException("Character address space requires a positive extent");
            this.extent = extent;
            this.getter = Objects.requireNonNull(getter);
            this.setter = Objects.requireNonNull(setter);
        }

        public CobolCharacterAddress at(int offset) {
            if (offset < 0 || offset > extent) throw new IndexOutOfBoundsException("Character address offset: " + offset);
            return addresses.computeIfAbsent(offset, key -> new CobolCharacterAddress(this, key));
        }

        private String current() {
            String value = Objects.requireNonNull(getter.get(), "Character address state");
            if (value.length() != extent) throw new IllegalStateException("Character address extent changed");
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
        return space.current().substring(offset, offset + width);
    }

    public void write(String value, int width) {
        checkWidth(width);
        String current = space.current();
        space.setter.accept(current.substring(0, offset) + CobolString.fixed(value, width)
                + current.substring(offset + width));
    }

    private void checkWidth(int width) {
        if (width < 1 || width > space.extent - offset) {
            throw new IndexOutOfBoundsException("Character address receiving width: " + width);
        }
    }
}
