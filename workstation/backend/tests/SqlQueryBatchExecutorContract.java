package io.metersphere.workstation.service;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Runs the production executor against strict JDBC boundary doubles, not a live database. */
public class SqlQueryBatchExecutorContract {
    private static int checks;
    private record Rows(int count) { }
    private record Payload(int rows, boolean truncated) { }

    private static final class Plan {
        final List<Object> values;
        SQLException executeFailure, moreFailure, closeFailure, readFailure;
        long advanceNanos;
        int cursor, reads, timeout, maxRows;
        boolean closed;
        Plan(Object... values) { this.values = List.of(values); }
        Object current() { return cursor < values.size() ? values.get(cursor) : null; }
    }

    private static final class Jdbc {
        final List<Plan> plans;
        final List<String> executed = new ArrayList<>();
        final Map<Statement, Plan> statements = new IdentityHashMap<>();
        final AtomicLong clock = new AtomicLong();
        final List<Integer> limits = new ArrayList<>();
        int created;
        final Connection connection;

        Jdbc(Plan... plans) {
            this.plans = List.of(plans);
            connection = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("createStatement")) throw new AssertionError("Unexpected connection call " + method);
                    Plan plan = this.plans.get(created++);
                    Statement statement = (Statement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Statement.class},
                        (self, operation, input) -> {
                            switch (operation.getName()) {
                                case "setQueryTimeout": plan.timeout = (Integer) input[0]; return null;
                                case "setMaxRows": plan.maxRows = (Integer) input[0]; return null;
                                case "execute":
                                    executed.add((String) input[0]);
                                    if (plan.executeFailure != null) throw plan.executeFailure;
                                    return plan.current() instanceof Rows;
                                case "getUpdateCount": return plan.current() instanceof Integer ? plan.current() : -1;
                                case "getMoreResults":
                                    eq(Statement.CLOSE_CURRENT_RESULT, input[0]);
                                    if (plan.moreFailure != null) throw plan.moreFailure;
                                    plan.cursor++;
                                    return plan.current() instanceof Rows;
                                case "close":
                                    plan.closed = true;
                                    if (plan.closeFailure != null) throw plan.closeFailure;
                                    return null;
                                default: throw new AssertionError("Unexpected statement call " + operation);
                            }
                        });
                    statements.put(statement, plan);
                    return statement;
                });
        }
        SqlQueryBatchExecutor.ReadResult<Payload> read(Statement statement, int limit) throws SQLException {
            Plan plan = statements.get(statement);
            if (plan.readFailure != null) throw plan.readFailure;
            limits.add(limit);
            plan.reads++;
            clock.addAndGet(plan.advanceNanos);
            int available = ((Rows) plan.current()).count();
            int count = Math.min(available, limit);
            return new SqlQueryBatchExecutor.ReadResult<>(new Payload(count, available > limit), count);
        }
        SqlQueryBatchExecutor.Batch<Payload> run(List<String> sql, int limit, int timeout) {
            return new SqlQueryBatchExecutor(clock::get).execute(connection, sql, limit, timeout, this::read);
        }
    }
    private static void eq(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + " but was " + actual);
    }
    private static void check(String name, Runnable test) { test.run(); checks++; System.out.println("PASS " + name); }
    private static void rejects(Runnable test) {
        try { test.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected input rejection");
    }

    public static void main(String[] args) {
        check("multiple statements retain every result and session order", () -> {
            Plan set = new Plan(0), a = new Plan(new Rows(2)), b = new Plan(new Rows(3));
            Jdbc jdbc = new Jdbc(set, a, b);
            var batch = jdbc.run(List.of("SET @x=1", "SELECT @x", "SELECT 3"), 1000, 30);
            eq(SqlQueryBatchExecutor.Status.SUCCESS, batch.status()); eq(5, batch.totalRows());
            eq(3, batch.statements().size()); eq(List.of(0), batch.statements().get(0).updateCounts());
            eq(List.of("SET @x=1", "SELECT @x", "SELECT 3"), jdbc.executed);
            eq(2, batch.statements().get(1).results().get(0).rows());
            eq(true, set.closed && a.closed && b.closed); eq(1001, a.maxRows);
        });
        check("SQL failure preserves prior result and skips following statements", () -> {
            Plan bad = new Plan(); bad.executeFailure = new SQLException("bad syntax", "42000", 1064);
            Jdbc jdbc = new Jdbc(new Plan(new Rows(2)), bad);
            var batch = jdbc.run(List.of("SELECT 1", "bad", "SELECT 3"), 10, 30);
            eq(SqlQueryBatchExecutor.Status.ERROR, batch.status()); eq(2, batch.totalRows());
            eq(SqlQueryBatchExecutor.Status.SKIPPED, batch.statements().get(2).status());
            eq("42000", batch.statements().get(1).sqlState()); eq(1064, batch.statements().get(1).errorCode());
            eq(2, jdbc.created); eq(true, bad.closed);
        });
        check("failure advancing result sets retains already read results", () -> {
            Plan plan = new Plan(new Rows(1)); plan.moreFailure = new SQLException("read failed");
            Jdbc jdbc = new Jdbc(plan);
            var batch = jdbc.run(List.of("CALL p()", "SELECT 2"), 10, 30);
            eq(SqlQueryBatchExecutor.Status.ERROR, batch.status()); eq(1, batch.totalRows());
            eq(1, batch.statements().get(0).results().size()); eq(1, jdbc.executed.size());
        });
        check("the aggregate row budget shrinks subsequent result limits", () -> {
            Jdbc jdbc = new Jdbc(new Plan(new Rows(4999)), new Plan(new Rows(100)));
            var batch = jdbc.run(List.of("SELECT a", "SELECT b", "SELECT c"), 5000, 30);
            eq(5000, batch.totalRows()); eq(List.of(5000, 1), jdbc.limits);
            eq(true, batch.statements().get(1).results().get(0).truncated());
            eq(SqlQueryBatchExecutor.Status.LIMIT, batch.status()); eq(2, jdbc.executed.size());
            eq(SqlQueryBatchExecutor.Status.SKIPPED, batch.statements().get(2).status());
        });
        check("empty result sets are not mistaken for absent results", () -> {
            Jdbc jdbc = new Jdbc(new Plan(new Rows(0)));
            var batch = jdbc.run(List.of("SELECT empty"), 10, 30);
            eq(0, batch.totalRows()); eq(1, batch.statements().get(0).results().size());
            eq(SqlQueryBatchExecutor.Status.SUCCESS, batch.status());
        });
        check("one statement may return several result sets and update counts", () -> {
            Jdbc jdbc = new Jdbc(new Plan(new Rows(1), 0, new Rows(2)));
            var batch = jdbc.run(List.of("CALL p()"), 10, 30);
            eq(2, batch.statements().get(0).results().size()); eq(List.of(0), batch.statements().get(0).updateCounts());
            eq(3, batch.totalRows());
        });
        check("JDBC results are bounded even when they contain zero rows", () -> {
            Object[] many = new Object[21]; java.util.Arrays.fill(many, new Rows(0));
            Jdbc jdbc = new Jdbc(new Plan(many));
            var batch = jdbc.run(List.of("CALL many()", "SELECT later"), 10, 30);
            eq(SqlQueryBatchExecutor.Status.LIMIT, batch.status());
            eq(20, batch.statements().get(0).results().size()); eq(20, jdbc.plans.get(0).reads); eq(1, jdbc.executed.size());
        });
        check("exactly the result cap without more work is successful", () -> {
            Object[] many = new Object[20]; java.util.Arrays.fill(many, 0);
            Jdbc jdbc = new Jdbc(new Plan(many));
            var batch = jdbc.run(List.of("CALL updates()"), 10, 30);
            eq(SqlQueryBatchExecutor.Status.SUCCESS, batch.status()); eq(20, batch.statements().get(0).updateCounts().size());
        });
        check("whole-batch time budget reduces subsequent JDBC timeouts", () -> {
            Plan first = new Plan(new Rows(1)); first.advanceNanos = 10_000_000_000L;
            Plan second = new Plan(new Rows(1)); Jdbc jdbc = new Jdbc(first, second);
            var batch = jdbc.run(List.of("SELECT slow", "SELECT fast"), 10, 30);
            eq(30, first.timeout); eq(20, second.timeout); eq(10000L, batch.executionTime());
        });
        check("deadline expiry stops further work without losing read results", () -> {
            Plan first = new Plan(new Rows(1)); first.advanceNanos = 2_000_000_000L;
            Jdbc jdbc = new Jdbc(first);
            var batch = jdbc.run(List.of("SELECT slow", "SELECT later"), 10, 1);
            eq(SqlQueryBatchExecutor.Status.ERROR, batch.status()); eq("HYT00", batch.statements().get(0).sqlState());
            eq(1, batch.totalRows()); eq(1, jdbc.executed.size()); eq(true, first.closed);
        });
        check("reader and close failures retain earlier statement output", () -> {
            Plan first = new Plan(new Rows(1)); Plan second = new Plan(new Rows(1));
            second.readFailure = new SQLException("conversion failed");
            Jdbc jdbc = new Jdbc(first, second);
            var batch = jdbc.run(List.of("SELECT a", "SELECT b", "SELECT c"), 10, 30);
            eq(1, batch.totalRows()); eq(SqlQueryBatchExecutor.Status.ERROR, batch.status()); eq(true, second.closed);
            Plan closing = new Plan(new Rows(1)); closing.closeFailure = new SQLException("close failed");
            var closed = new Jdbc(closing).run(List.of("SELECT d"), 10, 30);
            eq(1, closed.totalRows()); eq(SqlQueryBatchExecutor.Status.ERROR, closed.status());
        });
        check("invalid scripts and parameters are rejected before execution", () -> {
            rejects(() -> SqlQueryBatchExecutor.validateStatements(List.of()));
            rejects(() -> SqlQueryBatchExecutor.validateStatements(java.util.Collections.nCopies(21, "SELECT 1")));
            rejects(() -> SqlQueryBatchExecutor.validateStatements(List.of("x".repeat(100001))));
            Jdbc jdbc = new Jdbc(); rejects(() -> jdbc.run(List.of("SELECT 1"), 0, 30)); eq(0, jdbc.created);
        });
        check("a broken result reader cannot exceed the retained-row contract", () -> {
            Jdbc jdbc = new Jdbc(new Plan(new Rows(1)));
            var batch = new SqlQueryBatchExecutor().execute(jdbc.connection, List.of("SELECT a"), 1, 30,
                (statement, limit) -> new SqlQueryBatchExecutor.ReadResult<>("wrong", 2));
            eq(SqlQueryBatchExecutor.Status.ERROR, batch.status()); eq(0, batch.totalRows());
        });
        check("returned execution collections are immutable", () -> {
            var batch = new Jdbc(new Plan(new Rows(1))).run(List.of("SELECT a"), 10, 30);
            try { batch.statements().clear(); throw new AssertionError("mutable batch"); }
            catch (UnsupportedOperationException expected) { }
            try { batch.statements().get(0).results().clear(); throw new AssertionError("mutable results"); }
            catch (UnsupportedOperationException expected) { }
        });
        System.out.println("Passed " + checks + " JDBC executor contract tests");
    }
}
