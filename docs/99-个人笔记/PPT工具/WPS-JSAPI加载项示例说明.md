# WPS JSAPI 加载项示例 - 乐谱编辑器

## 项目来源

- **GitHub仓库：** [johyeonZX/Music-Score-Editor](https://github.com/johyeonZX/Music-Score-Editor)
- **本地路径：** `/Users/edy/ideaProjects/Music-Score-Editor`
- **开发文档：** 包含 `插件开发设计书.pdf`

## 这是什么？

这是一个**完整的WPS加载项项目**，展示了如何使用WPS JSAPI开发功能丰富的插件。

**与简单宏的区别：**
- ❌ 简单宏：只能运行JavaScript函数，无界面
- ✅ 加载项：有自定义工具栏、完整UI、独立应用

## 实现的功能

### 1. 自定义工具栏（Ribbon界面）

在WPS演示中添加"乐谱编辑器"选项卡，包含：

**文件管理组：**
- 新建乐谱
- 我的乐谱

**文件导入导出组：**
- 导入乐谱
- 导出乐谱
- 导出为PDF
- 导出为OFD
- 从MIDI文件加载

**播放控制组：**
- 开始播放
- 暂停
- 停止

**工具组：**
- 简谱设置
- 页面设置
- 脚本编辑
- 钢琴模拟

**帮助组：**
- 钢琴键盘映射表

### 2. 核心技术特点

**项目结构：**
```
MusicScoreEdit/
├── ribbon.xml          # 定义工具栏界面
├── main.js            # 主入口文件
├── package.json       # 项目配置
├── js/                # JavaScript代码
├── templates/         # HTML模板
└── ui/                # 界面资源
```

**关键配置（package.json）：**
```json
{
  "addonType": "wps",
  "scripts": {
    "test": "wpsjs debug",
    "build": "wpsjs build",
    "publish": "wpsjs publish"
  },
  "devDependencies": {
    "wps-jsapi": "^1.0.5"
  }
}
```

**Ribbon界面定义（ribbon.xml）：**
```xml
<customUI xmlns="http://schemas.microsoft.com/office/2006/01/customui"
          onLoad="OnAddinLoad">
    <ribbon>
        <tabs>
            <tab id="musicEditTab" label="乐谱编辑器">
                <group id="FileMngGroup" label="文件管理">
                    <button id="btn_newMuScore"
                            label="新建乐谱"
                            onAction="OnAction"/>
                </group>
            </tab>
        </tabs>
    </ribbon>
</customUI>
```

## 与你的工具的对比

| 特性 | 你的工具（简单宏） | Music-Score-Editor（加载项） |
|------|-------------------|----------------------------|
| 开发方式 | 直接写JavaScript函数 | 完整项目，需构建 |
| 界面 | 无，只能在宏列表运行 | 自定义工具栏和界面 |
| 安装 | 复制粘贴代码 | 需要打包发布 |
| 复杂度 | 简单 | 复杂 |
| 适用场景 | 快速批量处理 | 功能完整的应用 |

## 从这个项目能学到什么？

1. **如何创建自定义工具栏** - 使用 `ribbon.xml` 定义界面
2. **如何构建完整的WPS应用** - 项目结构、构建流程
3. **WPS JSAPI的完整用法** - 不只是简单的文本处理
4. **开发工具链** - `wps-jsapi` npm包的使用

## 开发文档

项目包含 **`插件开发设计书.pdf`**，这是WPS JSAPI的开发文档，包含：
- API参考
- 开发指南
- 示例代码

## 总结

- **你的工具**：适合快速批量处理任务（改颜色、替换文本）
- **这个项目**：展示如何开发功能完整的WPS应用

如果只是做简单的批量处理，你的工具就够用了。如果要开发复杂的WPS应用，可以参考这个项目。
