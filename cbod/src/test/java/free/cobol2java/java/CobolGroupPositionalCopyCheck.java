package free.cobol2java.java;

/** Minimal runtime check for positional COBOL group-to-group MOVE semantics. */
public final class CobolGroupPositionalCopyCheck {
    private CobolGroupPositionalCopyCheck() {
    }

    public static void main(String[] args) {
        SourceKey source = new SourceKey();
        source.business = "HKD";
        source.code = "ABC123456";
        TargetKey target = new TargetKey();

        target.copy(source);

        if (!"HKDABC123456".equals(target.bytes)) {
            throw new AssertionError("group MOVE was not copied by physical position: "
                    + target.bytes);
        }
        System.out.println("CobolGroup positional copy check passed.");
    }

    private static final class SourceKey implements CobolGroup {
        @FieldInfo(cobolType = "X(3)", byteLength = 3)
        private String business = "   ";
        @FieldInfo(cobolType = "X(9)", byteLength = 9)
        private String code = "         ";
    }

    private static final class TargetKey implements CobolGroup {
        @FieldInfo(cobolType = "X(12)", byteLength = 12)
        private String bytes = "            ";
    }
}
