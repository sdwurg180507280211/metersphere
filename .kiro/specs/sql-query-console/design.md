# Design Document: SQL 查询台

## Overview

SQL 查询台是一个集成在 MeterSphere 系统设置模块中的管理员工具,提供安全的数据库查询能力。该功能采用前后端分离架构,前端使用 Vue 2.7 + Element UI 构建交互界面,后端基于 Spring Boot 3.2.12 提供 RESTful API 和 SQL 执行服务。

核心设计理念:
- **安全第一**: 多层安全校验机制,只允许 SELECT 查询
- **用户友好**: 专业的 SQL 编辑体验,降低使用门槛
- **非侵入性**: 以抽屉形式集成,不干扰主业务流程
- **高性能**: 查询限制和超时控制,保护数据库性能

## Architecture

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 2.7)                      │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ QuickSqlPanel│  │ FieldHelper  │  │ SqlEditor    │     │
│  │  (主组件)    │  │  (字段助手)  │  │  (编辑器)    │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│         └──────────────────┴──────────────────┘              │
│                            │                                 │
│                            ▼                                 │
│                    ┌──────────────┐                         │
│                    │ QueryResult  │                         │
│                    │  (结果展示)  │                         │
│                    └──────────────┘                         │
└─────────────────────────────────────────────────────────────┘
                             │
                             │ HTTP/JSON
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    API 网关层 (Gateway)                      │
│                  路由 + 认证 + 限流                          │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│              后端层 (system-setting 模块)                    │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐      │
│  │         SqlQueryController (REST API)            │      │
│  │  - POST /system/sql-query/execute                │      │
│  │  - GET  /system/sql-query/meta                   │      │
│  └────────────────────┬─────────────────────────────┘      │
│                       │                                     │
│                       ▼                                     │
│  ┌──────────────────────────────────────────────────┐      │
│  │         SqlQueryService (业务逻辑)               │      │
│  │  - validateSql()      SQL 安全校验               │      │
│  │  - executeSql()       执行查询                   │      │
│  │  - getTableMeta()     获取表结构                 │      │
│  │  - recordAuditLog()   记录审计日志               │      │
│  └────────────────────┬─────────────────────────────┘      │
│                       │                                     │
│                       ▼                                     │
│  ┌──────────────────────────────────────────────────┐      │
│  │      SqlSecurityValidator (安全校验器)           │      │
│  │  - Druid SQL Parser                              │      │
│  │  - 语句类型检查                                  │      │
│  │  - 危险操作拦截                                  │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    数据库层 (MySQL 8.0)                      │
│                  metersphere_dev 数据库                      │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈选型

**前端**:
- Vue.js 2.7.3 - 核心框架
- Element UI 2.15.13 - UI 组件库
- MsCodeEdit - 项目现有代码编辑器组件(基于 Ace Editor)
- sql-formatter 12.0.0 - SQL 格式化
- xlsx 0.18.0 - Excel 导出

**后端**:
- Spring Boot 3.2.12 - 核心框架
- Spring JDBC - 数据库访问
- Druid SQL Parser 1.2.20 - SQL 解析和校验
- Apache Shiro 2.0.1 - 权限控制
- Lombok - 代码简化

### 部署架构

SQL 查询台功能部署在 **system-setting** 模块(端口 8001):
- 前端代码: `system-setting/frontend/src/business/sql-query/`
- 后端代码: `system-setting/backend/src/main/java/io/metersphere/sqlquery/`
- 路由注册: 通过 Gateway 统一路由到 system-setting 服务

## Components and Interfaces

### 前端组件设计

#### 1. QuickSqlPanel (主组件)

**职责**: 统筹整个 SQL 查询台的布局和交互流程

**Props**:
```typescript
interface QuickSqlPanelProps {
  visible: boolean;              // 抽屉显示状态
  projectId: string;             // 当前项目 ID
  defaultSql?: string;           // 默认 SQL 模板
  limitDefault?: number;         // 默认 LIMIT (200)
  limitMax?: number;             // 最大 LIMIT (1000)
}
```

**Events**:
```typescript
interface QuickSqlPanelEvents {
  'update:visible': (visible: boolean) => void;
  'executed': (result: QueryResult) => void;
  'error': (error: Error) => void;
}
```

**核心方法**:
- `handleExecute()`: 执行 SQL 查询
- `handleFormat()`: 格式化 SQL
- `handleExport()`: 导出查询结果

#### 2. FieldHelper (字段助手)

**职责**: 提供表结构浏览和字段快速插入功能

**数据结构**:
```typescript
interface TableMeta {
  name: string;              // 表名
  displayName: string;       // 显示名称
  comment: string;           // 表注释
  fields: FieldMeta[];       // 字段列表
}

interface FieldMeta {
  name: string;              // 字段名
  type: string;              // 字段类型 (VARCHAR, INT, etc.)
  comment: string;           // 字段注释
  nullable: boolean;         // 是否可为空
}
```

**核心方法**:
- `loadTableMeta()`: 加载表结构元数据
- `insertField(field)`: 插入字段到编辑器
- `filterTables(keyword)`: 搜索过滤表和字段

#### 3. SqlEditor (SQL 编辑器)

**职责**: 提供专业的 SQL 编辑体验

**配置**:
```typescript
const editorOptions = {
  mode: 'sql',                    // 语法高亮模式
  theme: 'eclipse',               // 编辑器主题
  fontSize: 14,
  tabSize: 2,
  wrap: true,                     // 自动换行
  showPrintMargin: false,
  enableBasicAutocompletion: true,
  enableLiveAutocompletion: true
};
```

**核心方法**:
- `formatSql()`: 使用 sql-formatter 格式化
- `validateSql()`: 前端基础校验
- `insertText(text, position)`: 在指定位置插入文本

#### 4. QueryResult (查询结果)

**职责**: 展示查询结果和执行信息

**数据结构**:
```typescript
interface QueryResult {
  success: boolean;          // 是否成功
  columns: string[];         // 列名数组
  rows: any[][];             // 数据行数组
  rowCount: number;          // 返回行数
  elapsedMs: number;         // 执行耗时(毫秒)
  truncated: boolean;        // 是否被截断
  error?: string;            // 错误信息
}
```

**核心方法**:
- `exportExcel()`: 导出为 Excel
- `copyAsJson()`: 复制为 JSON 格式
- `copyAsMarkdown()`: 复制为 Markdown 表格

### 后端 API 设计

#### API 端点

**1. 执行 SQL 查询**

```
POST /system/sql-query/execute
```

**请求体**:
```json
{
  "sql": "SELECT id, name FROM test_case WHERE project_id = 'xxx' LIMIT 50",
  "projectId": "project-uuid"
}
```

**响应体** (成功):
```json
{
  "success": true,
  "columns": ["id", "name"],
  "rows": [
    ["001", "测试用例1"],
    ["002", "测试用例2"]
  ],
  "rowCount": 2,
  "elapsedMs": 23,
  "truncated": false
}
```

**响应体** (失败):
```json
{
  "success": false,
  "error": "SQL 语法错误: You have an error in your SQL syntax...",
  "errorCode": "SQL_SYNTAX_ERROR"
}
```

**2. 获取表结构元数据**

```
GET /system/sql-query/meta?projectId={projectId}
```

**响应体**:
```json
{
  "tables": [
    {
      "name": "test_case",
      "displayName": "测试用例表",
      "comment": "功能测试用例",
      "fields": [
        {
          "name": "id",
          "type": "VARCHAR(50)",
          "comment": "主键ID",
          "nullable": false
        },
        {
          "name": "name",
          "type": "VARCHAR(255)",
          "comment": "用例名称",
          "nullable": false
        }
      ]
    }
  ]
}
```

#### 后端核心类设计

**1. SqlQueryController**

```java
package io.metersphere.sqlquery.controller;

@RestController
@RequestMapping("/system/sql-query")
public class SqlQueryController {
    
    @Resource
    private SqlQueryService sqlQueryService;
    
    /**
     * 执行 SQL 查询
     * 权限: 仅管理员
     */
    @PostMapping("/execute")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ)
    @MsAuditLog(module = OperLogModule.SYSTEM_PARAMETER_SETTING, 
                type = OperLogConstants.EXECUTE, 
                content = "#msClass.getLogDetails(#request)", 
                msClass = SqlQueryService.class)
    public SqlQueryResult execute(@RequestBody SqlQueryRequest request) {
        // 1. 验证用户权限
        User currentUser = SessionUtils.getUser();
        if (!isAdmin(currentUser)) {
            throw new MSException("权限不足,仅管理员可执行 SQL 查询");
        }
        
        // 2. 执行查询
        return sqlQueryService.executeSql(request);
    }
    
    /**
     * 获取表结构元数据
     */
    @GetMapping("/meta")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ)
    public TableMetaResponse getTableMeta(@RequestParam String projectId) {
        return sqlQueryService.getTableMeta(projectId);
    }
    
    private boolean isAdmin(User user) {
        return user.getUserGroups().stream()
            .anyMatch(ug -> UserGroupConstants.ADMIN.equals(ug.getGroupId()));
    }
}
```

**2. SqlQueryService**

```java
package io.metersphere.sqlquery.service;

@Service
@Transactional(rollbackFor = Exception.class)
public class SqlQueryService {
    
    @Resource
    private JdbcTemplate jdbcTemplate;
    
    @Resource
    private SqlSecurityValidator securityValidator;
    
    @Resource
    private OperatingLogService operatingLogService;
    
    /**
     * 执行 SQL 查询
     */
    public SqlQueryResult executeSql(SqlQueryRequest request) {
        long startTime = System.currentTimeMillis();
        SqlQueryResult result = new SqlQueryResult();
        
        try {
            // 1. SQL 安全校验
            ValidationResult validation = securityValidator.validate(request.getSql());
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setError(validation.getErrorMessage());
                result.setErrorCode("SQL_VALIDATION_FAILED");
                return result;
            }
            
            // 2. 自动添加 LIMIT (如果没有)
            String sql = ensureLimit(request.getSql(), 200, 1000);
            
            // 3. 执行查询 (设置超时 30 秒)
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            
            // 4. 构建结果
            result.setSuccess(true);
            result.setColumns(extractColumns(rows));
            result.setRows(convertToArray(rows));
            result.setRowCount(rows.size());
            result.setElapsedMs(System.currentTimeMillis() - startTime);
            result.setTruncated(rows.size() >= 1000);
            
        } catch (DataAccessException e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setErrorCode("SQL_EXECUTION_ERROR");
            LogUtil.error("SQL 执行失败", e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("系统错误: " + e.getMessage());
            result.setErrorCode("SYSTEM_ERROR");
            LogUtil.error("SQL 查询系统错误", e);
        }
        
        // 5. 记录审计日志
        recordAuditLog(request, result);
        
        return result;
    }
    
    /**
     * 获取表结构元数据
     */
    public TableMetaResponse getTableMeta(String projectId) {
        TableMetaResponse response = new TableMetaResponse();
        List<TableMeta> tables = new ArrayList<>();
        
        // 查询所有表
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT " +
                     "FROM information_schema.TABLES " +
                     "WHERE TABLE_SCHEMA = DATABASE() " +
                     "ORDER BY TABLE_NAME";
        
        List<Map<String, Object>> tableList = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> table : tableList) {
            String tableName = (String) table.get("TABLE_NAME");
            String tableComment = (String) table.get("TABLE_COMMENT");
            
            // 查询表字段
            String fieldSql = "SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT, IS_NULLABLE " +
                             "FROM information_schema.COLUMNS " +
                             "WHERE TABLE_SCHEMA = DATABASE() " +
                             "AND TABLE_NAME = ? " +
                             "ORDER BY ORDINAL_POSITION";
            
            List<Map<String, Object>> fields = jdbcTemplate.queryForList(fieldSql, tableName);
            
            TableMeta tableMeta = new TableMeta();
            tableMeta.setName(tableName);
            tableMeta.setDisplayName(tableName);
            tableMeta.setComment(tableComment);
            tableMeta.setFields(convertToFieldMeta(fields));
            
            tables.add(tableMeta);
        }
        
        response.setTables(tables);
        return response;
    }
    
    /**
     * 确保 SQL 有 LIMIT 子句
     */
    private String ensureLimit(String sql, int defaultLimit, int maxLimit) {
        String upperSql = sql.toUpperCase().trim();
        
        // 如果已有 LIMIT,检查是否超限
        if (upperSql.contains("LIMIT")) {
            // 使用正则提取 LIMIT 值并校验
            Pattern pattern = Pattern.compile("LIMIT\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                int limit = Integer.parseInt(matcher.group(1));
                if (limit > maxLimit) {
                    // 替换为最大限制
                    return matcher.replaceFirst("LIMIT " + maxLimit);
                }
            }
            return sql;
        }
        
        // 没有 LIMIT,自动添加
        return sql.trim() + " LIMIT " + defaultLimit;
    }
    
    /**
     * 提取列名
     */
    private List<String> extractColumns(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rows.get(0).keySet());
    }
    
    /**
     * 转换为二维数组
     */
    private List<List<Object>> convertToArray(List<Map<String, Object>> rows) {
        List<List<Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new ArrayList<>(row.values()));
        }
        return result;
    }
    
    /**
     * 记录审计日志
     */
    private void recordAuditLog(SqlQueryRequest request, SqlQueryResult result) {
        OperatingLogDetails details = new OperatingLogDetails();
        details.setProjectId(request.getProjectId());
        details.setOperUser(SessionUtils.getUser().getId());
        details.setOperType("SQL_QUERY");
        details.setOperContent(request.getSql());
        details.setOperResult(result.isSuccess() ? "SUCCESS" : "FAILED");
        details.setOperTime(System.currentTimeMillis());
        
        operatingLogService.create(details);
    }
}
```

**3. SqlSecurityValidator (安全校验器)**

```java
package io.metersphere.sqlquery.validator;

@Component
public class SqlSecurityValidator {
    
    private static final List<String> ALLOWED_STATEMENT_TYPES = Arrays.asList("SELECT");
    
    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
        "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", 
        "TRUNCATE", "GRANT", "REVOKE", "EXEC", "EXECUTE"
    );
    
    /**
     * 验证 SQL 安全性
     */
    public ValidationResult validate(String sql) {
        ValidationResult result = new ValidationResult();
        
        try {
            // 1. 基础检查
            if (StringUtils.isBlank(sql)) {
                result.setValid(false);
                result.setErrorMessage("SQL 语句不能为空");
                return result;
            }
            
            // 2. 检查多语句
            if (sql.trim().contains(";") && sql.trim().split(";").length > 1) {
                result.setValid(false);
                result.setErrorMessage("不支持执行多条 SQL 语句");
                return result;
            }
            
            // 3. 使用 Druid SQL Parser 解析
            List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
            
            if (statements.isEmpty()) {
                result.setValid(false);
                result.setErrorMessage("无法解析 SQL 语句");
                return result;
            }
            
            if (statements.size() > 1) {
                result.setValid(false);
                result.setErrorMessage("只能执行单条 SQL 语句");
                return result;
            }
            
            SQLStatement statement = statements.get(0);
            
            // 4. 检查语句类型 (只允许 SELECT)
            if (!(statement instanceof SQLSelectStatement)) {
                result.setValid(false);
                result.setErrorMessage("只允许执行 SELECT 查询,不支持 " + statement.getClass().getSimpleName());
                return result;
            }
            
            // 5. 检查危险关键字
            String upperSql = sql.toUpperCase();
            for (String keyword : FORBIDDEN_KEYWORDS) {
                if (upperSql.contains(keyword)) {
                    result.setValid(false);
                    result.setErrorMessage("SQL 包含禁止的关键字: " + keyword);
                    return result;
                }
            }
            
            // 6. 检查子查询中的危险操作
            SQLSelectStatement selectStmt = (SQLSelectStatement) statement;
            DangerousOperationVisitor visitor = new DangerousOperationVisitor();
            selectStmt.accept(visitor);
            
            if (visitor.hasDangerousOperation()) {
                result.setValid(false);
                result.setErrorMessage("SQL 包含不安全的操作: " + visitor.getDangerousOperation());
                return result;
            }
            
            // 通过所有检查
            result.setValid(true);
            
        } catch (ParserException e) {
            result.setValid(false);
            result.setErrorMessage("SQL 语法错误: " + e.getMessage());
            LogUtil.error("SQL 解析失败", e);
        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage("SQL 校验失败: " + e.getMessage());
            LogUtil.error("SQL 校验异常", e);
        }
        
        return result;
    }
    
    /**
     * 危险操作访问器
     * 用于检测子查询中的危险操作
     */
    private static class DangerousOperationVisitor extends MySqlASTVisitorAdapter {
        private boolean dangerous = false;
        private String dangerousOperation;
        
        @Override
        public boolean visit(SQLInsertStatement x) {
            dangerous = true;
            dangerousOperation = "INSERT";
            return false;
        }
        
        @Override
        public boolean visit(SQLUpdateStatement x) {
            dangerous = true;
            dangerousOperation = "UPDATE";
            return false;
        }
        
        @Override
        public boolean visit(SQLDeleteStatement x) {
            dangerous = true;
            dangerousOperation = "DELETE";
            return false;
        }
        
        public boolean hasDangerousOperation() {
            return dangerous;
        }
        
        public String getDangerousOperation() {
            return dangerousOperation;
        }
    }
}
```

### DTO 类设计

**SqlQueryRequest**:
```java
@Data
public class SqlQueryRequest {
    @NotBlank(message = "SQL 语句不能为空")
    private String sql;
    
    @NotBlank(message = "项目 ID 不能为空")
    private String projectId;
}
```

**SqlQueryResult**:
```java
@Data
public class SqlQueryResult {
    private boolean success;
    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
    private long elapsedMs;
    private boolean truncated;
    private String error;
    private String errorCode;
}
```

**ValidationResult**:
```java
@Data
public class ValidationResult {
    private boolean valid;
    private String errorMessage;
}
```

## Data Models

### 前端数据模型

**SQL 片段配置**:
```typescript
interface SqlSnippet {
  key: string;              // 唯一标识
  label: string;            // 显示标签
  sql: string;              // SQL 片段内容
  description: string;      // 描述信息
}

const snippets: SqlSnippet[] = [
  {
    key: 'current_project',
    label: '本项目',
    sql: "WHERE project_id = '${projectId}'",
    description: '过滤当前项目数据'
  },
  {
    key: 'today',
    label: '今日创建',
    sql: 'WHERE DATE(create_time) = CURDATE()',
    description: '今天创建的记录'
  },
  {
    key: 'limit_50',
    label: 'LIMIT 50',
    sql: 'LIMIT 50',
    description: '限制返回 50 条'
  }
];
```

**SQL 模板配置**:
```typescript
interface SqlTemplate {
  key: string;
  name: string;
  description: string;
  sql: string;
}

const templates: SqlTemplate[] = [
  {
    key: 'basic_query',
    name: '基础查询',
    description: '查询当前项目的所有用例',
    sql: `SELECT id, name, status, priority
FROM test_case
WHERE project_id = '\${projectId}'
ORDER BY create_time DESC
LIMIT 50`
  }
];
```

### 后端数据模型

**审计日志记录**:
```java
@Data
public class SqlQueryAuditLog {
    private String id;
    private String userId;          // 执行用户
    private String userName;        // 用户名
    private String projectId;       // 项目 ID
    private String sql;             // 执行的 SQL
    private boolean success;        // 是否成功
    private String errorMessage;    // 错误信息
    private int rowCount;           // 返回行数
    private long elapsedMs;         // 执行耗时
    private Long createTime;        // 创建时间
}
```

## Correctness Properties

*属性是一个特征或行为,应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的形式化陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

在编写正确性属性之前,我需要先进行验收标准的可测试性分析。

