package io.metersphere.workstation.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * JQL 缓存服务
 * 
 * 缓存 JQL 解析结果（AST）和转换后的 SQL
 * 避免重复解析相同的 JQL 查询语句
 * 
 * @author MeterSphere
 */
@Service
public class JQLCacheService {
    
    /**
     * AST 缓存（内存缓存）
     * Key: JQL 的 MD5 值
     * Value: 解析后的 AST 对象
     * 
     * 我在做：使用 ConcurrentHashMap 作为内存缓存
     * 目的是：避免重复解析相同的 JQL
     * 如果不这样做：每次查询都需要重新解析，影响性能
     */
    private final ConcurrentHashMap<String, Object> astCache = new ConcurrentHashMap<>();
    
    /**
     * SQL 缓存（内存缓存）
     * Key: JQL 的 MD5 值 + 模块名
     * Value: 转换后的 SQL WHERE 子句
     */
    private final ConcurrentHashMap<String, String> sqlCache = new ConcurrentHashMap<>();
    
    /**
     * 缓存 AST
     * 
     * @param jql JQL 查询语句
     * @param ast 解析后的 AST
     */
    public void cacheAST(String jql, Object ast) {
        String cacheKey = generateCacheKey(jql);
        astCache.put(cacheKey, ast);
    }
    
    /**
     * 获取缓存的 AST
     * 
     * @param jql JQL 查询语句
     * @return 缓存的 AST，如果不存在则返回 null
     */
    public Object getCachedAST(String jql) {
        String cacheKey = generateCacheKey(jql);
        return astCache.get(cacheKey);
    }
    
    /**
     * 缓存 SQL
     * 
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @param sql 转换后的 SQL WHERE 子句
     */
    public void cacheSQL(String jql, String module, String sql) {
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
     * 我在做：使用 MD5 算法生成 JQL 的哈希值作为缓存键
     * 目的是：确保缓存键唯一且长度固定
     * 如果不这样做：直接使用 JQL 字符串作为键会占用大量内存
     * 
     * @param jql JQL 查询语句
     * @return 缓存键（MD5 值）
     */
    public String generateCacheKey(String jql) {
        return DigestUtils.md5Hex(jql);
    }
    
    /**
     * 生成缓存键（带模块名）
     * 
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @return 缓存键
     */
    public String generateCacheKey(String jql, String module) {
        return DigestUtils.md5Hex(jql + ":" + module);
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        astCache.clear();
        sqlCache.clear();
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public String getCacheStats() {
        return String.format("AST Cache Size: %d, SQL Cache Size: %d", 
            astCache.size(), sqlCache.size());
    }
}
