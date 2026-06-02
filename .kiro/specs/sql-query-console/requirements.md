# Requirements Document

## Introduction

SQL 查询台是一个为测试人员和管理员提供的专业数据库查询工具,允许用户在系统内直接执行 SQL 查询,无需使用 Navicat 等外部工具。该功能旨在提高工作效率,同时确保数据安全性和查询规范性。

## Glossary

- **SQL_Query_Console**: SQL 查询台,提供 SQL 编辑、执行和结果展示的完整功能组件
- **Query_Executor**: 查询执行器,负责接收 SQL 语句并在数据库中执行
- **Field_Helper**: 字段助手,提供表结构和字段信息的辅助工具
- **Result_Table**: 结果表格,用于展示查询结果的动态表格组件
- **SQL_Parser**: SQL 解析器,用于验证和分析 SQL 语句的安全性
- **Admin_User**: 管理员用户,具有系统管理权限的用户角色
- **Query_Result**: 查询结果,包含查询返回的数据行、列信息和执行元数据
- **Druid_SQL_Parser**: Druid SQL 解析器,阿里巴巴开源的 SQL 解析工具
- **MsCodeEdit**: MeterSphere 项目中已有的代码编辑器组件

## Requirements

### Requirement 1: SQL 编辑器

**User Story:** 作为测试人员,我希望有一个专业的 SQL 编辑器,以便我能够高效地编写和编辑 SQL 查询语句。

#### Acceptance Criteria

1. WHEN 用户打开 SQL 查询台 THEN THE SQL_Query_Console SHALL 显示一个支持语法高亮的 SQL 编辑器
2. WHEN 用户在编辑器中输入 SQL 关键字 THEN THE SQL_Query_Console SHALL 自动高亮显示这些关键字
3. WHEN 用户按下格式化按钮 THEN THE SQL_Query_Console SHALL 自动格式化 SQL 语句并保持语义不变
4. WHEN 用户按下 Cmd/Ctrl+Enter 快捷键 THEN THE SQL_Query_Console SHALL 执行当前编辑器中的 SQL 语句
5. WHEN 用户点击清空按钮 THEN THE SQL_Query_Console SHALL 清空编辑器内容并保持编辑器可用状态

### Requirement 2: 字段助手

**User Story:** 作为测试人员,我希望能够快速查看和插入表字段,以便减少记忆负担和拼写错误。

#### Acceptance Criteria

1. WHEN 用户打开 SQL 查询台 THEN THE Field_Helper SHALL 显示当前项目可查询的表列表
2. WHEN 用户展开某个表 THEN THE Field_Helper SHALL 显示该表的所有字段及其类型和注释
3. WHEN 用户单击某个字段 THEN THE Field_Helper SHALL 将该字段名插入到编辑器光标位置
4. WHEN 用户在字段助手搜索框输入关键词 THEN THE Field_Helper SHALL 实时过滤匹配的表和字段
5. WHEN 用户点击 SQL 片段按钮 THEN THE Field_Helper SHALL 将预设的 SQL 片段插入到编辑器中

### Requirement 3: SQL 安全校验

**User Story:** 作为系统管理员,我希望系统只允许执行 SELECT 查询,以便保护数据库不被误操作破坏。

#### Acceptance Criteria

1. WHEN 用户提交 SQL 查询 THEN THE SQL_Parser SHALL 验证 SQL 语句类型是否为 SELECT
2. IF SQL 语句包含 INSERT、UPDATE、DELETE、DROP、CREATE、ALTER 等 DDL/DML 操作 THEN THE Query_Executor SHALL 拒绝执行并返回错误信息
3. IF SQL 语句包含多条语句(使用分号分隔) THEN THE Query_Executor SHALL 拒绝执行并返回错误信息
4. WHEN SQL 语句通过安全校验 THEN THE Query_Executor SHALL 执行查询并返回结果
5. WHEN SQL 语句未通过安全校验 THEN THE SQL_Query_Console SHALL 在前端显示详细的错误提示信息

### Requirement 4: 查询执行与结果展示

**User Story:** 作为测试人员,我希望能够快速执行查询并查看结果,以便验证数据和排查问题。

#### Acceptance Criteria

1. WHEN 用户执行 SQL 查询 THEN THE Query_Executor SHALL 在 30 秒内返回结果或超时错误
2. WHEN 查询成功执行 THEN THE Result_Table SHALL 以表格形式展示查询结果,包含所有列和行
3. WHEN 查询返回结果 THEN THE SQL_Query_Console SHALL 显示执行时间、返回行数等元数据信息
4. WHEN 查询结果超过 1000 行 THEN THE Query_Executor SHALL 只返回前 1000 行并提示用户结果已截断
5. WHEN 查询执行失败 THEN THE SQL_Query_Console SHALL 显示详细的错误信息和错误位置提示

### Requirement 5: 结果导出

**User Story:** 作为测试人员,我希望能够导出查询结果,以便进行进一步分析或分享给团队成员。

#### Acceptance Criteria

1. WHEN 查询成功返回结果 THEN THE SQL_Query_Console SHALL 提供导出 Excel 的功能按钮
2. WHEN 用户点击导出 Excel 按钮 THEN THE SQL_Query_Console SHALL 将查询结果导出为 .xlsx 格式文件
3. WHEN 用户点击复制 JSON 按钮 THEN THE SQL_Query_Console SHALL 将查询结果复制为 JSON 格式到剪贴板
4. WHEN 用户点击复制表格按钮 THEN THE SQL_Query_Console SHALL 将查询结果复制为 Markdown 表格格式到剪贴板
5. WHEN 导出的数据包含特殊字符 THEN THE SQL_Query_Console SHALL 正确转义并保持数据完整性

### Requirement 6: 权限控制

**User Story:** 作为系统管理员,我希望只有管理员能够访问 SQL 查询台,以便控制数据访问权限。

#### Acceptance Criteria

1. WHEN 非管理员用户尝试访问 SQL 查询台 THEN THE SQL_Query_Console SHALL 拒绝访问并显示权限不足提示
2. WHEN Admin_User 访问 SQL 查询台 THEN THE SQL_Query_Console SHALL 允许访问并显示完整功能
3. WHEN 用户执行查询 THEN THE Query_Executor SHALL 验证用户是否具有管理员权限
4. WHEN 用户执行查询 THEN THE SQL_Query_Console SHALL 记录审计日志,包含用户 ID、SQL 语句、执行时间和结果状态
5. WHEN 用户会话过期 THEN THE SQL_Query_Console SHALL 阻止查询执行并提示用户重新登录

### Requirement 7: 查询限制与性能

**User Story:** 作为系统管理员,我希望系统能够限制查询的资源消耗,以便保护数据库性能和系统稳定性。

#### Acceptance Criteria

1. WHEN 用户执行查询 THEN THE Query_Executor SHALL 自动为没有 LIMIT 子句的查询添加默认 LIMIT 200
2. WHEN 用户指定的 LIMIT 超过 1000 THEN THE Query_Executor SHALL 将 LIMIT 截断为 1000 并提示用户
3. WHEN 查询执行时间超过 30 秒 THEN THE Query_Executor SHALL 终止查询并返回超时错误
4. WHEN 查询结果集过大 THEN THE Query_Executor SHALL 使用流式处理避免内存溢出
5. WHEN 多个用户同时执行查询 THEN THE Query_Executor SHALL 使用连接池管理数据库连接

### Requirement 8: 用户界面交互

**User Story:** 作为测试人员,我希望 SQL 查询台的界面友好且不干扰主业务流程,以便我能够高效地在查询和业务操作之间切换。

#### Acceptance Criteria

1. WHEN 用户点击"SQL 快速查询"按钮 THEN THE SQL_Query_Console SHALL 以右侧抽屉形式打开,宽度为屏幕的 60%
2. WHEN 用户关闭抽屉 THEN THE SQL_Query_Console SHALL 保留当前编辑的 SQL 内容和查询结果
3. WHEN 用户调整编辑器高度 THEN THE SQL_Query_Console SHALL 保存用户的偏好设置
4. WHEN 用户在小屏幕设备上访问 THEN THE SQL_Query_Console SHALL 自动调整布局以适应屏幕尺寸
5. WHEN 用户按下 Esc 键 THEN THE SQL_Query_Console SHALL 关闭抽屉并返回主页面

### Requirement 9: SQL 模板与片段

**User Story:** 作为测试人员,我希望能够使用预设的 SQL 模板和片段,以便快速构建常用查询。

#### Acceptance Criteria

1. WHEN 用户点击模板按钮 THEN THE SQL_Query_Console SHALL 显示预设的 SQL 模板列表
2. WHEN 用户选择某个模板 THEN THE SQL_Query_Console SHALL 将模板内容填充到编辑器中,并替换变量占位符
3. WHEN 用户点击"本项目"片段按钮 THEN THE Field_Helper SHALL 插入当前项目 ID 的 WHERE 条件
4. WHEN 用户点击"今日创建"片段按钮 THEN THE Field_Helper SHALL 插入今日日期的 WHERE 条件
5. WHEN 用户点击"LIMIT 50"片段按钮 THEN THE Field_Helper SHALL 在 SQL 末尾添加 LIMIT 50 子句

### Requirement 10: 错误处理与用户反馈

**User Story:** 作为测试人员,我希望系统能够提供清晰的错误提示,以便我能够快速定位和解决问题。

#### Acceptance Criteria

1. WHEN SQL 语法错误 THEN THE SQL_Query_Console SHALL 显示数据库返回的详细错误信息
2. WHEN 表或字段不存在 THEN THE SQL_Query_Console SHALL 提示用户检查表名和字段名,并显示可用表列表
3. WHEN 查询超时 THEN THE SQL_Query_Console SHALL 提示用户优化 SQL 或添加索引
4. WHEN 网络请求失败 THEN THE SQL_Query_Console SHALL 显示重试按钮和错误详情
5. WHEN 查询执行中 THEN THE SQL_Query_Console SHALL 显示加载动画和"执行中"状态提示
