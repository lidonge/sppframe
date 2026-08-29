package free.cobol2java.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/** Pre-bound, strongly typed operators used by externalized procedure expressions. */
public final class CobolExpressionCalculator {
    private CobolExpressionCalculator() {
    }

    @FunctionalInterface
    public interface Add<T, A, B> {
        T add(A left, B right);
    }

    @FunctionalInterface
    public interface Subtract<T, A, B> {
        T subtract(A left, B right);
    }

    @FunctionalInterface
    public interface Multiply<T, A, B> {
        T multiply(A left, B right);
    }

    @FunctionalInterface
    public interface Divide<T, A, B> {
        T divide(A left, B right);
    }

    @FunctionalInterface
    public interface Remainder<T, A, B> {
        T remainder(A left, B right);
    }

    public static <A extends Number, B extends Number> Add<BigInteger, A, B> bigIntegerAdd() {
        return (left, right) -> bigInteger(left).add(bigInteger(right));
    }

    public static <A extends Number, B extends Number> Subtract<BigInteger, A, B> bigIntegerSubtract() {
        return (left, right) -> bigInteger(left).subtract(bigInteger(right));
    }

    public static <A extends Number, B extends Number> Multiply<BigInteger, A, B> bigIntegerMultiply() {
        return (left, right) -> bigInteger(left).multiply(bigInteger(right));
    }

    public static <A extends Number, B extends Number> Remainder<BigInteger, A, B> bigIntegerRemainder() {
        return (left, right) -> bigInteger(left).remainder(bigInteger(right));
    }

    public static <A extends Number, B extends Number> Add<BigDecimal, A, B> bigDecimalAdd() {
        return (left, right) -> bigDecimal(left).add(bigDecimal(right));
    }

    public static <A extends Number, B extends Number> Subtract<BigDecimal, A, B> bigDecimalSubtract() {
        return (left, right) -> bigDecimal(left).subtract(bigDecimal(right));
    }

    public static <A extends Number, B extends Number> Multiply<BigDecimal, A, B> bigDecimalMultiply() {
        return (left, right) -> bigDecimal(left).multiply(bigDecimal(right));
    }

    public static <A extends Number, B extends Number> Divide<BigDecimal, A, B> bigDecimalDivide() {
        return (left, right) -> bigDecimal(left).divide(bigDecimal(right), MathContext.DECIMAL128);
    }

    public static <A extends Number, B extends Number> Remainder<BigDecimal, A, B> bigDecimalRemainder() {
        return (left, right) -> bigDecimal(left).remainder(bigDecimal(right));
    }

    public static <A extends Number, B extends Number> Add<Double, A, B> doubleAdd() {
        return (left, right) -> left.doubleValue() + right.doubleValue();
    }

    public static <A extends Number, B extends Number> Subtract<Double, A, B> doubleSubtract() {
        return (left, right) -> left.doubleValue() - right.doubleValue();
    }

    public static <A extends Number, B extends Number> Multiply<Double, A, B> doubleMultiply() {
        return (left, right) -> left.doubleValue() * right.doubleValue();
    }

    public static <A extends Number, B extends Number> Divide<Double, A, B> doubleDivide() {
        return (left, right) -> left.doubleValue() / right.doubleValue();
    }

    public static <A extends Number, B extends Number> Remainder<Double, A, B> doubleRemainder() {
        return (left, right) -> left.doubleValue() % right.doubleValue();
    }

    public static <A, B> Add<String, A, B> stringAdd() {
        return (left, right) -> String.valueOf(left) + String.valueOf(right);
    }

    private static BigInteger bigInteger(Number value) {
        if (value instanceof BigInteger integer) return integer;
        if (value instanceof BigDecimal decimal) return decimal.toBigIntegerExact();
        return BigInteger.valueOf(value.longValue());
    }

    private static BigDecimal bigDecimal(Number value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof BigInteger integer) return new BigDecimal(integer);
        if (value instanceof Float || value instanceof Double) {
            return BigDecimal.valueOf(value.doubleValue());
        }
        return BigDecimal.valueOf(value.longValue());
    }
}
