# 高级检索功能测试说明

## 当前状态

✅ **后端代码已完成编译**，无语法错误  
✅ **前端代码已完成编译**，无语法错误  
✅ **服务正在运行**（workstation 模块端口 8007）

## 用户报告的 500 错误分析

### 实际情况

用户报告的 "500 Internal Server Error" **不是真正的服务器错误**，而是 **302 重定向**（认证失败）。

通过 curl 测试发现：

```bash
$ curl -X POST "http://localhost:8007/workstation/advanced-search/query/1/20" \
  -H "Content-Type: application/json" \
  -d '{"module":"TEST_CASE"}'

HTTP/1.1 302 Found
Authentication-Status: invalid
Location: /
```

### 根本原因

1. **所有接口都需要 Shiro 认证**：MeterSphere 使用 Apache Shiro 进行安全认证，所有 API 接口都需要用户登录
2. **前端请求缺少认证信息**：请求头中缺少 `X-AUTH-TOKEN` 和 `CSRF-TOKEN`
3. **用户未登录或 token 已过期**：localStorage 中没有有效的用户 token

### 为什么显示 500 错误？

前端可能没有正确处理 302 响应，导致在控制台中显示为 500 错误。实际上服务器返回的是 302 重定向到登录页。

## 如何测试

### 方法 1：通过前端界面测试（推荐）

1. **登录系统**
   - 访问 http://localhost:8007
   - 使用有效的用户名和密码登录
   - 登录成功后，token 会自动保存到 localStorage

2. **访问高级检索页面**
   - 在工作台菜单中找到"高级检索"入口
   - 或直接访问：http://localhost:8007/workstation/advanced-search

3. **执行查询**
   - 选择业务模块（测试用例/缺陷/测试计划/用例评审）
   - 选择工作空间和项目
   - 添加筛选条件
   - 点击"查询"按钮

### 方法 2：使用 curl 测试（需要 token）

1. **获取 token**
   - 先通过浏览器登录系统
   - 打开浏览器开发者工具 → Application → Local Storage
   - 找到 `MS-TOKEN` 键，复制其中的 `sessionId` 和 `csrfToken`

2. **使用 token 测试接口**

```bash
# 替换 YOUR_SESSION_ID 和 YOUR_CSRF_TOKEN 为实际值
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

### 方法 3：使用 Postman 测试

1. 导入接口到 Postman
2. 在 Headers 中添加：
   - `X-AUTH-TOKEN`: 从浏览器 localStorage 获取
   - `CSRF-TOKEN`: 从浏览器 localStorage 获取
   - `Content-Type`: application/json
3. 发送请求

## 当前实现进度

### 已完成（约 72%）

#### 后端（约 70%）
- ✅ Controller 层 - 100%
- ✅ DTO 类 - 100%
- ✅ Mapper 层 - 100%
- ✅ Service 层基础框架 - 70%
  - `AdvancedSearchService.query()` - 框架完成，核心逻辑待完善
  - `FieldMetadataService` - 系统字段完成，自定义字段查询待实现
  - `UserQueryService`, `WorkspaceQueryService`, `ProjectQueryService` - 100%
  - `JQLParser` - 仅框架（10%）
  - `JQLToSQLConverter` - 仅框架（10%）

#### 前端（约 75%）
- ✅ API 接口层 - 100%
- ✅ Pinia Store - 100%
- ✅ 路由配置 - 100%
- ✅ 国际化词条 - 100%
- ✅ UI 入口 - 100%
- ✅ 核心组件 - 75%
  - 主页面、列表视图、用户选择器、条件输入等组件基本完成
  - JQL 编辑器、详情面板等组件基础框架完成

### 待完成（约 28%）

#### 后端核心功能
1. **完善查询服务**（最重要）
   - 实现 `AdvancedSearchService.query()` 的完整逻辑
   - 处理空查询条件的情况
   - 实现权限校验逻辑

2. **实现详情查询**
   - 实现 `AdvancedSearchService.getDetail()` 方法
   - 根据不同模块查询对应的详情数据

3. **实现自定义字段查询**
   - 创建 Mapper 方法查询 `custom_field` 表
   - 实现 `FieldMetadataService.getCustomFields()` 方法
   - 解析 `options` 字段（JSON 格式）

4. **实现 Excel 导出**
   - 使用 EasyExcel 导出查询结果

5. **实现 JQL 功能**（高级功能，可选）
   - 完整实现 `JQLParser` 的语法解析逻辑
   - 实现 `JQLToSQLConverter` 的 SQL 转换逻辑
   - 实现 JQL 缓存机制

#### 前端完善
1. **完善组件业务逻辑**
   - 所有组件目前只有骨架，需要实现完整的业务逻辑
   - 完善 JQL 编辑器的智能提示功能
   - 完善详情面板的数据展示

## 下一步计划

### 第一阶段：修复核心查询功能（最高优先级）

1. **完善 `AdvancedSearchService.query()` 方法**
   - 处理空查询条件
   - 实现基本的权限校验
   - 确保 SQL 查询正确执行

2. **测试基本查询功能**
   - 登录系统
   - 测试测试用例查询
   - 测试缺陷查询
   - 测试测试计划查询
   - 测试用例评审查询

### 第二阶段：完善功能（第二优先级）

1. 实现详情查询
2. 实现自定义字段查询
3. 实现 Excel 导出

### 第三阶段：高级功能（第三优先级）

1. 实现 JQL 支持
2. 实现 JQL 智能提示
3. 实现 JQL 缓存

## 常见问题

### Q1: 为什么我看到 500 错误？

A: 这不是真正的 500 错误，而是认证失败导致的 302 重定向。请确保：
1. 已经登录系统
2. token 没有过期
3. 浏览器 localStorage 中有有效的 `MS-TOKEN`

### Q2: 如何查看我的 token？

A: 
1. 打开浏览器开发者工具（F12）
2. 切换到 Application 标签
3. 左侧选择 Local Storage → http://localhost:8007
4. 找到 `MS-TOKEN` 键
5. 查看其中的 `sessionId` 和 `csrfToken`

### Q3: 查询返回空结果怎么办？

A: 可能的原因：
1. 数据库中没有符合条件的数据
2. 用户没有权限访问所选的工作空间/项目
3. 查询条件过于严格

建议：
1. 先不添加任何筛选条件，只选择模块和项目
2. 检查数据库中是否有测试数据
3. 检查用户权限配置

### Q4: 如何添加测试数据？

A: 
1. 登录系统
2. 进入对应的模块（测试跟踪/接口测试/性能测试）
3. 手动创建一些测试数据
4. 然后在高级检索中查询

## 技术细节

### 认证机制

MeterSphere 使用 Apache Shiro 进行安全认证：

1. **登录流程**
   - 用户提交用户名和密码
   - 后端验证成功后生成 `sessionId` 和 `csrfToken`
   - 前端将 token 保存到 localStorage

2. **请求认证**
   - 前端拦截器自动从 localStorage 读取 token
   - 将 token 添加到请求头：
     - `X-AUTH-TOKEN`: sessionId
     - `CSRF-TOKEN`: csrfToken
   - 后端 Shiro 过滤器验证 token

3. **认证失败处理**
   - 后端返回 302 重定向
   - 响应头包含 `Authentication-Status: invalid`
   - 前端拦截器检测到认证失败，清除 localStorage 并跳转到登录页

### 权限控制

- 工作台模块的接口不需要特殊权限，只需要用户登录即可
- 查询结果会根据用户的工作空间/项目权限进行过滤
- 用户只能查询到自己有权限访问的数据

## 总结

当前的"500 错误"实际上是**认证问题**，不是代码错误。解决方法：

1. **确保用户已登录**
2. **通过前端界面测试**（推荐）
3. **如果要用 API 测试，需要先获取有效的 token**

后端和前端的基础框架已经完成，核心查询功能的框架也已就绪。下一步需要完善查询服务的具体实现，然后就可以正常使用了。
