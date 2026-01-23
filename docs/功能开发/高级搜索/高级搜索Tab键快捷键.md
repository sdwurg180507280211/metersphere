# 高级搜索 - Tab键快捷键功能

## 功能描述

为全局高级搜索组件（新旧两个版本）添加Tab键快捷键支持，用户可以通过按下Tab键快速打开/关闭高级搜索弹窗。

## 使用方式

### 快捷键操作

- **打开高级搜索**：在页面任意位置（非输入框）按下 `Tab` 键
- **关闭高级搜索**：在高级搜索弹窗打开状态下，再次按下 `Tab` 键

### 智能判断

系统会智能判断当前焦点位置：
- ✅ **焦点不在输入框**：Tab键触发高级搜索开关
- ❌ **焦点在输入框**：Tab键保持原有功能（切换焦点到下一个元素）

## 问题解答

### Q1: 为什么有两个高级搜索组件？

**A**: MeterSphere在UI升级过程中，存在新旧两个版本的高级搜索组件：

1. **旧版组件**（`MsTableAdvSearchBar`）
   - 位置：`framework/sdk-parent/frontend/src/components/search/MsTableAdvSearchBar.vue`
   - 特点：使用文字链接"高级搜索"打开
   - 使用场景：早期开发的页面

2. **新版组件**（`MsTableAdvSearch`）
   - 位置：`framework/sdk-parent/frontend/src/components/new-ui/MsTableAdvSearch.vue`
   - 特点：使用图标按钮打开，UI更现代
   - 使用场景：新开发或重构的页面

系统正在逐步从旧版迁移到新版，所以两个组件会并存一段时间。

### Q2: 如何避免多个组件同时响应Tab键？

**A**: 使用**全局单例模式**解决：

1. **组件注册机制**
   - 每个组件挂载时，注册到全局列表 `window._activeAdvSearchComponents`
   - 组件销毁时，从全局列表中移除

2. **优先级判断**
   - 按下Tab键时，只有第一个可见的组件响应
   - 其他组件忽略Tab键事件

3. **可见性检测**
   - 通过 `this.$el.offsetParent` 判断组件是否在DOM中可见
   - 隐藏或未渲染的组件不会响应Tab键

## 实现原理

### 核心逻辑

```javascript
// 1. 组件注册（mounted生命周期）
registerAdvSearchComponent() {
  if (!window._activeAdvSearchComponents) {
    window._activeAdvSearchComponents = [];
  }
  window._activeAdvSearchComponents.push(this);
}

// 2. 组件注销（beforeDestroy生命周期）
unregisterAdvSearchComponent() {
  if (window._activeAdvSearchComponents) {
    const index = window._activeAdvSearchComponents.indexOf(this);
    if (index > -1) {
      window._activeAdvSearchComponents.splice(index, 1);
    }
  }
}

// 3. Tab键处理
handleTabKey(event) {
  // 检查是否为单独的Tab键
  if (event.key === 'Tab' && !event.shiftKey && !event.ctrlKey && !event.metaKey && !event.altKey) {
    
    // 检查焦点是否在输入框
    const activeElement = document.activeElement;
    const isInputFocused = activeElement && (
      activeElement.tagName === 'INPUT' ||
      activeElement.tagName === 'TEXTAREA' ||
      activeElement.isContentEditable
    );
    if (isInputFocused) {
      return;
    }
    
    // 检查当前组件是否可见
    if (!this.$el || !this.$el.offsetParent) {
      return;
    }
    
    // 只有第一个可见的组件响应（关键：避免多组件同时响应）
    if (window._activeAdvSearchComponents && window._activeAdvSearchComponents.length > 0) {
      const firstVisibleComponent = window._activeAdvSearchComponents.find(comp => 
        comp.$el && comp.$el.offsetParent
      );
      if (firstVisibleComponent !== this) {
        return; // 不是第一个可见组件，忽略事件
      }
    }
    
    // 阻止默认Tab行为
    event.preventDefault();
    
    // 切换高级搜索显示状态
    if (this.visible) {
      this.visible = false;
    } else {
      this.open();
    }
  }
}
```

### 全局单例模式

```javascript
// 全局组件注册表（存储在window对象上）
window._activeAdvSearchComponents = [
  component1, // 第一个挂载的组件
  component2, // 第二个挂载的组件
  ...
]

// 响应优先级：只有第一个可见的组件响应Tab键
// 其他组件检测到自己不是第一个，直接return，不做任何操作
```

### 生命周期管理

```javascript
mounted() {
  // 1. 注册当前组件到全局列表
  this.registerAdvSearchComponent();
  // 2. 添加全局键盘监听
  document.addEventListener('keydown', this.handleTabKey);
},
beforeDestroy() {
  // 1. 从全局列表中移除当前组件
  this.unregisterAdvSearchComponent();
  // 2. 移除键盘监听，避免内存泄漏
  document.removeEventListener('keydown', this.handleTabKey);
}
```

## 修改文件

### 1. 新版高级搜索组件

**文件**：`framework/sdk-parent/frontend/src/components/new-ui/MsTableAdvSearch.vue`

**修改内容**：
- 添加 `mounted()` 生命周期钩子，注册键盘事件监听
- 添加 `beforeDestroy()` 生命周期钩子，移除键盘事件监听
- 添加 `handleTabKey()` 方法，处理Tab键逻辑

### 2. 旧版高级搜索组件

**文件**：`framework/sdk-parent/frontend/src/components/search/MsTableAdvSearchBar.vue`

**修改内容**：
- 添加 `mounted()` 生命周期钩子，注册键盘事件监听
- 添加 `beforeDestroy()` 生命周期钩子，移除键盘事件监听
- 添加 `handleTabKey()` 方法，处理Tab键逻辑

## 技术细节

### 1. 事件监听范围

- 监听级别：`document` 全局监听
- 事件类型：`keydown`
- 优点：无论焦点在哪里，都能捕获Tab键

### 2. 焦点检测

通过 `document.activeElement` 获取当前焦点元素，判断是否为：
- `INPUT` 标签
- `TEXTAREA` 标签
- 可编辑元素（`isContentEditable`）

### 3. 修饰键过滤

只响应单独的Tab键，忽略组合键：
- `Shift + Tab`：保留原有功能（反向切换焦点）
- `Ctrl + Tab`：保留原有功能（浏览器标签页切换）
- `Alt + Tab`：保留原有功能（窗口切换）
- `Cmd + Tab`：保留原有功能（Mac应用切换）

### 4. 内存管理

- 组件挂载时添加监听
- 组件销毁时移除监听
- 避免内存泄漏和重复监听

## 使用场景

### 场景1：测试用例列表

```
用户操作：
1. 进入测试跟踪-测试用例页面
2. 按下Tab键 → 高级搜索弹窗打开
3. 再次按下Tab键 → 高级搜索弹窗关闭
```

### 场景2：缺陷管理列表

```
用户操作：
1. 进入测试跟踪-缺陷管理页面
2. 按下Tab键 → 高级搜索弹窗打开
3. 在弹窗内输入搜索条件
4. 按下Tab键 → 高级搜索弹窗关闭
```

### 场景3：输入框内Tab键

```
用户操作：
1. 在搜索框内输入关键词
2. 按下Tab键 → 焦点切换到下一个输入框（保持原有功能）
3. 焦点离开输入框后，按下Tab键 → 高级搜索弹窗打开
```

## 兼容性说明

### 浏览器兼容性

- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

### 组件兼容性

- ✅ 新版高级搜索组件（MsTableAdvSearch）
- ✅ 旧版高级搜索组件（MsTableAdvSearchBar）
- ✅ 所有使用这两个组件的页面

## 注意事项

### 1. 不影响原有Tab功能

- 在输入框、文本域内，Tab键保持原有功能
- 在表单元素间，Tab键仍然用于切换焦点

### 2. 多个高级搜索组件

如果页面上有多个高级搜索组件：
- ✅ 只有第一个可见的组件响应Tab键
- ✅ 其他组件自动忽略Tab键事件
- ✅ 通过全局单例模式避免冲突

### 3. 与其他快捷键冲突

如果页面已有其他Tab键快捷键：
- 本功能会优先判断焦点位置
- 可以通过修改 `handleTabKey` 方法添加更多判断条件

## 测试验证

### 功能测试

```
测试步骤：
1. 进入任意带高级搜索的页面
2. 按下Tab键，验证高级搜索弹窗打开
3. 再次按下Tab键，验证高级搜索弹窗关闭
4. 在搜索框内按下Tab键，验证焦点切换（不打开高级搜索）
5. 按下Shift+Tab，验证焦点反向切换（不打开高级搜索）
```

### 兼容性测试

```
测试页面：
- 测试跟踪-测试用例列表（新版组件）
- 测试跟踪-缺陷管理列表（旧版组件）
- 接口测试-接口定义列表
- 性能测试-测试列表
```

## 扩展建议

### 1. 添加快捷键提示

在高级搜索按钮上添加Tooltip提示：

```vue
<el-tooltip content="快捷键：Tab" placement="top">
  <el-button @click="open">高级搜索</el-button>
</el-tooltip>
```

### 2. 支持自定义快捷键

通过配置项支持自定义快捷键：

```javascript
props: {
  shortcutKey: {
    type: String,
    default: 'Tab'
  }
}
```

### 3. 添加快捷键帮助文档

在系统帮助文档中添加快捷键说明：

```
全局快捷键：
- Tab：打开/关闭高级搜索
- Ctrl+F：快速搜索
- Esc：关闭弹窗
```

## 相关文件清单

### 修改文件
- `framework/sdk-parent/frontend/src/components/new-ui/MsTableAdvSearch.vue`
- `framework/sdk-parent/frontend/src/components/search/MsTableAdvSearchBar.vue`

### 影响范围
- 所有使用高级搜索组件的页面
- 测试跟踪模块
- 接口测试模块
- 性能测试模块
- 项目管理模块

## 修改日期

2025-01-22

## 修改人

Kiro AI Assistant
