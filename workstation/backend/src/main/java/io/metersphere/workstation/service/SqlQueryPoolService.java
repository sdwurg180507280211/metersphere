package io.metersphere.workstation.service;

import io.metersphere.base.domain.User;
import io.metersphere.request.SqlQueryPoolRequest;
import io.metersphere.service.BaseUserService;
import io.metersphere.workstation.dto.SqlQueryHistoryDTO;
import io.metersphere.workstation.dto.SqlQueryPoolDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SqlQueryPoolService {

    private static final int MAX_POOL_SIZE = 100;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @Resource
    private DataSource dataSource;

    @Resource
    private SqlQueryService sqlQueryService;

    @Resource
    private SqlQueryHistoryService sqlQueryHistoryService;

    @Resource
    private BaseUserService baseUserService;

    public List<SqlQueryPoolDTO> list(String userId, SqlQueryPoolRequest request) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id, sql_content, title, summary, description, create_user, "
            + "update_user, enabled, use_count, create_time, update_time "
            + "FROM workstation_sql_query_pool WHERE enabled = 1");
        List<Object> parameters = new ArrayList<>();

        String keyword = request == null ? null : StringUtils.trimToNull(request.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            sql.append(" AND (title LIKE ? OR summary LIKE ? OR description LIKE ? OR sql_content LIKE ?)");
            String keywordPattern = "%" + keyword + "%";
            parameters.add(keywordPattern);
            parameters.add(keywordPattern);
            parameters.add(keywordPattern);
            parameters.add(keywordPattern);
        }
        if (request != null && Boolean.TRUE.equals(request.getOnlyMine())) {
            sql.append(" AND create_user = ?");
            parameters.add(userId);
        }
        sql.append(" ORDER BY COALESCE(update_time, create_time) DESC LIMIT ?");
        parameters.add(MAX_POOL_SIZE);

        List<SqlQueryPoolDTO> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapPool(resultSet));
                }
            }
        }
        fillUserNames(result);
        return result;
    }

    public SqlQueryPoolDTO save(String userId, SqlQueryPoolRequest request) throws SQLException {
        String sqlContent = normalizeSql(request);
        String title = normalizeTitle(request.getTitle());
        String summary = normalizeSummary(request.getSummary());
        String description = normalizeDescription(request.getDescription());
        long now = System.currentTimeMillis();
        String titleMatchedId = getIdByTitle(title);

        if (StringUtils.isBlank(request.getId())) {
            if (StringUtils.isNotBlank(titleMatchedId)) {
                throw new IllegalArgumentException("公共池标题不能重复");
            }
            return insert(userId, sqlContent, title, summary, description, now);
        }

        if (StringUtils.isNotBlank(titleMatchedId) && !StringUtils.equals(titleMatchedId, request.getId())) {
            throw new IllegalArgumentException("公共池标题不能重复");
        }
        return update(userId, request.getId(), sqlContent, title, summary, description, now);
    }

    public void offline(String userId, String id) throws SQLException {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("公共池记录不存在");
        }
        String sql = "UPDATE workstation_sql_query_pool "
            + "SET enabled = 0, update_user = ?, update_time = ? "
            + "WHERE id = ? AND enabled = 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("公共池记录不存在");
            }
        }
    }

    public SqlQueryHistoryDTO copyToHistory(String userId, String id) throws SQLException {
        SqlQueryPoolDTO pool = getEnabledById(id);
        if (pool == null) {
            throw new IllegalArgumentException("公共池记录不存在");
        }
        recordUse(id);
        return sqlQueryHistoryService.saveCopy(userId, pool.getSql(), pool.getTitle(), pool.getDescription());
    }

    public void recordUse(String id) throws SQLException {
        if (StringUtils.isBlank(id)) {
            return;
        }
        String sql = "UPDATE workstation_sql_query_pool "
            + "SET use_count = use_count + 1, update_time = COALESCE(update_time, create_time) "
            + "WHERE id = ? AND enabled = 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }

    private SqlQueryPoolDTO insert(String userId, String sqlContent, String title, String summary, String description, long now) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO workstation_sql_query_pool "
            + "(id, sql_content, title, summary, description, create_user, update_user, enabled, use_count, create_time, update_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, sqlContent);
            statement.setString(3, title);
            statement.setString(4, summary);
            statement.setString(5, description);
            statement.setString(6, userId);
            statement.setString(7, userId);
            statement.setLong(8, now);
            statement.setLong(9, now);
            statement.executeUpdate();
        }
        return getEnabledById(id);
    }

    private SqlQueryPoolDTO update(String userId, String id, String sqlContent, String title, String summary, String description, long now) throws SQLException {
        String sql = "UPDATE workstation_sql_query_pool "
            + "SET sql_content = ?, title = ?, summary = ?, description = ?, update_user = ?, update_time = ? "
            + "WHERE id = ? AND enabled = 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sqlContent);
            statement.setString(2, title);
            statement.setString(3, summary);
            statement.setString(4, description);
            statement.setString(5, userId);
            statement.setLong(6, now);
            statement.setString(7, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("公共池记录不存在");
            }
        }
        return getEnabledById(id);
    }

    private SqlQueryPoolDTO getEnabledById(String id) throws SQLException {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        String sql = "SELECT id, sql_content, title, summary, description, create_user, update_user, enabled, "
            + "use_count, create_time, update_time "
            + "FROM workstation_sql_query_pool WHERE id = ? AND enabled = 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    SqlQueryPoolDTO dto = mapPool(resultSet);
                    fillUserNames(List.of(dto));
                    return dto;
                }
            }
        }
        return null;
    }

    private String getIdByTitle(String title) throws SQLException {
        String sql = "SELECT id FROM workstation_sql_query_pool WHERE title = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("id");
                }
            }
        }
        return null;
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            if (value == null) {
                statement.setNull(i + 1, Types.VARCHAR);
            } else if (value instanceof Integer) {
                statement.setInt(i + 1, (Integer) value);
            } else if (value instanceof Long) {
                statement.setLong(i + 1, (Long) value);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
        }
    }

    private String normalizeSql(SqlQueryPoolRequest request) {
        if (request == null || StringUtils.isBlank(request.getSql())) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        return sqlQueryService.normalizeExecutableSql(request.getSql());
    }

    private String normalizeTitle(String title) {
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("标题不能为空");
        }
        String normalized = title.trim();
        if (normalized.length() > MAX_TITLE_LENGTH) {
            return normalized.substring(0, MAX_TITLE_LENGTH);
        }
        return normalized;
    }

    private String normalizeSummary(String summary) {
        if (StringUtils.isBlank(summary)) {
            throw new IllegalArgumentException("简介不能为空");
        }
        String normalized = summary.trim();
        if (normalized.length() > MAX_SUMMARY_LENGTH) {
            return normalized.substring(0, MAX_SUMMARY_LENGTH);
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (StringUtils.isBlank(description)) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            return normalized.substring(0, MAX_DESCRIPTION_LENGTH);
        }
        return normalized;
    }

    private SqlQueryPoolDTO mapPool(ResultSet resultSet) throws SQLException {
        SqlQueryPoolDTO dto = new SqlQueryPoolDTO();
        dto.setId(resultSet.getString("id"));
        dto.setSql(resultSet.getString("sql_content"));
        dto.setTitle(resultSet.getString("title"));
        dto.setSummary(resultSet.getString("summary"));
        dto.setDescription(resultSet.getString("description"));
        dto.setCreateUser(resultSet.getString("create_user"));
        dto.setUpdateUser(resultSet.getString("update_user"));
        dto.setEnabled(resultSet.getBoolean("enabled"));
        dto.setUseCount(resultSet.getLong("use_count"));
        dto.setCreateTime(resultSet.getLong("create_time"));
        long updateTime = resultSet.getLong("update_time");
        dto.setUpdateTime(resultSet.wasNull() ? null : updateTime);
        return dto;
    }

    private void fillUserNames(List<SqlQueryPoolDTO> records) {
        Set<String> userIds = new LinkedHashSet<>();
        for (SqlQueryPoolDTO record : records) {
            if (StringUtils.isNotBlank(record.getCreateUser())) {
                userIds.add(record.getCreateUser());
            }
            if (StringUtils.isNotBlank(record.getUpdateUser())) {
                userIds.add(record.getUpdateUser());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        Map<String, User> users = baseUserService.queryNameByIds(new ArrayList<>(userIds));
        for (SqlQueryPoolDTO record : records) {
            record.setCreateUserName(resolveUserName(record.getCreateUser(), users));
            record.setUpdateUserName(resolveUserName(record.getUpdateUser(), users));
        }
    }

    private String resolveUserName(String userId, Map<String, User> users) {
        if (StringUtils.isBlank(userId)) {
            return "";
        }
        User user = users == null ? null : users.get(userId);
        if (user != null && StringUtils.isNotBlank(user.getName())) {
            return user.getName();
        }
        return userId;
    }
}
