package free.cobol2java.java;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
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
        Object constructed = constructWithBeanConstructor(beanType);
        if (beanType.isInstance(constructed)) {
            return (T) constructed;
        }
        return null;
    }

    private static Object constructWithBeanConstructor(Class<?> beanType) {
        try {
            for (Constructor<?> constructor : beanType.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1) {
                    Object dependency = beanByType(parameterTypes[0]);
                    if (dependency != null) {
                        return constructor.newInstance(dependency);
                    }
                }
            }
        } catch (Exception ignored) {
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

    public static Object selectOne(String repositoryClassName, String rowClassName, String columnName,
            String keyColumnName, Object keyValue) {
        Object[] row = selectRow(repositoryClassName, rowClassName, new String[] {columnName}, keyColumnName, keyValue);
        return row.length == 0 ? null : row[0];
    }

    public static Object[] selectRow(String repositoryClassName, String rowClassName, String[] columnNames,
            String keyColumnName, Object keyValue) {
        Object row = selectRepositoryRow(repositoryClassName, rowClassName, keyColumnName, keyValue);
        if (row == null) {
            return new Object[0];
        }
        Object[] values = new Object[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            values[i] = readSqlArtifactProperty(row, columnNames[i]);
        }
        return values;
    }

    public static int executeSql(String operation, String repositoryClassName, Object row) {
        Object repository = sqlArtifactRepository(repositoryClassName);
        String methodName = switch (operation.toUpperCase()) {
            case "INSERT" -> "insert";
            case "UPDATE" -> "update";
            case "DELETE" -> "deleteByKey";
            default -> throw new IllegalArgumentException("Unsupported SQL repository operation: " + operation);
        };
        return ((Number) invokeSqlArtifactMethod(repository, methodName, row)).intValue();
    }

    public static Object selectRepositoryRow(String repositoryClassName, String rowClassName,
            String keyColumnName, Object keyValue) {
        Object repository = sqlArtifactRepository(repositoryClassName);
        if (keyColumnName == null || keyColumnName.isBlank()) {
            Object rows = invokeSqlArtifactMethod(repository, "selectAll");
            if (rows instanceof List<?> list && !list.isEmpty()) {
                return list.get(0);
            }
            return null;
        }
        Object keyRow = newSqlArtifactRow(rowClassName);
        writeSqlArtifactProperty(keyRow, keyColumnName, keyValue);
        return unwrapOptional(invokeSqlArtifactMethod(repository, "selectByKey", keyRow));
    }

    public static void commit() {
        // Placeholder for transaction integration when repository/runtime wiring is ready.
    }

    public static void rollback() {
        // Placeholder for transaction rollback integration when repository/runtime wiring is ready.
    }

    public static String currentTimestamp() {
        return java.time.LocalDateTime.now()
                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
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

    private static Object sqlArtifactRepository(String repositoryClassName) {
        try {
            Class<?> repositoryType = Class.forName(repositoryClassName);
            Object repository = beanByType(repositoryType);
            if (repository == null) {
                throw new IllegalStateException("Unable to resolve MyBatis repository bean: " + repositoryClassName);
            }
            return repository;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load SQL artifact repository: " + repositoryClassName, e);
        }
    }

    private static Object newSqlArtifactRow(String rowClassName) {
        try {
            return Class.forName(rowClassName).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create SQL artifact row: " + rowClassName, e);
        }
    }

    private static Object readSqlArtifactProperty(Object row, String columnName) {
        return invokeSqlArtifactMethod(row, "get" + toJavaTypeName(columnName));
    }

    private static void writeSqlArtifactProperty(Object row, String columnName, Object value) {
        String methodName = "set" + toJavaTypeName(columnName);
        for (Method method : row.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                try {
                    method.invoke(row, castSqlArtifactValue(value, method.getParameterTypes()[0]));
                    return;
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to set SQL artifact row property: " + columnName, e);
                }
            }
        }
        throw new IllegalStateException("SQL artifact row setter not found: " + methodName);
    }

    private static Object invokeSqlArtifactMethod(Object target, String methodName, Object... args) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            try {
                return method.invoke(target, args);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to invoke SQL artifact method: " + methodName, e);
            }
        }
        throw new IllegalStateException("SQL artifact method not found: " + methodName);
    }

    private static Object unwrapOptional(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

    private static Object castSqlArtifactValue(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString());
        }
        if (targetType == Long.class || targetType == long.class) {
            return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
        }
        if (targetType == java.math.BigDecimal.class) {
            if (value instanceof java.math.BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return java.math.BigDecimal.valueOf(number.doubleValue());
            }
            return new java.math.BigDecimal(value.toString());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value instanceof Boolean bool ? bool : Boolean.valueOf(value.toString());
        }
        return value;
    }

    private static String toJavaTypeName(String name) {
        StringBuilder out = new StringBuilder();
        for (String part : name.toLowerCase().split("[^a-z0-9]+")) {
            if (!part.isBlank()) {
                out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return out.isEmpty() ? "Value" : out.toString();
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
            for (Method method : bean.getClass().getMethods()) {
                if (method.getParameterCount() == 0
                        && method.getName().startsWith("get")
                        && method.getName().substring(3).equalsIgnoreCase(propertyName)) {
                    try {
                        return method.invoke(bean);
                    } catch (Exception ignoredAgain) {
                        return null;
                    }
                }
            }
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
