# 任务清单：数据大屏 API 数据对接

> **策略**：V1 复用现有 TrackController 五个接口 + 现有 ApiKeyFilter 认证链，零代码改动，仅需生成 API Key 并按规范对接。V2 在需求测试流程模块上线后新增 workflow 统计接口。

---

## V1: 现有接口对接（零开发，仅配置 + 对接）

### 1. API Key 生成
- [ ] 1.1 确定用于数据大屏调用的账号（建议使用专用管理员账号）
- [ ] 1.2 登录 MeterSphere，调用 `GET /user/key/generate` 生成 API Key
- [ ] 1.3 保存 `accessKey` 和 `secretKey`（secretKey 仅展示一次）
- [ ] 1.4 确认该账号有目标项目的 `PROJECT_TRACK_HOME:READ` 权限

### 2. 数据大屏端对接开发
- [ ] 2.1 实现签名生成算法（参考 design.md 中的多语言示例）
- [ ] 2.2 实现定时轮询器，按 5-10 分钟间隔拉取数据
- [ ] 2.3 对接 `GET /track/count/{projectId}` — 用例数量统计
- [ ] 2.4 对接 `GET /track/relevance/count/{projectId}` — 关联用例覆盖率
- [ ] 2.5 对接 `GET /track/bug/count/{projectId}` — 缺陷统计
- [ ] 2.6 对接 `GET /track/case/bar/{projectId}` — 用例责任人分布
- [ ] 2.7 对接 `GET /track/failure/case/about/plan/{projectId}/{versionId}/{pageSize}/{goPage}` — 失败用例
- [ ] 2.8 实现接口异常重试和降级策略（接口超时或返回错误时不阻断大屏渲染）

### 3. 大屏端数据缓存与渲染
- [ ] 3.1 设计本地缓存层，缓存最近一次拉取的各接口数据
- [ ] 3.2 实现大屏可视化组件（用例总览、覆盖率仪表盘、缺陷趋势、人员分布、失败用例排行）
- [ ] 3.3 实现多项目轮询（依次拉取各 projectId 的数据）

### 4. 验证
- [ ] 4.1 验证签名生成正确，接口返回 200
- [ ] 4.2 验证签名过期（超过 30 分钟）时返回 401
- [ ] 4.3 验证禁用 API Key 后接口返回 401
- [ ] 4.4 验证多项目数据拉取正常

---

## V2: 需求测试流程统计接口（后续，依赖 requirement-flow 模块上线）

### 5. 后端开发
- [ ] 5.1 创建 `RequirementFlowStatisticsController`（路径 `/requirement-flow/statistics/`）
- [ ] 5.2 实现各阶段需求数量分布接口 `GET .../stage-distribution/{projectId}`
- [ ] 5.3 实现各阶段平均耗时接口 `GET .../stage-duration/{projectId}`
- [ ] 5.4 实现计划与实际偏差统计接口 `GET .../plan-deviation/{projectId}`
- [ ] 5.5 实现缺陷密度统计接口 `GET .../defect-density/{projectId}`
- [ ] 5.6 实现评审通过率趋势接口 `GET .../review-pass-rate/{projectId}`
- [ ] 5.7 创建 `WorkflowStatisticsDTO` 等 DTO 类
- [ ] 5.8 创建 `RequirementFlowStatisticsService` 及 Mapper 查询

### 6. 权限与认证
- [ ] 6.1 新增权限点 `PROJECT_WORKFLOW_STATISTICS:READ`
- [ ] 6.2 确保 API Key 认证链覆盖 `/requirement-flow/statistics/**`
- [ ] 6.3 为数据大屏专用账号授予新权限

### 7. 大屏端扩展
- [ ] 7.1 对接新增的 5 个 workflow 统计接口
- [ ] 7.2 新增 workflow 维度可视化组件

---

## 工时估算

| 阶段 | 内容 | 时间 |
|------|------|------|
| V1 | API Key 生成 + 大屏端对接开发 | 1 ~ 2 周（大屏端开发） |
| V2 | 后端 workflow 统计接口 | 0.5 ~ 1 周 |
| V2 | 大屏端扩展 | 0.5 周 |

---

## 依赖关系

```
V1: API Key 生成 → 大屏端签名实现 → 逐个接口对接 → 验证
                         ↓
              需求测试流程模块上线
                         ↓
V2: workflow 统计后端 → 权限配置 → 大屏端扩展
```
