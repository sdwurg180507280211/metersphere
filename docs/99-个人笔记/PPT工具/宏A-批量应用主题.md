# 宏A：批量应用主题模板

> **Microsoft PowerPoint** VBA 宏（Mac 版，路径为 `/Users/...` 格式）。批量打开指定目录下的 .ppt/.pptx/.pptm 文件，应用主题模板，保存并关闭。
> 使用方法：PowerPoint 中打开「工具 → 宏 → Visual Basic 编辑器」（或 Alt+F11），粘贴代码后运行 `StepA_ApplyTheme`。

```vba
Option Explicit

Sub StepA_ApplyTheme()
    Dim basePath As String
    Dim themePath As String
    Dim fileName As String, fullPath As String
    Dim pres As Presentation
    Dim ok As Long, fail As Long, failLog As String

    basePath = "/Users/edy/Desktop/baiduwangpan/其它/审核/20260805/pptm原/维立西呱_副本"
    themePath = "/Users/edy/Desktop/baiduwangpan/其它/审核/20260805/主题1.thmx"

    fileName = Dir(basePath & "/*.ppt*")
    Do While fileName <> ""
        If Left$(fileName, 2) <> "~$" Then
            fullPath = basePath & "/" & fileName
            On Error GoTo FileError
            Set pres = Presentations.Open(fullPath)
            pres.ApplyTemplate themePath
            pres.Save
            pres.Close
            Set pres = Nothing
            ok = ok + 1
            On Error GoTo 0
        End If
        fileName = Dir()
    Loop

    MsgBox "宏A完成：成功 " & ok & " 个，失败 " & fail & " 个", vbInformation
    Exit Sub

FileError:
    fail = fail + 1
    failLog = failLog & vbCrLf & fullPath & " -> " & Err.Number & " " & Err.Description
    On Error Resume Next
    If Not pres Is Nothing Then pres.Close
    Set pres = Nothing
    Err.Clear
    On Error GoTo 0
    Resume Next
End Sub
```

## 说明

- **功能**：遍历 `basePath` 目录下所有 `.ppt*` 文件（跳过 `~$` 开头的 Office 临时文件），逐个应用 `主题1.thmx` 模板后保存。
- **路径**：两处路径均为硬编码（Mac 格式 `/Users/edy/...`），换目录/主题需修改 `basePath`、`themePath` 两行。
- **错误处理**：单个文件打开/应用/保存失败会跳过并累计到 `fail`，不影响后续文件；弹窗只显示成功/失败数量，具体失败原因在 `failLog` 变量中（如需查看可加 `MsgBox failLog`）。
- **路径格式**：当前为 **PowerPoint for Mac** 的 POSIX 风格路径（`/Users/edy/...`、`Dir` 用正斜杠 `/*.ppt*`）；若改在 Windows 版 PowerPoint 运行，需把两处路径改为 `C:\...` 反斜杠格式，`Dir` 通配符改为 `\*.ppt*`。
- **权限提醒**：PowerPoint for Mac 首次运行 VBA 需在「工具 → 信任中心/安全性」中允许启用宏，否则脚本会被拦截。
