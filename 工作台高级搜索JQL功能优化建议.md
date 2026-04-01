# 工作台高级搜索 JQL 查询功能 - 优化建议文档

## 功能概述

工作台高级搜索 JQL 查询功能是一个支持用户使用类 JQL 语法进行跨模块、跨项目、跨工作空间高级搜索的功能，目前支持测试用例、缺陷、测试计划、用例评审四个业务模块。

## 当前架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端 (Vue.js)                              │
├─────────────────────────────────────────────────────────────────┤
│  AdvancedSearch.vue (主页面)                                      │
│   ├─ TopFilterBar (模块/工作空间/项目选择)                         │
│   ├─ JQLEditor (JQL编辑器 + 智能提示 + 语法验证)                   │
│   └─ ResultArea (列表视图/分屏详情视图)                            │
│                                                                     │
│  Pinia Store (状态管理)                                            │
│   ├─ store/advancedSearch.js  (旧写法 - 功能不完整)                 │
│   └─ store/modules/advancedSearch.js (新写法 - 完整功能)            │
│                                                                     │
│  API 层 (advanced-search.js)                                       │
│   ├─ queryData()        执行查询                                   │
│   ├─ validateJQL()      验证语法                                   │
│   ├─ getJQLSuggestions() 获取智能提示                               │
│   └─ getFieldMetadata()  获取字段元数据                             │
└────────────────────────────────────┬──────────────────────────────┘
                                 │ HTTP REST API
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot)                             │
├─────────────────────────────────────────────────────────────────┤
│  AdvancedSearchController (REST API入口)                          │
├─────────────────────────────────────────────────────────────────┤
│  JQL 处理流水线                                                    │
│                                                                     │
│  1. JQLParser.parseJQL()            词法分析 → 语法分析 → 生成AST  │
│  2. JQLToSQLConverter.convertToSQL()  AST → SQL WHERE子句         │
│  3. JQLCacheService                 缓存解析结果，提升性能          │
│                                                                     │
│  智能提示：JQLSuggestionService  分析光标位置上下文提供精准提示      │
│  字段元数据：FieldMetadataService  提供各模块可筛选字段定义          │
├─────────────────────────────────────────────────────────────────┤
│  MyBatis ExtAdvancedSearchMapper.xml                               │
│   - 联表查询 + 动态拼接SQL + 权限过滤                               │
└─────────────────────────────────────────────────────────────────┘
```

## 当前质量评估

| 维度 | 评分 | 评价 |
|------|------|------|
| **架构设计** | ⭐⭐⭐⭐⭐ | 架构清晰，设计合理，安全到位 |
| **代码质量** | ⭐⭐⭐⭐☆ | 整体质量好，有少量可优化点 |
| **功能完整性** | ⭐⭐⭐⭐☆ | 核心功能完整，可视化模式待完成 |
| **安全性** | ⭐⭐⭐⭐⭐ | 多重防护，无 SQL 注入风险 |
| **性能** | ⭐⭐⭐⭐⭐ | 缓存优化到位，性能良好 |
| **用户体验** | ⭐⭐⭐⭐☆ | 体验良好，可添加语法高亮增强 |
| **可维护性** | ⭐⭐⭐⭐☆ | 整体易维护，模块扩展可优化 |

**总体得分：88/100**

---

## 详细优化建议

### 1. 删除重复的 Pinia 状态管理文件 🔴 **高优先级**

**问题描述**：
项目中同时存在两份功能重复的状态管理文件：
- `workstation/frontend/src/store/advancedSearch.js` - 旧的对象写法（功能不完整，未被使用）
- `workstation/frontend/src/store/modules/advancedSearch.js` - 新的 `defineStore` 写法（功能完整，正在使用）

**影响**：
- 占用不必要的代码空间
- 容易造成混淆，不清楚应该使用哪一个
- 增加维护成本

**优化方案**：
删除不再使用的旧文件。

**操作步骤**：
```bash
rm -f /Users/zhaozhiwei/IdeaProjects/metersphere/workstation/frontend/src/store/advancedSearch.js
```

---

### 2. JQL 缓存添加 LRU 淘汰策略 🔴 **高优先级**

**问题描述**：
`JQLCacheService` 使用 `ConcurrentHashMap` 作为缓存，但没有容量限制和淘汰策略：
- 随着不同 JQL 查询数量增加，缓存会无限增长
- 可能导致内存占用过大，甚至内存泄漏

**当前代码**：
```java
private final ConcurrentHashMap<String, Object> astCache = new ConcurrentHashMap<>();
private final ConcurrentHashMap<String, String> sqlCache = new ConcurrentHashMap<>();
```

**优化方案**：
使用 Google Guava 的 `CacheBuilder` 构建 LRU 缓存，设置最大容量为 1000 条。

**修改后代码**：
```java
package io.metersphere.workstation.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JQL 缓存服务
 *
 * 缓存 JQL 解析结果（AST）和转换后的 SQL
 * 避免重复解析相同的 JQL 查询语句
 *
 * @author MeterSphere
 */
@Service
public class JQLCacheService {

    /**
     * AST 缓存（内存缓存，LRU淘汰）
     * Key: JQL 的 MD5 值
     * Value: 解析后的 AST 对象
     *
     * 使用 LRU 缓存，最多缓存 1000 条，避免内存无限增长
     */
    private final Cache<String, Object> astCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /**
     * SQL 缓存（内存缓存，LRU淘汰）
     * Key: JQL 的 MD5 值 + 模块名
     * Value: 转换后的 SQL WHERE 子句
     */
    private final Cache<String, String> sqlCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /**
     * 缓存 AST
     *
     * @param jql JQL 查询语句
     * @param ast 解析后的 AST
     */
    public void cacheAST(String jql, Object ast) {
        String cacheKey = generateCacheKey(jql);
        astCache.put(cacheKey, ast);
    }

    /**
     * 获取缓存的 AST
     *
     * @param jql JQL 查询语句
     * @return 缓存的 AST，如果不存在则返回 null
     */
    public Object getCachedAST(String jql) {
        String cacheKey = generateCacheKey(jql);
        return astCache.getIfPresent(cacheKey);
    }

    /**
     * 缓存 SQL
     *
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @param sql 转换后的 SQL WHERE 子句
     */
    public void cacheSQL(String jql, String module, String sql) {
        String cacheKey = generateCacheKey(jql, module);
        sqlCache.put(cacheKey, sql);
    }

    /**
     * 获取缓存的 SQL
     *
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @return 缓存的 SQL，如果不存在则返回 null
     */
    public String getCachedSQL(String jql, String module) {
        String cacheKey = generateCacheKey(jql, module);
        return sqlCache.getIfPresent(cacheKey);
    }

    /**
     * 生成缓存键
     *
     * 使用 MD5 算法生成 JQL 的哈希值作为缓存键
     * 确保缓存键唯一且长度固定，避免长字符串占用过多内存
     *
     * @param jql JQL 查询语句
     * @return 缓存键（MD5 值）
     */
    public String generateCacheKey(String jql) {
        return DigestUtils.md5Hex(jql);
    }

    /**
     * 生成缓存键（带模块名）
     *
     * @param jql JQL 查询语句
     * @param module 业务模块
     * @return 缓存键
     */
    public String generateCacheKey(String jql, String module) {
        return DigestUtils.md5Hex(jql + ":" + module);
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        astCache.invalidateAll();
        sqlCache.invalidateAll();
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    public String getCacheStats() {
        return String.format("AST Cache Size: %d, SQL Cache Size: %d, " +
                "AST Hit Rate: %.2f%%, SQL Hit Rate: %.2f%%",
            astCache.size(), sqlCache.size(),
            astCache.stats().hitRate() * 100,
            sqlCache.stats().hitRate() * 100);
    }
}
```

**好处**：
- 自动淘汰最少使用的缓存条目，控制内存占用
- 添加 30 分钟过期时间，避免旧缓存自动清理
- Guava Cache 是线程安全的，适用于并发场景
- 提供缓存命中率统计，方便监控和调优

**依赖检查**：
项目已引入 Guava 依赖（通过 `io.dropwizard:dropwizard-core 依赖 Guava），无需额外添加。

---

### 3. 添加 JQL 语法高亮 🟠 **中优先级**

**问题描述**：
当前 JQL 编辑器只显示普通文本，关键字、字段名、操作符、字符串没有颜色区分，可读性较差。

**优化方案**：
添加简单的语法高亮：
- **关键字**（AND/OR/IN/NOT IN/CONTAINS）：加粗蓝色
- **字段名**：绿色
- **操作符**（= != ~ > >= < <=）：深灰色
- **字符串常量**：橙色/棕色
- **括号**：黑色

**实现思路**：
使用 `contenteditable` div 替换 textarea，对不同类型的 token 包裹不同颜色的 span 标签。

好处：
- 大幅提升可读性
- 用户更容易发现语法错误
- 提升专业感

**预估工作量：1-2 天

---

### 4. 重构字段元数据扩展机制 🟠 **中优先级**

**问题描述**：
`FieldMetadataService` 中各模块的字段元数据是硬编码的，新增业务模块需要修改核心代码，违反开闭原则。

**优化方案**：
使用 Java SPI 扩展机制，让各业务模块自行注册字段元数据提供者。

1. **定义扩展接口**：
```java
package io.metersphere.workstation.service;

import io.metersphere.workstation.dto.FieldMetadata;
import java.util.List;

/**
 * 字段元数据提供者接口
 * 各业务模块实现此接口提供可搜索的字段定义
 */
public interface FieldMetadataProvider {

    /**
     * 获取模块编码
     * @return 模块编码，如 "TEST_CASE", "ISSUE"
     */
    String getModuleCode();

    /**
     * 获取系统字段列表
     * @return 系统字段列表
     */
    List<FieldMetadata> getSystemFields();
}
```

2. **使用 SPI 发现提供者**：
```java
// 在 FieldMetadataService 构造方法中：
private Map<String, FieldMetadataProvider> providers = new HashMap<>();

ServiceLoader<FieldMetadataProvider> loader =
    ServiceLoader.load(FieldMetadataProvider.class);
for (FieldMetadataProvider provider : loader) {
    providers.put(provider.getModuleCode(), provider);
}
```

好处：
- 新增模块不需要修改核心代码
- 核心模块和业务模块解耦
- 符合开闭原则，更容易维护

预估工作量：1-2 天

---

### 5. 完成可视化查询模式 🟠 **中优先级**

**问题描述**：
界面上有"可视化"和"JQL"两个选项卡，但可视化查询模式功能尚未实现。

**优化建议**：
- 如果计划开发，安排迭代开发
- 如果暂时不开发，可以隐藏选项卡，避免用户困惑

---

### 6. 重构 JQL 分词器提升可维护性 🟡 **低优先级**

**问题描述**：
`JQLParser` 中使用一个非常长的正则表达式匹配所有 Token 类型，可读性较差，修改困难。

当前正则约 200+ 字符，难以维护。

**优化建议**：
分步分词：
1. 先按空白字符分割
2. 再逐个识别 Token 类型（字符串、标识符、操作符等）
这样更容易理解和维护。

预估工作量：1-2 天

---

## 优化优先级汇总

| 优化项 | 优先级 | 预估工作量 | 影响范围 |
|--------|--------|------------|----------|
| 删除重复的 Pinia 文件 | 🔴 高 | 5分钟 | 删除无用代码，无功能影响 |
| 添加 LRU 缓存淘汰策略 | 🔴 高 | 30分钟 | 内存占用优化，防止内存泄漏 |
| 添加 JQL 语法高亮 | 🟠 中 | 1-2天 | 用户体验提升 |
| 重构字段元数据扩展机制 | 🟠 中 | 1-2天 | 架构可扩展性提升 |
| 完成可视化查询模式 | 🟠 中 | 3-5天 | 功能完整性 |
| 重构分词器提升可维护性 | 🟡 低 | 1-2天 | 可维护性提升 |

---

## 安全性确认 ✅

当前实现**不存在 SQL 注入风险**，安全机制到位：

| 安全机制 | 状态 |
|----------|------|
| 字段白名单校验：只有预定义字段才能转换为 SQL 列名 | ✅ 通过 |
| SQL 值转义：所有用户输入的字符串值都会转义单引号 | ✅ 通过 |
| 权限控制：只返回用户有权限访问的数据 | ✅ 通过 |
| 项目隔离：跨项目查询时限制字段访问 | ✅ 通过 |

---

## 实施检查清单

### 优化 1 - 删除重复文件

- [x] 删除 `workstation/frontend/src/store/advancedSearch.js`
- [ ] 检查代码编译正常
- [ ] 检查功能正常

### 优化 2 - LRU缓存淘汰

- [ ] 修改 `JQLCacheService.java 使用 Guava Cache
- [ ] 编译测试通过
- [ ] 功能测试：JQL 查询正常
- [ ] 验证缓存统计正常

### 后续优化

根据实际需求和时间安排，逐步实施。

---

## 总结

这是一个**设计优良、实现完整、安全可靠**的 JQL 查询功能。采用了标准的编译原理处理流程，代码结构清晰，安全防护到位，性能优化合理，用户体验良好，是一个高质量的功能实现。

只需要完成上述高优先级优化就能进一步提升代码质量和内存安全性。

---

## 页面交互体验评估

### ✅ 当前交互优点

| 特性 | 状态 | 评价 |
|------|------|------|
| 智能提示键盘导航 | ✅ 完成 | 支持上下箭头选择、回车确认、Esc关闭，符合用户习惯 |
| 智能提示鼠标操作 | ✅ 完成 | 支持点击选择，鼠标 hover 高亮当前项 |
| 输入防抖处理 | ✅ 完成 | 300ms 防抖，减少不必要的验证和提示请求 |
| 实时语法验证 | ✅ 完成 | 输入时自动验证，即时显示错误信息和位置 |
| 两种视图模式 | ✅ 完成 | 列表视图 / 分屏详情视图，满足不同使用场景 |
| 状态持久化 | ✅ 完成 | 视图模式、查询模式持久化到 localStorage |
| 错误反馈机制 | ✅ 完成 | 操作失败时有清晰的消息提示 |
| 内置语法帮助 | ✅ 完成 | 折叠面板包含操作符说明和查询示例 |
| 回车键全局搜索 | ✅ 完成 | 页面绑定回车键，方便快速执行搜索 |

### ⚠️ 交互体验优化建议

### 7. JQL 编辑器添加语法高亮 🟠 **中优先级**

**问题描述**：
当前使用普通 textarea，全部文字同色显示，语法关键词、字段、字符串没有区分，可读性较差。

**优化方案**：
使用 `contenteditable` div 替代 textarea，对不同类型的 Token 应用不同颜色：

| Token 类型 | 建议颜色 |
|-----------|----------|
| **关键字** (AND/OR/IN/NOT IN/CONTAINS) | 蓝色，加粗 |
| **字段名** | 绿色 |
| **操作符** (= != ~ > >= < <=) | 深灰色 |
| **字符串常量** | 橙色 |
| **括号** | 黑色 |

**预估工作量**：1-2 天

---

### 8. 智能提示下拉框视口自适应 🟡 **低优先级**

**问题描述**：
当前下拉框固定显示在输入框下方，如果页面滚动或在小屏幕上，可能超出视口被遮挡。

**优化方案**：
计算输入框位置和视口高度，如果下方空间不足则显示在输入框上方。

```javascript
calculateDropdownPosition() {
  const rect = textarea.getBoundingClientRect();
  const viewportHeight = window.innerHeight;
  const dropdownHeight = 300; // 最大高度

  if (rect.bottom + dropdownHeight > viewportHeight) {
    // 空间不足，显示在上方
    this.dropdownStyle = {
      bottom: `${rect.height}px`,
      left: '0px',
      width: `${rect.width}px`,
      top: 'auto'
    };
  } else {
    // 正常显示在下方
    this.dropdownStyle = {
      top: `${rect.height}px`,
      left: '0px',
      width: `${rect.width}px`
    };
  }
}
```

**预估工作量**：2小时

---

### 9. 修复编辑器回车事件冒泡 🔴 **高优先级**

**问题描述**：
`AdvancedSearch.vue` 第2行在根元素绑定了 `@keyup.enter.native="handleSearch"`，这导致：
- 在 JQL 编辑器中按回车想要选择提示项时，会同时触发页面搜索
- 存在事件冒泡冲突

**优化方案**：
移除根元素的回车事件绑定，改为只在搜索按钮上点击触发，或者在 JQLEditor 中阻止回车事件冒泡。

**修改方案**：在 JQLEditor 的键盘事件处理中，当显示提示框时阻止回车冒泡：

```javascript
// 在 onKeyDown 中，处理完回车后添加：
if (event.key === 'Enter' && this.showSuggestions) {
  event.stopPropagation();
}
```

**预估工作量**：5分钟

---

### 10. 帮助区示例支持点击插入 🟡 **低优先级**

**问题描述**：
语法帮助区的示例代码需要用户手动复制粘贴，不够便捷。

**优化方案**：
示例代码添加点击事件，点击后自动插入到编辑器光标位置。

```javascript
const insertExample = (exampleText) => {
  // 插入到当前光标位置
  const textarea = this.$refs.jqlInput.$el.querySelector('textarea');
  const cursorPosition = textarea.selectionStart;
  const before = this.localJql.substring(0, cursorPosition);
  const after = this.localJql.substring(cursorPosition);
  this.localJql = before + ' ' + exampleText + after;
  this.$emit('input', this.localJql);
};
```

**预估工作量**：1小时

---

### 11. 支持保存常用查询和历史记录 🟠 **中优先级**

**问题描述**：
当前用户无法保存常用的 JQL 查询，每次需要重新输入；也不能查看历史查询记录。

**优化方案**：
- 添加"保存查询"按钮，用户可以给当前 JQL 命名保存
- 添加"我的查询"下拉列表，快速选择已保存的查询
- 自动记录查询历史，最近使用的查询排在前面
- 保存到后端数据库，跨会话持久化

**数据模型**：
```java
// 新增表：advanced_search_saved_query
// id, user_id, module, name, jql, create_time, update_time
```

**预估工作量**：2-3 天

---

### 12. 编辑器支持自动高度扩展 🟡 **低优先级**

**问题描述**：
当前 textarea 固定 `:rows="3"`，对于较长的 JQL 查询，需要滚动查看，不便捷。

**优化方案**：
实现自动高度：内容增加时自动扩展高度，最大高度限制在 10 行。

可以使用简单的计算：
```javascript
onInput() {
  // 计算行数，自动调整高度
  const lineCount = this.localJql.split('\n').length;
  this.rows = Math.min(Math.max(lineCount + 1, 3), 10);
}
```

**预估工作量**：30分钟

---

### 13. 可视化模式未实现，建议隐藏 🟠 **中优先级**

**问题描述**：
当前界面显示"可视化"选项卡，但内容区域只有占位提示文字"可视化模式占位符"，功能尚未实现。

**优化方案**：
- 如果近期计划开发，可以保持显示，但添加"开发中"标签提示用户
- 如果短期内不开发，建议隐藏这个选项卡，避免用户困惑

---

## 交互优化优先级汇总

| 优化项 | 优先级 | 预估工作量 | 说明 |
|--------|--------|------------|------|
| 修复编辑器回车事件冒泡 | 🔴 高 | 5分钟 | 解决当前存在的冲突问题 |
| JQL 编辑器添加语法高亮 | 🟠 中 | 1-2天 | 大幅提升可读性 |
| 支持保存常用查询和历史记录 | 🟠 中 | 2-3天 | 提升重复查询效率 |
| 可视化模式未实现建议隐藏 | 🟠 中 | 5分钟 | 避免用户困惑 |
| 帮助区示例支持点击插入 | 🟡 低 | 1小时 | 提升学习和使用效率 |
| 智能提示下拉框视口自适应 | 🟡 低 | 2小时 | 改进小屏幕/滚动场景体验 |
| 编辑器自动高度扩展 | 🟡 低 | 30分钟 | 改进长JQL的可读性 |

---

## 总体优化优先级汇总（更新）

| 优化项 | 优先级 | 预估工作量 | 类别 |
|--------|--------|------------|------|
| 删除重复的 Pinia 文件 | 🔴 高 | 5分钟 | 代码清理 |
| 添加 LRU 缓存淘汰策略 | 🔴 高 | 30分钟 | 性能/内存 |
| 修复编辑器回车事件冒泡 | 🔴 高 | 5分钟 | 交互修复 |
| JQL 编辑器添加语法高亮 | 🟠 中 | 1-2天 | 体验提升 |
| 重构字段元数据扩展机制 | 🟠 中 | 1-2天 | 架构扩展 |
| 支持保存常用查询和历史记录 | 🟠 中 | 2-3天 | 功能增强 |
| 完成可视化查询模式 | 🟠 中 | 3-5天 | 功能完整性 |
| 可视化模式未实现建议隐藏 | 🟠 中 | 5分钟 | 体验优化 |
| 帮助区示例支持点击插入 | 🟡 低 | 1小时 | 体验提升 |
| 重构分词器提升可维护性 | 🟡 低 | 1-2天 | 可维护性 |
| 智能提示下拉框视口自适应 | 🟡 低 | 2小时 | 体验提升 |
| 编辑器自动高度扩展 | 🟡 低 | 30分钟 | 体验提升 |

---

## 安全性确认 ✅

当前实现**不存在 SQL 注入风险**，安全机制到位：

| 安全机制 | 状态 |
|----------|------|
| 字段白名单校验：只有预定义字段才能转换为 SQL 列名 | ✅ 通过 |
| SQL 值转义：所有用户输入的字符串值都会转义单引号 | ✅ 通过 |
| 权限控制：只返回用户有权限访问的数据 | ✅ 通过 |
| 项目隔离：跨项目查询时限制字段访问 | ✅ 通过 |

---

## 实施检查清单

### 优化 1 - 删除重复文件

- [ ] 删除 `workstation/frontend/src/store/advancedSearch.js`
- [ ] 检查代码编译正常
- [ ] 检查功能正常

### 优化 2 - LRU缓存淘汰

- [ ] 修改 `JQLCacheService.java 使用 Guava Cache
- [ ] 编译测试通过
- [ ] 功能测试：JQL 查询正常
- [ ] 验证缓存统计正常

### 优化 9 - 修复回车事件冒泡

- [ ] 在 JQLEditor.vue 的 onKeyDown 中阻止回车冒泡
- [ ] 测试：在编辑器按回车只选择提示，不触发搜索

### 后续优化

根据实际需求和时间安排，逐步实施。

---

## 文档信息

| 属性 | 值 |
|------|-----|
| **创建时间** | 2026-03-27 |
| **功能模块** | 工作台 / 高级搜索 |
| **当前版本** | v1.1 |
| **评估人** | Claude Code |
