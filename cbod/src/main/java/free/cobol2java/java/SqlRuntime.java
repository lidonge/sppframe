package free.cobol2java.java;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SqlRuntime {
    private static final Map<String, CursorState> CURSORS = new ConcurrentHashMap<>();
    private static final Map<String, String> PREPARED_SQL = new ConcurrentHashMap<>();

    private SqlRuntime() {
    }

    public static <T> T mapper(Class<T> mapperType) {
        if (mapperType == null) {
            return null;
        }
        T bean = beanByType(mapperType);
        if (bean != null) {
            return bean;
        }
        throw new IllegalStateException("Unable to resolve MyBatis mapper bean: " + mapperType.getName());
    }

    @SuppressWarnings("unchecked")
    public static <T> T beanByType(Class<T> beanType) {
        if (beanType == null) {
            return null;
        }
        Object fromServiceContainer = invokeServiceContainer(beanType);
        if (beanType.isInstance(fromServiceContainer)) {
            return (T) fromServiceContainer;
        }
        Object fromContextLoader = invokeSpringContext(beanType);
        if (beanType.isInstance(fromContextLoader)) {
            return (T) fromContextLoader;
        }
        return null;
    }

    private static Object invokeServiceContainer(Class<?> beanType) {
        try {
            Method getContainer = ServiceManager.class.getMethod("getServiceContainer");
            Object container = getContainer.invoke(null);
            if (container == null) {
                return null;
            }
            Method byType = container.getClass().getMethod("getService", Class.class);
            return byType.invoke(container, beanType);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object invokeSpringContext(Class<?> beanType) {
        try {
            Class<?> contextLoaderClass = Class.forName("org.springframework.web.context.ContextLoader");
            Method currentContext = contextLoaderClass.getMethod("getCurrentWebApplicationContext");
            Object applicationContext = currentContext.invoke(null);
            if (applicationContext == null) {
                return null;
            }
            Method getBean = applicationContext.getClass().getMethod("getBean", Class.class);
            return getBean.invoke(applicationContext, beanType);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void openCursor(String cursorName, List<?> rows) {
        if (cursorName == null || cursorName.isBlank()) {
            return;
        }
        List<?> safeRows = rows == null ? Collections.emptyList() : rows;
        CURSORS.put(cursorName.trim().toUpperCase(), new CursorState(safeRows.iterator()));
    }

    public static Object fetchCursor(String cursorName) {
        if (cursorName == null || cursorName.isBlank()) {
            return null;
        }
        CursorState cursorState = CURSORS.get(cursorName.trim().toUpperCase());
        if (cursorState == null) {
            throw new SqlRuntimeException(SqlRuntimeException.CURSOR_NOT_OPEN, "Cursor not open: " + cursorName);
        }
        if (!cursorState.iterator.hasNext()) {
            cursorState.currentRow = null;
            return null;
        }
        cursorState.currentRow = cursorState.iterator.next();
        return cursorState.currentRow;
    }

    public static Object getCurrentCursorRow(String cursorName) {
        if (cursorName == null || cursorName.isBlank()) {
            return null;
        }
        CursorState cursorState = CURSORS.get(cursorName.trim().toUpperCase());
        if (cursorState == null) {
            throw new SqlRuntimeException(SqlRuntimeException.CURSOR_NOT_OPEN, "Cursor not open: " + cursorName);
        }
        return cursorState.currentRow;
    }

    public static void closeCursor(String cursorName) {
        if (cursorName == null || cursorName.isBlank()) {
            return;
        }
        CursorState removed = CURSORS.remove(cursorName.trim().toUpperCase());
        if (removed == null) {
            throw new SqlRuntimeException(SqlRuntimeException.CURSOR_NOT_OPEN, "Cursor not open: " + cursorName);
        }
    }

    public static void commit() {
        // Placeholder for transaction integration when repository/runtime wiring is ready.
    }

    public static void rollback() {
        // Placeholder for transaction rollback integration when repository/runtime wiring is ready.
    }

    public static String currentTimestamp() {
        return java.time.LocalDateTime.now().toString();
    }

    public static void prepare(String statementName, Object sqlSource) {
        if (statementName == null || statementName.isBlank()) {
            return;
        }
        PREPARED_SQL.put(statementName.trim().toUpperCase(), sqlSource == null ? "" : sqlSource.toString());
    }

    public static void openPreparedCursor(String cursorName, String statementName) {
        if (statementName == null || statementName.isBlank()) {
            throw new SqlRuntimeException(SqlRuntimeException.INVALID_REQUEST, "Prepared statement name is blank.");
        }
        String sql = PREPARED_SQL.get(statementName.trim().toUpperCase());
        if (sql == null) {
            throw new SqlRuntimeException(SqlRuntimeException.INVALID_REQUEST, "Prepared statement not found: " + statementName);
        }
        openCursor(cursorName, Collections.emptyList());
    }

    public static <T> List<T> filterRowsByEquals(List<T> rows, String propertyName, Object expectedValue) {
        if (rows == null || rows.isEmpty() || propertyName == null || propertyName.isBlank()) {
            return rows == null ? Collections.emptyList() : rows;
        }
        return rows.stream()
                .filter(row -> valuesEqual(readProperty(row, propertyName), expectedValue))
                .collect(Collectors.toList());
    }

    public static <T> List<T> filterRowsByNull(List<T> rows, String propertyName) {
        if (rows == null || rows.isEmpty() || propertyName == null || propertyName.isBlank()) {
            return rows == null ? Collections.emptyList() : rows;
        }
        return rows.stream()
                .filter(row -> readProperty(row, propertyName) == null)
                .collect(Collectors.toList());
    }

    public static <T> List<T> filterRowsByNotNull(List<T> rows, String propertyName) {
        if (rows == null || rows.isEmpty() || propertyName == null || propertyName.isBlank()) {
            return rows == null ? Collections.emptyList() : rows;
        }
        return rows.stream()
                .filter(row -> readProperty(row, propertyName) != null)
                .collect(Collectors.toList());
    }

    public static <T> List<T> filterRowsByComparison(List<T> rows, String propertyName, Object expectedValue, String operator) {
        if (rows == null || rows.isEmpty() || propertyName == null || propertyName.isBlank()
                || operator == null || operator.isBlank()) {
            return rows == null ? Collections.emptyList() : rows;
        }
        Comparable<?> expectedComparable = comparableValue(expectedValue);
        return rows.stream()
                .filter(row -> compareByOperator(readProperty(row, propertyName), expectedComparable, operator))
                .collect(Collectors.toList());
    }

    public static <T> List<T> sortRows(List<T> rows, String propertyName, boolean descending) {
        if (rows == null || rows.size() <= 1 || propertyName == null || propertyName.isBlank()) {
            return rows == null ? Collections.emptyList() : rows;
        }
        List<T> sorted = rows.stream().collect(Collectors.toList());
        Comparator<T> comparator = (left, right) -> compareComparableValues(
                comparableValue(readProperty(left, propertyName)),
                comparableValue(readProperty(right, propertyName))
        );
        if (descending) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);
        return sorted;
    }

    private static Comparable<?> comparableValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Comparable<?> comparable) {
            return comparable;
        }
        return value.toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareComparableValues(Comparable left, Comparable right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean compareByOperator(Object leftValue, Comparable rightValue, String operator) {
        Comparable leftComparable = comparableValue(leftValue);
        int compareResult = compareComparableValues(leftComparable, rightValue);
        return switch (operator.trim()) {
            case ">" -> compareResult > 0;
            case ">=" -> compareResult >= 0;
            case "<" -> compareResult < 0;
            case "<=" -> compareResult <= 0;
            case "<>" -> compareResult != 0;
            default -> false;
        };
    }

    private static Object readProperty(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isBlank()) {
            return null;
        }
        String suffix = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
            Method getter = bean.getClass().getMethod("get" + suffix);
            return getter.invoke(bean);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean valuesEqual(Object left, Object right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.toString().trim().equals(right.toString().trim());
    }

    private static final class CursorState {
        private final Iterator<?> iterator;
        private Object currentRow;

        private CursorState(Iterator<?> iterator) {
            this.iterator = iterator;
        }
    }
}
