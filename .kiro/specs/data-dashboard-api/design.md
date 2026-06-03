# 设计文档：数据大屏所属系统视图对接

## 概述

V1 方案不新增后端接口，改为提供四个只读数据库视图。甲方人员直接查询数据库视图，以**所属系统**为查询条件获取测试跟踪-首页四个统计卡片的同口径数据。

**文档版本**：V1.1
**修订日期**：2026年6月2日

---

## 核心设计目标

1. **甲方直接查数据库**：不经过后端 API，不走 API Key 认证链
2. **所属系统维度**：甲方按 `system_code`（所属系统简称）查询，不按 `projectId` 查
3. **只读视图**：甲方数据库账号只授予四个视图的 `SELECT` 权限
4. **未设置归组**：没有填写所属系统的数据统一归入 `未设置`
5. **同口径不同维度**：统计逻辑与测试跟踪-首页卡片一致，维度从项目改为所属系统
6. **视图自查**：甲方查询不需要理解 `test_case`、`issues`、`custom_field_*` 等基础表结构

---

## 架构设计

### 甲方查询流程

```
┌──────────────────────────┐         MySQL read-only         ┌──────────────────────────┐
│  甲方数据大屏 / BI        │ ─────────────────────────────>  │  MeterSphere 数据库       │
│                          │   SELECT * FROM                 │                          │
│  ┌────────────────────┐  │   v_dashboard_case_count_      │  ┌──────────────────────┐ │
│  │ 定时查询器          │  │   by_system                    │  │ 四个只读统计视图      │ │
│  │ (5-10min, JDBC)    │  │   WHERE system_code = 'CMS'    │  │                      │ │
│  └────────────────────┘  │                                 │  │ 1. case_count        │ │
│                          │                                 │  │ 2. relevance_count   │ │
│  ┌────────────────────┐  │                                 │  │ 3. case_maintainer   │ │
│  │ 可视化渲染          │  │                                 │  │ 4. bug_count         │ │
│  └────────────────────┘  │                                 │  └──────────────────────┘ │
└──────────────────────────┘                                 └──────────────────────────┘
```

### V1 方案与原始 HTTP 方案的区别

| 维度 | 原始 HTTP 方案（备用） | 当前 V1 视图方案 |
|------|----------------------|------------------|
| 数据获取方式 | HTTP + API Key | MySQL 只读连接 |
| 统计维度 | 项目 `projectId` | 所属系统 `system_id` |
| 甲方查询方式 | 轮询 5 个接口 | 查询 4 个视图 |
| 认证方式 | API Key + AES 签名 | MySQL 账号密码 |
| 前端复杂度 | 需要签名、重试、缓存 | 直接 JDBC 查询 |

---

## 数据来源与关联关系

### 用例侧（卡片 1、2、3）

```
test_case                           # 功能用例 (只统计 status != 'Trash', latest = 1)
  ├── custom_field_test_case        # 关联用例所属系统
  │     field_id = 'case-associated-system-field-2024-12-01-002'
  │     value = associated_system.id (带引号的 UUID)
  ├── test_case_test                # 用例关联关系 (卡片 2 使用)
  │     test_type = 'testCase'     → api_test_case
  │     test_type = 'performance'  → load_test
  │     test_type = 'automation'   → api_scenario
  │     test_type = 'uiAutomation' → ui_scenario
  └── associated_system             # 所属系统主数据
        id = TRIM('"' FROM custom_field_test_case.value)

test_case.maintainer → user.id → user.name   # 维护人 (卡片 3 使用)
```

### 缺陷侧（卡片 4）

```
test_plan_test_case                 # 测试计划关联用例
  └── test_case_issues              # 用例关联缺陷
        resource_id = test_plan_test_case.id
        └── issues                  # 缺陷
              └── custom_field_issues
                    field_id = 'issue-associated-system-field-2024-12-16-001'
                    value = associated_system.id (带引号的 UUID)
              └── associated_system # 所属系统主数据
```

---

## 四个视图设计

### 视图 1: v_dashboard_case_count_by_system

对应页面卡片：**CaseCountCard**（用例数量统计）

| 属性 | 内容 |
|------|------|
| 所属页面 | 测试跟踪-首页，左上角卡片 |
| 统计对象 | 有效、最新功能用例 |
| 系统归属 | `custom_field_test_case` → `所属系统` 字段 |
| 分组逻辑 | 按 `associated_system.name` 分组，未设置归入 `未设置` |

**输出字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `system_id` | VARCHAR(50) | 所属系统 ID，未设置为 NULL |
| `system_name` | VARCHAR(64) | 所属系统名称，未设置为 `未设置` |
| `system_code` | VARCHAR(255) | 所属系统简称，未设置为 NULL |
| `case_count` | BIGINT | 用例总数 |
| `p0_count` | BIGINT | P0 数量 |
| `p1_count` | BIGINT | P1 数量 |
| `p2_count` | BIGINT | P2 数量 |
| `p3_count` | BIGINT | P3 数量 |
| `review_pass_count` | BIGINT | 评审通过数 |
| `review_unpass_count` | BIGINT | 评审未通过数 |
| `review_prepare_count` | BIGINT | 未评审数 |
| `review_rage` | DECIMAL(5,2) | 评审覆盖率（%） |
| `review_pass_rage` | DECIMAL(5,2) | 评审通过率（%） |
| `this_week_count` | BIGINT | 本周新增用例数 |

**数据口径**

- 用例范围：`test_case.status != 'Trash'` 或 `status IS NULL`，且 `latest = 1`
- 优先级按 `test_case.priority` 分组
- 评审状态按 `test_case.review_status` 分组
- 本周新增以当前周周一 00:00 到周日 23:59 的 `create_time` 为范围
- 评审覆盖率 = (评审通过 + 评审未通过) / 总用例数 × 100
- 评审通过率 = 评审通过 / (评审通过 + 评审未通过) × 100

---

### 视图 2: v_dashboard_relevance_count_by_system

对应页面卡片：**RelevanceCaseCard**（关联用例覆盖率）

| 属性 | 内容 |
|------|------|
| 所属页面 | 测试跟踪-首页，右上角卡片 |
| 统计对象 | 功能用例对应的 API/场景/性能/UI 用例关联数量 |
| 系统归属 | 通过功能用例的 `所属系统` 字段追溯 |
| 分组逻辑 | 按 `associated_system.name` 分组，未设置归入 `未设置` |

**输出字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `system_id` | VARCHAR(50) | 所属系统 ID |
| `system_name` | VARCHAR(64) | 所属系统名称 |
| `system_code` | VARCHAR(255) | 所属系统简称 |
| `api_case_count` | BIGINT | API 用例关联数 |
| `scenario_case_count` | BIGINT | 场景用例关联数 |
| `performance_case_count` | BIGINT | 性能用例关联数 |
| `ui_case_count` | BIGINT | UI 自动化用例关联数 |
| `coverage_count` | BIGINT | 已覆盖功能用例数 |
| `uncoverage_count` | BIGINT | 未覆盖功能用例数 |
| `total_case_count` | BIGINT | 功能用例总数 |
| `coverage_rage` | DECIMAL(5,2) | 关联覆盖率（%） |
| `this_week_count` | BIGINT | 本周新增关联数 |

**数据口径**

- 关联关系通过 `test_case` → `test_case_test` 获取
- 每种关联类型只统计对应目标表的有效记录
- `coverage_count`：至少有一条关联记录的功能用例数
- `uncoverage_count`：总功能用例数 - `coverage_count`
- 覆盖率 = 已覆盖用例数 / 总功能用例数 × 100
- **系统归属来自功能用例侧**，API 用例、场景用例、性能用例、UI 自动化用例本身没有所属系统字段

---

### 视图 3: v_dashboard_case_maintainer_by_system

对应页面卡片：**CaseMaintenance**（用例维护人分布）

| 属性 | 内容 |
|------|------|
| 所属页面 | 测试跟踪-首页，左中位置柱状图 |
| 统计对象 | 功能用例按维护人分组 |
| 系统归属 | `custom_field_test_case` → `所属系统` 字段 |
| 返回格式 | 多行，每行一个系统 + 维护人组合 |

**输出字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `system_id` | VARCHAR(50) | 所属系统 ID |
| `system_name` | VARCHAR(64) | 所属系统名称 |
| `system_code` | VARCHAR(255) | 所属系统简称 |
| `maintainer` | VARCHAR(64) | 维护人姓名，未分配维护人的归入 `未分配` |
| `function_case_count` | BIGINT | 功能用例数量 |
| `relevance_case_count` | BIGINT | 关联用例数量 |

**数据口径**

- 功能用例按 `test_case.maintainer` 关联 `user.name`
- 关联用例按 `test_case.maintainer` 关联 `user.name`，且用例在 `test_case_test` 中有关联记录
- 系统归属通过 `custom_field_test_case` → `所属系统`
- 维护人为空时归入 `未分配`

---

### 视图 4: v_dashboard_bug_count_by_system

对应页面卡片：**BugCountCard**（缺陷统计）

| 属性 | 内容 |
|------|------|
| 所属页面 | 测试跟踪-首页，左下角卡片 |
| 统计对象 | 测试计划关联的缺陷 |
| 系统归属 | `custom_field_issues` → `缺陷所属系统` 字段 |
| 分组逻辑 | 按 `associated_system.name` 分组，未设置归入 `未设置` |

**输出字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `system_id` | VARCHAR(50) | 所属系统 ID |
| `system_name` | VARCHAR(64) | 所属系统名称 |
| `system_code` | VARCHAR(255) | 所属系统简称 |
| `bug_total` | BIGINT | 缺陷总数 |
| `bug_unclosed` | BIGINT | 未关闭缺陷数 |
| `unclosed_rage` | DECIMAL(5,2) | 未关闭率（%） |
| `this_week_count` | BIGINT | 本周新增缺陷数 |
| `status_new` | BIGINT | 新增状态缺陷数 |
| `status_in_progress` | BIGINT | 处理中状态缺陷数 |
| `status_resolved` | BIGINT | 已解决状态缺陷数 |
| `status_closed` | BIGINT | 已关闭状态缺陷数 |

**数据口径**

- 缺陷范围与 `getTestPlanIssue` 一致：通过 `test_plan_test_case → test_case_issues → issues` 获取
- 排除 `issues.platform_status = 'delete'` 的缺陷
- 未关闭 = 缺陷 status != 'closed'
- 未关闭率 = 未关闭数 / 总缺陷数 × 100
- 本周新增按 `test_case_issues.relate_time` 筛选
- **缺陷不是项目全量缺陷，而是测试计划关联缺陷**

---

## 授权方案

### 创建甲方只读账号

```sql
CREATE USER 'dashboard_ro'@'%' IDENTIFIED BY '<strong-password>';

GRANT SELECT ON  v_dashboard_case_count_by_system   TO 'dashboard_ro'@'%';
GRANT SELECT ON  v_dashboard_relevance_count_by_system TO 'dashboard_ro'@'%';
GRANT SELECT ON  v_dashboard_case_maintainer_by_system TO 'dashboard_ro'@'%';
GRANT SELECT ON  v_dashboard_bug_count_by_system    TO 'dashboard_ro'@'%';

FLUSH PRIVILEGES;
```

### 安全约束

- 只授予四个视图 `SELECT`，不授予基础表任何权限
- 使用独立只读账号 `dashboard_ro`，不与业务读写账号共享
- 视图中不输出不必要的敏感字段
- 数据库访问地址、账号、密码由运维侧单独交付

---

## 甲方查询示例

### 查询单个所属系统

```sql
SELECT *
FROM v_dashboard_case_count_by_system
WHERE system_code = 'IT';
```

### 查询全部所属系统

```sql
SELECT *
FROM v_dashboard_case_count_by_system
ORDER BY system_code;
```

### 查询某系统的用例维护人分布

```sql
SELECT system_name, maintainer, function_case_count, relevance_case_count
FROM v_dashboard_case_maintainer_by_system
WHERE system_code = 'IT'
ORDER BY function_case_count DESC;
```

### 查询缺陷未关闭率高于 30% 的系统

```sql
SELECT system_name, bug_total, bug_unclosed, unclosed_rage
FROM v_dashboard_bug_count_by_system
WHERE unclosed_rage > 30
ORDER BY unclosed_rage DESC;
```

---

## 数据质量说明

| 维度 | 数值 | 说明 |
|------|------|------|
| 功能用例总数 | 510 | `status != 'Trash', latest = 1` |
| 有用例所属系统 | 176 | 占比 34.5%，其余 334 归入 `未设置` |
| 缺陷总数 | 257 | 其中测试计划关联 256 有所属系统 |
| 计划和评审本身 | 无直接所属系统 | 只能通过关联功能用例间接统计 |

---

## 与原 HTTP 方案的关系

原 HTTP 方案（API Key + 轮询 TrackController 五个接口）不删除，作为可选的后续对接方式保留：

- 如果甲方后续需要 HTTP 接口形式对接，可切换到原方案
- 如果甲方 BI 工具支持数据库视图查询，继续使用当前视图方案
- 两种方案的统计口径相同，仅获取方式不同

---

## 风险

| 风险 | 等级 | 处理方式 |
|------|------|----------|
| 数据库直连安全风险 | **高** | 只读视图 + 独立只读账号 + IP 白名单 |
| 所属系统字段覆盖率低（用例侧 34.5%） | 中 | 归入 `未设置`，不丢弃数据 |
| 视图查询性能 | 低 | 合理使用索引，甲方按 5-10 分钟频率查询 |
| 缺陷统计范围是测试计划关联缺陷 | 中 | 文档明确口径，避免甲方误以为全量缺陷 |
| 关联覆盖率的所属系统来自功能用例侧 | 中 | 文档说明 API/场景/性能/UI 本身无系统字段 |

---

## 附录：实施参考手册

### A.1 创建四个视图

连入 `metersphere_dev` 库，依次执行：

```sql
-- ============================================================
-- 视图 1：用例数量统计（对应 CaseCountCard）
-- ============================================================
CREATE OR REPLACE VIEW v_dashboard_case_count_by_system AS
SELECT
    COALESCE(sys.id, NULL)              AS system_id,
    COALESCE(sys.name, '未设置')         AS system_name,
    COALESCE(sys.description, NULL)      AS system_code,
    COUNT(tc.id)                        AS case_count,
    SUM(CASE WHEN tc.priority = 'P0' THEN 1 ELSE 0 END) AS p0_count,
    SUM(CASE WHEN tc.priority = 'P1' THEN 1 ELSE 0 END) AS p1_count,
    SUM(CASE WHEN tc.priority = 'P2' THEN 1 ELSE 0 END) AS p2_count,
    SUM(CASE WHEN tc.priority = 'P3' THEN 1 ELSE 0 END) AS p3_count,
    SUM(CASE WHEN tc.review_status = 'pass'    THEN 1 ELSE 0 END) AS review_pass_count,
    SUM(CASE WHEN tc.review_status = 'unpass'  THEN 1 ELSE 0 END) AS review_unpass_count,
    SUM(CASE WHEN tc.review_status = 'prepare' OR tc.review_status IS NULL THEN 1 ELSE 0 END) AS review_prepare_count,
    ROUND(
        SUM(CASE WHEN tc.review_status IN ('pass','unpass') THEN 1 ELSE 0 END)
        * 100.0 / NULLIF(COUNT(tc.id), 0), 2
    ) AS review_rage,
    ROUND(
        SUM(CASE WHEN tc.review_status = 'pass' THEN 1 ELSE 0 END)
        * 100.0 / NULLIF(SUM(CASE WHEN tc.review_status IN ('pass','unpass') THEN 1 ELSE 0 END), 0), 2
    ) AS review_pass_rage,
    SUM(CASE WHEN tc.create_time >= UNIX_TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)) * 1000
        THEN 1 ELSE 0 END) AS this_week_count
FROM test_case tc
LEFT JOIN custom_field_test_case cftc
    ON cftc.resource_id = tc.id
    AND cftc.field_id = 'case-associated-system-field-2024-12-01-002'
LEFT JOIN associated_system sys
    ON sys.id = TRIM('"' FROM COALESCE(cftc.value, cftc.text_value, ''))
WHERE (tc.status != 'Trash' OR tc.status IS NULL)
  AND tc.latest = 1
GROUP BY COALESCE(sys.id, NULL), COALESCE(sys.name, '未设置'), COALESCE(sys.description, NULL);
```

```sql
-- ============================================================
-- 视图 2：关联覆盖率统计（对应 RelevanceCaseCard）
-- ============================================================
CREATE OR REPLACE VIEW v_dashboard_relevance_count_by_system AS
WITH
system_cases AS (
    SELECT
        tc.id AS case_id,
        COALESCE(sys.id, NULL)              AS system_id,
        COALESCE(sys.name, '未设置')         AS system_name,
        COALESCE(sys.description, NULL)      AS system_code,
        tc.ref_id
    FROM test_case tc
    LEFT JOIN custom_field_test_case cftc
        ON cftc.resource_id = tc.id
        AND cftc.field_id = 'case-associated-system-field-2024-12-01-002'
    LEFT JOIN associated_system sys
        ON sys.id = TRIM('"' FROM COALESCE(cftc.value, cftc.text_value, ''))
    WHERE (tc.status != 'Trash' OR tc.status IS NULL) AND tc.latest = 1
),
relevance_by_type AS (
    SELECT sc.system_id, sc.system_name, sc.system_code, tct.test_type, COUNT(DISTINCT tct.test_id) AS cnt
    FROM system_cases sc
    INNER JOIN test_case_test tct ON tct.test_case_id = sc.case_id
    INNER JOIN api_test_case atc ON atc.id = tct.test_id AND (atc.status IS NULL OR atc.status != 'Trash')
    WHERE tct.test_type = 'testCase'
    GROUP BY sc.system_id, sc.system_name, sc.system_code, tct.test_type
    UNION ALL
    SELECT sc.system_id, sc.system_name, sc.system_code, tct.test_type, COUNT(DISTINCT tct.test_id) AS cnt
    FROM system_cases sc
    INNER JOIN test_case_test tct ON tct.test_case_id = sc.case_id
    INNER JOIN load_test lt ON lt.id = tct.test_id
    WHERE tct.test_type = 'performance'
    GROUP BY sc.system_id, sc.system_name, sc.system_code, tct.test_type
    UNION ALL
    SELECT sc.system_id, sc.system_name, sc.system_code, tct.test_type, COUNT(DISTINCT tct.test_id) AS cnt
    FROM system_cases sc
    INNER JOIN test_case_test tct ON tct.test_case_id = sc.case_id
    INNER JOIN api_scenario aps ON aps.id = tct.test_id AND aps.status != 'Trash'
    WHERE tct.test_type = 'automation'
    GROUP BY sc.system_id, sc.system_name, sc.system_code, tct.test_type
    UNION ALL
    SELECT sc.system_id, sc.system_name, sc.system_code, tct.test_type, COUNT(DISTINCT tct.test_id) AS cnt
    FROM system_cases sc
    INNER JOIN test_case_test tct ON tct.test_case_id = sc.case_id
    INNER JOIN ui_scenario uis ON uis.id = tct.test_id AND uis.status != 'Trash'
    WHERE tct.test_type = 'uiAutomation'
    GROUP BY sc.system_id, sc.system_name, sc.system_code, tct.test_type
),
coverage AS (
    SELECT
        sc.system_id, sc.system_name, sc.system_code,
        COUNT(DISTINCT CASE WHEN tct.test_case_id IS NOT NULL THEN sc.case_id END) AS coverage_count,
        COUNT(DISTINCT sc.case_id) AS total_count
    FROM system_cases sc
    LEFT JOIN test_case_test tct ON tct.test_case_id = sc.case_id
    GROUP BY sc.system_id, sc.system_name, sc.system_code
),
this_week AS (
    SELECT sc.system_id, sc.system_name, sc.system_code, COUNT(DISTINCT sc.ref_id) AS this_week_count
    FROM system_cases sc
    INNER JOIN test_case_test tct ON tct.test_case_id = sc.case_id
    WHERE tct.create_time >= UNIX_TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)) * 1000
    GROUP BY sc.system_id, sc.system_name, sc.system_code
)
SELECT
    cov.system_id, cov.system_name, cov.system_code,
    COALESCE(SUM(CASE WHEN rbt.test_type = 'testCase'     THEN rbt.cnt END), 0) AS api_case_count,
    COALESCE(SUM(CASE WHEN rbt.test_type = 'automation'   THEN rbt.cnt END), 0) AS scenario_case_count,
    COALESCE(SUM(CASE WHEN rbt.test_type = 'performance'  THEN rbt.cnt END), 0) AS performance_case_count,
    COALESCE(SUM(CASE WHEN rbt.test_type = 'uiAutomation' THEN rbt.cnt END), 0) AS ui_case_count,
    cov.coverage_count,
    cov.total_count - cov.coverage_count AS uncoverage_count,
    cov.total_count AS total_case_count,
    ROUND(cov.coverage_count * 100.0 / NULLIF(cov.total_count, 0), 2) AS coverage_rage,
    COALESCE(tw.this_week_count, 0) AS this_week_count
FROM coverage cov
LEFT JOIN relevance_by_type rbt
    ON COALESCE(cov.system_id, '') = COALESCE(rbt.system_id, '')
    AND cov.system_name = rbt.system_name
LEFT JOIN this_week tw
    ON COALESCE(cov.system_id, '') = COALESCE(tw.system_id, '')
    AND cov.system_name = tw.system_name
GROUP BY cov.system_id, cov.system_name, cov.system_code, cov.coverage_count, cov.total_count, tw.this_week_count;
```

```sql
-- ============================================================
-- 视图 3：用例维护人分布（对应 CaseMaintenance）
-- ============================================================
CREATE OR REPLACE VIEW v_dashboard_case_maintainer_by_system AS
SELECT
    COALESCE(sys.id, NULL)              AS system_id,
    COALESCE(sys.name, '未设置')         AS system_name,
    COALESCE(sys.description, NULL)      AS system_code,
    COALESCE(u.name, '未分配')           AS maintainer,
    COUNT(tc.id)                        AS function_case_count,
    COUNT(DISTINCT tct.test_case_id)    AS relevance_case_count
FROM test_case tc
LEFT JOIN user u ON u.id = tc.maintainer
LEFT JOIN custom_field_test_case cftc
    ON cftc.resource_id = tc.id
    AND cftc.field_id = 'case-associated-system-field-2024-12-01-002'
LEFT JOIN associated_system sys
    ON sys.id = TRIM('"' FROM COALESCE(cftc.value, cftc.text_value, ''))
LEFT JOIN test_case_test tct ON tct.test_case_id = tc.id
WHERE tc.status != 'Trash' AND tc.latest = 1
GROUP BY COALESCE(sys.id, NULL), COALESCE(sys.name, '未设置'), COALESCE(sys.description, NULL), COALESCE(u.name, '未分配');
```

```sql
-- ============================================================
-- 视图 4：测试计划遗留缺陷统计（对应 BugCountCard）
-- ============================================================
CREATE OR REPLACE VIEW v_dashboard_bug_count_by_system AS
WITH plan_issues AS (
    SELECT DISTINCT tci.issues_id AS issue_id
    FROM test_plan_test_case tptc
    JOIN test_plan tp ON tp.id = tptc.plan_id
    JOIN test_case_issues tci ON tptc.id = tci.resource_id
    JOIN issues i ON tci.issues_id = i.id
    WHERE tptc.is_del != 1
      AND (i.platform_status IS NULL OR i.platform_status != 'delete')
),
issue_system AS (
    SELECT
        pi.issue_id, i.status,
        COALESCE(sys.id, NULL)              AS system_id,
        COALESCE(sys.name, '未设置')         AS system_name,
        COALESCE(sys.description, NULL)      AS system_code
    FROM plan_issues pi
    JOIN issues i ON i.id = pi.issue_id
    LEFT JOIN custom_field_issues cfi
        ON cfi.resource_id = i.id
        AND cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
    LEFT JOIN associated_system sys
        ON sys.id = TRIM('"' FROM COALESCE(cfi.value, cfi.text_value, ''))
),
this_week_count AS (
    SELECT
        COALESCE(sys2.id, NULL)              AS system_id,
        COALESCE(sys2.name, '未设置')         AS system_name,
        COALESCE(sys2.description, NULL)      AS system_code,
        COUNT(DISTINCT tci2.issues_id)        AS cnt
    FROM test_plan_test_case tptc2
    JOIN test_plan tp2 ON tp2.id = tptc2.plan_id
    JOIN test_case_issues tci2 ON tptc2.id = tci2.resource_id
    JOIN issues i2 ON tci2.issues_id = i2.id
    LEFT JOIN custom_field_issues cfi2
        ON cfi2.resource_id = i2.id
        AND cfi2.field_id = 'issue-associated-system-field-2024-12-16-001'
    LEFT JOIN associated_system sys2
        ON sys2.id = TRIM('"' FROM COALESCE(cfi2.value, cfi2.text_value, ''))
    WHERE tptc2.is_del != 1
      AND (i2.platform_status IS NULL OR i2.platform_status != 'delete')
      AND tci2.relate_time >= UNIX_TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)) * 1000
    GROUP BY sys2.id, sys2.name, sys2.description
)
SELECT
    iss.system_id, iss.system_name, iss.system_code,
    COUNT(*) AS bug_total,
    SUM(CASE WHEN iss.status != 'closed' THEN 1 ELSE 0 END) AS bug_unclosed,
    ROUND(
        SUM(CASE WHEN iss.status != 'closed' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 2
    ) AS unclosed_rage,
    COALESCE(tw.cnt, 0) AS this_week_count,
    SUM(CASE WHEN iss.status = 'new'         THEN 1 ELSE 0 END) AS status_new,
    SUM(CASE WHEN iss.status = 'in_progress' THEN 1 ELSE 0 END) AS status_in_progress,
    SUM(CASE WHEN iss.status = 'resolved'    THEN 1 ELSE 0 END) AS status_resolved,
    SUM(CASE WHEN iss.status = 'closed'      THEN 1 ELSE 0 END) AS status_closed
FROM issue_system iss
LEFT JOIN this_week_count tw
    ON COALESCE(iss.system_id, '') = COALESCE(tw.system_id, '')
    AND iss.system_name = tw.system_name
GROUP BY iss.system_id, iss.system_name, iss.system_code;
```

### A.2 创建只读账号并授权

```sql
-- 删除旧账号（如存在）
DROP USER IF EXISTS 'dashboard_ro'@'%';

-- 创建只读账号
CREATE USER 'dashboard_ro'@'%' IDENTIFIED BY '<your-strong-password>';

-- 只授予四个视图的 SELECT 权限
GRANT SELECT ON  v_dashboard_case_count_by_system       TO 'dashboard_ro'@'%';
GRANT SELECT ON  v_dashboard_relevance_count_by_system  TO 'dashboard_ro'@'%';
GRANT SELECT ON  v_dashboard_case_maintainer_by_system  TO 'dashboard_ro'@'%';
GRANT SELECT ON  v_dashboard_bug_count_by_system        TO 'dashboard_ro'@'%';

FLUSH PRIVILEGES;
```

### A.3 验证

| 测试项 | 结果 |
|--------|------|
| `dashboard_ro` 查询 `v_dashboard_case_count_by_system` | 通过 |
| `dashboard_ro` 查询 `v_dashboard_relevance_count_by_system` | 通过 |
| `dashboard_ro` 查询 `v_dashboard_case_maintainer_by_system` | 通过 |
| `dashboard_ro` 查询 `v_dashboard_bug_count_by_system` | 通过 |
| `dashboard_ro` 查询 `test_case` | 拒绝（`SELECT command denied`） |
| `dashboard_ro` 查询 `issues` | 拒绝 |
| `dashboard_ro` 查询 `custom_field_test_case` | 拒绝 |

### A.4 连接信息交付甲方

| 参数 | 值 |
|------|-----|
| Host | 10.0.149.15 |
| Port | `3306` |
| Database | `metersphere` |
| User | `dashboard_ro` |
| Password | `dashboard_ro` |

### A.5 甲方查询示例

```sql
-- 按系统简称查
SELECT * FROM v_dashboard_case_count_by_system
WHERE system_code = 'ICBS-POS';

-- 查多个系统
SELECT * FROM v_dashboard_case_count_by_system
WHERE system_code IN ('HX', 'ICBS-POS', 'IT');

-- 查全部所属系统
SELECT * FROM v_dashboard_case_count_by_system
ORDER BY case_count DESC;

-- 查某系统的用例维护人分布
SELECT system_code, system_name, maintainer, function_case_count, relevance_case_count
FROM v_dashboard_case_maintainer_by_system
WHERE system_code = 'IT'
ORDER BY function_case_count DESC;

-- 查缺陷未关闭率高于 30% 的系统
SELECT system_code, system_name, bug_total, bug_unclosed, unclosed_rage
FROM v_dashboard_bug_count_by_system
WHERE unclosed_rage > 30
ORDER BY unclosed_rage DESC;
```
