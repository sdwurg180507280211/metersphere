# 需求文档

## 简介

高级检索功能为 MeterSphere 测试平台提供类似 Jira JQL 的综合查询能力，允许用户跨工作空间、跨项目对测试用例和缺陷进行灵活的条件筛选、多视图展示和结果管理。该功能集成在工作台（Workstation）模块中，作为"查询中心"的核心入口，支持可视化条件构建和JQL查询语法两种查询模式。

## 术语表

- **Advanced_Search_System**：高级检索系统，负责处理用户的查询请求并返回结果
- **Filter_Condition**：筛选条件，由字段、操作符和值组成的查询条件
- **Field_Metadata**：字段元数据，描述可筛选字段的类型、操作符和可选值
- **Search_Result**：检索结果，符合筛选条件的数据记录集合
- **View_Mode**：视图模式，结果展示方式（列表模式/详情模式）
- **Column_Config**：列配置，用户自定义的表格显示列设置
- **Business_Module**：业务模块，可检索的数据类型（测试用例/缺陷）
- **JQL**：Jira Query Language，类似Jira的查询语法
- **Query_Mode**：查询模式，支持可视化条件构建和JQL语法两种方式

## 需求

### 需求 1：业务模块切换

**用户故事：** 作为测试人员，我希望能够在多种测试跟踪数据类型之间切换查询目标，以便在同一界面集中查询所有测试相关数据。

#### 验收标准

1. WHEN 用户选择业务模块 THEN Advanced_Search_System SHALL 更新可用的筛选字段列表
2. WHEN 业务模块切换 THEN Advanced_Search_System SHALL 清空当前筛选条件并重置列配置
3. THE Advanced_Search_System SHALL 支持以下业务模块：测试用例（test_case）、缺陷（issue）、测试计划（test_plan）、用例评审（test_case_review）
4. WHEN 页面加载 THEN Advanced_Search_System SHALL 默认选中测试用例模块
5. THE Advanced_Search_System SHALL 在模块选择器中使用图标区分不同模块类型

### 需求 2：工作空间和项目筛选

**用户故事：** 作为测试人员，我希望能够选择多个工作空间和项目进行查询，以便跨项目汇总和分析测试数据。

#### 验收标准

1. THE Advanced_Search_System SHALL 显示当前用户有权限访问的所有工作空间
2. WHEN 用户选择工作空间 THEN Advanced_Search_System SHALL 级联更新可选项目列表
3. THE Advanced_Search_System SHALL 支持多选工作空间和多选项目
4. WHEN 工作空间选择变更 THEN Advanced_Search_System SHALL 自动过滤掉不属于已选工作空间的项目
5. IF 用户未选择任何工作空间 THEN Advanced_Search_System SHALL 禁用项目选择器

### 需求 3：动态筛选条件

**用户故事：** 作为测试人员，我希望能够动态添加和组合多个筛选条件，以便精确定位所需的测试数据。

#### 验收标准

1. WHEN 用户点击"筛选条件"按钮 THEN Advanced_Search_System SHALL 显示可选字段的 Popover 面板
2. WHEN 用户选择字段 THEN Advanced_Search_System SHALL 添加该字段作为筛选条件并以 Tag 形式展示
3. THE Advanced_Search_System SHALL 根据字段类型提供对应的输入控件（文本框/下拉选择/日期选择器）
4. THE Advanced_Search_System SHALL 根据字段类型提供对应的操作符（包含/是/不是）
5. WHEN 用户点击条件 Tag 的关闭按钮 THEN Advanced_Search_System SHALL 移除该筛选条件
6. WHEN 用户点击"清空"按钮 THEN Advanced_Search_System SHALL 移除所有筛选条件
7. THE Advanced_Search_System SHALL 使用 AND 逻辑组合所有筛选条件

### 需求 4：字段元数据管理

**用户故事：** 作为系统管理员，我希望系统能够根据业务模块提供正确的字段元数据，以便用户获得准确的筛选选项。

#### 验收标准

1. THE Advanced_Search_System SHALL 提供通用字段：标题、ID、状态、创建人、创建日期、更新时间
2. WHEN 业务模块为测试用例 THEN Advanced_Search_System SHALL 提供专属字段：优先级、维护人、用例类型、前置条件、所属模块、关联需求数
3. WHEN 业务模块为缺陷 THEN Advanced_Search_System SHALL 提供专属字段：严重程度、指派给、版本、环境、复现概率、截止日期、关联需求数
4. WHEN 业务模块为测试计划 THEN Advanced_Search_System SHALL 提供专属字段：负责人、计划阶段、计划开始日期、计划结束日期、实际开始日期、实际结束日期
5. WHEN 业务模块为用例评审 THEN Advanced_Search_System SHALL 提供专属字段：评审人、评审状态、评审结果、评审截止日期
6. THE Advanced_Search_System SHALL 为每个字段定义类型（text/select/date/user）和可用操作符
7. WHEN 字段类型为 select THEN Advanced_Search_System SHALL 提供预定义的选项列表
8. WHEN 字段类型为 user THEN Advanced_Search_System SHALL 支持多选用户并提供用户搜索功能

### 需求 4.1：用户字段多选

**用户故事：** 作为测试人员，我希望能够选择多个用户进行筛选，以便查询多人负责的数据。

#### 验收标准

1. WHEN 字段类型为 user THEN Advanced_Search_System SHALL 显示用户多选下拉框
2. THE Advanced_Search_System SHALL 支持在用户选择器中搜索用户（按用户名或姓名）
3. THE Advanced_Search_System SHALL 显示已选用户的头像和名称
4. WHEN 用户选择多个用户 THEN Advanced_Search_System SHALL 使用 OR 逻辑匹配任一用户
5. THE Advanced_Search_System SHALL 限制单个用户字段最多选择 10 个用户

### 需求 5：查询执行

**用户故事：** 作为测试人员，我希望能够执行查询并获得符合条件的结果，以便分析和处理测试数据。

#### 验收标准

1. WHEN 用户点击"查询"按钮 THEN Advanced_Search_System SHALL 根据当前条件执行数据库查询
2. WHILE 查询执行中 THEN Advanced_Search_System SHALL 显示加载状态
3. WHEN 查询完成 THEN Advanced_Search_System SHALL 显示结果总数
4. IF 查询条件无效 THEN Advanced_Search_System SHALL 显示错误提示
5. THE Advanced_Search_System SHALL 支持分页查询，默认每页 20 条

### 需求 6：列表视图模式

**用户故事：** 作为测试人员，我希望能够以表格形式查看查询结果，以便快速浏览和比较多条数据。

#### 验收标准

1. WHEN 视图模式为列表模式 THEN Advanced_Search_System SHALL 使用 el-table 展示结果
2. THE Advanced_Search_System SHALL 显示序号列和用户配置的数据列
3. THE Advanced_Search_System SHALL 对状态字段使用 Tag 样式展示
4. THE Advanced_Search_System SHALL 支持列内容超长时的 tooltip 提示
5. WHEN 用户点击行 THEN Advanced_Search_System SHALL 高亮选中行

### 需求 7：详情视图模式

**用户故事：** 作为测试人员，我希望能够在分屏视图中查看详情，以便同时浏览列表和详细信息。

#### 验收标准

1. WHEN 视图模式为详情模式 THEN Advanced_Search_System SHALL 显示左侧列表和右侧详情的分屏布局
2. WHEN 用户点击左侧列表项 THEN Advanced_Search_System SHALL 在右侧显示该记录的详细信息
3. THE Advanced_Search_System SHALL 在详情面板显示基本属性、详细描述和活动日志
4. WHEN 切换到详情模式且有结果 THEN Advanced_Search_System SHALL 自动选中第一条记录
5. THE Advanced_Search_System SHALL 在详情面板提供编辑、流转状态、评论、分享等操作按钮

### 需求 8：列配置

**用户故事：** 作为测试人员，我希望能够自定义表格显示的列，以便只关注我需要的信息。

#### 验收标准

1. WHEN 用户点击列配置按钮 THEN Advanced_Search_System SHALL 显示列选择 Popover
2. THE Advanced_Search_System SHALL 将可选列分组展示：基础信息、模块专属、审计追踪
3. WHEN 用户勾选/取消勾选列 THEN Advanced_Search_System SHALL 实时更新表格显示
4. THE Advanced_Search_System SHALL 为每个业务模块提供默认显示列配置
5. WHEN 业务模块切换 THEN Advanced_Search_System SHALL 重置为该模块的默认列配置

### 需求 9：侧边栏导航

**用户故事：** 作为测试人员，我希望能够通过侧边栏快速访问不同的查询功能，以便提高工作效率。

#### 验收标准

1. THE Advanced_Search_System SHALL 在侧边栏显示查询中心分组：高级检索、我的收藏、最近浏览
2. THE Advanced_Search_System SHALL 在侧边栏显示共享视图分组（预留功能）
3. WHEN 用户点击导航项 THEN Advanced_Search_System SHALL 高亮当前选中项
4. THE Advanced_Search_System SHALL 默认选中"高级检索"导航项

### 需求 10：数据导出

**用户故事：** 作为测试人员，我希望能够导出查询结果，以便进行离线分析或分享给他人。

#### 验收标准

1. WHEN 用户点击导出按钮 THEN Advanced_Search_System SHALL 导出当前查询结果
2. THE Advanced_Search_System SHALL 支持 Excel 格式导出
3. THE Advanced_Search_System SHALL 导出用户当前配置的显示列
4. IF 结果数量超过 10000 条 THEN Advanced_Search_System SHALL 提示用户缩小查询范围

### 需求 11：后端查询服务

**用户故事：** 作为系统，我需要提供高效的查询 API，以便前端能够获取符合条件的数据。

#### 验收标准

1. THE Advanced_Search_System SHALL 提供 RESTful API 接口 `/workstation/advanced-search/query`
2. WHEN 接收查询请求 THEN Advanced_Search_System SHALL 验证用户对所选工作空间和项目的访问权限
3. THE Advanced_Search_System SHALL 使用 MyBatis 动态 SQL 构建查询语句
4. THE Advanced_Search_System SHALL 对查询结果进行分页处理
5. IF 用户无权限访问指定资源 THEN Advanced_Search_System SHALL 返回 403 错误

### 需求 12：查询请求序列化

**用户故事：** 作为系统，我需要将前端的查询条件正确解析为数据库查询，以便返回准确的结果。

#### 验收标准

1. WHEN 接收查询请求 THEN Advanced_Search_System SHALL 解析 JSON 格式的筛选条件
2. THE Query_Parser SHALL 将筛选条件转换为 SQL WHERE 子句
3. THE Query_Parser SHALL 正确处理不同操作符（=、!=、LIKE）的 SQL 生成
4. THE Query_Parser SHALL 对用户输入进行 SQL 注入防护
5. FOR ALL 有效的查询请求，解析后执行再序列化 SHALL 产生等价的查询条件（往返一致性）

### 需求 13：JQL查询语法支持

**用户故事：** 作为高级用户，我希望能够使用类似Jira JQL的查询语法，以便快速构建复杂的查询条件。

#### 验收标准

1. THE Advanced_Search_System SHALL 支持JQL查询语法输入
2. THE Advanced_Search_System SHALL 支持基础操作符：=、!=、~、IN、NOT IN、>、>=、<、<=、CONTAINS
3. THE Advanced_Search_System SHALL 支持逻辑操作符：AND、OR，支持括号分组
4. WHEN 用户输入JQL THEN Advanced_Search_System SHALL 实时验证语法正确性
5. THE Advanced_Search_System SHALL 提供JQL语法错误提示和建议修复
6. THE Advanced_Search_System SHALL 支持字段名、操作符、值的智能提示
7. THE Advanced_Search_System SHALL 将JQL解析为SQL WHERE子句执行查询

### 需求 14：查询模式切换

**用户故事：** 作为用户，我希望能够在可视化条件构建和JQL语法之间自由切换，以便选择最适合的查询方式。

#### 验收标准

1. THE Advanced_Search_System SHALL 提供查询模式切换按钮（可视化/JQL）
2. WHEN 用户从可视化模式切换到JQL模式 THEN Advanced_Search_System SHALL 自动将当前条件转换为JQL语法
3. WHEN 用户从JQL模式切换到可视化模式 THEN Advanced_Search_System SHALL 解析JQL并转换为可视化条件
4. THE Advanced_Search_System SHALL 保持查询模式的用户偏好设置
5. IF JQL语法过于复杂无法转换为可视化条件 THEN Advanced_Search_System SHALL 提示用户并保持JQL模式

### 需求 15：JQL智能提示

**用户故事：** 作为用户，我希望在输入JQL时获得智能提示，以便快速构建正确的查询语句。

#### 验收标准

1. WHEN 用户在JQL输入框中输入 THEN Advanced_Search_System SHALL 提供实时智能提示
2. THE Advanced_Search_System SHALL 根据当前上下文提示可用字段名
3. THE Advanced_Search_System SHALL 根据字段类型提示可用操作符
4. THE Advanced_Search_System SHALL 根据字段类型提示可选值（如状态、优先级等）
5. THE Advanced_Search_System SHALL 支持用户名搜索和提示
6. THE Advanced_Search_System SHALL 提供JQL语法帮助和示例查询
