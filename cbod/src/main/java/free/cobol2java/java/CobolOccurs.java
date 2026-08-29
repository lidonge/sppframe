package free.cobol2java.java;

/** COBOL one-based OCCURS/ODO index validation. */
public final class CobolOccurs {
    private CobolOccurs() {
    }

    public static int index(int oneBasedIndex, int effectiveLength,
                            int effectiveMinimum, int physicalMaximum) {
        if (effectiveLength < effectiveMinimum || effectiveLength > physicalMaximum) {
            throw new IndexOutOfBoundsException(
                    "COBOL ODO effective length " + effectiveLength
                            + " outside declared range " + effectiveMinimum + ".." + physicalMaximum);
        }
        if (oneBasedIndex < 1 || oneBasedIndex > effectiveLength) {
            throw new IndexOutOfBoundsException(
                    "COBOL OCCURS index " + oneBasedIndex
                            + " outside effective range 1.." + effectiveLength);
        }
        return oneBasedIndex - 1;
    }
}
