package io.metersphere.workstation.service;

import io.metersphere.workstation.dto.SqlConnectionStatus;
import io.metersphere.workstation.dto.SqlQueryResult;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SqlQueryService {

    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 5000;
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final Pattern SELECT_PREFIX = Pattern.compile("^\\s*select\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WITH_PREFIX = Pattern.compile("^\\s*with\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(--|#|/\\*)");
    private static final Pattern WRITE_KEYWORDS = Pattern.compile("\\b(insert|update|delete|drop|alter|truncate|create|replace|merge|call|execute|grant|revoke|set|use|load|lock|unlock|rename|analyze|optimize|repair|handler|install|uninstall)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORBIDDEN_SELECT_PATTERNS = Pattern.compile("\\binto\\s+(outfile|dumpfile)\\b|\\bfor\\s+update\\b|\\block\\s+in\\s+share\\s+mode\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\blimit\\s+\\d+\\b", Pattern.CASE_INSENSITIVE);

    @Resource
    private DataSource dataSource;

    public SqlQueryResult query(String sql, Integer limit) throws SQLException {
        String safeSql = validateSelectSql(sql);
        int safeLimit = normalizeLimit(limit);
        String executableSql = appendLimitIfNecessary(safeSql, safeLimit);
        long start = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                statement.setMaxRows(safeLimit + 1);

                try (ResultSet resultSet = statement.executeQuery(executableSql)) {
                    List<String> columns = resolveColumns(resultSet.getMetaData());
                    List<Map<String, Object>> rows = readRows(resultSet, columns, safeLimit);

                    SqlQueryResult result = new SqlQueryResult();
                    result.setColumns(columns);
                    result.setRows(rows.size() > safeLimit ? rows.subList(0, safeLimit) : rows);
                    result.setRowCount(Math.min(rows.size(), safeLimit));
                    result.setExecutionTime(System.currentTimeMillis() - start);
                    result.setTruncated(rows.size() > safeLimit);
                    result.setLimit(safeLimit);
                    return result;
                }
            }
        }
    }

    public SqlConnectionStatus status() {
        SqlConnectionStatus status = new SqlConnectionStatus();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            status.setConnected(true);
            status.setHost(resolveHost(connection.getMetaData().getURL()));
            if (resultSet.next()) {
                status.setDatabase(resultSet.getString(1));
            }
            status.setMessage("connected");
        } catch (Exception e) {
            status.setConnected(false);
            status.setMessage(e.getMessage());
        }
        return status;
    }

    public String validateSelectSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }

        String normalized = stripTrailingSemicolon(sql);
        String maskedSql = maskLiterals(normalized);
        if (COMMENT_PATTERN.matcher(maskedSql).find()) {
            throw new IllegalArgumentException("SQL 查询台不允许包含注释");
        }
        if (hasMultipleStatements(normalized)) {
            throw new IllegalArgumentException("只允许执行单条 SELECT 语句");
        }
        if (!isSelectQuery(normalized, maskedSql)) {
            throw new IllegalArgumentException("只允许执行 SELECT 或 WITH ... SELECT 语句");
        }

        if (WRITE_KEYWORDS.matcher(maskedSql).find() || FORBIDDEN_SELECT_PATTERNS.matcher(maskedSql).find()) {
            throw new IllegalArgumentException("SQL 包含非只读或高风险语句");
        }
        return normalized;
    }

    private boolean isSelectQuery(String sql, String maskedSql) {
        if (SELECT_PREFIX.matcher(sql).find()) {
            return true;
        }
        if (!WITH_PREFIX.matcher(sql).find()) {
            return false;
        }
        return hasTopLevelSelectAfterWith(maskedSql);
    }

    private boolean hasTopLevelSelectAfterWith(String maskedSql) {
        int depth = 0;
        for (int i = 0; i < maskedSql.length(); i++) {
            char current = maskedSql.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')' && depth > 0) {
                depth--;
            } else if (depth == 0 && startsWithWord(maskedSql, i, "select")) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithWord(String value, int index, String word) {
        int end = index + word.length();
        if (end > value.length() || !value.startsWith(word, index)) {
            return false;
        }
        boolean leftBoundary = index == 0 || !isWordChar(value.charAt(index - 1));
        boolean rightBoundary = end == value.length() || !isWordChar(value.charAt(end));
        return leftBoundary && rightBoundary;
    }

    private boolean isWordChar(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String appendLimitIfNecessary(String sql, int limit) {
        String maskedSql = maskLiterals(sql);
        if (LIMIT_PATTERN.matcher(maskedSql).find()) {
            return sql;
        }
        return sql + " LIMIT " + (limit + 1);
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

    private String stripTrailingSemicolon(String sql) {
        String normalized = sql.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private boolean hasMultipleStatements(String sql) {
        String maskedSql = maskLiterals(sql);
        return maskedSql.contains(";");
    }

    private String maskLiterals(String sql) {
        StringBuilder builder = new StringBuilder(sql.length());
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (quote == 0) {
                if (current == '\'' || current == '"' || current == '`') {
                    quote = current;
                    builder.append(' ');
                } else {
                    builder.append(current);
                }
                continue;
            }

            builder.append(' ');
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
            } else if (current == quote) {
                quote = 0;
            }
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private List<String> resolveColumns(ResultSetMetaData metaData) throws SQLException {
        List<String> columns = new ArrayList<>();
        Map<String, Integer> columnCount = new HashMap<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String label = metaData.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = metaData.getColumnName(i);
            }
            int count = columnCount.getOrDefault(label, 0) + 1;
            columnCount.put(label, count);
            columns.add(count > 1 ? label + "_" + count : label);
        }
        return columns;
    }

    private List<Map<String, Object>> readRows(ResultSet resultSet, List<String> columns, int limit) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        int columnCount = columns.size();
        while (resultSet.next() && rows.size() <= limit) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(columns.get(i - 1), convertValue(resultSet.getObject(i)));
            }
            rows.add(row);
        }
        return rows;
    }

    private Object convertValue(Object value) throws SQLException {
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
}
