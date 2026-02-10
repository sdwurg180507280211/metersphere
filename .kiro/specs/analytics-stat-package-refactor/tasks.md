# Implementation Plan: Analytics-Stat 包名结构标准化改造

## Overview

本实施计划将 analytics-stat 模块的包名从 `io.metersphere.analyticsstat` 重构为 `io.metersphere`，使其与项目中其他所有模块保持一致。重构过程采用增量式方法，每个步骤都包含验证环节，确保可以及时发现和解决问题。

## Tasks

- [x] 1. 准备工作和环境检查
  - 创建 Git 分支用于重构
  - 确认当前代码可以正常编译和运行
  - 备份当前状态
  - _Requirements: 8.1, 8.2_

- [x] 2. 迁移 Application 启动类
  - [x] 2.1 移动 AnalyticsStatApplication.java 到新包路径
    - 从 `io/metersphere/analyticsstat/` 移动到 `io/metersphere/`
    - 更新文件中的 package 声明为 `package io.metersphere;`
    - 保持所有 import 语句和注解不变
    - _Requirements: 1.1, 1.3, 2.1, 2.2_
  
  - [x] 2.2 验证 Application 类迁移
    - 运行 `mvn clean compile -pl analytics-stat/backend`
    - 检查编译是否成功
    - 使用 getDiagnostics 检查是否有编译错误
    - _Requirements: 6.1, 6.4_

- [x] 3. 迁移 Controller 层
  - [x] 3.1 移动 HealthController.java
    - 从 `io/metersphere/analyticsstat/controller/` 移动到 `io/metersphere/controller/`
    - 更新 package 声明为 `package io.metersphere.controller;`
    - 验证 @RequestMapping 路径保持为 `/analytics-stat`
    - _Requirements: 1.4, 2.1, 2.2, 4.5_
  
  - [x] 3.2 移动 SystemSettingController.java
    - 从 `io/metersphere/analyticsstat/controller/remote/` 移动到 `io/metersphere/controller/remote/`
    - 更新 package 声明为 `package io.metersphere.controller.remote;`
    - 确认 BaseSystemSettingService 的注入代码不变
    - _Requirements: 1.4, 2.1, 2.2, 4.4_
  
  - [x] 3.3 验证 Controller 层迁移
    - 运行 `mvn clean compile -pl analytics-stat/backend`
    - 检查编译是否成功
    - 使用 getDiagnostics 检查是否有编译错误
    - _Requirements: 6.1, 6.4_

- [x] 4. 迁移 Config 层
  - [x] 4.1 移动 WebMvcConfig.java
    - 从 `io/metersphere/analyticsstat/config/` 移动到 `io/metersphere/config/`
    - 更新 package 声明为 `package io.metersphere.config;`
    - 确认静态资源映射配置 `/analytics/**` 保持不变
    - _Requirements: 1.5, 2.1, 2.2, 4.6_
  
  - [x] 4.2 验证 Config 层迁移
    - 运行 `mvn clean compile -pl analytics-stat/backend`
    - 检查编译是否成功
    - 使用 getDiagnostics 检查是否有编译错误
    - _Requirements: 6.1, 6.4_

- [x] 5. 清理旧包结构
  - [x] 5.1 删除空的旧包目录
    - 删除 `analytics-stat/backend/src/main/java/io/metersphere/analyticsstat/` 目录
    - 确认该目录下已无任何文件
    - _Requirements: 2.5_
  
  - [x] 5.2 搜索并清理旧包名引用
    - 在整个 analytics-stat 模块中搜索 `io.metersphere.analyticsstat`
    - 确认没有任何残留引用
    - _Requirements: 3.4_

- [x] 6. Checkpoint - 编译和包结构验证
  - 运行完整编译：`mvn clean package -pl analytics-stat/backend -DskipTests`
  - 验证 JAR 文件成功生成
  - 检查新包结构是否正确：`ls -la analytics-stat/backend/src/main/java/io/metersphere/`
  - 确认旧包结构已删除：`ls -la analytics-stat/backend/src/main/java/io/metersphere/analyticsstat/` 应该不存在
  - _Requirements: 6.1, 6.2_

- [x] 7. Spring Boot 启动和集成测试
  - [x] 7.1 启动 analytics-stat 服务
    - 运行 `mvn spring-boot:run -pl analytics-stat/backend`
    - 检查启动日志，确认无错误
    - 验证 BaseSystemSettingService Bean 被成功注册和注入
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  
  - [x] 7.2 测试 REST API 端点
    - 测试健康检查接口：`curl http://localhost:8008/analytics-stat/health`
    - 验证返回正确的 JSON 响应
    - 确认 API 路径未发生变化
    - _Requirements: 4.5_
  
  - [x] 7.3 验证 qiankun 微前端集成
    - 检查静态资源是否可以正常访问：`curl http://localhost:8008/analytics/`
    - 确认 WebMvcConfig 的资源映射配置生效
    - _Requirements: 4.6_

- [x] 8. 与其他模块对比验证
  - [x] 8.1 对比 Application 类包名
    - 检查 api-test、test-track、system-setting 的 Application 类包名
    - 确认 analytics-stat 的 Application 类包名与它们一致
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  
  - [x] 8.2 对比目录结构
    - 对比各模块的 `src/main/java/io/metersphere/` 目录结构
    - 确认 analytics-stat 的结构符合项目标准
    - _Requirements: 5.5_

- [x] 9. 文档和注释更新
  - [x] 9.1 审查代码注释
    - 检查所有迁移文件的 Javadoc 注释
    - 更新注释中提到的包名或文件路径
    - _Requirements: 7.1, 7.2, 7.3_
  
  - [x] 9.2 更新相关文档
    - 检查 analytics-stat/README.md 是否需要更新
    - 更新文档中提到的包名引用
    - _Requirements: 7.4_

- [x] 10. 最终验证和提交
  - [x] 10.1 运行完整构建
    - 运行 `mvn clean install -pl analytics-stat/backend`
    - 确认构建成功，所有测试通过
    - _Requirements: 6.1, 6.2, 6.3_
  
  - [x] 10.2 代码审查检查清单
    - ✅ 所有文件已迁移到新包结构
    - ✅ 所有 package 声明已更新
    - ✅ 无旧包名引用残留
    - ✅ Maven 编译成功
    - ✅ Spring Boot 启动成功
    - ✅ 所有 Bean 正确注册和注入
    - ✅ REST API 端点正常访问
    - ✅ 与其他模块包结构一致
    - _Requirements: 所有需求_
  
  - [x] 10.3 提交代码变更
    - 提交 Git 变更，使用清晰的 commit message
    - Commit message 建议：`refactor(analytics-stat): 统一包名结构为 io.metersphere`
    - _Requirements: 8.3_

## Notes

### 重要提示

1. **增量验证**：每完成一个迁移步骤，立即进行编译验证，及时发现问题
2. **保持 API 不变**：所有 REST 端点路径必须保持不变，确保前端和其他服务的调用不受影响
3. **Spring 扫描范围**：重构的核心目标是让 Spring Boot 能够扫描到 SDK 中的 Bean，特别是 BaseSystemSettingService
4. **Git 版本控制**：每个主要步骤完成后提交一次，便于回滚
5. **测试优先**：在提交代码前，必须确保服务能够正常启动和运行

### 文件迁移映射表

| 原路径 | 新路径 |
|--------|--------|
| `io/metersphere/analyticsstat/AnalyticsStatApplication.java` | `io/metersphere/AnalyticsStatApplication.java` |
| `io/metersphere/analyticsstat/controller/HealthController.java` | `io/metersphere/controller/HealthController.java` |
| `io/metersphere/analyticsstat/controller/remote/SystemSettingController.java` | `io/metersphere/controller/remote/SystemSettingController.java` |
| `io/metersphere/analyticsstat/config/WebMvcConfig.java` | `io/metersphere/config/WebMvcConfig.java` |

### Package 声明变更

| 原 package 声明 | 新 package 声明 |
|----------------|----------------|
| `package io.metersphere.analyticsstat;` | `package io.metersphere;` |
| `package io.metersphere.analyticsstat.controller;` | `package io.metersphere.controller;` |
| `package io.metersphere.analyticsstat.controller.remote;` | `package io.metersphere.controller.remote;` |
| `package io.metersphere.analyticsstat.config;` | `package io.metersphere.config;` |

### 验证命令速查

```bash
# 编译验证
mvn clean compile -pl analytics-stat/backend

# 完整构建
mvn clean package -pl analytics-stat/backend -DskipTests

# 启动服务
mvn spring-boot:run -pl analytics-stat/backend

# 测试健康检查接口
curl http://localhost:8008/analytics-stat/health

# 搜索旧包名引用
grep -r "io.metersphere.analyticsstat" analytics-stat/backend/src/

# 检查新包结构
ls -la analytics-stat/backend/src/main/java/io/metersphere/

# 检查旧包结构（应该不存在）
ls -la analytics-stat/backend/src/main/java/io/metersphere/analyticsstat/
```

### 回滚方案

如果重构过程中遇到问题，可以通过以下方式回滚：

```bash
# 查看当前分支的提交历史
git log --oneline

# 回滚到指定提交
git reset --hard <commit-hash>

# 或者放弃所有未提交的更改
git checkout .
```

### 成功标准

重构成功的标志：
1. ✅ Maven 编译无错误和警告
2. ✅ Spring Boot 应用成功启动
3. ✅ 启动日志中显示 BaseSystemSettingService Bean 被注册
4. ✅ 健康检查接口返回正确响应
5. ✅ 代码中无 `io.metersphere.analyticsstat` 引用
6. ✅ 包结构与其他模块完全一致
