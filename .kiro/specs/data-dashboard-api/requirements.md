# 需求文档：数据大屏 API 数据对接

## 简介

数据大屏是一个独立的外部可视化系统，需要通过 HTTP API 从 MeterSphere 定时拉取测试跟踪模块的统计数据，在大型展示屏上呈现测试质量全貌。

与普通业务接口不同，数据大屏的调用方是**外部系统**而非登录用户，因此不依赖 Session/Cookie 认证，而是通过 **API Key + 签名** 的方式进行身份验证。现有 `TrackController` 中已有 5 个统计类接口可以直接被数据大屏复用，后续随着需求测试流程（requirement-flow）模块建设，需要新增 workflow 维度的统计接口。

**文档版本**：V1.0
**修订日期**：2026年6月1日

---

## 术语表

| 术语 | 定义 |
|------|------|
| **数据大屏** | 外部独立可视化系统，通过 API 定时拉取 MeterSphere 数据并展示 |
| **API Key** | 一对 `accessKey` + `secretKey`，由 MeterSphere 用户生成，用于外部系统身份认证 |
| **签名** | 使用 `secretKey` 对 `accessKey\|timestamp` 做 AES 加密得到的结果，放在 HTTP Header 中 |
| **TrackController** | 测试跟踪模块首页统计 Controller，路径前缀 `/track` |
| **需求测试流程** | planning 中的新模块，围绕需求规格书串联测试全流程（见 `platform-transformation` spec） |

---

## 需求

### 需求 1: API Key 认证

**用户故事：** 作为数据大屏运维人员，我希望使用 API Key 调用 MeterSphere 接口，而不是用个人账号密码登录，以便系统间自动化对接。

#### 验收标准

1. THE 系统 SHALL 支持通过 HTTP Header 传递 `accessKey` 和 `signature` 进行认证
2. THE `signature` SHALL 为 `AES(secretKey, accessKey\|timestamp)` 的加密结果，其中 `accessKey` 同时作为 AES 的 IV
3. THE 系统 SHALL 校验时间戳在 30 分钟内，超时返回 401
4. THE 系统 SHALL 校验 `accessKey` 状态为 ACTIVE，已禁用或删除的 Key 不可用
5. EACH 用户 SHALL 最多生成 5 个 API Key
6. THE API Key 认证通过后 SHALL 以该 Key 所属用户的身份执行后续权限校验

---

### 需求 2: 用例数量统计接口

**用户故事：** 作为数据大屏，我希望获取指定项目下的测试用例数量分布（按优先级、评审状态、本周新增），以便在首页展示项目测试用例总览。

#### 验收标准

1. THE 系统 SHALL 提供 `GET /track/count/{projectId}` 接口
2. THE 响应 SHALL 包含 P0/P1/P2/P3 各级用例数量
3. THE 响应 SHALL 包含评审状态统计（通过/未通过/未评审）
4. THE 响应 SHALL 包含本周新增用例数和评审通过率
5. THE 接口 SHALL 要求权限 `PROJECT_TRACK_HOME:READ`

---

### 需求 3: 关联用例覆盖率统计接口

**用户故事：** 作为数据大屏，我希望获取项目的用例覆盖情况（API 用例、场景用例、性能用例等关联数量），以便展示测试覆盖度。

#### 验收标准

1. THE 系统 SHALL 提供 `GET /track/relevance/count/{projectId}` 接口
2. THE 响应 SHALL 包含各类用例关联数量：API 用例、场景用例、性能用例、UI 用例
3. THE 响应 SHALL 包含已覆盖数、未覆盖数、覆盖率百分比
4. THE 响应 SHALL 包含本周新增关联数
5. THE 接口 SHALL 要求权限 `PROJECT_TRACK_HOME:READ`

---

### 需求 4: 测试计划遗留缺陷统计接口

**用户故事：** 作为数据大屏，我希望获取项目的缺陷统计数据（未关闭数、各状态分布、各测试计划缺陷数），以便展示质量风险状况。

#### 验收标准

1. THE 系统 SHALL 提供 `GET /track/bug/count/{projectId}` 接口
2. THE 响应 SHALL 包含缺陷总数、未关闭缺陷数和未关闭率
3. THE 响应 SHALL 包含新增/已解决/已拒绝/未知状态缺陷数量
4. THE 响应 SHALL 包含本周新增缺陷数和缺陷用例比
5. THE 响应 SHALL 包含各测试计划的缺陷分布列表
6. THE 接口 SHALL 要求权限 `PROJECT_TRACK_HOME:READ`

---

### 需求 5: 用例责任人分布统计接口

**用户故事：** 作为数据大屏，我希望获取项目下按责任人维度统计的功能用例和关联用例数量，以便展示人员工作负载分布。

#### 验收标准

1. THE 系统 SHALL 提供 `GET /track/case/bar/{projectId}` 接口
2. THE 响应 SHALL 为柱状图数据列表，每条包含 x 轴标签（责任人）和 y 轴数值（用例数）
3. THE 数据 SHALL 按 `FUNCTIONCASE` 和 `RELEVANCECASE` 分组
4. THE 接口 SHALL 要求权限 `PROJECT_TRACK_HOME:READ`

---

### 需求 6: 失败用例统计接口

**用户故事：** 作为数据大屏，我希望获取最近 7 天内执行失败的用例列表，以便追踪高频失败用例并推动修复。

#### 验收标准

1. THE 系统 SHALL 提供 `GET /track/failure/case/about/plan/{projectId}/{versionId}/{pageSize}/{goPage}` 接口
2. THE `versionId` 参数为 `default` 时 SHALL 视为不过滤版本
3. THE 响应 SHALL 包含分页的失败用例列表（用例名称、所属测试计划、失败次数、用例类型）
4. THE 时间范围 SHALL 默认最近 7 天
5. THE 接口 SHALL 要求权限 `PROJECT_TRACK_HOME:READ`

---

### 需求 7: 数据大屏调用规范

**用户故事：** 作为数据大屏开发人员，我希望有明确的接口调用规范文档，以便正确对接 MeterSphere。

#### 验收标准

1. THE 规范 SHALL 包含 API Key 生成步骤（由 MeterSphere 管理员在系统中生成）
2. THE 规范 SHALL 包含签名生成算法（Java/Python/JavaScript 等语言示例）
3. THE 规范 SHALL 包含所有可用接口的请求/响应示例
4. THE 规范 SHALL 包含定时拉取的建议频率（建议 5-10 分钟一次）
5. THE 规范 SHALL 包含错误码说明

---

### 需求 8: 需求测试流程统计接口（规划）

**用户故事：** 作为数据大屏，我希望在需求测试流程模块上线后，获取 workflow 维度的统计数据（各阶段需求数量、平均耗时、缺陷密度等）。

#### 验收标准

1. THE 系统 SHALL 预留需求测试流程统计接口路径 `/requirement-flow/statistics/`
2. THE 接口 SHALL 覆盖以下统计维度（后续 spec 细化）：
   - 各阶段需求数量分布
   - 阶段平均耗时
   - 计划与实际偏差统计
   - 缺陷密度（缺陷数/用例数）
   - 评审通过率
3. THE 接口 SHALL 使用与 TrackController 相同的 API Key 认证方式
4. THE 接口 SHALL 要求对应 workflow 模块的查看权限

---

## 安全要求

1. 签名算法采用 AES/CBC/PKCS5Padding，`secretKey` 为 128-bit 密钥，`accessKey` 为 128-bit IV
2. 签名原文格式为 `accessKey|timestamp`（管道符分隔），时间戳为毫秒值
3. 时间戳窗口为 ±30 分钟，防止重放攻击
4. API Key 一经生成只展示一次 `secretKey`，后续只能查看 `accessKey`
5. 用户最多持有 5 个有效 API Key
6. 可对单个 Key 执行启用/禁用/删除操作

---

## 性能要求

| 指标 | 目标值 |
|------|--------|
| 单次统计接口响应时间 | < 3 秒 |
| 数据大屏定时轮询间隔 | 5-10 分钟（由大屏端控制） |
| 并发大屏数量 | 支持 3-5 个数据大屏同时轮询 |

---

## 约束

1. 接口仅在 MeterSphere 有权限的项目范围内返回数据（以 API Key 所属用户的权限为准）
2. 接口不支持跨工作空间查询，需按 projectId 逐一拉取
3. V1 仅复用现有 TrackController 接口，不新增独立的大屏聚合接口
