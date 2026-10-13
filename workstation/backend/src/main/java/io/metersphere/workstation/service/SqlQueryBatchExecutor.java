package io.metersphere.workstation.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Bounded, sequential JDBC execution. The caller owns the authorised read-only connection.
 * A single connection preserves USE/session variables; an error never replays a statement.
 * This class does not parse SQL or establish database permissions.
 */
public final class SqlQueryBatchExecutor {
    public static final int MAX_STATEMENTS = 20;
    public static final int MAX_SQL_LENGTH = 100_000;
    public static final int MAX_TOTAL_ROWS = 5_000;
    public static final int MAX_RESULTS = 20;

    public enum Status { SUCCESS, ERROR, LIMIT, SKIPPED }

    public record ReadResult<T>(T value, int rowCount) { }

    @FunctionalInterface
    public interface ResultReader<T> {
        ReadResult<T> read(Statement statement, int limit) throws SQLException;
    }

    public record Execution<T>(int index, String sql, Status status, long executionTime,
                               List<T> results, List<Integer> updateCounts,
                               String message, String sqlState, Integer errorCode) {
        public Execution {
            results = List.copyOf(results);
            updateCounts = List.copyOf(updateCounts);
        }
    }

    public record Batch<T>(int protocolVersion, Status status, List<Execution<T>> statements,
                           long executionTime, int totalRows, int maxTotalRows, int maxResults) {
        public Batch { statements = List.copyOf(statements); }
    }

    private final LongSupplier nanoTime;

    public SqlQueryBatchExecutor() { this(System::nanoTime); }

    // A monotonic clock is injectable for deterministic deadline tests, without sleeps.
    SqlQueryBatchExecutor(LongSupplier nanoTime) { this.nanoTime = Objects.requireNonNull(nanoTime); }

    public static void validateStatements(List<String> statements) {
        if (statements == null || statements.isEmpty()) throw new IllegalArgumentException("SQL 不能为空");
        if (statements.size() > MAX_STATEMENTS) {
            throw new IllegalArgumentException("单次最多执行 " + MAX_STATEMENTS + " 条 SQL，请拆分脚本");
        }
        long characters = 0;
        for (String sql : statements) {
            if (sql == null || sql.isBlank()) throw new IllegalArgumentException("SQL 不能为空");
            characters += sql.length();
        }
        if (characters > MAX_SQL_LENGTH) throw new IllegalArgumentException("SQL 脚本长度超过上限");
    }

    public <T> Batch<T> execute(Connection connection, List<String> statements, int limit,
                                int timeoutSeconds, ResultReader<T> reader) {
        validateStatements(statements);
        Objects.requireNonNull(connection);
        Objects.requireNonNull(reader);
        if (limit < 1 || limit > MAX_TOTAL_ROWS || timeoutSeconds < 1 || timeoutSeconds > 300) {
            throw new IllegalArgumentException("无效的查询行数或超时时间");
        }
        long started = nanoTime.getAsLong();
        long timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
        List<Execution<T>> executions = new ArrayList<>();
        int totalRows = 0;
        int totalResults = 0;
        Status batchStatus = Status.SUCCESS;
        String stopMessage = null;

        for (int i = 0; i < statements.size(); i++) {
            String sql = statements.get(i);
            if (batchStatus == Status.SUCCESS && (totalRows >= MAX_TOTAL_ROWS || totalResults >= MAX_RESULTS)) {
                batchStatus = Status.LIMIT;
                stopMessage = limitMessage();
            }
            if (batchStatus != Status.SUCCESS) {
                executions.add(new Execution<>(i + 1, sql, Status.SKIPPED, 0, List.of(), List.of(),
                    stopMessage, null, null));
                continue;
            }

            long statementStarted = nanoTime.getAsLong();
            Status status = Status.SUCCESS;
            String message = null;
            String sqlState = null;
            Integer errorCode = null;
            List<T> results = new ArrayList<>();
            List<Integer> updateCounts = new ArrayList<>();
            try {
                int remainingSeconds = remainingSeconds(started, timeoutNanos);
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(remainingSeconds);
                    statement.setMaxRows(Math.min(limit, MAX_TOTAL_ROWS - totalRows) + 1);
                    boolean hasResultSet = statement.execute(sql);
                    remainingSeconds(started, timeoutNanos);
                    while (hasResultSet || statement.getUpdateCount() != -1) {
                        if (totalResults >= MAX_RESULTS || (hasResultSet && totalRows >= MAX_TOTAL_ROWS)) {
                            status = Status.LIMIT;
                            message = limitMessage();
                            break;
                        }
                        remainingSeconds(started, timeoutNanos);
                        if (hasResultSet) {
                            int allowed = Math.min(limit, MAX_TOTAL_ROWS - totalRows);
                            ReadResult<T> read = reader.read(statement, allowed);
                            if (read == null || read.value() == null || read.rowCount() < 0 || read.rowCount() > allowed) {
                                throw new SQLException("查询结果读取器返回了无效的行数", "HY000");
                            }
                            results.add(read.value());
                            totalRows += read.rowCount();
                        } else {
                            updateCounts.add(statement.getUpdateCount());
                        }
                        totalResults++;
                        remainingSeconds(started, timeoutNanos);
                        hasResultSet = statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
                    }
                    remainingSeconds(started, timeoutNanos);
                }
            } catch (SQLException error) {
                status = Status.ERROR;
                message = error.getMessage() == null ? "SQL 执行失败" : error.getMessage();
                sqlState = error.getSQLState();
                errorCode = error.getErrorCode();
            }
            executions.add(new Execution<>(i + 1, sql, status, elapsedMillis(statementStarted),
                results, updateCounts, message, sqlState, errorCode));
            if (status != Status.SUCCESS) {
                batchStatus = status;
                stopMessage = message;
            }
        }
        return new Batch<>(1, batchStatus, executions, elapsedMillis(started), totalRows, MAX_TOTAL_ROWS, MAX_RESULTS);
    }

    private int remainingSeconds(long started, long timeoutNanos) throws SQLTimeoutException {
        long remaining = timeoutNanos - (nanoTime.getAsLong() - started);
        if (remaining <= 0) throw new SQLTimeoutException("批量查询总执行时间超过上限，后续语句未执行", "HYT00");
        // JDBC timeouts use whole seconds. Checks between operations also enforce the batch deadline.
        return (int) ((remaining + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
    }

    private long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0, nanoTime.getAsLong() - started));
    }

    private static String limitMessage() {
        return "已达到整批查询返回上限（" + MAX_TOTAL_ROWS + " 行或 " + MAX_RESULTS
            + " 个 JDBC 结果），已保留的数据可查看；未读取的结果不再读取，后续语句未执行";
    }
}
