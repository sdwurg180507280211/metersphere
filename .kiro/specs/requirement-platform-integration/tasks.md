# 任务清单：全流程平台对接

> **项目周期**: 2026年3月10日 - 2026年6月30日
> **当前周次**: W07 (2026-04-27 ~ 2026-05-01)
> **开发人员**: 单人开发、部署、联调

---

## 阶段 1：方案冻结 (W01-W02: 03/10 ~ 03/27)

### 1.1 范围与需求确认
- [ ] 1.1.1 确认 V1 范围边界（必须完成 vs 本期不做）
- [ ] 1.1.2 冻结字段清单（dmpNum、name1、operationType 等核心字段）
- [ ] 1.1.3 确认消息方向（双向 RocketMQ 通信）
- [ ] 1.1.4 明确 `actName` 与 `operationType` 口径区分
- [ ] 1.1.5 书面确认"本期不做项"

### 1.2 详细设计
- [ ] 1.2.1 设计需求池主表结构（requirement_pool）
- [ ] 1.2.2 设计同步日志表（如需要）
- [ ] 1.2.3 定义状态流转规则（PENDING → CREATED → CANCELLED）
- [ ] 1.2.4 定义幂等规则（dmpNum + eventTime）
- [ ] 1.2.5 梳理回传字段清单
- [ ] 1.2.6 梳理异常场景清单（重复消息、乱序消息、取消需求等）

### 1.3 外部对接确认
- [ ] 1.3.1 获取需求平台消息样例
- [ ] 1.3.2 确认 RocketMQ Topic 命名规范
- [ ] 1.3.3 确认消费组命名规范
- [ ] 1.3.4 确认字段命名和数据类型（name1、createUser1 等）

---

## 阶段 2：后端主链路 (W03-W06: 03/30 ~ 04/24)

### 2.1 后端骨架搭建 (W03)
- [ ] 2.1.1 添加 RocketMQ Maven 依赖（test-track/backend/pom.xml）
- [ ] 2.1.2 配置 RocketMQ 连接信息（metersphere.properties）
- [x] 2.1.3 创建数据库 Migration 文件（V19__requirement_pool.sql）
- [x] 2.1.4 创建 RequirementPool 实体类
- [x] 2.1.5 创建 RequirementPoolMapper 接口和 XML

### 2.2 需求同步主链路开发 (W04)
- [ ] 2.2.1 创建 RequirementSyncMessage DTO
- [ ] 2.2.2 实现 RequirementSyncConsumer MQ 消费者
- [ ] 2.2.3 实现 RequirementSyncService 同步业务逻辑
- [ ] 2.2.4 实现新增/更新/取消处理的落库逻辑
- [ ] 2.2.5 保留原始消息和 trace 信息

### 2.3 幂等与审计 (W05)
- [ ] 2.3.1 实现幂等检查逻辑（dmpNum + eventTime）
- [ ] 2.3.2 实现重复消费防护
- [ ] 2.3.3 实现版本/时间比较逻辑（旧消息过滤）
- [ ] 2.3.4 补充同步审计字段（last_sync_time、trace_id）
- [ ] 2.3.5 本地模拟重复消息和乱序消息测试

### 2.4 查询接口与部署预演 (W06)
- [x] 2.4.1 创建 RequirementPoolRequest.java（page.condition 格式）
- [x] 2.4.2 实现需求池高级搜索查询接口（/requirement-pool/list）
- [x] 2.4.3 实现 RequirementPoolMapper.xml SQL 处理（filters、combine、orders）
- [ ] 2.4.4 整理部署步骤和配置项清单
- [ ] 2.4.5 做一次非正式环境部署预演

---

## 阶段 3：功能闭环 (W07-W10: 04/27 ~ 05/22)

### 3.1 需求池页面开发 (W07)
- [x] 3.1.1 创建需求池列表页（`/track/requirement-pool/list`）
- [x] 3.1.2 实现状态列筛选（使用 ms-table-column 的 :filters 属性，模仿测试计划状态字段）
- [x] 3.1.3 定义状态筛选器 statusFilters（PENDING/CREATED/CANCELLED）
- [x] 3.1.4 实现列表字段展示（编号、名称、状态、操作）
- [x] 3.1.5 设置默认排序（按创建时间降序，最新同步的最先显示）
- [x] 3.1.6 左上角按钮改为"创建需求"
- [x] 3.1.7 新增创建需求弹窗和前端提交流程
- [x] 3.1.8 实现创建需求接口（`POST /requirement-pool/add`）
- [ ] 3.1.9 跑通"消息入池 → 页面可见"主链路

### 3.2 创建测试计划流程开发 (W08)
- [x] 3.2.1 复用测试计划页面的 TestPlanEdit 弹窗组件（不创建单独的 CreatePlanDialog）
- [ ] 3.2.2 实现从需求池创建测试计划接口（`/requirement-pool/create-test-plan`）
- [ ] 3.2.3 实现需求编号绑定逻辑（requirementNumber = dmpNum）
- [ ] 3.2.4 实现计划名称自动填充（只读，不可编辑）
- [ ] 3.2.5 实现需求池状态更新（PENDING → CREATED）
- [ ] 3.2.6 实现并发控制（数据库唯一索引）

### 3.3 测试计划扩展开发 (W09)
- [x] 3.3.1 扩展 test_plan 表（添加 requirement_number 字段）
- [x] 3.3.2 添加 uk_requirement_number 唯一索引
- [x] 3.3.3 在 TestPlan 实体类添加 requirementNumber 字段
- [ ] 3.3.4 修改直接创建测试计划逻辑（requirementNumber = null）
- [x] 3.3.5 在测试计划列表展示关联需求编码（TestPlanList.vue 添加 requirementNumber 列，search-components.js 添加搜索配置，ExtTestPlanMapper.xml 添加 combine 条件）

### 3.4 回传能力开发 (W10)
- [ ] 3.4.1 创建 RequirementCallbackMessage DTO
- [ ] 3.4.2 实现 RequirementCallbackProducer MQ 生产者
- [ ] 3.4.3 明确回传触发点（测试计划状态变更）
- [ ] 3.4.4 实现状态映射逻辑（TestPlanStatus → 回传状态）
- [ ] 3.4.5 实现回传失败记录（CallbackRecord）

---

## 阶段 4：异常处理与完善 (W11: 05/18 ~ 05/22)

### 4.1 异常路径补齐
- [ ] 4.1.1 处理取消需求场景（CANCELLED 消息处理）
- [ ] 4.1.2 处理旧消息过滤（eventTime 比较）
- [ ] 4.1.3 处理坏数据兜底（字段缺失、格式错误）
- [ ] 4.1.4 处理空字段校验（dmpNum、name1 必填检查）
- [ ] 4.1.5 整理失败重试策略

### 4.2 监控与日志
- [ ] 4.2.1 补充消息消费日志（traceId、处理结果）
- [ ] 4.2.2 补充测试计划创建日志（操作人、时间、需求编号）
- [ ] 4.2.3 补充状态回传日志（回传时间、内容、成功与否）
- [ ] 4.2.4 实现通过 traceId 查询完整链路

### 4.3 联调准备
- [ ] 4.3.1 整理第一轮内网联调所需部署包
- [ ] 4.3.2 整理配置清单和变更说明
- [ ] 4.3.3 整理问题清单第一版

---

## 阶段 5：内网联调 (W12-W14: 05/25 ~ 06/12)

### 5.1 第一轮内网联调 (W12: 05/25 ~ 05/29)
- [ ] 5.1.1 完成 VDI 环境首次部署
- [ ] 5.1.2 验证 RocketMQ 双向通信（消费 + 生产）
- [ ] 5.1.3 验证网络连通性（NameServer、Broker）
- [ ] 5.1.4 验证 Topic 和消费组配置
- [ ] 5.1.5 跑通主流程（同步入池 → 创建计划 → 回传）
- [ ] 5.1.6 记录环境问题和配置差异

### 5.2 联调修复与初始化 (W13: 06/01 ~ 06/05)
- [ ] 5.2.1 集中修复第一轮联调问题
- [ ] 5.2.2 整理历史数据初始化方案
- [ ] 5.2.3 编写历史数据导入脚本
- [ ] 5.2.4 做一轮修复后验证
- [ ] 5.2.5 准备部署包第二版

### 5.3 第二轮内网联调 (W14: 06/08 ~ 06/12)
- [ ] 5.3.1 验证第一轮问题修复结果
- [ ] 5.3.2 打通"同步 → 创建计划 → 回传"完整闭环
- [ ] 5.3.3 验证历史初始化流程
- [ ] 5.3.4 确认上线配置、参数、脚本定稿
- [ ] 5.3.5 整理剩余问题清单（必须为少量低风险项）

---

## 阶段 6：验收与上线 (W15-W17: 06/15 ~ 06/30)

### 6.1 验收与回归 (W15: 06/15 ~ 06/19)
- [ ] 6.1.1 编写测试用例
- [ ] 6.1.2 编写操作说明文档
- [ ] 6.1.3 执行回归测试
- [ ] 6.1.4 问题收口（修复剩余缺陷）
- [ ] 6.1.5 准备待发布版本

### 6.2 上线准备 (W16: 06/22 ~ 06/26)
- [ ] 6.2.1 编写回滚方案
- [ ] 6.2.2 编写上线检查单（Checklist）
- [ ] 6.2.3 执行发版演练
- [ ] 6.2.4 预留缓冲处理突发问题
- [ ] 6.2.5 准备最终发布包

### 6.3 上线窗口 (W17: 06/29 ~ 06/30)
- [ ] 6.3.1 正式上线部署
- [ ] 6.3.2 执行冒烟验证
- [ ] 6.3.3 记录遗留问题和后续优化项
- [ ] 6.3.4 保留快速回退和排障窗口

---

## 开发步骤清单（详细）

| 步骤 | 任务 | 对应章节 | 负责人建议 | 预计工时 | 状态 |
|------|------|----------|-----------|----------|------|
| 1 | 配置 RocketMQ 连接信息和 Maven 依赖 | 9.1 | 开发者A | 0.5天 | ☐ |
| 2 | 创建数据库表（Migration SQL） | 9.2 | 开发者A | 0.5天 | ☑ |
| 3 | 创建 RequirementPool 实体类 | 9.3 | 开发者A | 0.5天 | ☑ |
| 4 | 创建 RequirementPoolMapper 接口和 XML（含高级搜索 SQL） | 9.3 | 开发者A | 0.5天 | ☑ |
| 5 | 创建消息 DTO（RequirementSyncMessage） | 9.4 | 开发者A | 0.5天 | ☐ |
| 6 | 实现 RocketMQ 消费者（RequirementSyncConsumer） | 9.4 | 开发者A | 1天 | ☐ |
| 7 | 实现同步服务（RequirementSyncService） | 9.5 | 开发者A | 1天 | ☐ |
| 8 | 创建查询 Request 类（RequirementPoolRequest.java） | 高级搜索 | 开发者A | 0.5天 | ☑ |
| 9 | 实现需求池高级搜索查询接口（/requirement-pool/list） | 高级搜索 | 开发者A | 0.5天 | ☑ |
| 10 | 创建高级搜索配置 REQUIREMENT_POOL_LIST | 高级搜索 | 开发者B | 1天 | ☑ |
| 11 | 实现需求池高级搜索页面（RequirementPoolList.vue） | 高级搜索 | 开发者B | 1.5天 | ☑ |
| 12 | 扩展 test_plan 表和服务（createFromRequirement） | 9.6 | 开发者B | 2天 | ☐ |
| 13 | 创建回传消息 DTO（RequirementCallbackMessage） | 9.7 | 开发者B | 0.5天 | ☐ |
| 14 | 实现状态回传生产者（RequirementCallbackProducer） | 9.7 | 开发者B | 1天 | ☐ |
| 15 | 在测试计划状态更新时触发回传 | 9.7 | 开发者B | 0.5天 | ☐ |

**关键依赖**:
- 步骤 1-4 必须顺序完成（基础设施）
- 步骤 5-7 依赖步骤 4（需要 Mapper）
- 步骤 8-10 可以和步骤 5-7 并行
- 步骤 11-14 依赖步骤 4（需要实体类），可以和步骤 5-10 并行

---

## 关键里程碑检查点

| 里程碑 | 截止时间 | 检查项 |
|--------|----------|--------|
| M1 | 3月27日 | 方案冻结、数据库设计完成、后端骨架可启动 |
| M2 | 4月24日 | 同步主链路可运行、幂等可用、部署预演完成 |
| M3 | 5月22日 | 页面可操作、创建计划跑通、回传可运行 |
| M4 | 6月12日 | 两轮联调完成、主流程闭环、配置定稿 |
| M5 | 6月26日 | 回归完成、上线材料齐备、演练通过 |
| M6 | 6月30日 | 正式上线、冒烟通过 |

---

## 每周工作节奏建议

考虑到 VDI 部署和 RocketMQ 联调成本较高，建议固定周节奏：

| 日期 | 工作内容 |
|------|----------|
| 周一、周二 | 集中开发新功能，不进行正式环境部署 |
| 周三 | 本地联调、自测、整理部署包和变更说明 |
| 周四 | 部署到 VDI，验证配置、消息链路和关键流程 |
| 周五 | 修复环境问题、补文档、做周收口，不开启新需求 |

---

## 风险应对

| 风险 | 应对策略 |
|------|----------|
| RocketMQ 联调依赖外部 | 提前在 W02 确认消息样例和 Topic 权限 |
| 字段契约频繁变更 | W03 后冻结字段，变更需走审批 |
| 第一轮联调未通过 | 立即停止新增需求，优先处理环境问题 |
| 单人开发缓冲有限 | 严格执行 25% 缓冲预留，6月不接新需求 |

---

## 依赖关系图

```
阶段1: 方案冻结
    ↓
阶段2: 后端主链路
    ├── 2.1 骨架搭建
    ├── 2.2 同步主链路 ←→ 2.3 幂等审计
    └── 2.4 查询接口
    ↓
阶段3: 功能闭环
    ├── 3.1 页面开发 ←→ 3.2 创建计划流程
    ├── 3.3 测试计划扩展
    └── 3.4 回传能力
    ↓
阶段4: 异常处理
    ↓
阶段5: 内网联调 (两轮)
    ↓
阶段6: 验收上线
```

---

## 备注

- 所有任务完成后需在对应 checkbox 打勾 `[x]`
- 遇到阻塞问题需立即更新风险状态
- 每周五更新任务进度

---

## 步骤验证方法

### 步骤1-2（环境配置）验证:
```bash
# 验证 Maven 依赖是否正确引入
mvn dependency:tree | grep rocketmq

# 验证配置文件是否正确
cat /opt/metersphere/conf/metersphere.properties | grep rocketmq
```

### 步骤2（数据库表）验证:
```sql
-- 验证表是否创建成功
SHOW TABLES LIKE 'requirement_pool';
DESC requirement_pool;

-- 验证索引是否创建
SHOW INDEX FROM requirement_pool;

-- 验证 test_plan 表是否添加了新字段
DESC test_plan;
```

### 步骤3-4（实体类和Mapper）验证:
```java
// 编写单元测试
@Test
public void testInsert() {
    RequirementPool pool = new RequirementPool();
    pool.setId(UUID.randomUUID().toString());
    pool.setDmpNum("TEST-001");
    pool.setRequirementName("测试需求");
    pool.setPoolStatus("PENDING");
    pool.setCreatedAt(System.currentTimeMillis());
    pool.setUpdatedAt(System.currentTimeMillis());

    int result = requirementPoolMapper.insert(pool);
    Assert.assertEquals(1, result);
}
```

### 步骤5-7（消息消费）验证:
```bash
# 1. 查看消费者是否启动成功
docker logs test-track | grep "RequirementSyncConsumer"

# 2. 手动发送测试消息（使用 RocketMQ 控制台或命令行工具）
# 访问 http://10.12.105.254:19876 进入控制台
# Topic: topic-requirement-to-metersphere
# 消息体示例：
{
  "dmpNum": "TEST-001",
  "name1": "测试需求",
  "operationType": "CREATED",
  "reqManagerName": "张三",
  "createTime": 1710234567000,
  "eventTime": 1710234567000,
  "traceId": "test-trace-001"
}

# 3. 验证数据是否写入数据库
SELECT * FROM requirement_pool WHERE dmp_num = 'TEST-001';
```

### 步骤8-10（需求池接口）验证:
```bash
# 使用 curl 测试接口
# 1. 测试列表查询
curl -X POST http://localhost:8001/requirement-pool/list \
  -H "Content-Type: application/json" \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "poolStatus": "PENDING"
  }'

# 2. 测试详情查询
curl http://localhost:8001/requirement-pool/TEST-001
```

---

## 常见问题和解决方案

### 问题1: RocketMQ 消费者启动失败
```
错误: connect to 10.12.105.254:9876 failed
解决: 检查 RocketMQ 服务是否启动，网络是否可达
验证: nc -zv 10.12.105.254 9876
```

### 问题2: 消息消费后数据库没有数据
```
原因: 可能是幂等判断拦截了，或者事务回滚
排查:
1. 查看日志是否有异常
2. 检查 eventTime 是否正确
3. 验证数据库连接是否正常
```

### 问题3: 创建测试计划时提示"需求已创建测试计划"
```
原因: requirement_pool.linked_plan_id 不为空
解决: 检查数据库中该需求的状态
SELECT pool_status, linked_plan_id FROM requirement_pool WHERE dmp_num = 'xxx';
```

### 问题4: test_plan 表插入失败，提示唯一索引冲突
```
错误: Duplicate entry 'REQ-001' for key 'uk_requirement_number'
原因: 该需求编号已经关联了测试计划
解决: 先查询是否已存在，或者在业务层做好幂等控制
```

---

## 调试技巧

### 1. 开启详细日志
```properties
# 在 application.properties 中添加
logging.level.io.metersphere.track=DEBUG
logging.level.org.apache.rocketmq=INFO
```

### 2. 查看 RocketMQ 消息消费情况
```bash
# 访问 RocketMQ 控制台
http://10.12.105.254:19876

# 查看 Consumer 消费进度
# 查看 Topic 消息堆积情况
```

### 3. 数据库调试
```sql
-- 查看最近同步的需求
SELECT * FROM requirement_pool ORDER BY last_sync_time DESC LIMIT 10;

-- 查看某个需求的完整信息
SELECT * FROM requirement_pool WHERE dmp_num = 'xxx'\G

-- 查看关联的测试计划
SELECT tp.* FROM test_plan tp
JOIN requirement_pool rp ON tp.id = rp.linked_plan_id
WHERE rp.dmp_num = 'xxx';
```

---

## 开发最佳实践

1. **先本地测试，再联调** - 用单元测试验证 Mapper 和 Service
2. **使用 traceId 追踪** - 每个消息都带上 traceId，方便排查问题
3. **日志要详细** - 关键步骤都要打日志（消息接收、数据写入、状态更新）
4. **异常要捕获** - 消费者中要 catch 异常，避免消息丢失
5. **数据要校验** - 必填字段要检查，避免空指针

---

## 推荐分配方案（2人协作）

**开发者A（负责需求同步和查询）**:
- 任务1: 创建数据库 Migration 文件（0.5天）
- 任务2: 创建 RequirementPool 实体类和 Mapper（1天）
- 任务3: 实现 RocketMQ 消费者和同步服务（2天）
- 任务4: 实现需求池查询服务和接口（1.5天）

**开发者B（负责测试计划扩展和回传）**:
- 等待任务2完成后开始
- 任务5: 扩展 test_plan 表和创建接口（2天）
- 任务6: 实现状态回传功能（1.5天）

**总工期: 约5-6天**
