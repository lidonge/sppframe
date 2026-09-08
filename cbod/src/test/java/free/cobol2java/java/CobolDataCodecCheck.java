package free.cobol2java.java;

/** Executable check; no testing-library dependency is needed by the runtime module. */
public final class CobolDataCodecCheck {
    public static void main(String[] args) {
        for (CobolDataCodec codec : CobolDataCodec.values()) {
            require(CobolDataCodec.forCodepage(codec.codepage()) == codec, "codec identity");
            require(codec.decodeByte(0x15).equals("\u0085"), "15 is NEL");
            require(codec.decodeByte(0x25).equals("\n"), "25 is LF");
            require(codec.encodeCharacter('\u0085') == 0x15, "NEL byte");
            require(codec.encodeCharacter('\n') == 0x25, "LF byte");
            require(codec.decodeByte(0x9f).equals(codec == CobolDataCodec.IBM_1140 ? "\u20ac" : "\u00a4"),
                    "currency character");
            byte[] all = new byte[256];
            for (int value = 0; value < all.length; value++) {
                all[value] = (byte) value;
                require(codec.encodeCharacter(codec.decodeByte(value).charAt(0)) == value,
                        "byte round-trip: " + codec + "/" + value);
            }
            require(java.util.Arrays.equals(all, codec.encode(codec.decode(all))), "array round-trip");
            expect(IllegalArgumentException.class, () -> codec.encodeCharacter('\u4e2d'));
            expect(IndexOutOfBoundsException.class, () -> codec.decodeByte(-1));
            expect(IndexOutOfBoundsException.class, () -> codec.decodeByte(256));
        }
        expect(IllegalArgumentException.class, () -> CobolDataCodec.forCodepage(1208));
        System.out.println("CobolDataCodecCheck passed: 3 codepages, 768 byte round-trips and control boundaries");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            if (expected.isInstance(failure)) return;
            throw failure;
        }
        throw new AssertionError("Expected " + expected.getName());
    }
}
