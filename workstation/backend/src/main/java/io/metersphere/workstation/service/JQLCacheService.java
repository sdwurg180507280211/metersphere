package io.metersphere.workstation.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JQL 缓存服务
 *
 * 缓存转换后的 SQL WHERE 子句
 * 避免重复解析相同的 JQL 查询语句
 *
 * @author MeterSphere
 */
@Service
public class JQLCacheService {

    /**
     * 最大缓存条目数，防止内存溢出
     */
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * 短JQL阈值，短于这个长度的直接使用原始字符串作为key
     */
    private static final int SHORT_JQL_THRESHOLD = 100;

    /**
     * SQL 缓存（内存缓存）
     * Key: JQL 的 MD5 值 + 模块名，或短JQL直接作为key
     * Value: 转换后的 SQL WHERE 子句
     */
    private final ConcurrentHashMap<String, String> sqlCache = new ConcurrentHashMap<>();

    /**
     * 缓存 SQL
     *
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @param sql 转换后的 SQL WHERE 子句
     */
    public void cacheSQL(String jql, String module, String sql) {
        if (sqlCache.size() >= MAX_CACHE_SIZE) {
            // 缓存已满，清空一半旧缓存（简单策略）
            clearHalfCache();
        }
        String cacheKey = generateCacheKey(jql, module);
        sqlCache.put(cacheKey, sql);
    }

    /**
     * 获取缓存的 SQL
     *
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @return 缓存的 SQL，如果不存在则返回 null
     */
    public String getCachedSQL(String jql, String module) {
        String cacheKey = generateCacheKey(jql, module);
        return sqlCache.get(cacheKey);
    }

    /**
     * 生成缓存键
     *
     * 我在做：根据JQL长度选择合适的缓存键生成策略
     * 目的是：短JQL直接使用字符串，避免MD5计算开销；长JQL使用MD5节省内存
     *
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @return 缓存键
     */
    public String generateCacheKey(String jql, String module) {
        String combined = jql + ":" + module;
        if (combined.length() <= SHORT_JQL_THRESHOLD) {
            // 短字符串直接使用，避免MD5计算开销
            return combined;
        }
        // 长字符串使用MD5，节省内存
        return DigestUtils.md5Hex(combined);
    }

    /**
     * 清空一半缓存
     * 简单的LRU替代策略：删除较早插入的条目
     */
    private void clearHalfCache() {
        int targetSize = MAX_CACHE_SIZE / 2;
        int count = 0;
        for (String key : sqlCache.keySet()) {
            if (count >= targetSize) {
                break;
            }
            sqlCache.remove(key);
            count++;
        }
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        sqlCache.clear();
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    public String getCacheStats() {
        return String.format("SQL Cache Size: %d, Max Cache Size: %d",
            sqlCache.size(), MAX_CACHE_SIZE);
    }
}
