# page.condition 核心机制深度解析（Part 2）

## 一、combine（组合查询）

### 1.1 核心概念

**作用**：支持模糊匹配、范围查询的高级搜索

**数据结构**：
```javascript
combine: {
  'fieldKey': {
    operator: 'like',      // 操作符
    value: '搜索值'        // 值
  }
}
```

**特点**：
- ✅ 支持模糊匹配（LIKE）
- ✅ 支持范围查询（BETWEEN、GT、LT）
- ✅ 翻页时保持
- ✅ 搜索时保持
- ✅ 支持多种操作符

---

### 1.2 支持的操作符

**操作符列表**：
```javascript
// 位置：framework/sdk-parent/frontend/src/components/search/search-components.js

export const OPERATORS = {
  LIKE: { label: "包含", value: "like" },
  NOT_LIKE: { label: "不包含", value: "not like" },
  IN: { label: "属于", value: "in" },
  NOT_IN: { label: "不属于", value: "not in" },
  GT: { label: "大于", value: "gt" },
  GE: { label: "大于等于", value: "ge" },
  LT: { label: "小于", value: "lt" },
  LE: { label: "小于等于", value: "le" },
  EQ: { label: "等于", value: "eq" },
  BETWEEN: { label: "之间", value: "between" },
  CURRENT_USER: { label: "当前用户", value: "current user" }
}
```

---

### 1.3 模糊匹配（LIKE）

**示例1：标题模糊搜索**
```javascript
// 前端设置
this.page.condition.combine['title'] = {
  operator: 'like',
  value: '缺陷'
};

// 后端SQL
WHERE issues.title LIKE '%缺陷%'
```

**示例2：描述模糊搜索**
```javascript
// 前端设置
this.page.condition.combine['description'] = {
  operator: 'like',
  value: '登录失败'
};

// 后端SQL
WHERE issues.description LIKE '%登录失败%'
```

**示例3：NOT LIKE（不包含）**
```javascript
// 前端设置
this.page.condition.combine['title'] = {
  operator: 'not like',
  value: '测试'
};

// 后端SQL
WHERE issues.title NOT LIKE '%测试%'
```

---

### 1.4 范围查询（BETWEEN）

**示例1：创建时间范围**
```javascript
// 前端设置
this.page.condition.combine['createTime'] = {
  operator: 'between',
  value: [1704067200000, 1735689599000]  // 2024-01-01 到 2024-12-31
};

// 后端SQL
WHERE issues.create_time BETWEEN 1704067200000 AND 1735689599000
```

**示例2：更新时间范围**
```javascript
// 前端设置
this.page.condition.combine['updateTime'] = {
  operator: 'between',
  value: ['2024-01-01 00:00:00', '2024-12-31 23:59:59']
};

// 后端SQL
WHERE issues.update_time BETWEEN '2024-01-01 00:00:00' AND '2024-12-31 23:59:59'
```

---

### 1.5 比较查询（GT、LT、GE、LE、EQ）

**示例1：大于（GT）**
```javascript
// 前端设置
this.page.condition.combine['createTime'] = {
  operator: 'gt',
  value: 1704067200000
};

// 后端SQL
WHERE issues.create_time > 1704067200000
```

**示例2：小于等于（LE）**
```javascript
// 前端设置
this.page.condition.combine['updateTime'] = {
  operator: 'le',
  value: 1735689599000
};

// 后端SQL
WHERE issues.update_time <= 1735689599000
```

**示例3：等于（EQ）**
```javascript
// 前端设置
this.page.condition.combine['priority'] = {
  operator: 'eq',
  value: 'P0'
};

// 后端SQL
WHERE issues.priority = 'P0'
```

---

### 1.6 自定义字段的 combine 查询

**关键点**：自定义字段的 combine 查询需要特殊处理

**数据结构**：
```javascript
combine: {
  customs: [
    {
      id: 'field-001',           // 字段ID
      name: '处理人',            // 字段名称
      type: 'member',            // 字段类型
      operator: 'in',            // 操作符
      value: ['user-001']        // 值
    }
  ]
}
```

**示例1：自定义字段模糊搜索**
```javascript
// 前端设置
this.page.condition.combine.customs = [
  {
    id: 'field-001',
    name: '缺陷描述',
    type: 'richText',
    operator: 'like',
    value: '登录'
  }
];

// 后端SQL
WHERE issues.id IN (
  SELECT resource_id FROM issue_custom_field
  WHERE field_id = 'field-001'
  AND value LIKE '%登录%'
)
```

**示例2：自定义字段范围查询**
```javascript
// 前端设置
this.page.condition.combine.customs = [
  {
    id: 'field-002',
    name: '预计工时',
    type: 'int',
    operator: 'between',
    value: [1, 10]
  }
];

// 后端SQL
WHERE issues.id IN (
  SELECT resource_id FROM issue_custom_field
  WHERE field_id = 'field-002'
  AND CAST(value AS SIGNED) BETWEEN 1 AND 10
)
```

---

### 1.7 后端SQL处理

**位置**：`test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`

**SQL片段**：
```xml
<!-- 处理 combine 参数 -->
<if test="request.combine != null">
  
  <!-- 内置字段 -->
  <foreach collection="request.combine.entrySet()" index="key" item="condition">
    <if test="key != 'customs'">
      
      <!-- LIKE 操作符 -->
      <if test="condition.operator == 'like'">
        AND ${key} LIKE CONCAT('%', #{condition.value}, '%')
      </if>
      
      <!-- NOT LIKE 操作符 -->
      <if test="condition.operator == 'not like'">
        AND ${key} NOT LIKE CONCAT('%', #{condition.value}, '%')
      </if>
      
      <!-- BETWEEN 操作符 -->
      <if test="condition.operator == 'between'">
        AND ${key} BETWEEN #{condition.value[0]} AND #{condition.value[1]}
      </if>
      
      <!-- GT 操作符 -->
      <if test="condition.operator == 'gt'">
        AND ${key} > #{condition.value}
      </if>
      
      <!-- LT 操作符 -->
      <if test="condition.operator == 'lt'">
        AND ${key} < #{condition.value}
      </if>
      
      <!-- GE 操作符 -->
      <if test="condition.operator == 'ge'">
        AND ${key} >= #{condition.value}
      </if>
      
      <!-- LE 操作符 -->
      <if test="condition.operator == 'le'">
        AND ${key} <= #{condition.value}
      </if>
      
      <!-- EQ 操作符 -->
      <if test="condition.operator == 'eq'">
        AND ${key} = #{condition.value}
      </if>
      
    </if>
  </foreach>
  
  <!-- 自定义字段 -->
  <if test="request.combine.customs != null and request.combine.customs.size() > 0">
    <foreach collection="request.combine.customs" item="custom">
      
      <!-- LIKE 操作符 -->
      <if test="custom.operator == 'like'">
        AND issues.id IN (
          SELECT resource_id FROM issue_custom_field
          WHERE field_id = #{custom.id}
          AND value LIKE CONCAT('%', #{custom.value}, '%')
        )
      </if>
      
      <!-- BETWEEN 操作符 -->
      <if test="custom.operator == 'between'">
        AND issues.id IN (
          SELECT resource_id FROM issue_custom_field
          WHERE field_id = #{custom.id}
          AND CAST(value AS SIGNED) BETWEEN #{custom.value[0]} AND #{custom.value[1]}
        )
      </if>
      
    </foreach>
  </if>
  
</if>
```

---

### 1.8 combine vs filters 使用场景

| 场景 | 使用 | 原因 |
|-----|------|------|
| 表头筛选 | filters | 精确匹配，翻页保持 |
| 权限过滤 | filters | 精确匹配，可选择性清除 |
| 标题搜索 | combine | 需要模糊匹配 |
| 时间范围 | combine | 需要范围查询 |
| 高级搜索 | combine | 支持多种操作符 |

---

## 二、orders（排序规则）

### 2.1 核心概念

**作用**：定义列表的排序规则

**数据结构**：
```javascript
orders: [
  { name: 'create_time', type: 'desc' },  // 按创建时间降序
  { name: 'num', type: 'asc' }            // 按编号升序
]
```

**特点**：
- ✅ 支持多字段排序
- ✅ 支持升序（asc）和降序（desc）
- ✅ 翻页时保持
- ✅ 搜索时保持

---

### 2.2 单字段排序

**示例1：按创建时间降序**
```javascript
// 前端设置
this.page.condition.orders = [
  { name: 'create_time', type: 'desc' }
];

// 后端SQL
ORDER BY create_time DESC
```

**示例2：按编号升序**
```javascript
// 前端设置
this.page.condition.orders = [
  { name: 'num', type: 'asc' }
];

// 后端SQL
ORDER BY num ASC
```

**示例3：按更新时间降序**
```javascript
// 前端设置
this.page.condition.orders = [
  { name: 'update_time', type: 'desc' }
];

// 后端SQL
ORDER BY update_time DESC
```

---

### 2.3 多字段排序

**示例：先按优先级降序，再按创建时间降序**
```javascript
// 前端设置
this.page.condition.orders = [
  { name: 'priority', type: 'desc' },
  { name: 'create_time', type: 'desc' }
];

// 后端SQL
ORDER BY priority DESC, create_time DESC
```

**执行顺序**：
1. 先按 `priority` 降序排序
2. 如果 `priority` 相同，再按 `create_time` 降序排序

---

### 2.4 自定义字段排序

**关键点**：自定义字段的排序需要特殊处理

**示例：按自定义字段"处理人"排序**
```javascript
// 前端设置
this.page.condition.orders = [
  { name: 'custom_field_handler', type: 'asc' }
];

// 后端SQL
ORDER BY (
  SELECT value FROM issue_custom_field
  WHERE resource_id = issues.id
  AND field_id = 'field-001'
  LIMIT 1
) ASC
```

---

### 2.5 getLastTableSortField 函数

**位置**：`framework/sdk-parent/frontend/src/utils/tableUtils.js`

**作用**：从 localStorage 获取上次的排序规则

**使用示例**：
```javascript
// IssueList.vue
getIssues() {
  // 获取上次的排序规则
  this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);
  // 结果：[{ name: 'create_time', type: 'desc' }]
  
  // 发送请求
  getIssues(this.page.currentPage, this.page.pageSize, this.page.condition);
}
```

**localStorage 存储**：
```javascript
// 存储格式
localStorage.setItem('ISSUE_LIST_SORT', JSON.stringify([
  { name: 'create_time', type: 'desc' }
]));

// 读取
const orders = JSON.parse(localStorage.getItem('ISSUE_LIST_SORT'));
```

---

### 2.6 表头点击排序

**MsTable 组件自动处理**：
```vue
<!-- IssueList.vue -->
<ms-table
  :data="page.data"
  :condition="page.condition"
  @order="getIssues"
>
  <ms-table-column
    prop="createTime"
    label="创建时间"
    sortable="custom"
  />
</ms-table>
```

**点击表头时**：
1. MsTable 自动更新 `page.condition.orders`
2. 触发 `@order` 事件
3. 调用 `getIssues()` 重新加载数据
4. 将排序规则保存到 localStorage

---

### 2.7 后端SQL处理

**位置**：`test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`

**SQL片段**：
```xml
<!-- 处理 orders 参数 -->
<if test="request.orders != null and request.orders.size() > 0">
  ORDER BY
  <foreach collection="request.orders" item="order" separator=",">
    ${order.name} ${order.type}
  </foreach>
</if>

<!-- 默认排序 -->
<if test="request.orders == null or request.orders.size() == 0">
  ORDER BY create_time DESC
</if>
```

---

### 2.8 排序最佳实践

**1. 始终提供默认排序**
```javascript
// ✅ 正确
if (!this.page.condition.orders || this.page.condition.orders.length === 0) {
  this.page.condition.orders = [{ name: 'create_time', type: 'desc' }];
}

// ❌ 错误：不提供默认排序（数据顺序不确定）
```

**2. 使用 getLastTableSortField 恢复排序**
```javascript
// ✅ 正确
this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);

// ❌ 错误：不恢复排序（用户体验差）
this.page.condition.orders = [];
```

**3. 多字段排序时注意顺序**
```javascript
// ✅ 正确：先按优先级，再按时间
this.page.condition.orders = [
  { name: 'priority', type: 'desc' },
  { name: 'create_time', type: 'desc' }
];

// ❌ 错误：顺序反了
this.page.condition.orders = [
  { name: 'create_time', type: 'desc' },
  { name: 'priority', type: 'desc' }
];
```

---

## 三、小结

### 3.1 combine 核心要点

1. **模糊匹配**：支持 LIKE、NOT LIKE
2. **范围查询**：支持 BETWEEN、GT、LT、GE、LE
3. **数据结构**：`{ key: { operator, value } }`
4. **翻页保持**：翻页时不清除
5. **搜索保持**：搜索时保持
6. **自定义字段**：使用 `combine.customs` 数组

### 3.2 orders 核心要点

1. **多字段排序**：支持多个字段组合排序
2. **数据结构**：`[{ name, type }]`
3. **翻页保持**：翻页时不清除
4. **搜索保持**：搜索时保持
5. **持久化**：保存到 localStorage
6. **默认排序**：始终提供默认排序

### 3.3 三者对比

| 参数 | 匹配方式 | 翻页保持 | 搜索保持 | 使用场景 |
|-----|---------|---------|---------|---------|
| filters | 精确匹配 | ✅ | ❌（可选） | 表头筛选、权限过滤 |
| combine | 模糊匹配 | ✅ | ✅ | 高级搜索 |
| orders | 排序 | ✅ | ✅ | 列排序 |

**下一篇**：components（搜索组件配置）详解
