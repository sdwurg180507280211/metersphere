/**
 * 查看上方30%区域所有文本框的位置信息
 */
function showTextPositions() {
    const pres = Application.ActivePresentation;
    if (!pres) {
        console.log("错误：没有打开的演示文稿");
        return;
    }

    const slideHeight = pres.PageSetup.SlideHeight;
    const slideWidth = pres.PageSetup.SlideWidth;

    console.log(`幻灯片尺寸: 宽=${slideWidth}, 高=${slideHeight}`);
    console.log("=".repeat(60));

    // 遍历所有幻灯片
    for (let i = 1; i <= pres.Slides.Count; i++) {
        const slide = pres.Slides.Item(i);
        console.log(`\n【幻灯片 ${i}】`);

        for (let j = 1; j <= slide.Shapes.Count; j++) {
            showShapePosition(slide.Shapes.Item(j), slideHeight, j);
        }
    }
}

function showShapePosition(shape, slideHeight, index) {
    try {
        // 只显示有文本的形状
        if (!shape.HasTextFrame || !shape.TextFrame.HasText) {
            return;
        }

        // 只显示上方30%区域的
        if (shape.Top > slideHeight * 0.3) {
            return;
        }

        const text = shape.TextFrame.TextRange.Text.substring(0, 20); // 只显示前20个字符
        console.log(`  形状${index}: "${text}..."`);
        console.log(`    左边距(Left): ${shape.Left.toFixed(2)}`);
        console.log(`    上边距(Top): ${shape.Top.toFixed(2)}`);
        console.log(`    宽度: ${shape.Width.toFixed(2)}`);
        console.log(`    高度: ${shape.Height.toFixed(2)}`);

    } catch (e) {
        // 忽略错误
    }
}
