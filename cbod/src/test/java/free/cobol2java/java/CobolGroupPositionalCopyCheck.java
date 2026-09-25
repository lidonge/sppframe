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

        SourceProductKey productSource = new SourceProductKey();
        productSource.business = "BLN";
        productSource.record = "ABCDEFGHIJ1234567890uvwxyz";
        TargetProductKey productTarget = new TargetProductKey();
        productTarget.copy(productSource);
        if (!"BLN".equals(productTarget.business)
                || !"ABCDEFGHIJ1234567890uvwxyz".equals(productTarget.getBlnRecord())) {
            throw new AssertionError("character subgroup with byteLength only was not copied: "
                    + productTarget.business + productTarget.getBlnRecord());
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

    /** Mirrors BOC MBWRMAS's named character subgroup MBW-PROD-BLN-REC. */
    private static final class SourceProductKey implements CobolGroup {
        @FieldInfo(cobolType = "X(3)", precision = 3, byteLength = 3)
        private String business = "   ";
        @FieldInfo(cobolType = "X(26)", precision = 26, byteLength = 26)
        private String record = " ".repeat(26);
    }

    /** The generated subgroup state has no PIC: its exact size is in byteLength. */
    private static final class TargetProductKey implements CobolGroup {
        @FieldInfo(cobolType = "X(3)", precision = 3, byteLength = 3)
        private String business = "   ";
        @FieldInfo(cobolType = "", precision = -1, byteLength = 26)
        private String blnRecord = " ".repeat(26);

        private String getBlnRecord() {
            return blnRecord;
        }
    }
}
