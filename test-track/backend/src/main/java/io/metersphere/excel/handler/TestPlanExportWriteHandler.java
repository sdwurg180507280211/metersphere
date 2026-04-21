package io.metersphere.excel.handler;

import com.alibaba.excel.util.BooleanUtils;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.RowWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * 测试计划导出列宽设置处理器
 */
public class TestPlanExportWriteHandler implements SheetWriteHandler, RowWriteHandler {

    /**
     * Excel 列宽单位（1个字符宽度 = 256个单位）
     */
    private static final int UNIT_WIDTH = 256;

    /**
     * 列宽设置（单位：字符数，1个汉字约占2个字符）
     */
    private static final int[] COLUMN_WIDTHS = {
            30,  // 名称
            12,  // 状态
            12,  // 测试阶段
            12,  // 测试进度
            25,  // 所属项目
            25,  // 所属模块
            20,  // 计划开始时间
            20,  // 计划结束时间
            20,  // 实际开始时间
            20,  // 实际结束时间
            20,  // 标签
            15,  // 定时任务
            10,  // 通过率
            15,  // 创建人
            12,  // 功能用例数
            12,  // 接口用例数
            12,  // 场景用例数
            12,  // UI场景数
            12,  // 性能用例数
            15,  // 责任人
            20,  // 创建时间
            20   // 更新时间
    };

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        // 设置列宽
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * UNIT_WIDTH);
        }
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        // 设置表头行高
        if (BooleanUtils.isTrue(context.getHead())) {
            context.getRow().setHeightInPoints(25);
        }
    }
}
