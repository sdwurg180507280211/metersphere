# Requirements Document

## Introduction

本需求文档定义了 analytics-stat 模块包名结构标准化改造的需求。当前 analytics-stat 模块使用 `io.metersphere.analyticsstat` 作为基础包名，与项目中其他所有模块（api-test、test-track、system-setting 等）使用的 `io.metersphere` 标准不一致。这种不一致导致 Spring Boot 的组件扫描无法发现 SDK 中的 Bean（如 BaseSystemSettingService），因为 @SpringBootApplication 默认只扫描当前包及其子包。

本改造将统一 analytics-stat 模块的包名结构，使其符合项目标准，确保 Spring 容器能够正确扫描和注册所有必需的 Bean。

## Glossary

- **Analytics_Stat_Module**: 分析统计微服务模块，提供综合查询、SQL查询台、统计图表等功能
- **Base_Package**: Java 包的根路径，用于 Spring Boot 组件扫描的起点
- **Component_Scanning**: Spring Boot 自动扫描并注册 Bean 的机制
- **SDK**: framework/sdk-parent/sdk 中的共享类库，包含所有模块共用的工具类和服务
- **Application_Class**: Spring Boot 应用的启动类，标注 @SpringBootApplication 注解
- **Controller**: REST API 端点类，处理 HTTP 请求
- **Config_Class**: Spring 配置类，标注 @Configuration 注解
- **Standard_Package**: 项目标准包名 `io.metersphere`，其他所有模块使用的基础包名
- **Legacy_Package**: 当前 analytics-stat 使用的非标准包名 `io.metersphere.analyticsstat`

## Requirements

### Requirement 1: 包名结构标准化

**User Story:** 作为开发人员，我希望 analytics-stat 模块使用与其他模块一致的包名结构，以便 Spring Boot 能够正确扫描所有组件。

#### Acceptance Criteria

1. THE Analytics_Stat_Module SHALL use `io.metersphere` as the Base_Package
2. WHEN the Application_Class is located at `io.metersphere`, THEN Component_Scanning SHALL discover all beans in SDK
3. THE Application_Class SHALL be named `AnalyticsStatApplication` and located at `io.metersphere.AnalyticsStatApplication`
4. THE Controller classes SHALL be located under `io.metersphere.controller` package
5. THE Config_Class classes SHALL be located under `io.metersphere.config` package

### Requirement 2: 文件迁移完整性

**User Story:** 作为开发人员，我希望所有 Java 源文件都被正确迁移到新的包结构，以确保没有遗漏的文件。

#### Acceptance Criteria

1. WHEN migrating files, THE system SHALL move all Java files from Legacy_Package to Standard_Package
2. WHEN a file is moved, THE system SHALL update its package declaration to match the new location
3. WHEN a file is moved, THE system SHALL update all import statements that reference the old package
4. THE system SHALL preserve the subdirectory structure (controller, config, etc.) under the new Base_Package
5. WHEN migration is complete, THE Legacy_Package directory SHALL be empty or removed

### Requirement 3: Import 语句更新

**User Story:** 作为开发人员，我希望所有 import 语句都被正确更新，以确保代码能够编译通过。

#### Acceptance Criteria

1. WHEN a package declaration is updated, THE system SHALL update all import statements in the same file
2. WHEN other files import classes from Legacy_Package, THE system SHALL update those import statements to reference Standard_Package
3. THE system SHALL preserve imports from SDK and other external packages unchanged
4. WHEN all updates are complete, THE codebase SHALL contain no references to `io.metersphere.analyticsstat`

### Requirement 4: 功能保持不变

**User Story:** 作为用户，我希望包名重构后所有功能保持正常工作，不受任何影响。

#### Acceptance Criteria

1. WHEN the refactoring is complete, THE Analytics_Stat_Module SHALL start successfully
2. WHEN the module starts, THE system SHALL register all Controller beans
3. WHEN the module starts, THE system SHALL register all Config_Class beans
4. WHEN the module starts, THE system SHALL discover and inject BaseSystemSettingService from SDK
5. THE REST API endpoints SHALL remain accessible at their original paths (e.g., `/analytics-stat/health`)
6. THE qiankun micro-frontend integration SHALL continue to function correctly

### Requirement 5: 与其他模块保持一致

**User Story:** 作为架构师，我希望 analytics-stat 模块的包结构与其他模块（api-test、test-track）完全一致，以便于维护和理解。

#### Acceptance Criteria

1. THE Analytics_Stat_Module package structure SHALL match the pattern used by api-test module
2. THE Analytics_Stat_Module package structure SHALL match the pattern used by test-track module
3. THE Analytics_Stat_Module package structure SHALL match the pattern used by system-setting module
4. WHEN comparing Application_Class files, THE package declaration SHALL be consistent across all modules
5. WHEN comparing directory structures, THE subdirectory organization SHALL be consistent across all modules

### Requirement 6: 构建和测试验证

**User Story:** 作为 DevOps 工程师，我希望重构后的代码能够通过 Maven 构建和测试，确保没有编译错误。

#### Acceptance Criteria

1. WHEN running `mvn clean compile`, THE build SHALL succeed without errors
2. WHEN running `mvn clean package`, THE build SHALL produce a valid JAR file
3. WHEN running the application, THE Spring Boot context SHALL load successfully
4. WHEN checking diagnostics, THE IDE SHALL report no compilation errors
5. THE MyBatis Mapper scanning SHALL continue to work correctly with the new package structure

### Requirement 7: 文档和注释更新

**User Story:** 作为开发人员，我希望代码注释和文档能够反映新的包结构，避免混淆。

#### Acceptance Criteria

1. WHEN package names are updated, THE Javadoc comments SHALL be reviewed for accuracy
2. WHEN file paths are mentioned in comments, THE system SHALL update them to reflect new locations
3. THE Application_Class comments SHALL accurately describe the module's package structure
4. WHEN documentation references package names, THE system SHALL update those references

### Requirement 8: 回滚能力

**User Story:** 作为项目经理，我希望在出现问题时能够快速回滚到原始状态，降低风险。

#### Acceptance Criteria

1. WHEN starting the refactoring, THE system SHALL be under version control
2. WHEN changes are made, THE system SHALL commit changes in logical groups
3. WHEN a problem is detected, THE system SHALL be able to revert to the previous state
4. THE refactoring process SHALL be documented with clear steps for rollback
