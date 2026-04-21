package free.cobol2java.java.redefines;

import java.math.BigDecimal;

public class BigDecimalCobolRedefines extends AbstractCobolRedefines<BigDecimal> {
    public BigDecimalCobolRedefines(int length) {
        super(length);
    }

    public BigDecimalCobolRedefines(byte[] storage) {
        super(storage);
    }

    public BigDecimalCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public BigDecimal get() {
        String value = readTrimmedString();
        return value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    @Override
    public void set(BigDecimal value) {
        writeString(value == null ? "0" : value.toPlainString());
    }

}
