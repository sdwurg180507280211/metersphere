package io.metersphere.workstation.service;

import io.metersphere.workstation.dto.SqlConnectionStatus;
import io.metersphere.workstation.dto.SqlQueryColumn;
import io.metersphere.workstation.dto.SqlQueryResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SqlQueryService {

    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 5000;
    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 30;
    private static final int MAX_QUERY_TIMEOUT_SECONDS = 300;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ZoneId DISPLAY_ZONE = ZoneId.systemDefault();
    private static final long MIN_EPOCH_SECONDS = 946684800L;
    private static final long MAX_EPOCH_SECONDS = 4102444800L;
    private static final long MIN_EPOCH_MILLIS = MIN_EPOCH_SECONDS * 1000;
    private static final long MAX_EPOCH_MILLIS = MAX_EPOCH_SECONDS * 1000;
    private static final long MIN_EPOCH_MICROS = MIN_EPOCH_MILLIS * 1000;
    private static final long MAX_EPOCH_MICROS = MAX_EPOCH_MILLIS * 1000;

    @Value("${spring.datasource.url:}")
    private String defaultDatasourceUrl;

    @Value("${metersphere.sql-query.datasource.url:}")
    private String sqlQueryDatasourceUrl;

    @Value("${metersphere.sql-query.datasource.username:}")
    private String sqlQueryDatasourceUsername;

    @Value("${metersphere.sql-query.datasource.password:}")
    private String sqlQueryDatasourcePassword;

    public SqlQueryResult query(String sql, Integer limit, Integer timeoutSeconds) throws SQLException {
        List<String> statements = normalizeExecutableStatements(sql);
        int safeLimit = normalizeLimit(limit);
        int safeTimeoutSeconds = normalizeQueryTimeoutSeconds(timeoutSeconds);
        long start = System.currentTimeMillis();

        SqlQueryResult result = null;
        try (Connection connection = getQueryConnection()) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(safeTimeoutSeconds);
                statement.setMaxRows(safeLimit + 1);

                for (String statementSql : statements) {
                    boolean hasResultSet = statement.execute(statementSql);
                    while (true) {
                        if (hasResultSet) {
                            result = readCurrentResultSet(statement, safeLimit);
                        } else if (statement.getUpdateCount() == -1) {
                            break;
                        }
                        hasResultSet = statement.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
                    }
                }
            }
        }

        if (result == null) {
            result = emptyResult(safeLimit);
        }
        result.setExecutionTime(System.currentTimeMillis() - start);
        return result;
    }

    public SqlConnectionStatus status() {
        SqlConnectionStatus status = new SqlConnectionStatus();
        try (Connection connection = getQueryConnection()) {
            connection.setReadOnly(true);
            status.setConnected(true);
            status.setHost(resolveHost(connection.getMetaData().getURL()));
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
                if (resultSet.next()) {
                    status.setDatabase(resultSet.getString(1));
                }
            }
            status.setMessage("connected");
        } catch (Exception e) {
            status.setConnected(false);
            status.setMessage(e.getMessage());
        }
        return status;
    }

    public String normalizeExecutableSql(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        normalizeExecutableStatements(sql);
        return sql.trim();
    }

    private List<String> normalizeExecutableStatements(String sql) {
        List<String> statements = splitStatements(sql);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        return statements;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int normalizeQueryTimeoutSeconds(Integer timeoutSeconds) {
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return DEFAULT_QUERY_TIMEOUT_SECONDS;
        }
        return Math.min(timeoutSeconds, MAX_QUERY_TIMEOUT_SECONDS);
    }

    private Connection getQueryConnection() throws SQLException {
        String jdbcUrl = resolveQueryDatasourceUrl();
        if (StringUtils.isBlank(jdbcUrl)) {
            throw new IllegalStateException("SQL 查询台数据库 URL 未配置，请配置 spring.datasource.url 或 metersphere.sql-query.datasource.url");
        }
        if (StringUtils.isBlank(sqlQueryDatasourceUsername) || StringUtils.isBlank(sqlQueryDatasourcePassword)) {
            throw new IllegalStateException("SQL 查询台只读数据库账号未配置，请配置 metersphere.sql-query.datasource.username/password");
        }
        return DriverManager.getConnection(jdbcUrl, sqlQueryDatasourceUsername, sqlQueryDatasourcePassword);
    }

    private String resolveQueryDatasourceUrl() {
        return StringUtils.defaultIfBlank(sqlQueryDatasourceUrl, defaultDatasourceUrl);
    }

    private String resolveHost(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "";
        }
        String sanitized = jdbcUrl.replaceFirst("^jdbc:mysql://", "");
        int slashIndex = sanitized.indexOf('/');
        if (slashIndex >= 0) {
            return sanitized.substring(0, slashIndex);
        }
        int questionIndex = sanitized.indexOf('?');
        return questionIndex >= 0 ? sanitized.substring(0, questionIndex) : sanitized;
    }

    private List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        if (StringUtils.isBlank(sql)) {
            return statements;
        }
        StringBuilder statement = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : 0;

            if (lineComment) {
                statement.append(current);
                if (current == '\n' || current == '\r') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                statement.append(current);
                if (current == '*' && next == '/') {
                    statement.append(next);
                    i++;
                    blockComment = false;
                }
                continue;
            }
            if (quote == 0) {
                if (isDashCommentStart(sql, i)) {
                    statement.append(current).append(next);
                    i++;
                    lineComment = true;
                    continue;
                }
                if (current == '#') {
                    statement.append(current);
                    lineComment = true;
                    continue;
                }
                if (current == '/' && next == '*') {
                    statement.append(current).append(next);
                    i++;
                    blockComment = true;
                    continue;
                }
                if (current == '\'' || current == '"' || current == '`') {
                    quote = current;
                    statement.append(current);
                    continue;
                }
                if (current == ';') {
                    addExecutableStatement(statements, statement);
                    statement.setLength(0);
                    continue;
                }
                statement.append(current);
                continue;
            }

            statement.append(current);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && quote != '`') {
                escaped = true;
            } else if (current == quote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                    statement.append(sql.charAt(i + 1));
                    i++;
                } else {
                    quote = 0;
                }
            }
        }
        addExecutableStatement(statements, statement);
        return statements;
    }

    private void addExecutableStatement(List<String> statements, StringBuilder statement) {
        String normalized = statement.toString().trim();
        if (StringUtils.isNotBlank(trimLeadingWhitespaceAndComments(normalized))) {
            statements.add(normalized);
        }
    }

    private String trimLeadingWhitespaceAndComments(String sql) {
        int index = 0;
        while (index < sql.length()) {
            while (index < sql.length() && Character.isWhitespace(sql.charAt(index))) {
                index++;
            }
            if (isDashCommentStart(sql, index)) {
                index = skipLineComment(sql, index + 2);
                continue;
            }
            if (index < sql.length() && sql.charAt(index) == '#') {
                index = skipLineComment(sql, index + 1);
                continue;
            }
            if (index + 1 < sql.length() && sql.charAt(index) == '/' && sql.charAt(index + 1) == '*') {
                int end = sql.indexOf("*/", index + 2);
                index = end >= 0 ? end + 2 : sql.length();
                continue;
            }
            break;
        }
        return sql.substring(index).trim();
    }

    private boolean isDashCommentStart(String sql, int index) {
        if (index + 1 >= sql.length() || sql.charAt(index) != '-' || sql.charAt(index + 1) != '-') {
            return false;
        }
        return index + 2 >= sql.length() || Character.isWhitespace(sql.charAt(index + 2));
    }

    private int skipLineComment(String sql, int index) {
        while (index < sql.length() && sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
            index++;
        }
        return index;
    }

    private SqlQueryResult readCurrentResultSet(Statement statement, int safeLimit) throws SQLException {
        try (ResultSet resultSet = statement.getResultSet()) {
            if (resultSet == null) {
                return emptyResult(safeLimit);
            }
            List<ColumnDescriptor> columns = resolveColumns(resultSet.getMetaData());
            List<Map<String, Object>> rows = readRows(resultSet, columns, safeLimit);

            SqlQueryResult result = new SqlQueryResult();
            result.setColumns(toDisplayColumns(columns));
            result.setRows(rows.size() > safeLimit ? new ArrayList<>(rows.subList(0, safeLimit)) : rows);
            result.setRowCount(Math.min(rows.size(), safeLimit));
            result.setTruncated(rows.size() > safeLimit);
            result.setLimit(safeLimit);
            return result;
        }
    }

    private SqlQueryResult emptyResult(int safeLimit) {
        SqlQueryResult result = new SqlQueryResult();
        result.setColumns(new ArrayList<>());
        result.setRows(new ArrayList<>());
        result.setRowCount(0);
        result.setTruncated(false);
        result.setLimit(safeLimit);
        return result;
    }

    private List<ColumnDescriptor> resolveColumns(ResultSetMetaData metaData) throws SQLException {
        List<ColumnDescriptor> columns = new ArrayList<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String label = StringUtils.defaultString(metaData.getColumnLabel(i));
            String sourceName = metaData.getColumnName(i);
            columns.add(new ColumnDescriptor("col_" + i, label, sourceName, metaData.getColumnType(i)));
        }
        return columns;
    }

    private List<SqlQueryColumn> toDisplayColumns(List<ColumnDescriptor> columns) {
        List<SqlQueryColumn> displayColumns = new ArrayList<>();
        for (ColumnDescriptor column : columns) {
            displayColumns.add(new SqlQueryColumn(column.getKey(), column.getSourceLabel()));
        }
        return displayColumns;
    }

    private List<Map<String, Object>> readRows(ResultSet resultSet, List<ColumnDescriptor> columns, int limit) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        int columnCount = columns.size();
        while (resultSet.next() && rows.size() <= limit) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                ColumnDescriptor column = columns.get(i - 1);
                row.put(column.getKey(), convertValue(resultSet.getObject(i), column));
            }
            rows.add(row);
        }
        return rows;
    }

    private Object convertValue(Object value, ColumnDescriptor column) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().format(DATE_TIME_FORMATTER);
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().format(DATE_FORMATTER);
        }
        if (value instanceof Time) {
            return ((Time) value).toLocalTime().format(TIME_FORMATTER);
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DATE_TIME_FORMATTER);
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DATE_FORMATTER);
        }
        if (value instanceof LocalTime) {
            return ((LocalTime) value).format(TIME_FORMATTER);
        }
        if (value instanceof java.util.Date) {
            return DATE_TIME_FORMATTER.format(((java.util.Date) value).toInstant().atZone(DISPLAY_ZONE));
        }
        if (value instanceof Number && isLikelyTimestampColumn(column)) {
            String formattedEpoch = formatEpochValue((Number) value);
            if (formattedEpoch != null) {
                return formattedEpoch;
            }
        }
        if (value instanceof byte[]) {
            return "[BINARY " + ((byte[]) value).length + " bytes]";
        }
        if (value instanceof Blob) {
            return "[BLOB " + ((Blob) value).length() + " bytes]";
        }
        if (value instanceof Clob) {
            Clob clob = (Clob) value;
            long length = clob.length();
            int readLength = (int) Math.min(length, 2000);
            return clob.getSubString(1, readLength) + (length > readLength ? "..." : "");
        }
        return value;
    }

    private boolean isLikelyTimestampColumn(ColumnDescriptor column) {
        String normalizedLabel = normalizeColumnName(column.getSourceLabel());
        String normalizedSourceName = normalizeColumnName(column.getSourceName());
        return containsTimeHint(normalizedLabel)
            || containsTimeHint(normalizedSourceName)
            || containsChineseTimeHint(column.getSourceLabel())
            || containsChineseTimeHint(column.getSourceName());
    }

    private boolean containsTimeHint(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.contains("duration") || name.contains("elapsed") || name.contains("cost")
            || name.contains("timeout") || name.contains("response_time") || name.contains("execution_time")) {
            return false;
        }
        return name.endsWith("_time") || name.endsWith("_date") || name.endsWith("_at")
            || name.contains("_time_") || name.contains("_date_");
    }

    private boolean containsChineseTimeHint(String name) {
        return name != null && (name.contains("时间") || name.contains("日期"));
    }

    private String formatEpochValue(Number value) {
        long epoch = value.longValue();
        if (epoch >= MIN_EPOCH_MILLIS && epoch <= MAX_EPOCH_MILLIS) {
            return formatEpochMillis(epoch);
        }
        if (epoch >= MIN_EPOCH_SECONDS && epoch <= MAX_EPOCH_SECONDS) {
            return formatEpochMillis(epoch * 1000);
        }
        if (epoch >= MIN_EPOCH_MICROS && epoch <= MAX_EPOCH_MICROS) {
            return formatEpochMillis(epoch / 1000);
        }
        return null;
    }

    private String formatEpochMillis(long epochMillis) {
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(DISPLAY_ZONE));
    }

    private String normalizeColumnName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .replace('-', '_')
            .replaceAll("[^A-Za-z0-9]+", "_")
            .replaceAll("^_+|_+$", "")
            .toLowerCase(Locale.ROOT);
    }

    private static class ColumnDescriptor {
        private final String key;
        private final String sourceLabel;
        private final String sourceName;
        private final int jdbcType;

        ColumnDescriptor(String key, String sourceLabel, String sourceName, int jdbcType) {
            this.key = key;
            this.sourceLabel = sourceLabel;
            this.sourceName = sourceName;
            this.jdbcType = jdbcType;
        }

        String getKey() {
            return key;
        }

        String getSourceLabel() {
            return sourceLabel;
        }

        String getSourceName() {
            return sourceName;
        }

        int getJdbcType() {
            return jdbcType;
        }
    }
}
