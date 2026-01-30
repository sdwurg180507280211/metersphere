# 快速 SQL 查询功能 - UI 设计方案

## 文档版本
- 版本：v1.0
- 日期：2024-12-25
- 设计目标：为功能用例列表页提供专家级 SQL 查询能力，兼顾易用性与专业性

---

## 1. 设计理念

### 1.1 核心目标
- **专业性**：提供类 Navicat 的 SQL 编辑体验，满足专家用户需求
- **易用性**：通过字段助手、SQL 片段等降低使用门槛
- **非侵入性**：不干扰业务列表的主流程，可快速开启/关闭
- **安全性**：前端提示 + 后端强制校验，双重保障

### 1.2 设计原则
- **渐进式增强**：默认隐藏，按需展开
- **即时反馈**：执行状态、结果信息实时展示
- **减少记忆负担**：提供字段列表、SQL 模板、快捷键提示
- **保持一致性**：复用项目现有组件体系（MsCodeEdit、Element UI）

---

## 2. 整体布局方案

### 2.1 入口形态对比

| 形态 | 优点 | 缺点 | 适用场景 | 推荐度 |
|------|------|------|----------|--------|
| **抽屉(Drawer)** | • 不占列表空间<br>• 可全屏专注<br>• 关闭后状态保留<br>• 可调整宽度 | • 需额外点击打开 | 需要专注编辑 SQL 的场景 | ⭐⭐⭐⭐⭐ |
| 折叠面板(Collapse) | • 切换快速<br>• 可见性高 | • 占用列表空间<br>• 展开后列表被挤压<br>• 宽度受限 | 简单快速查询 | ⭐⭐⭐ |
| 弹窗(Modal) | • 独立空间<br>• 聚焦性强 | • 完全遮挡列表<br>• 无法边看边写<br>• 关闭即丢失状态 | 不推荐 | ⭐⭐ |

### 2.2 推荐方案：**右侧抽屉 + 三区域布局**

```
┌─────────────────────────────────────────────────────────────┐
│  功能用例列表页                       [SQL 快速查询 🔍]      │
├─────────────────────────────────────────────────────────────┤
│                                   │                          │
│                                   │  ┌─ 抽屉标题 ─────────┐ │
│                                   │  │ SQL 快速查询    [×] │ │
│   业务列表区域                    │  ├─────────────────────┤ │
│   (不受影响)                      │  │ ┌─ 字段助手 ───┐  │ │
│                                   │  │ │ [搜索框...]    │  │ │
│   ┌──────┬──────┬──────┐        │  │ │               │  │ │
│   │ ID   │ 名称 │ 状态 │        │  │ │ □ test_case   │  │ │
│   ├──────┼──────┼──────┤        │  │ │   ⊕ id        │  │ │
│   │ 001  │ xxx  │ 通过 │        │  │ │   ⊕ name      │  │ │
│   │ 002  │ yyy  │ 失败 │        │  │ │   ⊕ status    │  │ │
│   └──────┴──────┴──────┘        │  │ │               │  │ │
│                                   │  │ │ ○ SQL 片段    │  │ │
│                                   │  │ │  [本项目]     │  │ │
│                                   │  │ │  [今日创建]   │  │ │
│                                   │  │ └───────────────┘  │ │
│                                   │  │                     │ │
│                                   │  │ ┌─ SQL 编辑器 ──┐ │ │
│                                   │  │ │ SELECT * FROM │ │ │
│                                   │  │ │ test_case     │ │ │
│                                   │  │ │ WHERE ...     │ │ │
│                                   │  │ │ LIMIT 200     │ │ │
│                                   │  │ └───────────────┘ │ │
│                                   │  │ [▶执行] [清空]    │ │
│                                   │  │                     │ │
│                                   │  │ ┌─ 执行结果 ────┐ │ │
│                                   │  │ │ ✓ 23ms | 2行  │ │ │
│                                   │  │ │ ┌──┬────┬───┐ │ │ │
│                                   │  │ │ │id│name│...│ │ │ │
│                                   │  │ │ └──┴────┴───┘ │ │ │
│                                   │  │ └───────────────┘ │ │
│                                   │  └─────────────────────┘ │
└───────────────────────────────────┴──────────────────────────┘
```

**布局参数：**
- 抽屉宽度：60%（最小 800px，最大 1200px）
- 字段助手宽度：200-240px（可折叠）
- 编辑器高度：200-300px（可调整）
- 结果区高度：最大 400px（超出滚动）

---

## 3. 核心组件设计

### 3.1 组件层级结构

```
QuickSqlPanel (公共组件)
├── FieldHelper (字段助手)
│   ├── TableList (表列表)
│   ├── FieldList (字段列表)
│   └── SqlSnippets (SQL 片段)
├── SqlEditor (SQL 编辑器)
│   ├── EditorToolbar (工具栏)
│   ├── MsCodeEdit (编辑器核心)
│   └── QuickActions (快捷操作)
└── QueryResult (查询结果)
    ├── ResultHeader (执行信息)
    └── ResultTable (结果表格)
```

### 3.2 组件 Props 设计

#### QuickSqlPanel（主组件）

```typescript
interface QuickSqlPanelProps {
  // 基础配置
  title?: string;                    // 面板标题，默认 "SQL 快速查询"
  visible?: boolean;                 // 是否显示（v-model）
  projectId: string;                 // 当前项目 ID（必填）
  
  // SQL 配置
  defaultSql?: string;               // 默认 SQL 模板
  executeUrl?: string;               // 执行接口地址
  metaUrl?: string;                  // 元数据接口地址
  
  // 限制配置
  limitDefault?: number;             // 默认 limit，默认 200
  limitMax?: number;                 // 最大 limit，默认 500
  
  // 表配置
  tables?: string[];                 // 允许查询的表白名单（前端提示用）
  defaultTable?: string;             // 默认表名
  tableAlias?: string;               // 默认表别名
  
  // UI 配置
  drawerSize?: string;               // 抽屉宽度，默认 "60%"
  editorHeight?: number;             // 编辑器高度，默认 240
  showFieldHelper?: boolean;         // 是否显示字段助手，默认 true
  
  // 功能开关
  enableExport?: boolean;            // 是否启用导出，默认 true
  enableFormat?: boolean;            // 是否启用格式化，默认 true
  enableCopy?: boolean;              // 是否启用复制，默认 true
}
```

#### 组件事件

```typescript
interface QuickSqlPanelEvents {
  'update:visible': (visible: boolean) => void;
  'before-execute': (sql: string) => boolean | Promise<boolean>;  // 返回 false 可阻止执行
  'executed': (result: QueryResult) => void;
  'error': (error: Error) => void;
  'export': (data: any[]) => void;
}
```

### 3.3 插槽设计

```vue
<template>
  <quick-sql-panel>
    <!-- 自定义工具栏 -->
    <template #toolbar>
      <el-button>自定义按钮</el-button>
    </template>
    
    <!-- 自定义提示区 -->
    <template #tips>
      <el-alert type="info">当前可查询表：test_case, test_plan</el-alert>
    </template>
    
    <!-- 自定义字段面板 -->
    <template #field-panel="{ tables, fields, onInsert }">
      <!-- 完全自定义字段展示方式 -->
    </template>
    
    <!-- 自定义结果区上方内容 -->
    <template #result-extra="{ result }">
      <div>共 {{ result.rowCount }} 条数据</div>
    </template>
  </quick-sql-panel>
</template>
```

---

## 4. 详细交互设计

### 4.1 字段助手（FieldHelper）

#### 4.1.1 布局结构

```vue
<template>
  <div class="field-helper" :class="{ collapsed }">
    <!-- 折叠/展开按钮 -->
    <div class="helper-header">
      <span>字段助手</span>
      <el-button 
        type="text" 
        icon="el-icon-s-fold" 
        @click="collapsed = !collapsed"
      />
    </div>
    
    <!-- 搜索框 -->
    <el-input
      v-model="searchKeyword"
      size="small"
      placeholder="搜索表或字段..."
      prefix-icon="el-icon-search"
      clearable
    />
    
    <!-- 表和字段列表 -->
    <el-collapse v-model="activeTables" accordion>
      <el-collapse-item
        v-for="table in filteredTables"
        :key="table.name"
        :title="table.displayName"
        :name="table.name"
      >
        <!-- 字段列表 -->
        <div class="field-list">
          <div
            v-for="field in table.fields"
            :key="field.name"
            class="field-item"
            @click="handleInsertField(table.name, field)"
            @dblclick="handleInsertFieldWithTable(table.name, field)"
          >
            <i :class="getFieldTypeIcon(field.type)" class="field-icon" />
            <span class="field-name">{{ field.name }}</span>
            <el-tag size="mini" type="info" class="field-type">
              {{ field.type }}
            </el-tag>
            <el-tooltip v-if="field.comment" placement="right">
              <i class="el-icon-info" />
              <template #content>{{ field.comment }}</template>
            </el-tooltip>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
    
    <!-- SQL 片段 -->
    <div class="sql-snippets">
      <div class="snippets-title">常用片段</div>
      <div class="snippet-buttons">
        <el-button
          v-for="snippet in snippets"
          :key="snippet.key"
          size="mini"
          @click="handleInsertSnippet(snippet)"
        >
          {{ snippet.label }}
        </el-button>
      </div>
    </div>
  </div>
</template>
```

#### 4.1.2 交互行为

| 操作 | 行为 | 示例 |
|------|------|------|
| **单击字段** | 插入带反引号的字段名到光标位置 | `` `id` `` |
| **双击字段** | 插入带表前缀的字段名 | `` `test_case`.`id` `` |
| **Ctrl+单击** | 追加到 SELECT 子句 | `SELECT id, name, ...` |
| **搜索框输入** | 实时过滤表和字段（支持拼音） | 输入 "cj" 匹配 "create_time" |
| **点击片段按钮** | 插入预设 SQL 片段 | `WHERE project_id = '${projectId}'` |

#### 4.1.3 SQL 片段配置（示例）

```typescript
const snippets = [
  {
    key: 'current_project',
    label: '本项目',
    sql: "WHERE project_id = '${projectId}'",
    description: '过滤当前项目数据'
  },
  {
    key: 'today',
    label: '今日创建',
    sql: 'WHERE DATE(create_time) = CURDATE()',
    description: '今天创建的记录'
  },
  {
    key: 'this_week',
    label: '本周创建',
    sql: 'WHERE YEARWEEK(create_time) = YEARWEEK(NOW())',
    description: '本周创建的记录'
  },
  {
    key: 'recent_update',
    label: '最近更新',
    sql: 'ORDER BY update_time DESC',
    description: '按更新时间倒序'
  },
  {
    key: 'limit_50',
    label: 'LIMIT 50',
    sql: 'LIMIT 50',
    description: '限制返回 50 条'
  },
  {
    key: 'limit_200',
    label: 'LIMIT 200',
    sql: 'LIMIT 200',
    description: '限制返回 200 条'
  }
];
```

### 4.2 SQL 编辑器（SqlEditor）

#### 4.2.1 工具栏设计

```vue
<template>
  <div class="sql-editor-wrapper">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button-group size="small">
          <el-button @click="handleFormat">
            <i class="el-icon-magic-stick" /> 格式化
          </el-button>
          <el-button @click="handleShowTemplates">
            <i class="el-icon-document-add" /> 模板
          </el-button>
          <el-button @click="handleValidate">
            <i class="el-icon-circle-check" /> 检查
          </el-button>
        </el-button-group>
      </div>
      
      <div class="toolbar-right">
        <el-tag size="mini" type="info" effect="plain">
          <i class="el-icon-info" /> ⌘+Enter 执行
        </el-tag>
        <el-tag size="mini" type="warning" effect="plain">
          <i class="el-icon-warning" /> 最多返回 {{ limitMax }} 行
        </el-tag>
      </div>
    </div>
    
    <!-- SQL 编辑器 -->
    <ms-code-edit
      ref="editorRef"
      v-model="sql"
      mode="sql"
      :height="editorHeight"
      :options="editorOptions"
      @keydown.meta.enter.prevent="handleExecute"
      @keydown.ctrl.enter.prevent="handleExecute"
    />
    
    <!-- 底部操作栏 -->
    <div class="editor-actions">
      <el-button
        type="primary"
        size="small"
        :loading="executing"
        :disabled="!sql.trim()"
        @click="handleExecute"
      >
        <i class="el-icon-video-play" /> 执行查询 (⌘Enter)
      </el-button>
      
      <el-button size="small" @click="handleClear">
        <i class="el-icon-delete" /> 清空
      </el-button>
      
      <el-button size="small" @click="handleReset">
        <i class="el-icon-refresh-left" /> 重置为默认
      </el-button>
      
      <div class="action-extra">
        <el-checkbox v-model="autoLimit" size="small">
          自动添加 LIMIT
        </el-checkbox>
      </div>
    </div>
  </div>
</template>
```

#### 4.2.2 编辑器配置

```typescript
const editorOptions = {
  fontSize: 14,
  fontFamily: 'Consolas, Monaco, "Courier New", monospace',
  tabSize: 2,
  useSoftTabs: true,
  wrap: true,
  showPrintMargin: false,
  highlightActiveLine: true,
  highlightSelectedWord: true,
  enableBasicAutocompletion: true,
  enableLiveAutocompletion: true,
  enableSnippets: true
};
```

#### 4.2.3 前端校验规则

| 校验项 | 校验时机 | 提示方式 | 是否阻断 |
|--------|----------|----------|----------|
| **SQL 为空** | 点击执行 | 轻提示 | ✅ 阻断 |
| **包含非 SELECT** | 点击执行 | 警告弹窗 | ✅ 阻断 |
| **包含 JOIN** | 点击执行 | 警告提示 | ⚠️ 可继续（后端会拦截） |
| **无 LIMIT** | 点击执行 | 提示框：是否自动添加 | ❌ 不阻断 |
| **LIMIT 超限** | 实时 | 编辑器底部警告栏 | ❌ 不阻断（后端会截断） |
| **多语句（;）** | 点击执行 | 错误提示 | ✅ 阻断 |

#### 4.2.4 SQL 模板弹窗

```vue
<el-dialog title="选择 SQL 模板" :visible.sync="templateDialogVisible">
  <el-radio-group v-model="selectedTemplate" class="template-list">
    <el-radio
      v-for="tpl in templates"
      :key="tpl.key"
      :label="tpl.key"
      class="template-item"
    >
      <div class="template-title">{{ tpl.name }}</div>
      <div class="template-desc">{{ tpl.description }}</div>
      <pre class="template-sql">{{ tpl.sql }}</pre>
    </el-radio>
  </el-radio-group>
  
  <template #footer>
    <el-button @click="templateDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleApplyTemplate">应用</el-button>
  </template>
</el-dialog>
```

**预设模板示例：**

```typescript
const templates = [
  {
    key: 'basic_query',
    name: '基础查询',
    description: '查询当前项目的所有用例',
    sql: `SELECT id, name, status, priority
FROM test_case
WHERE project_id = '\${projectId}'
ORDER BY create_time DESC
LIMIT 50`
  },
  {
    key: 'recent_failed',
    name: '近期失败用例',
    description: '查询最近 7 天内执行失败的用例',
    sql: `SELECT id, name, last_execute_time, last_execute_result
FROM test_case
WHERE project_id = '\${projectId}'
  AND last_execute_result = 'FAILED'
  AND last_execute_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY last_execute_time DESC
LIMIT 100`
  },
  {
    key: 'unexecuted',
    name: '未执行用例统计',
    description: '统计从未执行过的用例数量',
    sql: `SELECT priority, COUNT(*) as count
FROM test_case
WHERE project_id = '\${projectId}'
  AND (last_execute_time IS NULL OR last_execute_time = '')
GROUP BY priority
ORDER BY priority`
  }
];
```

### 4.3 查询结果（QueryResult）

#### 4.3.1 执行状态展示

```vue
<template>
  <div class="query-result">
    <!-- 执行信息栏 -->
    <div v-if="result" class="result-header">
      <!-- 成功状态 -->
      <div v-if="result.success" class="status-success">
        <i class="el-icon-success" />
        <span class="status-text">查询成功</span>
        <el-divider direction="vertical" />
        <span class="elapsed-time">
          <i class="el-icon-time" /> {{ result.elapsedMs }} ms
        </span>
        <el-divider direction="vertical" />
        <span class="row-count">
          <i class="el-icon-document" /> {{ result.rowCount }} 行
        </span>
        
        <!-- 截断提示 -->
        <el-tag
          v-if="result.truncated"
          type="warning"
          size="mini"
          effect="plain"
        >
          <i class="el-icon-warning" />
          已截断（超过 LIMIT {{ limitMax }}）
        </el-tag>
      </div>
      
      <!-- 失败状态 -->
      <div v-else class="status-error">
        <i class="el-icon-error" />
        <span class="status-text">查询失败</span>
        <el-divider direction="vertical" />
        <span class="error-message">{{ result.error }}</span>
      </div>
      
      <!-- 操作按钮 -->
      <div class="result-actions">
        <el-button-group size="mini">
          <el-button @click="handleExportExcel">
            <i class="el-icon-download" /> 导出 Excel
          </el-button>
          <el-button @click="handleCopyAsJson">
            <i class="el-icon-document-copy" /> 复制 JSON
          </el-button>
          <el-button @click="handleCopyAsTable">
            <i class="el-icon-document-copy" /> 复制表格
          </el-button>
        </el-button-group>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-if="!result" class="result-empty">
      <i class="el-icon-info" />
      <p>请编写 SQL 并点击执行</p>
    </div>
    
    <!-- 结果表格 -->
    <el-table
      v-if="result && result.success"
      :data="result.rows"
      border
      stripe
      size="small"
      max-height="400"
      :empty-text="result.rowCount === 0 ? '查询结果为空' : '加载中...'"
      style="width: 100%"
    >
      <el-table-column type="index" label="#" width="50" fixed />
      
      <el-table-column
        v-for="col in result.columns"
        :key="col"
        :prop="col"
        :label="col"
        min-width="120"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span :class="getCellClass(row[col])">
            {{ formatCellValue(row[col]) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

#### 4.3.2 数据格式化

```typescript
// 单元格值格式化
function formatCellValue(value: any): string {
  if (value === null || value === undefined) {
    return 'NULL';
  }
  if (typeof value === 'boolean') {
    return value ? 'TRUE' : 'FALSE';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

// 单元格样式
function getCellClass(value: any): string {
  if (value === null || value === undefined) {
    return 'cell-null';
  }
  if (typeof value === 'number') {
    return 'cell-number';
  }
  if (typeof value === 'boolean') {
    return 'cell-boolean';
  }
  return 'cell-string';
}
```

#### 4.3.3 导出功能

**导出 Excel：**
```typescript
async function handleExportExcel() {
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet(
    result.rows.map(row => {
      const obj: any = {};
      result.columns.forEach((col, idx) => {
        obj[col] = row[idx];
      });
      return obj;
    })
  );
  
  XLSX.utils.book_append_sheet(workbook, worksheet, 'QueryResult');
  XLSX.writeFile(workbook, `sql_result_${Date.now()}.xlsx`);
}
```

**复制为 JSON：**
```typescript
function handleCopyAsJson() {
  const data = result.rows.map(row => {
    const obj: any = {};
    result.columns.forEach((col, idx) => {
      obj[col] = row[idx];
    });
    return obj;
  });
  
  navigator.clipboard.writeText(JSON.stringify(data, null, 2));
  ElMessage.success('已复制到剪贴板');
}
```

**复制为 Markdown 表格：**
```typescript
function handleCopyAsTable() {
  let markdown = '| ' + result.columns.join(' | ') + ' |\n';
  markdown += '| ' + result.columns.map(() => '---').join(' | ') + ' |\n';
  
  result.rows.forEach(row => {
    markdown += '| ' + row.join(' | ') + ' |\n';
  });
  
  navigator.clipboard.writeText(markdown);
  ElMessage.success('已复制为 Markdown 表格');
}
```

---

## 5. 完整交互流程

### 5.1 用户操作路径

```
主流程：
┌─────────────────┐
│ 1. 点击工具栏   │
│ "SQL 快速查询" │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. 抽屉滑出     │
│ 加载默认 SQL    │
└────────┬────────┘
         │
         ▼
┌─────────────────────┐
│ 3. 编写/修改 SQL    │
│ • 手写               │
│ • 点击字段插入       │
│ • 使用 SQL 片段     │
│ • 应用模板          │
└────────┬────────────┘
         │
         ▼
┌─────────────────┐
│ 4. 执行查询     │
│ (⌘+Enter)       │
└────────┬────────┘
         │
         ▼
┌─────────────────────┐
│ 5. 查看结果         │
│ • 表格展示          │
│ • 查看执行信息      │
│ • 导出/复制数据     │
└────────┬────────────┘
         │
         ▼
┌─────────────────┐
│ 6. 继续调整     │
│ 或关闭抽屉      │
└─────────────────┘
```

### 5.2 关键交互时序图

```
用户            前端组件           后端API          数据库
 │                │                  │                │
 │  点击"执行"    │                  │                │
 ├───────────────>│                  │                │
 │                │  前端校验SQL    │                │
 │                ├─────────┐       │                │
 │                │         │       │                │
 │                │<────────┘       │                │
 │                │                  │                │
 │                │  POST /execute   │                │
 │                ├─────────────────>│                │
 │                │                  │  安全校验     │
 │                │                  ├──────┐        │
 │                │                  │      │        │
 │                │                  │<─────┘        │
 │                │                  │                │
 │                │                  │  执行查询     │
 │                │                  ├───────────────>│
 │                │                  │                │
 │  显示Loading  │                  │      查询中... │
 │<───────────────┤                  │                │
 │                │                  │                │
 │                │                  │  返回结果     │
 │                │                  │<───────────────┤
 │                │                  │                │
 │                │  返回响应        │                │
 │                │<─────────────────┤                │
 │                │  写审计日志      │                │
 │                │                  ├──────┐         │
 │                │                  │      │         │
 │                │                  │<─────┘         │
 │                │                  │                │
 │  展示结果      │                  │                │
 │<───────────────┤                  │                │
 │                │                  │                │
```

### 5.3 错误处理流程

| 错误类型 | 检测位置 | 提示方式 | 恢复操作 |
|----------|----------|----------|----------|
| **SQL 语法错误** | 后端 | 错误信息栏展示详细错误 | 修改 SQL 后重试 |
| **权限不足** | 后端 | 弹窗提示 + 引导联系管理员 | - |
| **超时** | 后端（5-10s） | 错误信息栏："查询超时，请优化 SQL" | 简化 SQL 或添加索引 |
| **表不存在** | 后端 | 错误信息栏 + 显示可用表列表 | 检查表名拼写 |
| **LIMIT 超限** | 后端 | 警告提示："已截断为 200 行" | 增大 LIMIT 上限（需权限） |
| **网络错误** | 前端 | 重试按钮 + 错误详情 | 点击重试 |

---

## 6. 视觉设计规范

### 6.1 颜色体系

```scss
// 主题色
$primary-color: #409EFF;        // 主色（执行按钮、高亮）
$success-color: #67C23A;        // 成功状态
$warning-color: #E6A23C;        // 警告（截断提示）
$danger-color: #F56C6C;         // 错误状态
$info-color: #909399;           // 信息提示

// 背景色
$bg-white: #FFFFFF;
$bg-light: #F5F7FA;             // 字段助手背景
$bg-code: #FAFAFA;              // 代码编辑器背景
$bg-hover: #ECF5FF;             // 悬停背景

// 边框色
$border-base: #DCDFE6;
$border-light: #E4E7ED;

// 文本色
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$text-placeholder: #C0C4CC;
```

### 6.2 字体规范

```scss
// 代码字体
$font-code: 'Consolas', 'Monaco', 'Courier New', monospace;

// 常规字体
$font-base: -apple-system, BlinkMacSystemFont, 'Segoe UI', 
            'Helvetica Neue', Arial, sans-serif;

// 字号
$font-size-large: 16px;         // 标题
$font-size-base: 14px;          // 正文
$font-size-small: 13px;         // 辅助文本
$font-size-mini: 12px;          // 提示信息
```

### 6.3 间距规范

```scss
// 基础间距单位（8px 体系）
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-md: 16px;
$spacing-lg: 24px;
$spacing-xl: 32px;

// 组件内边距
$padding-base: 12px 16px;
$padding-small: 8px 12px;
$padding-mini: 4px 8px;
```

### 6.4 组件样式示例

#### 字段项悬停效果
```scss
.field-item {
  padding: $spacing-sm $spacing-md;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all 0.2s;
  
  &:hover {
    background-color: $bg-hover;
    border-left-color: $primary-color;
  }
  
  &:active {
    background-color: darken($bg-hover, 5%);
  }
}
```

#### SQL 编辑器样式
```scss
.ace_editor {
  border: 1px solid $border-base;
  border-radius: 4px;
  font-family: $font-code;
  font-size: 14px;
  line-height: 1.6;
  
  &:focus-within {
    border-color: $primary-color;
    box-shadow: 0 0 0 2px rgba($primary-color, 0.1);
  }
}
```

#### 执行状态栏样式
```scss
.result-header {
  padding: $spacing-md;
  background: linear-gradient(to right, #f5f7fa, #ffffff);
  border-bottom: 1px solid $border-light;
  
  .status-success {
    color: $success-color;
    
    .elapsed-time {
      color: $text-secondary;
      font-family: $font-code;
    }
  }
  
  .status-error {
    color: $danger-color;
    
    .error-message {
      font-family: $font-code;
      font-size: $font-size-small;
    }
  }
}
```

---

## 7. 响应式设计

### 7.1 断点定义

```scss
// 屏幕尺寸断点
$screen-xs: 768px;      // 小屏（平板竖屏）
$screen-sm: 992px;      // 中屏（平板横屏）
$screen-md: 1200px;     // 大屏（笔记本）
$screen-lg: 1920px;     // 超大屏（台式机）
```

### 7.2 自适应策略

| 屏幕尺寸 | 抽屉宽度 | 字段助手 | 编辑器高度 | 结果区高度 |
|----------|----------|----------|------------|------------|
| **< 768px** | 100% | 隐藏 | 150px | 250px |
| **768px - 992px** | 80% | 可折叠 | 180px | 300px |
| **992px - 1200px** | 70% | 显示 | 240px | 350px |
| **> 1200px** | 60% | 显示 | 240px | 400px |

### 7.3 小屏幕优化

```vue
<template>
  <div class="quick-sql-panel" :class="screenClass">
    <!-- 小屏幕：字段助手改为下拉菜单 -->
    <el-dropdown v-if="isSmallScreen" trigger="click">
      <el-button size="small">
        <i class="el-icon-menu" /> 插入字段
      </el-button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item
          v-for="field in allFields"
          :key="field.name"
          @click.native="insertField(field)"
        >
          {{ field.name }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    
    <!-- 正常屏幕：侧边栏 -->
    <field-helper v-else ... />
  </div>
</template>
```

---

## 8. 可访问性（A11y）

### 8.1 键盘导航

| 快捷键 | 功能 | 生效范围 |
|--------|------|----------|
| **Cmd/Ctrl + Enter** | 执行 SQL | 编辑器聚焦时 |
| **Cmd/Ctrl + K** | 清空编辑器 | 编辑器聚焦时 |
| **Cmd/Ctrl + Shift + F** | 格式化 SQL | 编辑器聚焦时 |
| **Esc** | 关闭抽屉 | 抽屉打开时 |
| **Tab** | 切换焦点 | 全局 |
| **↑/↓** | 选择字段 | 字段列表聚焦时 |
| **Enter** | 插入选中字段 | 字段列表聚焦时 |

### 8.2 屏幕阅读器支持

```vue
<template>
  <!-- ARIA 标签示例 -->
  <div
    role="region"
    aria-label="SQL 快速查询工具"
    class="quick-sql-panel"
  >
    <div
      role="textbox"
      aria-label="SQL 编辑器"
      aria-multiline="true"
    >
      <ms-code-edit ... />
    </div>
    
    <div
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      {{ statusMessage }}
    </div>
    
    <table
      role="grid"
      aria-label="查询结果表格"
      aria-rowcount="{{ result.rowCount }}"
    >
      ...
    </table>
  </div>
</template>
```

---

## 9. 性能优化

### 9.1 前端优化

| 优化项 | 实现方式 | 预期效果 |
|--------|----------|----------|
| **字段列表虚拟滚动** | `vue-virtual-scroller` | 支持 1000+ 字段流畅滚动 |
| **结果表格分页** | 客户端分页（每页 50 行） | 避免一次渲染大量 DOM |
| **防抖搜索** | `debounce(300ms)` | 减少过滤计算频率 |
| **代码高亮延迟** | `requestIdleCallback` | 避免阻塞主线程 |
| **组件懒加载** | 抽屉打开后再加载字段元数据 | 减少初始加载时间 |

### 9.2 示例：虚拟滚动实现

```vue
<template>
  <RecycleScroller
    :items="filteredFields"
    :item-size="32"
    key-field="name"
    class="field-list"
  >
    <template #default="{ item }">
      <div class="field-item" @click="insertField(item)">
        {{ item.name }}
      </div>
    </template>
  </RecycleScroller>
</template>
```

---

## 10. 多页面复用示例

### 10.1 功能用例列表页

```vue
<template>
  <div class="test-case-list-page">
    <!-- 工具栏 -->
    <div class="page-toolbar">
      <el-button type="primary" @click="sqlDrawerVisible = true">
        <i class="el-icon-s-operation" /> SQL 快速查询
      </el-button>
    </div>
    
    <!-- 业务列表 -->
    <el-table :data="caseList" ...>
      ...
    </el-table>
    
    <!-- SQL 查询抽屉 -->
    <el-drawer
      title="SQL 快速查询"
      :visible.sync="sqlDrawerVisible"
      direction="rtl"
      size="60%"
      :destroy-on-close="false"
    >
      <quick-sql-panel
        :project-id="currentProjectId"
        :default-sql="defaultSql"
        execute-url="/test/case/quick-sql/execute"
        meta-url="/test/case/quick-sql/meta"
        :tables="['test_case']"
        default-table="test_case"
        @executed="handleExecuted"
        @error="handleError"
      />
    </el-drawer>
  </div>
</template>

<script>
export default {
  data() {
    return {
      sqlDrawerVisible: false,
      defaultSql: `SELECT id, name, status, priority, create_time
FROM test_case
WHERE project_id = '${this.currentProjectId}'
ORDER BY create_time DESC
LIMIT 50`
    };
  },
  methods: {
    handleExecuted(result) {
      console.log('查询成功', result);
      // 可选：统计埋点
    },
    handleError(error) {
      this.$message.error('查询失败：' + error.message);
    }
  }
};
</script>
```

### 10.2 缺陷列表页（复用示例）

```vue
<template>
  <div class="issues-list-page">
    <el-drawer ...>
      <quick-sql-panel
        :project-id="currentProjectId"
        :default-sql="issueDefaultSql"
        execute-url="/issues/quick-sql/execute"
        meta-url="/issues/quick-sql/meta"
        :tables="['issues']"
        default-table="issues"
        :snippets="issueSnippets"
      />
    </el-drawer>
  </div>
</template>

<script>
export default {
  data() {
    return {
      issueDefaultSql: `SELECT id, title, status, severity, create_time
FROM issues
WHERE project_id = '${this.currentProjectId}'
  AND status != 'CLOSED'
ORDER BY severity DESC, create_time DESC
LIMIT 100`,
      
      // 自定义 SQL 片段
      issueSnippets: [
        {
          key: 'high_severity',
          label: '高危缺陷',
          sql: "WHERE severity IN ('BLOCKER', 'CRITICAL')"
        },
        {
          key: 'unresolved',
          label: '未解决',
          sql: "WHERE status NOT IN ('RESOLVED', 'CLOSED')"
        }
      ]
    };
  }
};
</script>
```

---

## 11. 开发清单

### 11.1 前端组件开发

- [ ] **QuickSqlPanel.vue**（主组件）
  - [ ] 基础结构和 props 定义
  - [ ] 抽屉/折叠面板模式切换
  - [ ] 事件和插槽系统
  
- [ ] **FieldHelper.vue**（字段助手）
  - [ ] 表和字段列表渲染
  - [ ] 搜索和过滤功能
  - [ ] 字段插入逻辑
  - [ ] SQL 片段按钮
  
- [ ] **SqlEditor.vue**（SQL 编辑器）
  - [ ] 集成 MsCodeEdit
  - [ ] 工具栏功能（格式化、模板、检查）
  - [ ] 快捷键绑定
  - [ ] 前端 SQL 校验
  
- [ ] **QueryResult.vue**（查询结果）
  - [ ] 执行状态展示
  - [ ] 动态列表格渲染
  - [ ] 导出功能（Excel、JSON、Markdown）
  - [ ] 错误信息展示
  
- [ ] **公共工具函数**
  - [ ] SQL 格式化（sql-formatter）
  - [ ] SQL 简单校验（关键字检测）
  - [ ] 字段类型图标映射
  - [ ] 数据格式化工具

### 11.2 样式开发

- [ ] 组件 SCSS 样式
- [ ] 响应式断点适配
- [ ] 暗色模式支持（可选）
- [ ] 打印样式（可选）

### 11.3 测试

- [ ] 单元测试
  - [ ] SQL 校验逻辑
  - [ ] 字段插入逻辑
  - [ ] 数据格式化函数
  
- [ ] 集成测试
  - [ ] 完整查询流程
  - [ ] 错误处理流程
  - [ ] 导出功能
  
- [ ] E2E 测试
  - [ ] 用户操作路径
  - [ ] 键盘快捷键
  - [ ] 多浏览器兼容性

---

## 12. 未来扩展方向

### 12.1 短期增强（3-6 个月）

1. **SQL 自动补全**
   - 基于表结构的智能提示
   - 关键字补全
   - 函数提示

2. **查询历史**
   - 记录最近 20 条查询
   - 快速重新执行
   - 历史记录搜索

3. **结果增强**
   - 简单图表（柱状图、饼图）
   - 数据对比（两次查询结果 diff）
   - 单元格编辑（仅前端，不回写）

### 12.2 中期增强（6-12 个月）

1. **收藏管理**
   - 保存常用 SQL
   - 分类管理
   - 团队共享

2. **JOIN 支持**
   - 放开 JOIN 限制
   - 可视化 JOIN 构建器
   - 表关系图谱

3. **多数据源**
   - 支持切换数据源
   - 跨库查询
   - 数据源权限管理

### 12.3 长期愿景（1-2 年）

1. **完整 BI 能力**
   - 自定义仪表盘
   - 定时查询和报表
   - 数据订阅

2. **协作功能**
   - SQL 评论和讨论
   - 查询结果分享链接
   - 团队知识库

3. **AI 辅助**
   - 自然语言转 SQL
   - SQL 性能优化建议
   - 异常检测和告警

---

## 附录

### A. 浏览器兼容性

| 浏览器 | 最低版本 | 备注 |
|--------|----------|------|
| Chrome | 90+ | 推荐 |
| Edge | 90+ | 推荐 |
| Firefox | 88+ | 支持 |
| Safari | 14+ | 支持 |

### B. 依赖库清单

```json
{
  "dependencies": {
    "vue": "^2.6.14",
    "element-ui": "^2.15.0",
    "ace-builds": "^1.15.0",           // 代码编辑器
    "sql-formatter": "^12.0.0",        // SQL 格式化
    "xlsx": "^0.18.0",                 // Excel 导出
    "vue-virtual-scroller": "^1.0.10"  // 虚拟滚动
  },
  "devDependencies": {
    "@vue/test-utils": "^1.3.0",
    "jest": "^27.0.0"
  }
}
```

### C. 参考资料

- [Navicat 产品设计](https://www.navicat.com/)
- [DataGrip 交互设计](https://www.jetbrains.com/datagrip/)
- [Element UI 设计规范](https://element.eleme.io/#/zh-CN/guide/design)
- [WCAG 2.1 可访问性指南](https://www.w3.org/WAI/WCAG21/quickref/)

---

**文档结束**
