package io.metersphere.workstation.service;

import io.metersphere.request.SqlQueryHistoryRequest;
import io.metersphere.workstation.dto.SqlQueryHistoryDTO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SqlQueryHistoryService {

    private static final int MAX_HISTORY_SIZE = 100;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @Resource
    private DataSource dataSource;

    @Resource
    private SqlQueryService sqlQueryService;

    public List<SqlQueryHistoryDTO> listSaved(String userId) throws SQLException {
        String sql = "SELECT id, sql_content, title, description, saved, create_time, update_time "
            + "FROM workstation_sql_query_history "
            + "WHERE user_id = ? AND saved = 1 "
            + "ORDER BY COALESCE(update_time, create_time) DESC "
            + "LIMIT ?";
        List<SqlQueryHistoryDTO> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setInt(2, MAX_HISTORY_SIZE);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapHistory(resultSet));
                }
            }
        }
        return result;
    }

    public SqlQueryHistoryDTO save(String userId, SqlQueryHistoryRequest request) throws SQLException {
        String normalizedSql = normalizeSql(request);
        String title = normalizeTitle(request.getTitle());
        String description = normalizeDescription(request.getDescription());
        long now = System.currentTimeMillis();
        String titleMatchedId = getIdByTitle(userId, title);
        if (StringUtils.isBlank(request.getId())) {
            if (StringUtils.isNotBlank(titleMatchedId)) {
                throw new IllegalArgumentException("标题不能重复");
            }
            return insert(userId, normalizedSql, title, description, now);
        }

        if (StringUtils.isNotBlank(titleMatchedId) && !StringUtils.equals(titleMatchedId, request.getId())) {
            throw new IllegalArgumentException("标题不能重复");
        }
        return update(userId, request.getId(), normalizedSql, title, description, now);
    }

    private SqlQueryHistoryDTO insert(String userId, String sqlContent, String title, String description, long now) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO workstation_sql_query_history "
            + "(id, user_id, sql_content, title, description, saved, create_time, update_time) "
            + "VALUES (?, ?, ?, ?, ?, 1, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, userId);
            statement.setString(3, sqlContent);
            statement.setString(4, title);
            statement.setString(5, description);
            statement.setLong(6, now);
            statement.setLong(7, now);
            statement.executeUpdate();
        }
        return getById(userId, id);
    }

    private SqlQueryHistoryDTO update(String userId, String id, String sqlContent, String title, String description, long now) throws SQLException {
        String sql = "UPDATE workstation_sql_query_history "
            + "SET sql_content = ?, title = ?, description = ?, saved = 1, update_time = ? "
            + "WHERE id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sqlContent);
            statement.setString(2, title);
            statement.setString(3, description);
            statement.setLong(4, now);
            statement.setString(5, id);
            statement.setString(6, userId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("历史记录不存在");
            }
        }
        return getById(userId, id);
    }

    private SqlQueryHistoryDTO getById(String userId, String id) throws SQLException {
        String sql = "SELECT id, sql_content, title, description, saved, create_time, update_time "
            + "FROM workstation_sql_query_history WHERE id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapHistory(resultSet);
                }
            }
        }
        return null;
    }

    private String getIdByTitle(String userId, String title) throws SQLException {
        String sql = "SELECT id FROM workstation_sql_query_history WHERE user_id = ? AND title = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("id");
                }
            }
        }
        return null;
    }

    public void delete(String userId, String id) throws SQLException {
        String sql = "DELETE FROM workstation_sql_query_history WHERE id = ? AND user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, userId);
            statement.executeUpdate();
        }
    }

    private String normalizeSql(SqlQueryHistoryRequest request) {
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

    private SqlQueryHistoryDTO mapHistory(ResultSet resultSet) throws SQLException {
        SqlQueryHistoryDTO dto = new SqlQueryHistoryDTO();
        dto.setId(resultSet.getString("id"));
        dto.setSql(resultSet.getString("sql_content"));
        dto.setTitle(resultSet.getString("title"));
        dto.setDescription(resultSet.getString("description"));
        dto.setSaved(resultSet.getBoolean("saved"));
        dto.setCreateTime(resultSet.getLong("create_time"));
        long updateTime = resultSet.getLong("update_time");
        dto.setUpdateTime(resultSet.wasNull() ? null : updateTime);
        return dto;
    }
}
