/**
 * WPS 批量文本颜色工具 - 零交互版
 * 直接执行，无任何对话框
 * 功能：将上方30%区域的蓝色文本改为黑色
 */

/**
 * 主函数：批量修改文本颜色
 * 规则：
 * - 只处理位于幻灯片上方30%区域的文本
 * - 只处理蓝色系文本（蓝色分量>=100）
 * - 目标颜色：黑色 RGB(0,0,0)
 */
function batchChangeColor() {
    const pres = Application.ActivePresentation;
    if (!pres) {
        console.log("错误：没有打开的演示文稿");
        return;
    }

    const slideHeight = pres.PageSetup.SlideHeight;
    let totalChanged = 0;

    // 遍历所有幻灯片
    for (let i = 1; i <= pres.Slides.Count; i++) {
        const slide = pres.Slides.Item(i);
        totalChanged += processSlide(slide, slideHeight);
    }

    console.log(`完成！共修改 ${totalChanged} 个文本对象`);
    return totalChanged;
}

/**
 * 处理单个幻灯片
 */
function processSlide(slide, slideHeight) {
    let changed = 0;
    for (let i = 1; i <= slide.Shapes.Count; i++) {
        changed += processShape(slide.Shapes.Item(i), slideHeight);
    }
    return changed;
}

/**
 * 处理单个形状（递归处理组合）
 */
function processShape(shape, slideHeight) {
    let changed = 0;

    try {
        // 处理组合形状
        if (shape.Type === 6) {
            for (let i = 1; i <= shape.GroupItems.Count; i++) {
                changed += processShape(shape.GroupItems.Item(i), slideHeight);
            }
            return changed;
        }

        // 检查是否有文本
        if (!shape.HasTextFrame || !shape.TextFrame.HasText) {
            return 0;
        }

        // 位置筛选：只处理上方30%区域
        if (shape.Top > slideHeight * 0.3) {
            return 0;
        }

        // 颜色筛选：只处理蓝色文本
        const color = shape.TextFrame.TextRange.Font.Color.RGB;
        const r = color & 0xFF;
        const g = (color >> 8) & 0xFF;
        const b = (color >> 16) & 0xFF;

        if (b >= 100 && b >= r + 30 && b >= g + 30) {
            // 修改为黑色 RGB(0, 0, 0)
            shape.TextFrame.TextRange.Font.Color.RGB = 0;
            changed = 1;
        }
    } catch (e) {
        // 忽略错误
    }

    return changed;
}
