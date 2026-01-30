# page.condition 核心机制深度解析（Part 1）

## 一、概述

`page.condition` 是 MeterSphere 前端列表查询的**统一数据结构**，包含4个核心参数：

| 参数 | 作用 | 数据类型 | 使用场景 |
|-----|------|---------|---------|
| **filters** | 列过滤（精确匹配） | Object | 表头筛选、权限过滤 |
| **combine** | 组合查询（模糊匹配） | Object | 高级搜索 |
| **orders** | 排序规则 | Array | 列排序 |
| **components** | 搜索组件配置 | Array | 高级搜索UI |

---

## 二、filters（列过滤）

### 2.1 核心概念

**作用**：精确匹配过滤，用于表头筛选和权限过滤

**数据结构**：
```javascript
filters: {
  'fieldKey': ['value1', 'value2', ...]
}
```

**特点**：
- ✅ 精确匹配（IN 查询）
- ✅ 支持多选
- ✅ 翻页时保持
- ❌ 搜索时清除（可选）
- ❌ 不支持模糊匹配

---

### 2.2 内置字段过滤

**示例1：创建人过滤**
```javascript
// 前端设置
this.page.condition.filters['creator'] = ['user-001', 'user-002'];

// 后端SQL
WHERE issues.creator IN ('user-001', 'user-002')
```

**示例2：状态过滤**
```javascript
// 前端设置
this.page.condition.filters['status'] = ['new', 'in_progress'];

// 后端SQL
WHERE issues.status IN ('new', 'in_progress')
```

**示例3：平台过滤**
```javascript
// 前端设置
this.page.condition.filters['platform'] = ['Jira', 'Tapd'];

// 后端SQL
WHERE issues.platform IN ('Jira', 'Tapd')
```

---

### 2.3 自定义字段过滤

**关键点**：自定义字段的 key 格式为 `custom_{type}-{fieldId}`

**字段类型映射**：
```javascript
// member（成员）类型 → custom_single
'custom_single-field-001'

// multipleSelect（多选）类型 → custom_multiple
'custom_multiple-field-002'

// select（单选）类型 → custom_single
'custom_single-field-003'
```

**示例1：处理人过滤（member类型）**
```javascript
// 前端设置
const handlerField = { id: 'field-001', type: 'member', name: '处理人' };
const filterKey = generateColumnKey(handlerField);
// 结果：'custom_single-field-001'

this.page.condition.filters[filterKey] = ['user-001'];

// 后端SQL
WHERE issues.id IN (
  SELECT resource_id FROM issue_custom_field
  WHERE field_id = 'field-001'
  AND value IN ('user-001')
)
```

**示例2：所属系统过滤（multipleSelect类型）**
```javascript
// 前端设置
const systemField = { id: 'field-002', type: 'multipleSelect', name: '所属系统' };
const filterKey = generateColumnKey(systemField);
// 结果：'custom_multiple-field-002'

this.page.condition.filters[filterKey] = ['system-001', 'system-002'];

// 后端SQL
WHERE issues.id IN (
  SELECT resource_id FROM issue_custom_field
  WHERE field_id = 'field-002'
  AND value IN ('system-001', 'system-002')
)
```

---

### 2.4 generateColumnKey 函数

**位置**：`framework/sdk-parent/frontend/src/components/search/custom-component.js`

**作用**：根据字段类型生成正确的 filterKey

**源码**：
```javascript
export function generateColumnKey(field) {
  if (field.type === 'member' || field.type === 'select') {
    return 'custom_single-' + field.id;
  } else if (field.type === 'multipleSelect' || field.type === 'multipleMember') {
    return 'custom_multiple-' + field.id;
  } else {
    return field.id;
  }
}
```

**使用示例**：
```javascript
// ✅ 正确：使用 generateColumnKey
const filterKey = generateColumnKey(handlerField);
this.page.condition.filters[filterKey] = [userId];

// ❌ 错误：手动拼接（可能出错）
this.page.condition.filters['custom_single-' + handlerField.id] = [userId];
```

---

### 2.5 权限过滤应用

**场景**：开发人员只能看到处理人是自己的缺陷

**实现步骤**：

**步骤1：查找"处理人"字段**
```javascript
const handlerField = this.issueTemplate.customFields.find(f => f.name === '处理人');
// 结果：{ id: 'field-001', type: 'member', name: '处理人' }
```

**步骤2：生成 filterKey**
```javascript
const filterKey = generateColumnKey(handlerField);
// 结果：'custom_single-field-001'
```

**步骤3：设置过滤条件**
```javascript
const currentUserId = getCurrentUserId();
this.page.condition.filters[filterKey] = [currentUserId];
// 结果：{ 'custom_single-field-001': ['user-001'] }
```

**步骤4：记录 filterKey（用于后续清除）**
```javascript
this.userGroupFilterKeys.push(filterKey);
// 结果：['custom_single-field-001']
```

**步骤5：发送请求**
```javascript
getIssues(this.page.currentPage, this.page.pageSize, this.page.condition);
```

**实际请求**：
```http
POST /issues/list/1/10
Content-Type: application/json

{
  "projectId": "project-001",
  "filters": {
    "custom_single-field-001": ["user-001"]
  }
}
```

**后端SQL**：
```sql
SELECT * FROM issues
WHERE project_id = 'project-001'
AND issues.id IN (
  SELECT resource_id FROM issue_custom_field
  WHERE field_id = 'field-001'
  AND value IN ('user-001')
)
ORDER BY create_time DESC
LIMIT 0, 10
```

---

### 2.6 清除过滤条件

**场景**：用户点击"搜索"或"重置"时，清除权限过滤

**实现**：
```javascript
clearUserGroupFilter() {
  if (!this.page.condition.filters) {
    return;
  }

  // 清除之前记录的过滤条件
  this.userGroupFilterKeys.forEach(key => {
    delete this.page.condition.filters[key];
  });
  this.userGroupFilterKeys = [];
}
```

**调用时机**：
```javascript
search() {
  // 1. 清除权限过滤
  this.clearUserGroupFilter();
  
  // 2. 回到第一页
  this.page.currentPage = 1;
  
  // 3. 重新加载数据
  this.getIssues();
}
```

---

### 2.7 翻页时保持过滤

**原理**：翻页时不清除 `page.condition.filters`

**实现**：
```javascript
handlePageChange() {
  // 只改变页码，不改变 filters
  this.page.currentPage = 2;
  
  // page.condition.filters 保持不变
  // { 'custom_single-field-001': ['user-001'] }
  
  // 重新加载数据
  this.getIssues();
}
```

**为什么会保持？**
```javascript
// 因为 page.condition.filters 是一个对象引用
// 翻页时只改变 page.currentPage，不改变 page.condition

// 第1页
this.page.currentPage = 1;
this.page.condition.filters = { 'creator': ['user-001'] };

// 翻到第2页
this.page.currentPage = 2;
// this.page.condition.filters 仍然是 { 'creator': ['user-001'] }
```

---

### 2.8 后端SQL处理

**位置**：`test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`

**SQL片段**：
```xml
<!-- 处理 filters 参数 -->
<if test="request.filters != null and request.filters.size() > 0">
  <foreach collection="request.filters.entrySet()" index="key" item="values">
    <if test="values != null and values.size() > 0">
      
      <!-- 内置字段：创建人 -->
      <if test="key == 'creator'">
        AND issues.creator IN
        <foreach collection="values" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
      </if>
      
      <!-- 内置字段：状态 -->
      <if test="key == 'status'">
        AND issues.status IN
        <foreach collection="values" item="value" open="(" close=")" separator=",">
          #{value}
        </foreach>
      </if>
      
      <!-- 自定义字段：custom_single -->
      <if test="key.startsWith('custom_single-')">
        <bind name="fieldId" value="key.substring(14)" />
        AND issues.id IN (
          SELECT resource_id FROM issue_custom_field
          WHERE field_id = #{fieldId}
          AND value IN
          <foreach collection="values" item="value" open="(" close=")" separator=",">
            #{value}
          </foreach>
        )
      </if>
      
      <!-- 自定义字段：custom_multiple -->
      <if test="key.startsWith('custom_multiple-')">
        <bind name="fieldId" value="key.substring(16)" />
        AND issues.id IN (
          SELECT resource_id FROM issue_custom_field
          WHERE field_id = #{fieldId}
          AND value IN
          <foreach collection="values" item="value" open="(" close=")" separator=",">
            #{value}
          </foreach>
        )
      </if>
      
    </if>
  </foreach>
</if>
```

---

### 2.9 filters vs combine 对比

| 特性 | filters | combine |
|-----|---------|---------|
| 匹配方式 | 精确匹配（IN） | 支持模糊匹配（LIKE） |
| 数据结构 | `{ key: [values] }` | `{ key: { operator, value } }` |
| 翻页保持 | ✅ 是 | ✅ 是 |
| 搜索保持 | ❌ 否（可选） | ✅ 是 |
| 使用场景 | 表头筛选、权限过滤 | 高级搜索 |
| 后端处理 | 简单（IN查询） | 复杂（支持多种操作符） |

**示例对比**：
```javascript
// filters：精确匹配
filters: {
  'creator': ['user-001', 'user-002']
}
// SQL: WHERE creator IN ('user-001', 'user-002')

// combine：模糊匹配
combine: {
  'title': { operator: 'like', value: '缺陷' }
}
// SQL: WHERE title LIKE '%缺陷%'
```

---

### 2.10 最佳实践

**1. 权限过滤使用 filters**
```javascript
// ✅ 正确
this.page.condition.filters['creator'] = [currentUserId];

// ❌ 错误：使用 combine（会被搜索保留）
this.page.condition.combine['creator'] = { operator: 'in', value: [currentUserId] };
```

**2. 使用 generateColumnKey 生成 key**
```javascript
// ✅ 正确
const filterKey = generateColumnKey(field);
this.page.condition.filters[filterKey] = [value];

// ❌ 错误：手动拼接
this.page.condition.filters['custom_single-' + field.id] = [value];
```

**3. 记录需要清除的 key**
```javascript
// ✅ 正确：记录 key，方便清除
this.userGroupFilterKeys.push(filterKey);
this.page.condition.filters[filterKey] = [userId];

// 清除时
this.userGroupFilterKeys.forEach(key => {
  delete this.page.condition.filters[key];
});

// ❌ 错误：不记录 key，无法精确清除
this.page.condition.filters = {};  // 会清除所有过滤条件
```

**4. 确保 filters 对象存在**
```javascript
// ✅ 正确
if (!this.page.condition.filters) {
  this.page.condition.filters = {};
}
this.page.condition.filters['creator'] = [userId];

// ❌ 错误：不检查，可能报错
this.page.condition.filters['creator'] = [userId];  // TypeError: Cannot set property
```

---

## 三、小结

**filters 的核心要点**：

1. **精确匹配**：用于表头筛选和权限过滤
2. **数据结构**：`{ key: [values] }`
3. **翻页保持**：翻页时不清除
4. **搜索清除**：搜索时可选择性清除
5. **自定义字段**：使用 `generateColumnKey` 生成 key
6. **后端处理**：IN 查询或子查询

**下一篇**：combine（组合查询）详解
