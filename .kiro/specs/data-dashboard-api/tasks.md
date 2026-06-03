# 任务清单：数据大屏所属系统视图对接

> **策略**：V1 提供四个只读数据库视图，统计口径与测试跟踪-首页四个卡片一致，以所属系统为维度。甲方按 `system_code`（所属系统简称）查询。不新增后端 HTTP 接口。

---

## 阶段 0：口径确认

- [ ] 0.1 与甲方确认四个卡片的统计口径：用例数量、关联覆盖率、维护人分布、缺陷统计
- [ ] 0.2 确认甲方能接受的未设置分组命名：建议使用 `未设置`
- [ ] 0.3 确认维护人为空时的默认分组名称：建议使用 `未分配`
- [ ] 0.4 确认所属系统字段的当前实际 ID（用例侧：`case-associated-system-field-2024-12-01-002`，缺陷侧：`issue-associated-system-field-2024-12-16-001`）
- [ ] 0.5 确认缺陷统计范围为测试计划关联缺陷，不是项目全量缺陷

---

## 阶段 1：数据库视图开发

### 1.1 视图 1 — 用例数量统计
- [ ] 1.1.1 创建 `v_dashboard_case_count_by_system` 视图
- [ ] 1.1.2 统计有效最新功能用例按所属系统分组的优先级分布（P0/P1/P2/P3）
- [ ] 1.1.3 统计按所属系统分组的评审状态、评审覆盖率、评审通过率、本周新增
- [ ] 1.1.4 未设置所属系统的用例归入 `未设置`

### 1.2 视图 2 — 关联覆盖率统计
- [ ] 1.2.1 创建 `v_dashboard_relevance_count_by_system` 视图
- [ ] 1.2.2 通过功能用例所属系统维度统计 API/场景/性能/UI 关联数量
- [ ] 1.2.3 统计按所属系统分组的已覆盖数、未覆盖数、覆盖率、本周新增
- [ ] 1.2.4 未设置所属系统的用例归入 `未设置`

### 1.3 视图 3 — 用例维护人分布
- [ ] 1.3.1 创建 `v_dashboard_case_maintainer_by_system` 视图
- [ ] 1.3.2 按所属系统和维护人统计功能用例数量和关联用例数量
- [ ] 1.3.3 未设置所属系统的用例归入 `未设置`
- [ ] 1.3.4 未分配维护人的归入 `未分配`

### 1.4 视图 4 — 测试计划遗留缺陷统计
- [ ] 1.4.1 创建 `v_dashboard_bug_count_by_system` 视图
- [ ] 1.4.2 按缺陷所属系统统计测试计划关联缺陷总数、未关闭数、未关闭率、本周新增、状态分布
- [ ] 1.4.3 未设置缺陷所属系统的缺陷归入 `未设置`

### 1.5 Flyway 脚本
- [ ] 1.5.1 编写四个视图的 Flyway V 脚本（或手动 SQL 脚本）
- [ ] 1.5.2 脚本包含视图的 `CREATE OR REPLACE VIEW` 语句
- [ ] 1.5.3 执行脚本并在 `metersphere_dev` 数据库中验证

---

## 阶段 2：授权与安全

### 乙方侧
- [ ] 2.1 创建甲方只读数据库账号 `dashboard_ro`
- [ ] 2.2 授予四个视图 SELECT 权限，不授予基础表任何权限
- [ ] 2.3 配置 IP 白名单
- [ ] 2.4 验证使用 `dashboard_ro` 账号可以查询视图
- [ ] 2.5 验证使用 `dashboard_ro` 账号不能查询 `test_case`、`issues`、`custom_field_*`、`associated_system` 等基础表
- [ ] 2.6 将只读账号连接信息（host、port、user、password、database）通过安全渠道交付给甲方

---

## 阶段 3：验证

- [ ] 3.1 验证 `v_dashboard_case_count_by_system` 按系统维度统计结果正确
- [ ] 3.2 验证 `v_dashboard_relevance_count_by_system` 覆盖率计算正确
- [ ] 3.3 验证 `v_dashboard_case_maintainer_by_system` 维护人分布正确
- [ ] 3.4 验证 `v_dashboard_bug_count_by_system` 缺陷统计范围与 `getTestPlanIssue` 一致
- [ ] 3.5 验证视图查询性能（建议 < 3 秒）
- [ ] 3.6 验证未设置分组数据完整
- [ ] 3.7 验证甲方按 `system_code` 查询均可正常工作

---

## 阶段 4：交付与文档

- [ ] 4.1 编写视图查询说明文档，提供给甲方
- [ ] 4.2 编写各个视图的查询示例 SQL
- [ ] 4.3 明确说明卡片原始页面是项目维度，视图是所属系统维度
- [ ] 4.4 明确缺陷统计不是全量缺陷，而是测试计划关联缺陷
- [ ] 4.5 明确关联覆盖率统计中系统归属来自功能用例侧

---

## 工时估算

| 阶段 | 内容 | 时间 |
|------|------|------|
| 1 | 视图开发（4 个视图 + Flyway 脚本） | 1 ~ 2 天 |
| 2 | 授权与安全配置 | 0.5 天 |
| 3 | 验证 | 0.5 ~ 1 天 |
| 4 | 交付与文档 | 0.5 天 |
| **总计** | | **2.5 ~ 4 天** |

---

## 依赖关系

```
阶段 0 口径确认
      ↓
阶段 1 视图开发
      ↓
阶段 2 授权与安全
      ↓
阶段 3 验证
      ↓
阶段 4 交付与文档
```

## 相关文件

| 文件 | 说明 |
|------|------|
| `ExtTestCaseMapper.xml` | 现有用例统计 SQL（countPriority、countRelevance、countFuncMaintainer 等） |
| `ExtIssuesMapper.xml` | 现有缺陷统计 SQL（getTestPlanIssue） |
| `ExtTestPlanTestCaseMapper.xml` | 现有功能用例失败查询 |
| `ExtTestPlanScenarioCaseMapper.xml` | 现有场景用例失败查询 |
| `TrackService.java` | 现有统计服务逻辑（getPlanBugSize、getPlanBugStatusSize） |
| `TrackStatisticsDTO.java` | 现有统计 DTO（countPriority、countRelevance、countCoverage） |
