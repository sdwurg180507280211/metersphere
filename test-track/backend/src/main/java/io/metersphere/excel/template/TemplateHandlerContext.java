package io.metersphere.excel.template;

/**
 * 模板处理器上下文
 * 用于传递模板处理过程中需要的上下文信息
 * 
 * @author metersphere
 */
public class TemplateHandlerContext {
    
    /**
     * Excel文件名（不含路径）
     */
    private String excelFileName;
    
    /**
     * 项目ID
     */
    private String projectId;
    
    public String getExcelFileName() {
        return excelFileName;
    }
    
    public void setExcelFileName(String excelFileName) {
        this.excelFileName = excelFileName;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * 获取Excel文件名（不含扩展名）
     */
    public String getExcelFileNameWithoutExtension() {
        if (excelFileName == null) {
            return null;
        }
        int lastDotIndex = excelFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return excelFileName.substring(0, lastDotIndex);
        }
        return excelFileName;
    }
}

