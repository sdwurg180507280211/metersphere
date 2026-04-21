# NodeTree组件升级对比分析

## 组件路径对比

**旧版组件(接口自动化使用):**
- 路径: `api-test/frontend/src/business/commons/NodeTree.vue`
- 图标库: Element UI 图标 (el-icon)

**新版组件(功能用例使用):**
- 路径: `framework/sdk-parent/frontend/src/components/new-ui/MsNodeTree.vue`
- 图标库: SVG 图标 (svg-icon)

## 详细差异对比

### 1. 图标系统

**旧版:**
```vue
<i class="el-icon-folder" />
```
- 使用 Element UI 内置图标
- 图标固定不变,不随选中状态改变

**新版:**
```vue
<svg-icon :icon-class="node.isCurrent ? 'icon_folder_selected' : 'icon_folder'"/>
```
- 使用自定义 SVG 图标
- 根据 `node.isCurrent` 动态切换图标
- 选中时显示 `icon_folder_selected`
- 未选中时显示 `icon_folder`

### 2. 拖拽图标

**旧版:**
- ❌ 无拖拽图标

**新版:**
```vue
<svg-icon v-if="data.id !== 'root' && !hideNodeOperator" icon-class="icon_drag_outlined"/>
```
- ✅ 有专门的拖拽图标 `icon_drag_outlined`
- 位置: 节点左侧,相对定位 `left: -40px`

### 3. 节点选中状态 - 背景色

**旧版:**
- ❌ 无选中背景色
- 仅依赖 el-tree 的 `highlight-current` 默认样式

**新版:**
```css
:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background-color: rgba(120, 56, 135, 0.1);
}
```
- ✅ 选中节点有紫色半透明背景 `rgba(120, 56, 135, 0.1)`
- 圆角: 4px

### 4. 节点选中状态 - 文字颜色

**旧版:**
- ❌ 文字颜色不变
- 默认颜色: 继承父元素

**新版:**
```css
:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content .el-tooltip.node-title.item) {
  color: #783887;
  font-weight: 500;
}
```
- ✅ 选中节点文字颜色: `#783887` (紫色)
- ✅ 选中节点文字加粗: `font-weight: 500`

### 5. 节点选中状态 - 数量颜色

**旧版:**
```vue
<span style="color: var(--primary_color)">{{ data.caseNum }}</span>
```
- 数量颜色固定为主题色,不随选中状态改变

**新版:**
```css
:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content .case-num) {
  color: #783887;
  font-weight: 500;
}
```
- ✅ 选中节点数量颜色: `#783887` (紫色)
- ✅ 选中节点数量加粗: `font-weight: 500`
- 未选中时颜色: `#8F959E` (灰色)

### 6. Hover 效果

**旧版:**
- ❌ 无 hover 背景色
- 仅依赖 el-tree 默认 hover 效果

**新版:**
```css
:deep(.el-tree-node__content:hover){
  background-color: rgba(31, 35, 41, 0.1);
  border-radius: 4px;
}
```
- ✅ hover 时显示灰色半透明背景 `rgba(31, 35, 41, 0.1)`
- ✅ 圆角: 4px

### 7. 节点高度和布局

**旧版:**
```css
.custom-tree-node {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
  width: 100%;
}
```
- ❌ 无固定高度
- 布局: `justify-content: space-between`

**新版:**
```css
:deep(.el-tree-node__content) {
  width: auto;
  height: 40px;
  border-radius: 4px;
}
```
- ✅ 固定高度: 40px
- ✅ 圆角: 4px
- 布局更统一规范

### 8. 文字样式细节

**旧版:**
```css
.node-title {
  width: 0px;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1 1 auto;
  padding: 0px 5px;
  overflow: hidden;
}
```
- 无字体族、字重、颜色定义
- padding: 0px 5px

**新版:**
```css
.node-title {
  width: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1 1 auto;
  padding: 0 0 0 9px;
  overflow: hidden;
  font-family: 'PingFang SC';
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  display: flex;
  align-items: center;
  color: #1F2329;
  margin-right: 11px;
}
```
- ✅ 指定字体族: 'PingFang SC'
- ✅ 字重: 400
- ✅ 颜色: #1F2329 (深灰色)
- ✅ padding: 0 0 0 9px (左侧间距更大)

### 9. 操作按钮样式

**旧版:**
- 使用 el-button + el-tooltip
- 按钮直接显示在节点上
- 图标: el-icon-plus, el-icon-edit, el-icon-delete

**新版:**
- 使用 svg-icon + el-dropdown
- 更多操作收纳在下拉菜单中
- 图标: icon_global_rename, icon_delete-trash_outlined_red
- 视觉效果更现代

### 10. 展开/收起图标

**旧版:**
- 使用 el-tree 默认的展开图标

**新版:**
```css
:deep(.el-tree-node__expand-icon.el-icon-caret-right:before) {
  color: #646A73;
  font-size: 15px;
}
```
- ✅ 自定义展开图标颜色: #646A73 (灰色)
- ✅ 图标大小: 15px

## 升级所需改动总结

### 必须改动项(影响视觉效果)

1. **图标系统** - 从 el-icon 改为 svg-icon,需要引入 svg 图标资源
2. **选中背景色** - 添加紫色半透明背景 `rgba(120, 56, 135, 0.1)`
3. **选中文字颜色** - 改为紫色 `#783887` 并加粗
4. **选中数量颜色** - 改为紫色 `#783887` 并加粗
5. **Hover 背景色** - 添加灰色半透明背景 `rgba(31, 35, 41, 0.1)`
6. **节点高度** - 固定为 40px
7. **节点圆角** - 添加 4px 圆角

### 可选改动项(优化体验)

8. **拖拽图标** - 添加专门的拖拽图标
9. **操作按钮** - 改用下拉菜单收纳更多操作
10. **文字样式** - 指定字体族和颜色
11. **展开图标** - 自定义颜色和大小

## 实施建议

### 方案1: 完全升级组件(推荐但工作量大)
- 将 ApiScenarioModule 改为使用新版 MsNodeTree
- 需要调整所有 props 和事件处理
- 需要充分测试以确保不影响现有功能
- 工作量: 2-3天

### 方案2: 仅升级样式(快速实现)
- 保持使用旧版 NodeTree 组件
- 通过 CSS 添加选中状态的背景色、文字颜色等
- 图标暂时使用 el-icon 的动态切换
- 工作量: 半天

### 方案3: 分阶段升级(平衡方案)
- 第一阶段: 添加关键样式(背景色、文字颜色、hover效果)
- 第二阶段: 升级图标系统
- 第三阶段: 优化操作按钮和其他细节
- 工作量: 1-2天
