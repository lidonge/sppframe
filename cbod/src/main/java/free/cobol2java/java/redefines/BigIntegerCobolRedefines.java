package free.cobol2java.java.redefines;

import java.math.BigInteger;

public class BigIntegerCobolRedefines extends AbstractCobolRedefines<BigInteger> {
    public BigIntegerCobolRedefines(int length) {
        super(length);
    }

    public BigIntegerCobolRedefines(byte[] storage) {
        super(storage);
    }

    public BigIntegerCobolRedefines(byte[] storage, int start, int length) {
        super(storage, start, length);
    }

    @Override
    public BigInteger get() {
        String value = readTrimmedString();
        return value.isEmpty() ? BigInteger.ZERO : new BigInteger(value);
    }

    @Override
    public void set(BigInteger value) {
        writeString(value == null ? "0" : value.toString());
    }

}
