package free.cobol2java.java;

import java.time.LocalDate;
import java.util.Random;

/**
 * @author lidong@date 2024-10-14@version 1.0
 * Small runtime wrappers for COBOL intrinsic functions already emitted by the translator.
 */
public class Function {
    private static final Random RANDOM = new Random();

    /**
     * Returns the uppercase form of the source string.
     * Null input is mapped to null to keep caller-side handling simple.
     */
    public static String upperCase(String src) {
        return src == null ? null : src.toUpperCase();
    }

    /**
     * Returns the lowercase form of the source string.
     * Null input is mapped to null to keep caller-side handling simple.
     */
    public static String lowerCase(String src) {
        return src == null ? null : src.toLowerCase();
    }

    /**
     * Returns the arithmetic remainder of a divided by b.
     */
    public static int mod(int a, int b){
        return a%b;
    }

    /**
     * COBOL FUNCTION REM maps to the same arithmetic remainder behavior here.
     */
    public static int rem(int a, int b) {
        return a % b;
    }

    /**
     * Returns the current calendar date in YYYYMMDD integer form.
     * This matches the numeric shape commonly used by generated COBOL runtime code.
     */
    public static Integer currentDate() {
        LocalDate today = LocalDate.now();
        return today.getYear() * 10000 + today.getMonthValue() * 100 + today.getDayOfMonth();
    }

    /**
     * Returns a non-negative pseudo-random integer.
     * When a seed is provided the generator is reseeded first, which makes the
     * next returned value deterministic for the same seed.
     */
    public static int random(Integer seed) {
        if (seed != null) {
            RANDOM.setSeed(seed.longValue());
        }
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }
}
