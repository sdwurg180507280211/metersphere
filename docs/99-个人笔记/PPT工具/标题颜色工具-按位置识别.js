/**
 * WPS PPT 万能工具箱
 *
 * 功能列表：
 * 1. 标题颜色修改 - 按位置识别（≤2.5cm）并批量修改颜色
 * 2. 标题布局调整 - 统一调整标题位置和大小
 * 3. 文本批量替换 - 替换所有位置的文本内容（含表格单元格）
 * 4. 字体批量替换 - 替换指定字体为其他字体（含表格单元格）
 *
 * 使用方法：复制全部代码到WPS演示文稿宏编辑器，运行对应的快捷函数
 */

/**
 * 将标题区域文本改为指定颜色
 * @param {number} targetR - 目标红色值 (0-255)
 * @param {number} targetG - 目标绿色值 (0-255)
 * @param {number} targetB - 目标蓝色值 (0-255)
 */
function changeTitleColor(targetR, targetG, targetB) {
    const pres = Application.ActivePresentation;
    if (!pres) {
        console.log("错误：没有打开的演示文稿");
        return;
    }

    const titleThreshold = 70.9;  // 2.5cm
    const targetRGB = targetR + (targetG << 8) + (targetB << 16);
    let totalChanged = 0;

    for (let i = 1; i <= pres.Slides.Count; i++) {
        const slide = pres.Slides.Item(i);
        totalChanged += processTitleShapes(slide, titleThreshold, targetRGB);
    }

    console.log(`完成！共修改 ${totalChanged} 个标题文本`);
    return totalChanged;
}

function processTitleShapes(slide, titleThreshold, targetRGB) {
    let changed = 0;
    for (let i = 1; i <= slide.Shapes.Count; i++) {
        changed += processTitleShape(slide.Shapes.Item(i), titleThreshold, targetRGB);
    }
    return changed;
}

/**
 * 调整标题位置和大小
 * @param {Shape} shape - 要调整的形状对象
 */
function adjustTitleLayout(shape) {
    shape.Left = 96.4;    // 3.40cm
    shape.Top = 25.5;     // 0.90cm
    shape.Width = 850.5;  // 30cm
}

function processTitleShape(shape, titleThreshold, targetRGB) {
    let changed = 0;

    try {
        // 处理组合
        if (shape.Type === 6) {
            for (let i = 1; i <= shape.GroupItems.Count; i++) {
                changed += processTitleShape(shape.GroupItems.Item(i), titleThreshold, targetRGB);
            }
            return changed;
        }

        // 检查文本
        if (!shape.HasTextFrame || !shape.TextFrame.HasText) {
            return 0;
        }

        // 位置判断：≤ 2.5cm 就是标题
        if (shape.Top <= titleThreshold) {
            // 修改颜色
            shape.TextFrame.TextRange.Font.Color.RGB = targetRGB;

            // 调整位置和大小
            adjustTitleLayout(shape);

            changed = 1;
        }
    } catch (e) {
        // 忽略错误
    }

    return changed;
}

// ============================================
// 快捷函数（可在WPS宏列表中直接运行）
// ============================================

// 标题改为黑色
function titleToBlack() { changeTitleColor(0, 0, 0); }

// 标题改为蓝色
function titleToBlue() { changeTitleColor(0, 112, 192); }

// 标题改为红色
function titleToRed() { changeTitleColor(255, 0, 0); }

// 标题改为绿色
function titleToGreen() { changeTitleColor(0, 176, 80); }

// 标题改为黄色
function titleToYellow() { changeTitleColor(255, 255, 0); }

// 标题改为白色
function titleToWhite() { changeTitleColor(255, 255, 255); }

// 标题改为深蓝
function titleToDarkBlue() { changeTitleColor(0, 32, 96); }

// 标题改为橙色
function titleToOrange() { changeTitleColor(255, 192, 0); }

/**
 * 批量替换文本内容（所有位置）
 * @param {Array} rules - 替换规则数组 [[from, to], ...]
 */
function replaceTitleText(rules) {
    const pres = Application.ActivePresentation;
    if (!pres) {
        console.log("错误：没有打开的演示文稿");
        return;
    }

    let totalReplaced = 0;

    for (let i = 1; i <= pres.Slides.Count; i++) {
        const slide = pres.Slides.Item(i);
        totalReplaced += replaceInSlide(slide, rules);
    }

    console.log(`完成！共替换 ${totalReplaced} 个文本对象`);
    return totalReplaced;
}

function replaceInSlide(slide, rules) {
    let replaced = 0;
    for (let i = 1; i <= slide.Shapes.Count; i++) {
        replaced += replaceInShape(slide.Shapes.Item(i), rules);
    }
    return replaced;
}

function replaceInShape(shape, rules) {
    let replaced = 0;

    try {
        // 处理组合
        if (shape.Type === 6) {
            for (let i = 1; i <= shape.GroupItems.Count; i++) {
                replaced += replaceInShape(shape.GroupItems.Item(i), rules);
            }
            return replaced;
        }

        // 处理表格：遍历每个单元格
        if (shape.HasTable) {
            const table = shape.Table;
            for (let row = 1; row <= table.Rows.Count; row++) {
                for (let col = 1; col <= table.Columns.Count; col++) {
                    try {
                        const cell = table.Cell(row, col);
                        const cellShape = cell.Shape;
                        if (cellShape.HasTextFrame && cellShape.TextFrame.HasText) {
                            replaced += replaceInTextRange(cellShape.TextFrame.TextRange, rules);
                        }
                    } catch (e) {
                        // 跳过合并单元格等异常
                    }
                }
            }
            return replaced;
        }

        // 处理普通文本框
        if (!shape.HasTextFrame || !shape.TextFrame.HasText) {
            return 0;
        }

        replaced += replaceInTextRange(shape.TextFrame.TextRange, rules);
    } catch (e) {
        console.log("替换异常，shape类型: " + shape.Type + "，错误: " + e.message);
    }

    return replaced;
}

/**
 * 在 TextRange 上执行替换规则
 * @param {TextRange} textRange - 文本范围对象
 * @param {Array} rules - 替换规则数组 [[from, to], ...]
 * @returns {number} 替换命中次数
 */
function replaceInTextRange(textRange, rules) {
    let count = 0;
    for (let j = 0; j < rules.length; j++) {
        const [from, to] = rules[j];
        if (textRange.Text.indexOf(from) >= 0) {
            textRange.Replace(from, to, false, false, false);
            count++;
        }
    }
    return count;
}

// ============================================
// 文本替换快捷函数
// ============================================

/**
 * 替换药品名称（示例）
 */
function replaceDrugNames() {
    const rules = [
        // 第一轮：带®符号的
        ["尼膜同®", "尼莫地平片"],
        ["唯可同®", "维立西呱片"],
        ["拜瑞妥®", "利伐沙班片"],
        ["拜新同®", "硝苯地平控释片"],
        ["可申达®", "非奈利酮片"],
        ["希维她®", ""],
        // 第二轮：不带®符号的
        ["尼膜同", "尼莫地平片"],
        ["唯可同", "维立西呱片"],
        ["拜瑞妥", "利伐沙班片"],
        ["拜新同", "硝苯地平控释片"],
        ["可申达", "非奈利酮片"],
        ["希维她", ""],
        // 第三轮：删除品牌名
        ["拜耳", ""]
    ];

    replaceTitleText(rules);
}

// ============================================
// 字体替换功能
// ============================================

/**
 * 批量替换字体
 * @param {string} fromFont - 源字体名称
 * @param {string} toFont - 目标字体名称
 */
function replaceFontName(fromFont, toFont) {
    const pres = Application.ActivePresentation;
    if (!pres) {
        console.log("错误：没有打开的演示文稿");
        return;
    }

    let totalChanged = 0;

    for (let i = 1; i <= pres.Slides.Count; i++) {
        const slide = pres.Slides.Item(i);
        totalChanged += replaceFontInSlide(slide, fromFont, toFont);
    }

    console.log(`完成！共修改 ${totalChanged} 个文本对象的字体`);
    return totalChanged;
}

function replaceFontInSlide(slide, fromFont, toFont) {
    let changed = 0;
    for (let i = 1; i <= slide.Shapes.Count; i++) {
        changed += replaceFontInShape(slide.Shapes.Item(i), fromFont, toFont);
    }
    return changed;
}

function replaceFontInShape(shape, fromFont, toFont) {
    let changed = 0;

    try {
        // 处理组合
        if (shape.Type === 6) {
            for (let i = 1; i <= shape.GroupItems.Count; i++) {
                changed += replaceFontInShape(shape.GroupItems.Item(i), fromFont, toFont);
            }
            return changed;
        }

        // 处理表格：遍历每个单元格
        if (shape.HasTable) {
            const table = shape.Table;
            for (let row = 1; row <= table.Rows.Count; row++) {
                for (let col = 1; col <= table.Columns.Count; col++) {
                    try {
                        const cell = table.Cell(row, col);
                        const cellShape = cell.Shape;
                        if (cellShape.HasTextFrame && cellShape.TextFrame.HasText) {
                            changed += replaceFontInTextRange(cellShape.TextFrame.TextRange, fromFont, toFont);
                        }
                    } catch (e) {
                        // 跳过合并单元格等异常
                    }
                }
            }
            return changed;
        }

        // 处理普通文本框
        if (!shape.HasTextFrame || !shape.TextFrame.HasText) {
            return 0;
        }

        changed += replaceFontInTextRange(shape.TextFrame.TextRange, fromFont, toFont);
    } catch (e) {
        console.log("字体替换异常，shape类型: " + shape.Type + "，错误: " + e.message);
    }

    return changed;
}

/**
 * 在 TextRange 上执行字体替换
 * @param {TextRange} textRange - 文本范围对象
 * @param {string} fromFont - 源字体名称
 * @param {string} toFont - 目标字体名称
 * @returns {number} 是否替换成功 (0 或 1)
 */
function replaceFontInTextRange(textRange, fromFont, toFont) {
    if (textRange.Font.Name === fromFont) {
        textRange.Font.Name = toFont;
        return 1;
    }
    return 0;
}

// 快捷函数：苹方-简 → 微软雅黑
function pingfangToYahei() {
    replaceFontName("苹方-简", "微软雅黑");
}

