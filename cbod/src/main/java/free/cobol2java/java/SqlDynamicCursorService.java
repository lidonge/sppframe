package free.cobol2java.java;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.WeakHashMap;
import javax.sql.DataSource;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Source-scoped dynamic SELECT execution. MyBatis owns the statement; rows retain JDBC column order. */
public final class SqlDynamicCursorService implements IService {
    private static final String STATEMENT_PREFIX = "free.cobol2java.dynamic.";
    private final Map<Object, Scope> scopes = Collections.synchronizedMap(new WeakHashMap<>());

    public void prepare(Object owner, String statementName, String sql, int sourceLength) {
        Scope scope = scope(owner);
        String name = requiredName(statementName, "prepared statement");
        if (sql == null || sourceLength < 0 || sourceLength > sql.length())
            throw new IllegalArgumentException("SQL VARCHAR length is outside its declared text");
        String text = sql.substring(0, sourceLength);
        if (text.isBlank()) throw new IllegalArgumentException("Prepared SQL is empty");
        DataSource dataSource = source();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (var prepared = connection.prepareStatement(text)) {
            // JDBC PREPARE reports syntax and driver errors at the source PREPARE position.
        } catch (SQLException failure) {
            throw new IllegalStateException("SQL PREPARE failed", failure);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        synchronized (scope) {
            scope.prepared.put(name, text);
        }
    }

    public void open(Object owner, String cursorName, String statementName, boolean withHold) {
        Scope scope = scope(owner);
        String cursor = requiredName(cursorName, "cursor");
        String prepared = requiredName(statementName, "prepared statement");
        String sql;
        synchronized (scope) {
            if (scope.open.containsKey(cursor))
                throw new SqlRuntimeException(SqlRuntimeException.INVALID_REQUEST, "Cursor already open: " + cursor);
            sql = scope.prepared.get(prepared);
        }
        if (sql == null)
            throw new SqlRuntimeException(SqlRuntimeException.INVALID_REQUEST, "Prepared statement not found: " + prepared);
        DataSource dataSource = source();
        SqlSessionFactory factory = newSessionFactory(dataSource);
        Configuration configuration = factory.getConfiguration();
        String statementId = STATEMENT_PREFIX + "select";
        var sqlSource = new StaticSqlSource(configuration, sql);
        var resultMap = new ResultMap.Builder(configuration, statementId + ".result", List.class, List.of()).build();
        var mapped = new MappedStatement.Builder(configuration, statementId, sqlSource, SqlCommandType.SELECT)
                .resultMaps(List.of(resultMap)).build();
        configuration.addMappedStatement(mapped);
        Connection connection = DataSourceUtils.getConnection(dataSource);
        SqlSession session = null;
        try {
            session = factory.openSession(connection);
            Cursor<List<Object>> rows = session.selectCursor(statementId);
            Open open = new Open(rows, session, connection, dataSource, withHold);
            synchronized (scope) {
                scope.open.put(cursor, open);
            }
            if (withHold && TransactionSynchronizationManager.isActualTransactionActive()
                    && TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) { open.detachForCommit(); }
                    @Override public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED && withHold) return;
                        synchronized (scope) {
                            scope.open.remove(cursor, open);
                        }
                        open.release();
                    }
                });
            }
        } catch (RuntimeException failure) {
            if (session != null) session.close();
            DataSourceUtils.releaseConnection(connection, dataSource);
            throw failure;
        }
    }

    public List<Object> fetch(Object owner, String cursorName) {
        Open open = opened(owner, cursorName);
        return open.next();
    }

    public void close(Object owner, String cursorName) {
        Scope scope = scope(owner);
        String name = requiredName(cursorName, "cursor");
        Open open;
        synchronized (scope) {
            open = scope.open.remove(name);
        }
        if (open == null)
            throw new SqlRuntimeException(SqlRuntimeException.CURSOR_NOT_OPEN, "Cursor not open: " + name);
        open.release();
    }

    /** Read one column by source ordinal; a NULL without an indicator is an explicit error. */
    public static String textColumn(List<Object> row, int ordinal) {
        if (row == null || ordinal < 0 || ordinal >= row.size())
            throw new IllegalArgumentException("SQL result column is absent: " + ordinal);
        Object value = row.get(ordinal);
        if (value == null)
            throw new SqlRuntimeException(-305,
                    "SQL NULL has no receiving indicator at column " + (ordinal + 1));
        return String.valueOf(value);
    }

    private Open opened(Object owner, String cursorName) {
        Scope scope = scope(owner);
        String name = requiredName(cursorName, "cursor");
        synchronized (scope) {
            Open open = scope.open.get(name);
            if (open != null) return open;
        }
        throw new SqlRuntimeException(SqlRuntimeException.CURSOR_NOT_OPEN, "Cursor not open: " + name);
    }

    private Scope scope(Object owner) {
        Objects.requireNonNull(owner, "COBOL program invocation");
        synchronized (scopes) {
            return scopes.computeIfAbsent(owner, ignored -> new Scope());
        }
    }

    private static String requiredName(String value, String kind) {
        if (value == null || value.isBlank())
            throw new SqlRuntimeException(SqlRuntimeException.INVALID_REQUEST, kind + " name is blank");
        return value;
    }

    private DataSource source() {
        DataSource bean = ServiceManager.getBean(DataSource.class);
        if (bean == null) throw new IllegalStateException("Dynamic SQL DataSource is unavailable");
        return bean;
    }

    private static SqlSessionFactory newSessionFactory(DataSource dataSource) {
        var transactions = new ManagedTransactionFactory();
        var options = new Properties();
        options.setProperty("closeConnection", "false");
        transactions.setProperties(options);
        var configuration = new Configuration(new Environment("dynamic-sql", transactions, dataSource));
        configuration.addInterceptor(new OrdinalResultPlugin());
        configuration.setCacheEnabled(false);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static final class Scope {
        final Map<String, String> prepared = new java.util.HashMap<>();
        final Map<String, Open> open = new java.util.HashMap<>();
    }

    private static final class Open {
        private Cursor<List<Object>> cursor;
        private Iterator<List<Object>> iterator;
        private SqlSession session;
        private Connection connection;
        private DataSource dataSource;
        private final boolean withHold;
        private final ArrayDeque<List<Object>> heldRows = new ArrayDeque<>();
        private boolean detached;

        Open(Cursor<List<Object>> cursor, SqlSession session, Connection connection,
                DataSource dataSource, boolean withHold) {
            this.cursor = cursor;
            this.iterator = cursor.iterator();
            this.session = session;
            this.connection = connection;
            this.dataSource = dataSource;
            this.withHold = withHold;
        }

        synchronized List<Object> next() {
            if (detached) return heldRows.pollFirst();
            if (iterator == null) throw new SqlRuntimeException(SqlRuntimeException.CURSOR_NOT_OPEN, "Cursor closed");
            return iterator.hasNext() ? iterator.next() : null;
        }

        synchronized void detachForCommit() {
            if (!withHold || detached) return;
            while (iterator != null && iterator.hasNext()) heldRows.addLast(iterator.next());
            releasePhysical();
            detached = true;
        }

        synchronized void release() {
            releasePhysical();
            heldRows.clear();
            iterator = null;
            detached = true;
        }

        private void releasePhysical() {
            RuntimeException failure = null;
            if (cursor != null) {
                try { cursor.close(); } catch (Exception closeFailure) {
                    failure = new IllegalStateException("Cursor close failed", closeFailure);
                }
                cursor = null;
            }
            if (session != null) {
                try { session.close(); } catch (RuntimeException closeFailure) {
                    if (failure == null) failure = closeFailure;
                    else failure.addSuppressed(closeFailure);
                }
                session = null;
            }
            if (connection != null) {
                try { DataSourceUtils.releaseConnection(connection, dataSource); }
                catch (RuntimeException closeFailure) {
                    if (failure == null) failure = closeFailure;
                    else failure.addSuppressed(closeFailure);
                }
                connection = null;
                dataSource = null;
            }
            if (failure != null) throw failure;
        }
    }

    @Intercepts(@Signature(type = ResultSetHandler.class, method = "handleCursorResultSets", args = Statement.class))
    public static final class OrdinalResultPlugin implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            var mapped = (MappedStatement) SystemMetaObject.forObject(invocation.getTarget())
                    .getValue("mappedStatement");
            if (!mapped.getId().startsWith(STATEMENT_PREFIX)) return invocation.proceed();
            Statement statement = (Statement) invocation.getArgs()[0];
            return new OrdinalCursor(statement.getResultSet(), statement);
        }
    }

    private static final class OrdinalCursor implements Cursor<List<Object>> {
        private final ResultSet result;
        private final Statement statement;
        private boolean open = true;
        private boolean consumed;
        private int index = -1;
        private List<Object> next;
        private boolean ready;

        OrdinalCursor(ResultSet result, Statement statement) throws SQLException {
            this.result = Objects.requireNonNull(result, "Dynamic SELECT result set");
            this.statement = statement;
        }

        @Override public Iterator<List<Object>> iterator() {
            return new Iterator<>() {
                @Override public boolean hasNext() {
                    if (!open || consumed) return false;
                    if (ready) return true;
                    try {
                        if (!result.next()) { consumed = true; return false; }
                        ResultSetMetaData metadata = result.getMetaData();
                        List<Object> values = new ArrayList<>(metadata.getColumnCount());
                        for (int column = 1; column <= metadata.getColumnCount(); column++)
                            values.add(result.getObject(column));
                        next = Collections.unmodifiableList(values);
                        ready = true;
                        return true;
                    } catch (SQLException failure) {
                        throw new IllegalStateException("Dynamic SQL FETCH failed", failure);
                    }
                }
                @Override public List<Object> next() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    List<Object> row = next;
                    next = null;
                    ready = false;
                    index++;
                    return row;
                }
            };
        }

        @Override public boolean isOpen() { return open; }
        @Override public boolean isConsumed() { return consumed; }
        @Override public int getCurrentIndex() { return index; }
        @Override public void close() throws java.io.IOException {
            if (!open) return;
            open = false;
            SQLException failure = null;
            try { result.close(); } catch (SQLException closeFailure) { failure = closeFailure; }
            try { statement.close(); } catch (SQLException closeFailure) {
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
            if (failure != null) throw new java.io.IOException("Dynamic cursor close failed", failure);
        }
    }
}
