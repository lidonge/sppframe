package free.cobol2java.java;

/**
 * @author lidong@date 2024-09-04@version 1.0
 */
public class Util {
    public static void output(Object ...args) {
    }

    public static String Input() {
        return "";
    }

    public static String now() {
        return "";
    }

    public static Object init(Class typeClass, String pictureStr) {
        return "";
    }

    public static String subvalue(Object obj, Integer start, Integer len) {
        return null;
    }

    public static Integer sizeof(Object obj) {
        return 0;
    }

    public static Integer copyCastToInteger(Object src) {
        return null;
    }
    public static Double copyCastToDouble(Object src) {
        return null;
    }
    public static String copyCastToString(Object src) {
        return null;
    }
    public static <T> T copyCast(Object src, T target) {
        return null;
    }

    public static int lengthOf(Object fmCurrCod) {
        return 0;
    }
    public static int compare(Object subvalue, CobolConstant constant) {
        return 0;
    }

    public static int compare(Object subvalue, Object cSaDrwByPsbk) {
        return 0;
    }

    public static <T> T copyObject(Object src, T target, Integer start, Integer length) {
        return null;
    }

    public static String substring(Object wsStringA, int len) {
        return null;
    }

    /**
     * Cast from to castTo, return the result.
     * @param castTo
     * @param from
     * @return
     * @param <T>
     */
    public static <T> T cast(T castTo, Object from) {
        return null;
    }

    /**
     * Copy the same name property value from src to target
     * @param src
     * @param target
     * @param <T>
     */
    public static <T> void copySameField(T src, Object target) {
    }

    /**
     * Init a string by annotation
     * FIXME
     * @param str
     * @return
     */
    public static String copyInitString(boolean isAll, String str) {
        return str;
    }
}
