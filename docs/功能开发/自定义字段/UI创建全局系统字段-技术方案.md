# UI创建全局系统字段 - 技术方案

## 💡 需求背景

**当前问题**：
- ❌ 需要编写SQL脚本
- ❌ 需要修改前端国际化代码
- ❌ 需要重新编译打包
- ❌ 技术门槛高，操作繁琐

**期望效果**：
- ✅ 通过UI界面创建全局系统字段
- ✅ 无需写代码
- ✅ 无需打包部署
- ✅ 中文能正常显示即可（国际化可选）

---

## 🔍 现状分析

### 当前代码限制

**后端代码**：`CustomFieldService.java`

```java
public String add(CustomField customField) {
    checkExist(customField);
    customField.setId(UUID.randomUUID().toString());
    customField.setCreateTime(System.currentTimeMillis());
    customField.setUpdateTime(System.currentTimeMillis());
    customField.setGlobal(false);  // ⚠️ 强制设置为 false，不允许创建全局字段
    customField.setThirdPart(false);
    customField.setCreateUser(SessionUtils.getUserId());
    customFieldMapper.insert(customField);
    return customField.getId();
}
```

**限制原因**：
1. **权限隔离**：项目管理员只能管理本项目资源
2. **数据安全**：防止误操作影响其他项目
3. **设计初衷**：全局字段由系统管理员通过脚本创建

---

## ✅ 技术可行性分析

### 结论：**完全可行！**

只需要做以下改动：

| 层级 | 改动内容 | 难度 | 工作量 |
|------|---------|------|--------|
| **后端** | 增加创建全局字段的API | ⭐⭐ 中等 | 2-3小时 |
| **前端** | 增加创建全局字段的UI界面 | ⭐⭐ 中等 | 3-4小时 |
| **权限** | 限制只有系统管理员能创建 | ⭐ 简单 | 1小时 |
| **测试** | 功能测试和边界测试 | ⭐⭐ 中等 | 2小时 |

**总计**：1-2个工作日

---

## 📐 技术方案设计

### 方案1：最小化改动（推荐）⭐⭐⭐⭐⭐

**设计思路**：
- 在现有的"自定义字段"管理页面增加一个开关/选项
- 只有系统管理员能看到这个选项
- 创建时可以选择"全局字段"或"项目字段"

#### 1.1 后端改动

##### 📁 `CustomFieldService.java`

**新增方法**：创建全局系统字段

```java
/**
 * 创建全局系统字段（仅系统管理员）
 */
public String addGlobalSystemField(CustomField customField) {
    // 1. 权限检查
    if (!SessionUtils.isSystemAdmin()) {
        throw new MSException("只有系统管理员可以创建全局系统字段");
    }
    
    // 2. 检查字段名是否已存在
    checkExist(customField);
    
    // 3. 设置字段属性
    customField.setId(UUID.randomUUID().toString());
    customField.setCreateTime(System.currentTimeMillis());
    customField.setUpdateTime(System.currentTimeMillis());
    customField.setGlobal(true);      // ⭐ 设置为全局
    customField.setSystem(true);      // ⭐ 设置为系统字段
    customField.setProjectId(null);   // ⭐ 全局字段 project_id 为 NULL
    customField.setThirdPart(false);
    customField.setCreateUser(SessionUtils.getUserId());
    
    // 4. 插入数据库
    customFieldMapper.insert(customField);
    
    // 5. 关联到所有全局模板（Local, Jira, Zentao, TAPD）
    associateToGlobalTemplates(customField);
    
    // 6. 关联到所有项目级模板
    associateToAllProjectTemplates(customField);
    
    return customField.getId();
}

/**
 * 关联到全局模板
 */
private void associateToGlobalTemplates(CustomField customField) {
    List<String> globalTemplateIds = Arrays.asList(
        "5d7c87d2-f405-4ec1-9a3d-71b514cdfda3",  // Local
        "c7f26296-cf62-4149-a4d2-ce2492729e41",  // Jira
        "f2b70824-471e-426e-9219-f82aba6dd560",  // Zentao
        "f2cd9e48-f136-4528-8249-a649c15aa3a4"   // TAPD
    );
    
    for (String templateId : globalTemplateIds) {
        CustomFieldTemplate relation = new CustomFieldTemplate();
        relation.setId(UUID.randomUUID().toString());
        relation.setFieldId(customField.getId());
        relation.setTemplateId(templateId);
        relation.setScene(customField.getScene());
        relation.setRequired(false);
        relation.setOrder(100);  // 默认放在最后
        relation.setDefaultValue("");
        
        customFieldTemplateMapper.insert(relation);
    }
}

/**
 * 关联到所有项目级模板
 */
private void associateToAllProjectTemplates(CustomField customField) {
    // 查询所有项目级模板
    IssueTemplateExample example = new IssueTemplateExample();
    example.createCriteria().andGlobalEqualTo(false);
    List<IssueTemplate> projectTemplates = issueTemplateMapper.selectByExample(example);
    
    // 批量关联
    for (IssueTemplate template : projectTemplates) {
        CustomFieldTemplate relation = new CustomFieldTemplate();
        relation.setId(UUID.randomUUID().toString());
        relation.setFieldId(customField.getId());
        relation.setTemplateId(template.getId());
        relation.setScene(customField.getScene());
        relation.setRequired(false);
        relation.setOrder(100);
        relation.setDefaultValue("");
        
        customFieldTemplateMapper.insert(relation);
    }
}
```

##### 📁 `CustomFieldController.java`

**新增接口**：

```java
@PostMapping("/add/global")
@RequiresPermissions("SYSTEM_SETTING:READ+EDIT")  // 系统管理员权限
public String addGlobalSystemField(@RequestBody CustomField customField) {
    return customFieldService.addGlobalSystemField(customField);
}
```

---

#### 1.2 前端改动

##### 📁 创建字段表单（增加"全局字段"选项）

**位置**：项目设置 → 模版管理 → 自定义字段 → 创建

**UI设计**：

```vue
<template>
  <el-dialog title="创建字段" :visible.sync="dialogVisible">
    <el-form :model="form" label-width="100px">
      <!-- 现有字段：名称、场景、类型等 -->
      <el-form-item label="字段名称">
        <el-input v-model="form.name" />
      </el-form-item>
      
      <el-form-item label="使用场景">
        <el-select v-model="form.scene">
          <el-option label="缺陷" value="ISSUE" />
          <el-option label="用例" value="TEST_CASE" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="字段类型">
        <el-select v-model="form.type">
          <el-option label="文本框" value="input" />
          <el-option label="下拉框" value="select" />
          <el-option label="日期" value="date" />
        </el-select>
      </el-form-item>
      
      <!-- ⭐ 新增：全局字段选项（仅系统管理员可见） -->
      <el-form-item label="字段范围" v-if="isSystemAdmin">
        <el-radio-group v-model="form.isGlobal">
          <el-radio :label="false">项目字段（仅本项目可见）</el-radio>
          <el-radio :label="true">
            <span style="color: #f56c6c; font-weight: bold;">
              全局系统字段（所有项目可见，不可删除）
            </span>
          </el-radio>
        </el-radio-group>
        <div v-if="form.isGlobal" style="color: #e6a23c; margin-top: 10px;">
          <i class="el-icon-warning"></i>
          全局系统字段将自动关联到所有项目，并且无法删除，请谨慎创建！
        </div>
      </el-form-item>
      
      <!-- 其他字段配置 -->
    </el-form>
    
    <div slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="createField">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  data() {
    return {
      form: {
        name: '',
        scene: 'ISSUE',
        type: 'input',
        isGlobal: false,  // ⭐ 新增
      },
      isSystemAdmin: false,  // ⭐ 是否系统管理员
    }
  },
  
  mounted() {
    // 检查当前用户是否是系统管理员
    this.checkSystemAdmin();
  },
  
  methods: {
    checkSystemAdmin() {
      // 调用后端接口或读取 store 中的用户角色
      this.isSystemAdmin = this.$store.state.user.isSystemAdmin;
    },
    
    createField() {
      // 根据 isGlobal 选择不同的接口
      const url = this.form.isGlobal 
        ? '/custom/field/add/global'   // 创建全局字段
        : '/custom/field/add';          // 创建项目字段
      
      this.$post(url, this.form).then(res => {
        this.$success('创建成功');
        this.dialogVisible = false;
        this.refresh();
      });
    }
  }
}
</script>
```

---

#### 1.3 权限控制

**系统管理员判断**：

```java
// SessionUtils.java
public static boolean isSystemAdmin() {
    User user = getCurrentUser();
    if (user == null) {
        return false;
    }
    
    // 检查用户角色
    List<String> roles = user.getRoles();
    return roles.contains("admin") || roles.contains("system_admin");
}
```

---

#### 1.4 国际化处理（可选）

**方案A：不处理国际化（简单）**

- 字段名直接显示为中文
- 切换语言后，字段名仍显示中文
- **优点**：无需额外开发
- **缺点**：英文/繁体环境显示中文字段名

**方案B：半自动国际化（推荐）**

- 创建时只填中文名称
- 系统自动生成 i18n key（使用拼音或英文翻译）
- 默认英文/繁体都显示中文名
- 后续可在"国际化管理"页面手动修改翻译

```javascript
// 自动生成 i18n key
function generateI18nKey(chineseName) {
  // 方案1：使用拼音
  return pinyin(chineseName, { style: 'NORMAL' }).join('_');
  // 示例："手机" → "shou_ji"
  
  // 方案2：使用序号
  return `custom_field_${Date.now()}`;
  // 示例："custom_field_1701234567890"
}

// 自动创建翻译记录
{
  "zh-CN": { "custom_field_shouji": "手机" },
  "en-US": { "custom_field_shouji": "手机" },  // 默认也是中文
  "zh-TW": { "custom_field_shouji": "手機" }   // 可以自动简繁转换
}
```

---

### 方案2：独立管理页面（高级）⭐⭐⭐

**设计思路**：
- 新增"系统设置" → "全局字段管理"页面
- 专门管理全局系统字段
- 功能更强大，权限控制更清晰

**UI位置**：

```
系统设置（System Settings）
├── 用户管理
├── 组织管理
├── 全局字段管理 ★（新增）
│   ├── 缺陷字段
│   ├── 用例字段
│   └── 接口字段
└── 系统参数
```

**优点**：
- ✅ 功能更专业
- ✅ 权限控制更明确
- ✅ 可以批量管理全局字段

**缺点**：
- ❌ 开发工作量更大（3-5天）
- ❌ 需要新增菜单和路由

---

## 📊 方案对比

| 特性 | 方案1：最小化改动 | 方案2：独立管理页面 |
|------|------------------|-------------------|
| **开发工作量** | ⭐⭐ 1-2天 | ⭐⭐⭐ 3-5天 |
| **代码改动** | 小（现有页面增强） | 大（新增页面） |
| **权限控制** | ✅ 简单有效 | ✅ 更清晰 |
| **用户体验** | ⭐⭐⭐ 较好 | ⭐⭐⭐⭐⭐ 优秀 |
| **维护成本** | ⭐⭐ 低 | ⭐⭐⭐ 中等 |
| **扩展性** | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐⭐ 很强 |
| **推荐度** | ✅ 推荐 | ⚠️ 看需求 |

---

## ⚠️ 需要注意的问题

### 1. 权限控制

**问题**：谁能创建全局系统字段？

**方案**：
```
只有系统管理员（admin）能创建全局系统字段
  ├── 检查用户角色
  ├── 前端隐藏选项（非admin不显示）
  └── 后端接口拦截（非admin返回403）
```

### 2. 字段唯一性

**问题**：如何防止重复创建？

**方案**：
```sql
-- 检查字段是否已存在
SELECT COUNT(*) FROM custom_field 
WHERE name = '手机' 
  AND scene = 'ISSUE' 
  AND global = 1;

-- 如果存在，提示用户：
"全局字段'手机'已存在，请使用其他名称"
```

### 3. 模板自动关联

**问题**：创建后如何确保所有项目都能看到？

**方案**：
```java
// 创建时自动关联
1. 关联4个全局模板（Local, Jira, Zentao, TAPD）
2. 批量关联所有项目级模板（global=0）

// 新建项目时自动包含
- 基于全局模板创建项目模板时，会自动复制字段关联
```

### 4. 国际化问题

**问题**：不支持国际化，切换语言怎么办？

**方案A：简单处理**
```
所有语言都显示中文
- 中文：手机
- English: 手机（显示中文）
- 繁體: 手機（简繁自动转换）
```

**方案B：智能处理**
```
自动生成 i18n key，默认都是中文
后续可在UI手动编辑翻译

创建时：
  - name: "手机"
  - i18n_key: "custom_field_shouji"（自动生成）
  
翻译表：
  - zh-CN: "手机"
  - en-US: "手机"（默认）
  - zh-TW: "手機"（自动简繁转换）
  
后续可编辑：
  - en-US: "Phone"（手动修改）
```

### 5. 字段删除保护

**问题**：系统字段不能删除，如何限制？

**方案**：
```javascript
// 前端：隐藏删除按钮
<el-button 
  v-if="!row.system"  // 系统字段不显示删除按钮
  @click="deleteField(row)">
  删除
</el-button>

// 后端：接口拦截
public void delete(String id) {
    CustomField field = customFieldMapper.selectByPrimaryKey(id);
    if (field.getSystem()) {
        throw new MSException("系统字段不能删除");
    }
    customFieldMapper.deleteByPrimaryKey(id);
}
```

---

## 🚀 实施步骤

### 阶段1：后端开发（1天）

1. ✅ 修改 `CustomFieldService.java`
   - 新增 `addGlobalSystemField()` 方法
   - 新增 `associateToGlobalTemplates()` 方法
   - 新增 `associateToAllProjectTemplates()` 方法

2. ✅ 修改 `CustomFieldController.java`
   - 新增 `/add/global` 接口
   - 添加权限注解

3. ✅ 权限工具类
   - `SessionUtils.isSystemAdmin()` 方法

### 阶段2：前端开发（1天）

1. ✅ 修改创建字段表单
   - 增加"字段范围"选项
   - 系统管理员才显示
   - 添加警告提示

2. ✅ 调用接口
   - 根据 `isGlobal` 选择接口
   - 错误处理

3. ✅ 列表页面调整
   - 系统字段标识
   - 隐藏删除按钮

### 阶段3：测试（半天）

1. ✅ 功能测试
   - 创建全局字段
   - 验证所有项目可见
   - 验证不能删除

2. ✅ 权限测试
   - 非系统管理员不能创建
   - 接口权限拦截

3. ✅ 边界测试
   - 重复字段名
   - 特殊字符

---

## 💡 推荐方案

### 最佳实践：方案1 + 方案B国际化

```
✅ 采用"最小化改动"方案
✅ 在现有页面增加"全局字段"选项
✅ 自动生成 i18n key（使用拼音或序号）
✅ 默认所有语言显示中文
✅ 后续可手动编辑翻译（可选）
```

**优点**：
- ✅ 开发工作量小（1-2天）
- ✅ 用户体验好（无需写代码）
- ✅ 兼容现有流程
- ✅ 未来可扩展（升级为方案2）

**实施成本**：
- 后端：4小时
- 前端：6小时
- 测试：2小时
- **总计：1.5个工作日**

---

## 📝 示例：通过UI创建"手机"字段

### 操作流程

```
1. 登录系统（系统管理员账号）
   ↓
2. 进入"项目设置" → "模版管理" → "自定义字段"
   ↓
3. 点击"创建"按钮
   ↓
4. 填写字段信息：
   - 字段名称：手机
   - 使用场景：缺陷
   - 字段类型：文本框
   - ⭐ 字段范围：选择"全局系统字段"
   ↓
5. 点击"确定"
   ↓
6. 系统自动：
   - 创建 custom_field 记录（system=1, global=1）
   - 关联到4个全局模板
   - 关联到所有项目模板
   - 生成 i18n key：custom_field_shouji
   ↓
7. 完成！所有项目立即可见
```

**对比现有流程**：

| 步骤 | 现有流程 | UI创建 |
|------|---------|--------|
| 1. 编写SQL | ✅ 需要 | ❌ 不需要 |
| 2. 修改i18n代码 | ✅ 需要 | ❌ 自动生成 |
| 3. 编译打包 | ✅ 需要（5-8分钟） | ❌ 不需要 |
| 4. 重启服务 | ✅ 需要 | ❌ 不需要 |
| 5. 清除缓存 | ✅ 需要 | ❌ 不需要 |
| **总耗时** | 15-30分钟 | **1分钟** ✅ |

---

## ✅ 结论

**完全可行！强烈推荐实施！**

### 收益分析

| 收益项 | 说明 |
|--------|------|
| **效率提升** | 从30分钟 → 1分钟（提升30倍） |
| **技术门槛** | 从需要SQL+代码 → UI点击操作 |
| **维护成本** | 从需要打包部署 → 即时生效 |
| **用户体验** | ⭐⭐⭐⭐⭐ 大幅提升 |
| **开发成本** | 1.5个工作日（一次性投入） |

### 下一步

1. ✅ **确认需求**：是否采纳此方案
2. ✅ **排期开发**：安排1.5个工作日
3. ✅ **测试验证**：功能和权限测试
4. ✅ **上线发布**：合并到主分支

---

**这个功能一旦实现，将极大简化全局字段的管理，强烈建议优先开发！** 🚀

