Option Explicit

' ============================================================
' WPS演示 批量文本颜色工具
' 兼容 WPS Office 和 Microsoft Office PowerPoint
' 通过 InputBox 对话框让用户自定义所有参数
' ============================================================

Public Type TextColorToolConfig
    PreviewOnly As Boolean
    RequireNearTop As Boolean
    TopRatio As Double
    RequireRedish As Boolean
    RedMin As Long
    RedMargin As Long
    MatchByName As Boolean
    NameKeyword As String
    MatchByText As Boolean
    TextKeyword As String
    TargetColor As Long
End Type

' 主入口 - 交互式配置
Public Sub InteractiveBatchTextColorTool()
    Dim cfg As TextColorToolConfig

    If Not ShowConfigDialog(cfg) Then
        MsgBox "操作已取消。", vbInformation
        Exit Sub
    End If

    RunTextColorTool cfg
End Sub

' 快速入口 - 预览模式
Public Sub QuickPreview()
    Dim cfg As TextColorToolConfig
    BuildDefaultConfig cfg
    cfg.PreviewOnly = True
    RunTextColorTool cfg
End Sub

' 快速入口 - 应用模式
Public Sub QuickApply()
    Dim cfg As TextColorToolConfig
    BuildDefaultConfig cfg
    cfg.PreviewOnly = False

    If MsgBox("确定要批量修改文本颜色吗？" & vbCrLf & _
              "建议先运行 QuickPreview 预览。", _
              vbYesNo + vbQuestion) = vbNo Then
        Exit Sub
    End If

    RunTextColorTool cfg
End Sub

' ============================================================
' 配置对话框
' ============================================================

Private Function ShowConfigDialog(ByRef cfg As TextColorToolConfig) As Boolean
    Dim response As String
    Dim targetR As Long, targetG As Long, targetB As Long

    BuildDefaultConfig cfg

    ' 步骤 1: 选择模式
    response = InputBox( _
        "选择运行模式：" & vbCrLf & vbCrLf & _
        "1 = 预览模式（只查看，不修改）" & vbCrLf & _
        "2 = 应用模式（实际修改）" & vbCrLf & vbCrLf & _
        "请输入 1 或 2：", _
        "步骤 1/6 - 运行模式", "1")

    If response = "" Then
        ShowConfigDialog = False
        Exit Function
    End If

    cfg.PreviewOnly = (response = "1")

    ' 步骤 2: 位置筛选
    response = InputBox( _
        "是否只处理位置靠上的文本？" & vbCrLf & vbCrLf & _
        "Y = 是，只处理上方文本" & vbCrLf & _
        "N = 否，处理所有位置" & vbCrLf & vbCrLf & _
        "请输入 Y 或 N：", _
        "步骤 2/6 - 位置筛选", "Y")

    If response = "" Then
        ShowConfigDialog = False
        Exit Function
    End If

    cfg.RequireNearTop = (UCase(response) = "Y")

    If cfg.RequireNearTop Then
        response = InputBox( _
            "定义【靠上】的范围（页面高度百分比）：" & vbCrLf & vbCrLf & _
            "例如：" & vbCrLf & _
            "  30 = 上方 30% 区域" & vbCrLf & _
            "  50 = 上方 50% 区域" & vbCrLf & vbCrLf & _
            "请输入数字（0-100）：", _
            "步骤 2b/6 - 位置范围", "30")

        If response = "" Then
            ShowConfigDialog = False
            Exit Function
        End If

        cfg.TopRatio = Val(response) / 100
        If cfg.TopRatio <= 0 Or cfg.TopRatio > 1 Then cfg.TopRatio = 0.3
    End If

    ' 步骤 3: 颜色筛选
    response = InputBox( _
        "是否只处理红色文本？" & vbCrLf & vbCrLf & _
        "Y = 是，只处理红色系文本" & vbCrLf & _
        "N = 否，处理所有颜色" & vbCrLf & vbCrLf & _
        "请输入 Y 或 N：", _
        "步骤 3/6 - 颜色筛选", "Y")

    If response = "" Then
        ShowConfigDialog = False
        Exit Function
    End If

    cfg.RequireRedish = (UCase(response) = "Y")

    If cfg.RequireRedish Then
        response = InputBox( _
            "红色判断阈值（推荐 160）：" & vbCrLf & vbCrLf & _
            "红色分量需要 >= 此值" & vbCrLf & _
            "范围：0-255" & vbCrLf & vbCrLf & _
            "请输入数字：", _
            "步骤 3b/6 - 红色阈值", "160")

        If response = "" Then
            ShowConfigDialog = False
            Exit Function
        End If

        cfg.RedMin = Val(response)
        If cfg.RedMin < 0 Or cfg.RedMin > 255 Then cfg.RedMin = 160
    End If

    ' 步骤 4: 名称筛选
    response = InputBox( _
        "是否按形状名称筛选？" & vbCrLf & vbCrLf & _
        "留空 = 不筛选" & vbCrLf & _
        "输入关键词 = 只处理名称包含该关键词的形状" & vbCrLf & vbCrLf & _
        "例如：Title、TextBox" & vbCrLf & vbCrLf & _
        "请输入关键词（或留空）：", _
        "步骤 4/6 - 名称筛选", "")

    If Len(Trim(response)) > 0 Then
        cfg.MatchByName = True
        cfg.NameKeyword = Trim(response)
    Else
        cfg.MatchByName = False
    End If

    ' 步骤 5: 文本内容筛选
    response = InputBox( _
        "是否按文本内容筛选？" & vbCrLf & vbCrLf & _
        "留空 = 不筛选" & vbCrLf & _
        "输入关键词 = 只处理包含该关键词的文本" & vbCrLf & vbCrLf & _
        "例如：标题、重要" & vbCrLf & vbCrLf & _
        "请输入关键词（或留空）：", _
        "步骤 5/6 - 文本筛选", "")

    If Len(Trim(response)) > 0 Then
        cfg.MatchByText = True
        cfg.TextKeyword = Trim(response)
    Else
        cfg.MatchByText = False
    End If

    ' 步骤 6: 目标颜色
    response = InputBox( _
        "设置目标颜色（RGB 格式）：" & vbCrLf & vbCrLf & _
        "格式：R,G,B" & vbCrLf & _
        "例如：" & vbCrLf & _
        "  0,112,192 = Office 蓝" & vbCrLf & _
        "  0,176,80 = 绿色" & vbCrLf & _
        "  255,0,0 = 红色" & vbCrLf & vbCrLf & _
        "请输入（格式：R,G,B）：", _
        "步骤 6/6 - 目标颜色", "0,112,192")

    If response = "" Then
        ShowConfigDialog = False
        Exit Function
    End If

    If Not ParseRGB(response, targetR, targetG, targetB) Then
        MsgBox "颜色格式错误，使用默认蓝色。", vbExclamation
        targetR = 0: targetG = 112: targetB = 192
    End If

    cfg.TargetColor = RGB(targetR, targetG, targetB)

    ' 显示配置摘要
    Dim summary As String
    summary = "配置摘要：" & vbCrLf & vbCrLf & _
              "模式：" & IIf(cfg.PreviewOnly, "预览", "应用") & vbCrLf & _
              "位置筛选：" & IIf(cfg.RequireNearTop, "是（上方 " & Int(cfg.TopRatio * 100) & "%）", "否") & vbCrLf & _
              "颜色筛选：" & IIf(cfg.RequireRedish, "是（红色系）", "否") & vbCrLf & _
              "名称筛选：" & IIf(cfg.MatchByName, cfg.NameKeyword, "否") & vbCrLf & _
              "文本筛选：" & IIf(cfg.MatchByText, cfg.TextKeyword, "否") & vbCrLf & _
              "目标颜色：RGB(" & targetR & "," & targetG & "," & targetB & ")" & vbCrLf & vbCrLf & _
              "确认执行吗？"

    If MsgBox(summary, vbYesNo + vbQuestion, "确认配置") = vbNo Then
        ShowConfigDialog = False
        Exit Function
    End If

    ShowConfigDialog = True
End Function

Private Function ParseRGB(ByVal inputStr As String, ByRef r As Long, ByRef g As Long, ByRef b As Long) As Boolean
    Dim parts() As String

    On Error GoTo Failed

    parts = Split(Trim(inputStr), ",")
    If UBound(parts) <> 2 Then GoTo Failed

    r = CLng(Trim(parts(0)))
    g = CLng(Trim(parts(1)))
    b = CLng(Trim(parts(2)))

    If r < 0 Or r > 255 Or g < 0 Or g > 255 Or b < 0 Or b > 255 Then GoTo Failed

    ParseRGB = True
    Exit Function

Failed:
    ParseRGB = False
End Function

' ============================================================
' 默认配置
' ============================================================

Private Sub BuildDefaultConfig(ByRef cfg As TextColorToolConfig)
    cfg.PreviewOnly = True
    cfg.RequireNearTop = True
    cfg.TopRatio = 0.3
    cfg.RequireRedish = True
    cfg.RedMin = 160
    cfg.RedMargin = 30
    cfg.MatchByName = False
    cfg.NameKeyword = ""
    cfg.MatchByText = False
    cfg.TextKeyword = ""
    cfg.TargetColor = RGB(0, 112, 192)
End Sub

' ============================================================
' 主执行逻辑
' ============================================================

Private Sub RunTextColorTool(ByRef cfg As TextColorToolConfig)
    Dim sld As Slide
    Dim totalMatched As Long
    Dim totalChanged As Long
    Dim slideHeight As Single

    If ActivePresentation Is Nothing Then
        MsgBox "当前没有打开的演示文稿。", vbExclamation
        Exit Sub
    End If

    slideHeight = ActivePresentation.PageSetup.SlideHeight
    totalMatched = 0
    totalChanged = 0

    For Each sld In ActivePresentation.Slides
        ProcessSlide sld, slideHeight, cfg, totalMatched, totalChanged
    Next sld

    Dim summary As String
    If cfg.PreviewOnly Then
        summary = "预览完成。" & vbCrLf & _
                  "命中对象数: " & totalMatched & vbCrLf & vbCrLf & _
                  "详细信息已输出到立即窗口（按 Ctrl+G 查看）。"
    Else
        summary = "处理完成！" & vbCrLf & _
                  "命中对象数: " & totalMatched & vbCrLf & _
                  "实际修改数: " & totalChanged
    End If

    MsgBox summary, vbInformation, "批量文本颜色工具"
End Sub

Private Sub ProcessSlide(ByVal sld As Slide, _
                         ByVal slideHeight As Single, _
                         ByRef cfg As TextColorToolConfig, _
                         ByRef totalMatched As Long, _
                         ByRef totalChanged As Long)
    Dim shp As Shape

    For Each shp In sld.Shapes
        ProcessShape shp, sld, slideHeight, cfg, totalMatched, totalChanged
    Next shp
End Sub

Private Sub ProcessShape(ByVal shp As Shape, _
                        ByVal sld As Slide, _
                        ByVal slideHeight As Single, _
                        ByRef cfg As TextColorToolConfig, _
                        ByRef matchedCount As Long, _
                        ByRef changedCount As Long)
    On Error Resume Next

    ' 递归处理组合
    If shp.Type = msoGroup Then
        Dim subShape As Shape
        For Each subShape In shp.GroupItems
            ProcessShape subShape, sld, slideHeight, cfg, matchedCount, changedCount
        Next subShape
        Exit Sub
    End If

    ' 检查是否有文本
    If Not shp.HasTextFrame Then Exit Sub
    If Not shp.TextFrame.HasText Then Exit Sub

    ' 应用筛选条件
    If Not ShapeMatchesConfig(shp, slideHeight, cfg) Then Exit Sub

    ' 命中
    matchedCount = matchedCount + 1
    Debug.Print "[Slide " & sld.SlideIndex & "] " & shp.Name & " - " & _
                Left(Replace(shp.TextFrame.TextRange.Text, vbCr, " "), 50)

    ' 应用颜色
    If Not cfg.PreviewOnly Then
        shp.TextFrame.TextRange.Font.Color.RGB = cfg.TargetColor
        changedCount = changedCount + 1
    End If

    On Error GoTo 0
End Sub

' ============================================================
' 筛选逻辑
' ============================================================

Private Function ShapeMatchesConfig(ByVal shp As Shape, _
                                    ByVal slideHeight As Single, _
                                    ByRef cfg As TextColorToolConfig) As Boolean
    ' 位置筛选
    If cfg.RequireNearTop Then
        If shp.Top > slideHeight * cfg.TopRatio Then
            ShapeMatchesConfig = False
            Exit Function
        End If
    End If

    ' 颜色筛选
    If cfg.RequireRedish Then
        If Not IsTextRedish(shp, cfg.RedMin, cfg.RedMargin) Then
            ShapeMatchesConfig = False
            Exit Function
        End If
    End If

    ' 名称筛选
    If cfg.MatchByName Then
        If InStr(1, shp.Name, cfg.NameKeyword, vbTextCompare) = 0 Then
            ShapeMatchesConfig = False
            Exit Function
        End If
    End If

    ' 文本筛选
    If cfg.MatchByText Then
        Dim txt As String
        On Error Resume Next
        txt = shp.TextFrame.TextRange.Text
        On Error GoTo 0
        If InStr(1, txt, cfg.TextKeyword, vbTextCompare) = 0 Then
            ShapeMatchesConfig = False
            Exit Function
        End If
    End If

    ShapeMatchesConfig = True
End Function

Private Function IsTextRedish(ByVal shp As Shape, _
                              ByVal redMin As Long, _
                              ByVal redMargin As Long) As Boolean
    Dim colorValue As Long
    Dim r As Long, g As Long, b As Long

    On Error Resume Next
    colorValue = shp.TextFrame.TextRange.Font.Color.RGB
    On Error GoTo 0

    r = colorValue And &HFF
    g = (colorValue \ 256) And &HFF
    b = (colorValue \ 65536) And &HFF

    IsTextRedish = (r >= redMin And r >= g + redMargin And r >= b + redMargin)
End Function
