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

    public BigDecimalCobolRedefines(CobolRedefinesBuffer storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public BigDecimal get() {
        String value = readTrimmedString();
        return value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    @Override
    public void set(BigDecimal value) {
        writeString((value == null ? BigDecimal.ZERO : value).toPlainString());
    }

    @Override
    public void set(ICobolRedefines<?> value) {
        Object actual = value == null ? null : value.get();
        if (actual instanceof BigDecimal decimal) {
            set(decimal);
        } else if (actual instanceof Number number) {
            set(BigDecimal.valueOf(number.longValue()));
        } else if (actual == null) {
            set(BigDecimal.ZERO);
        } else {
            String text = actual.toString().trim();
            set(text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text));
        }
    }

}
