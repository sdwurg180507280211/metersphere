# 服务重启说明

## 当前状态

✅ **后端代码已完成编译**（无语法错误）  
✅ **前端代码已完成编译**（无语法错误）  
✅ **Mapper XML 文件位置已修正**（从 resources 移动到 java 目录）  
⚠️ **服务需要重启**以加载新的 Mapper XML 文件

## 问题根因

**Mapper XML 文件位置错误**：

- ❌ 错误位置：`workstation/backend/src/main/resources/mapper/ext/ExtAdvancedSearchMapper.xml`
- ✅ 正确位置：`workstation/backend/src/main/java/io/metersphere/base/mapper/ext/ExtAdvancedSearchMapper.xml`

**MeterSphere 项目的特殊配置**：

- 该项目将 Mapper XML 文件放在 `src/main/java` 目录下，而不是标准的 `src/main/resources` 目录
- 这是通过 Maven 的 `resources` 配置实现的，将 `src/main/java` 下的 XML 文件也复制到 `target/classes`
- 参考其他模块（如 SDK）的 Mapper 文件位置：`framework/sdk-parent/sdk/src/main/java/io/metersphere/base/mapper/*.xml`

## 为什么需要重启？

1. **MyBatis Mapper 加载时机**：MyBatis 在应用启动时扫描并加载所有 Mapper XML 文件
2. **文件位置已修正**：XML 文件已移动到正确位置，需要重启才能被 MyBatis 识别
3. **Service 层修改**：`AdvancedSearchService` 的方法签名已修复，需要重启生效

## 如何重启服务

### 方法 1：通过 IDE 重启（推荐）

1. 在 IntelliJ IDEA 中找到 `WorkstationApplication` 运行配置
2. 点击停止按钮（红色方块）
3. 等待服务完全停止
4. 点击运行按钮（绿色三角形）重新启动

### 方法 2：通过命令行重启

```bash
# 1. 找到 workstation 进程
ps aux | grep WorkstationApplication

# 2. 杀死进程（替换 PID 为实际进程号）
kill -9 <PID>

# 3. 重新启动（在 IDE 中运行或使用 Maven）
mvn spring-boot:run -pl workstation/backend
```

## 重启后验证

### 1. 检查服务启动日志

查看日志中是否有以下内容：

```
Mapped "{[/workstation/advanced-search/query/{goPage}/{pageSize}],methods=[POST]}"
```

### 2. 测试接口

```bash
# 确保已登录并获取 token
curl -X POST "http://localhost:8007/workstation/advanced-search/query/1/20" \
  -H "Content-Type: application/json" \
  -H "X-AUTH-TOKEN: YOUR_SESSION_ID" \
  -H "CSRF-TOKEN: YOUR_CSRF_TOKEN" \
  -d '{
    "module": "TEST_CASE",
    "workspaceIds": [],
    "projectIds": [],
    "useJQL": false,
    "combine": {},
    "filters": {},
    "orders": [{"name": "update_time", "type": "desc"}]
  }'
```

### 3. 检查 MyBatis Mapper 加载

查看启动日志中是否有：

```
Mapped statement collection already contains value for io.metersphere.base.mapper.ext.ExtAdvancedSearchMapper.queryTestCases
Mapped statement collection already contains value for io.metersphere.base.mapper.ext.ExtAdvancedSearchMapper.queryIssues
Mapped statement collection already contains value for io.metersphere.base.mapper.ext.ExtAdvancedSearchMapper.queryTestPlans
Mapped statement collection already contains value for io.metersphere.base.mapper.ext.ExtAdvancedSearchMapper.queryTestCaseReviews
```

## 预期结果

重启后，之前的 MyBatis binding 错误应该消失：

- ❌ 之前：`Invalid bound statement (not found): io.metersphere.base.mapper.ext.ExtAdvancedSearchMapper.queryIssues`
- ✅ 现在：接口正常返回查询结果

## 文件位置总结

```
workstation/backend/
├── src/main/java/
│   └── io/metersphere/
│       ├── workstation/
│       │   ├── controller/
│       │   │   └── AdvancedSearchController.java
│       │   └── service/
│       │       └── AdvancedSearchService.java
│       └── base/mapper/
│           └── ext/
│               ├── ExtAdvancedSearchMapper.java      ← Mapper 接口
│               └── ExtAdvancedSearchMapper.xml       ← Mapper XML（正确位置）
└── target/classes/
    └── io/metersphere/base/mapper/ext/
        └── ExtAdvancedSearchMapper.xml               ← 编译后的位置
```

## 注意事项

1. **确保编译完成**：重启前确保 `mvn clean compile` 已成功执行
2. **检查端口占用**：如果重启失败，检查 8007 端口是否被占用
3. **查看完整日志**：如果仍有问题，查看完整的启动日志以定位错误
4. **不要删除 resources 目录下的文件**：虽然 Mapper XML 在 java 目录，但 resources 目录下可能有其他配置文件

## 下一步

重启成功后，继续完成以下任务：

1. 测试所有 4 个模块的查询功能（测试用例、缺陷、测试计划、用例评审）
2. 实现详情查询功能
3. 实现自定义字段查询
4. 实现 Excel 导出功能
5. 完善前端组件的业务逻辑
