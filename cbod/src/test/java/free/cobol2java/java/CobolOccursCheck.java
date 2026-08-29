package free.cobol2java.java;

/** Minimal runtime check for one-based OCCURS and ODO effective bounds. */
public final class CobolOccursCheck {
    private CobolOccursCheck() {
    }

    public static void main(String[] args) {
        if (CobolOccurs.index(2, 2, 1, 5) != 1) {
            throw new AssertionError("COBOL one-based index was not converted to Java index");
        }
        expectOutOfBounds(() -> CobolOccurs.index(3, 2, 1, 5));
        expectOutOfBounds(() -> CobolOccurs.index(1, 0, 1, 5));
        expectOutOfBounds(() -> CobolOccurs.index(1, 6, 1, 5));
        if (CobolOccurs.index(1, 1, 0, 5) != 0) {
            throw new AssertionError("ODO declaration with lower bound zero rejected valid access");
        }
        System.out.println("CobolOccurs runtime check passed.");
    }

    private static void expectOutOfBounds(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // Expected physical/effective OCCURS boundary failure.
        }
    }
}
