# 测试用例Excel模板驱动设计文档

## 一、业务处理流程

### 1.1 导入流程概览

```
用户上传Excel文件
    ↓
TestCaseService.testCaseExcelImport()
    ↓
创建 TestCaseNoModelDataListener（构造函数中初始化模板处理器）
    ↓
EasyExcel读取Excel（触发Listener的回调方法）
    ↓
1. invokeHeadMap() - 处理表头，使用模板处理器的列名映射
    ↓
2. invoke() - 逐行处理数据
    ├── parseDataToModel() - 解析数据，使用模板处理器的默认值和数据处理
    └── buildUpdateOrErrorList() - 验证数据，使用模板处理器的验证规则
    ↓
保存数据到数据库
```

### 1.2 模板处理器在业务代码中的使用位置

| 使用位置 | 文件位置 | 调用的方法 | 作用 |
|---------|---------|-----------|------|
| **初始化** | `TestCaseNoModelDataListener.java:165-190` | `getHandler()`<br>`getHeadMapping()` | 根据模板文件名获取处理器，获取列名映射 |
| **表头处理** | `TestCaseNoModelDataListener.java:932-945` | `getHeadMapping()` | 将Excel表头名称映射为内部字段名 |
| **数据解析** | `TestCaseNoModelDataListener.java:902-915` | `getDefaultStepModel()` | 获取默认的步骤编辑模式（STEP/TEXT） |
| **数据处理** | `TestCaseNoModelDataListener.java:890-892` | `processData()` | 执行模板特定的数据处理逻辑 |
| **数据验证** | `TestCaseNoModelDataListener.java:372-376` | `validateData()` | 执行模板特定的验证规则 |

### 1.3 完整调用链

```
用户上传Excel
    ↓
TestCaseController.testCaseImport()
    ↓
TestCaseService.testCaseImport()
    ↓
TestCaseService.testCaseExcelImport()
    ↓
new TestCaseNoModelDataListener(request, clazz, mergeInfoSet)
    ├─→ loadCustomHeadMapping(projectId)
    │   ├─→ TrackTestCaseTemplateService.getTemplate(projectId)
    │   ├─→ TestCaseTemplateHandlerFactory.getHandler(templateFileName)
    │   │   └─→ 返回 TestCaseTemplate1Handler/2Handler/3Handler
    │   └─→ templateHandler.getHeadMapping()
    │       └─→ 返回列名映射Map，存入 customHeadMapping
    │
    └─→ EasyExcelFactory.read(inputStream, listener)
        │
        ├─→ listener.invokeHeadMap(headMap, context)
        │   └─→ formatHeadMap()
        │       └─→ 使用 customHeadMapping 映射表头名称
        │
        └─→ listener.invoke(data, context) [每行数据]
            ├─→ parseDataToModel(data)
            │   ├─→ getDefaultStepModelByTemplate()
            │   │   └─→ templateHandler.getDefaultStepModel()
            │   └─→ templateHandler.processData(data)
            │
            └─→ buildUpdateOrErrorList(data)
                └─→ validate(data, errMsg)
                    └─→ templateHandler.validateData(data)
```

## 二、业务代码集成点

### 2.1 位置1：TestCaseNoModelDataListener 构造函数

**文件位置：** `TestCaseNoModelDataListener.java:139-159`

**业务逻辑：**
```java
public TestCaseNoModelDataListener(TestCaseImportRequest request, Class c, Set<ExcelMergeInfo> mergeInfoSet) {
    // ... 其他初始化代码
    
    // 根据项目模板加载列名映射配置
    loadCustomHeadMapping(request.getProjectId());  // ← 在这里初始化模板处理器
}
```

**调用时机：** Excel导入开始时，在 `TestCaseService.testCaseExcelImport()` 中创建Listener时

**处理内容：**
1. 从数据库获取项目绑定的模板信息（`TestCaseTemplateDao`）
2. 获取模板文件名（`excelTemplateFile` 字段）
3. 通过工厂获取对应的模板处理器
4. 从模板处理器获取列名映射配置，存入 `customHeadMapping`

**关键代码：**
```java
// TestCaseNoModelDataListener.java:165-190
private void loadCustomHeadMapping(String projectId) {
    TrackTestCaseTemplateService trackTestCaseTemplateService = CommonBeanFactory.getBean(TrackTestCaseTemplateService.class);
    TestCaseTemplateDao template = trackTestCaseTemplateService.getTemplate(projectId);
    
    String templateFileName = template != null ? template.getExcelTemplateFile() : null;
    
    // 获取模板处理器
    templateHandler = TestCaseTemplateHandlerFactory.getHandler(templateFileName);
    
    // 获取列名映射配置
    customHeadMapping = templateHandler.getHeadMapping();
}
```

### 2.2 位置2：invokeHeadMap() - 表头处理

**文件位置：** `TestCaseNoModelDataListener.java:193-203`

**业务逻辑：**
```java
@Override
public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
    this.headMap = headMap;
    genExcelHeadToFieldNameDicAndGetNotRequiredFields();
    formatHeadMap();  // ← 在这里使用模板处理器的列名映射
    super.invokeHeadMap(headMap, context);
}
```

**调用时机：** EasyExcel读取Excel表头时自动调用

**处理内容：**
- 将Excel表头名称转换为内部字段名称
- 使用模板处理器的 `getHeadMapping()` 返回的映射关系进行转换

**关键代码：**
```java
// TestCaseNoModelDataListener.java:932-945
private void formatHeadMap() {
    for (Integer key : headMap.keySet()) {
        String name = headMap.get(key);
        // 先检查自定义映射（从模板处理器获取的映射）
        if (customHeadMapping.containsKey(name)) {
            name = customHeadMapping.get(name);  // ← 使用模板处理器的映射
            headMap.put(key, name);
        }
        // 再检查标准映射（从注解中读取的映射）
        if (excelHeadToFieldNameDic.containsKey(name)) {
            headMap.put(key, excelHeadToFieldNameDic.get(name));
        }
    }
}
```

**示例：**
- Excel表头：`"用例ID"` → 通过模板处理器映射 → 内部字段：`"ID"`
- Excel表头：`"设计人"` → 通过模板2处理器映射 → 内部字段：`"设计人"`

### 2.3 位置3：parseDataToModel() - 数据解析

**文件位置：** `TestCaseNoModelDataListener.java:845-895`

**业务逻辑：**
```java
private TestCaseExcelData parseDataToModel(Map<Integer, String> row) {
    TestCaseExcelData data = new TestCaseExcelDataFactory().getTestCaseExcelDataLocal();
    
    // 解析Excel行数据到对象
    for (Map.Entry<Integer, String> headEntry : headMap.entrySet()) {
        // ... 字段赋值逻辑
    }
    
    // 设置默认的步骤编辑模式
    if (StringUtils.isBlank(data.getStepModel())) {
        String defaultStepModel = getDefaultStepModelByTemplate();  // ← 使用模板处理器的默认值
        data.setStepModel(defaultStepModel);
    }
    
    // 使用模板处理器进行模板特定的数据处理
    if (templateHandler != null) {
        templateHandler.processData(data);  // ← 调用模板处理器的数据处理方法
    }
    
    return data;
}
```

**调用时机：** 每读取一行Excel数据时调用

**处理内容：**
1. 获取默认步骤编辑模式（STEP或TEXT）
2. 调用模板处理器的 `processData()` 进行模板特定的数据处理

**关键代码：**
```java
// TestCaseNoModelDataListener.java:902-915
private String getDefaultStepModelByTemplate() {
    // 优先使用数据库模板配置
    if (currentTemplate != null && StringUtils.isNotBlank(currentTemplate.getStepModel())) {
        return currentTemplate.getStepModel();
    }
    
    // 使用模板处理器的默认值
    if (templateHandler != null) {
        return templateHandler.getDefaultStepModel();  // ← 模板1返回"STEP"，模板2返回"TEXT"
    }
    
    return TestCaseConstants.StepModel.STEP.name();
}
```

**示例：**
- 模板1：`getDefaultStepModel()` 返回 `"STEP"` → 数据使用步骤编辑模式
- 模板2：`getDefaultStepModel()` 返回 `"TEXT"` → 数据使用文本编辑模式

### 2.4 位置4：validate() - 数据验证

**文件位置：** `TestCaseNoModelDataListener.java:361-378`

**业务逻辑：**
```java
public void validate(TestCaseExcelData data, StringBuilder errMsg) {
    // 通用验证
    validateCustomNum(data, errMsg);
    validateModule(data, errMsg);
    validateCustomField(data, errMsg);
    validateIdExist(data, errMsg);
    validateDbExist(data, errMsg);
    
    // 使用模板处理器进行模板特定的验证
    if (templateHandler != null) {
        String templateValidationError = templateHandler.validateData(data);  // ← 调用模板验证
        if (StringUtils.isNotBlank(templateValidationError)) {
            errMsg.append(templateValidationError);
        }
    }
}
```

**调用时机：** 每行数据解析完成后，在 `buildUpdateOrErrorList()` 中调用

**处理内容：**
- 执行通用验证（自定义编号、模块、字段等）
- 执行模板特定的验证规则

**示例：**
- 模板1：验证步骤描述不能为空
- 模板2：验证设计人、执行人字段

### 2.5 位置5：TestCaseService.testCaseExcelImport() - 导入入口

**文件位置：** `TestCaseService.java:1282-1289`

**业务逻辑：**
```java
private ExcelResponse testCaseExcelImport(MultipartFile multipartFile, 
                                         TestCaseImportRequest request, 
                                         HttpServletRequest httpRequest) {
    // ... 前置处理
    
    // 创建Listener（构造函数中会初始化模板处理器）
    TestCaseNoModelDataListener easyExcelListener = 
        new TestCaseNoModelDataListener(request, clazz, mergeInfoSet);
    
    // 设置Excel文件名到模板处理器上下文（用于模板特定的处理逻辑）
    if (multipartFile.getOriginalFilename() != null) {
        easyExcelListener.setExcelFileName(multipartFile.getOriginalFilename());
    }
    
    // 读取Excel数据（会触发Listener的回调方法）
    EasyExcelFactory.read(multipartFile.getInputStream(), easyExcelListener).sheet().doRead();
    
    // 获取错误列表
    errList = easyExcelListener.getErrList();
    
    return getImportResponse(errList, isUpdated, request.isIgnore());
}
```

**调用时机：** 用户上传Excel文件导入时

**处理内容：**
- 创建 `TestCaseNoModelDataListener`（此时会初始化模板处理器）
- **设置Excel文件名到模板处理器上下文**（用于模板2等需要文件名的模板）
- 使用EasyExcel读取Excel文件
- EasyExcel会自动调用Listener的回调方法，模板处理器在这些回调中被使用

## 三、架构设计

### 2.1 类结构

```
TestCaseTemplateHandler (接口)
    ↑
    |
AbstractTestCaseTemplateHandler (抽象基类)
    ↑
    ├── TestCaseTemplate1Handler (模板1处理器)
    ├── TestCaseTemplate2Handler (模板2处理器)
    ├── TestCaseTemplate3Handler (模板3处理器)
    └── DefaultTestCaseTemplateHandler (默认处理器)

TestCaseTemplateHandlerFactory (工厂类)
```

### 2.2 核心组件

#### 2.2.1 接口层：`TestCaseTemplateHandler`

定义模板处理器的标准接口，包含以下方法：

- `getTemplateFileName()`: 获取模板文件名
- `getHeadMapping()`: 获取列名映射配置
- `getDefaultStepModel()`: 获取默认步骤编辑模式
- `validateData()`: 验证数据
- `processData()`: 处理数据
- `getDescription()`: 获取模板描述
- `setContext()`: 设置模板处理器上下文（用于传递Excel文件名等信息）

#### 2.2.2 抽象基类：`AbstractTestCaseTemplateHandler`

提供通用的实现逻辑，减少子类代码重复：

- **通用列名映射**：所有模板都支持的基础字段映射（用例ID、用例名称、步骤描述等）
- **通用数据处理**：统一的步骤编辑模式设置逻辑
- **模板方法模式**：定义处理流程，子类只需实现特定部分
- **上下文支持**：提供 `context` 字段，子类可以通过上下文获取Excel文件名等信息

#### 2.2.3 具体处理器：`TestCaseTemplateXHandler`

每个模板的独立处理器，只需实现模板特定的逻辑：

- 模板特定的列名映射（通过 `getSpecificHeadMapping()` 方法）
- 模板特定的验证规则（重写 `validateData()` 方法）
- 模板特定的数据处理（重写 `doProcessData()` 方法）

#### 2.2.4 工厂类：`TestCaseTemplateHandlerFactory`

负责根据模板文件名获取对应的处理器：

- 支持精确匹配和模糊匹配
- 提供默认处理器作为兜底方案
- 使用缓存机制提高性能

## 四、数据流转示例

### 4.1 模板1（STEP模式）的数据流转

```
1. 用户上传 testcase-template-1.xlsx
   ↓
2. TestCaseNoModelDataListener 构造函数
   - 获取模板文件名：testcase-template-1.xlsx
   - 工厂返回：TestCaseTemplate1Handler
   - 获取列名映射：{"用例ID"→"ID", "用例描述"→"用例名称", ...}
   ↓
3. invokeHeadMap() 处理表头
   - Excel表头："用例ID" → 映射为 "ID"
   - Excel表头："用例描述" → 映射为 "用例名称"
   ↓
4. invoke() 处理每行数据
   - parseDataToModel() 解析数据
     - 如果 stepModel 为空，调用 handler.getDefaultStepModel() → 返回 "STEP"
     - 调用 handler.processData(data) → 设置 stepModel = "STEP"
   - validate() 验证数据
     - 调用 handler.validateData(data) → 验证步骤描述不能为空
   ↓
5. 保存到数据库（stepModel = "STEP"）
```

### 4.2 模板2（TEXT模式）的数据流转

```
1. 用户上传 testcase-template-2.xlsx（文件名为：测试用例.xlsx）
   ↓
2. TestCaseService.testCaseExcelImport()
   - 创建 TestCaseNoModelDataListener
   - 调用 setExcelFileName("测试用例.xlsx") 设置文件名到上下文
   ↓
3. TestCaseNoModelDataListener 构造函数
   - 获取模板文件名：testcase-template-2.xlsx
   - 工厂返回：TestCaseTemplate2Handler
   - 获取列名映射：{"用例ID"→"ID", "设计人"→"设计人", "执行人"→"执行人", ...}
   - 创建 TemplateHandlerContext，设置 projectId 和 excelFileName
   ↓
4. invokeHeadMap() 处理表头
   - Excel表头："用例ID" → 映射为 "ID"
   - Excel表头："设计人" → 映射为 "设计人"（模板2特有）
   - Excel表头："执行人" → 映射为 "执行人"（模板2特有）
   ↓
5. invoke() 处理每行数据
   - parseDataToModel() 解析数据
     - 如果 stepModel 为空，调用 handler.getDefaultStepModel() → 返回 "TEXT"
     - 调用 handler.processData(data) → 执行模板2特定处理
       - 检查所属模块字段（nodePath）
       - 如果所属模块为空 → 设置为 "/测试用例"
       - 如果所属模块为 "/模块1" → 设置为 "/测试用例/模块1"
       - 如果所属模块已经是 "/测试用例/模块1" → 保持不变
   - validate() 验证数据
     - 调用 handler.validateData(data) → 验证步骤描述不能为空
   ↓
6. 保存到数据库（stepModel = "TEXT"，所属模块 = "/测试用例/模块1"，包含设计人、执行人字段）
```

## 五、模板驱动设计的优势

### 3.1 代码清晰性

**重构前（硬编码方式）：**
```java
// 业务代码中充斥着大量的 if-else 判断
if (StringUtils.contains(templateFileName, "template-1")) {
    // 模板1的处理逻辑
    mapping.put("用例ID", "ID");
    // ... 更多模板1的规则
} else if (StringUtils.contains(templateFileName, "template-2")) {
    // 模板2的处理逻辑
    mapping.put("设计人", "设计人");
    // ... 更多模板2的规则
} else if (StringUtils.contains(templateFileName, "template-3")) {
    // 模板3的处理逻辑
    // ... 更多模板3的规则
}
```

**重构后（模板驱动方式）：**
```java
// 业务代码简洁清晰，无需关心具体模板
TestCaseTemplateHandler handler = TestCaseTemplateHandlerFactory.getHandler(templateFileName);
Map<String, String> mapping = handler.getHeadMapping();
handler.validateData(data);
handler.processData(data);
```

### 3.2 易于扩展

**添加新模板只需三步：**

1. 创建新的处理器类继承 `AbstractTestCaseTemplateHandler`
2. 实现模板特定的逻辑
3. 在工厂类中注册新处理器

**示例：**
```java
public class TestCaseTemplate4Handler extends AbstractTestCaseTemplateHandler {
    @Override
    public String getTemplateFileName() {
        return "testcase-template-4.xlsx";
    }
    
    @Override
    protected Map<String, String> getSpecificHeadMapping() {
        // 模板4特有的映射规则
        Map<String, String> mapping = new HashMap<>();
        mapping.put("新字段", "内部字段名");
        return mapping;
    }
    
    // ... 其他方法
}
```

### 3.3 易于维护

**单一职责原则：**
- 每个处理器类只负责一个模板的规则
- 修改某个模板的规则不会影响其他模板
- 代码职责清晰，易于定位问题

**开闭原则：**
- 对扩展开放：可以轻松添加新模板
- 对修改封闭：修改模板规则只需修改对应的处理器类，不影响业务代码

### 3.4 减少代码重复

**通过抽象基类提取公共逻辑：**

- 通用列名映射：所有模板共享的基础字段映射
- 通用数据处理：统一的步骤编辑模式设置逻辑
- 子类只需关注模板特定的差异部分

**代码量对比：**
- 重构前：每个模板的规则分散在业务代码中，存在大量重复
- 重构后：公共逻辑提取到基类，子类代码量减少约60%

### 3.5 提高可测试性

**每个处理器可以独立测试：**

```java
@Test
public void testTemplate1Handler() {
    TestCaseTemplateHandler handler = new TestCaseTemplate1Handler();
    
    // 测试列名映射
    Map<String, String> mapping = handler.getHeadMapping();
    assertEquals("ID", mapping.get("用例ID"));
    
    // 测试验证逻辑
    TestCaseExcelData data = new TestCaseExcelData();
    String errors = handler.validateData(data);
    assertTrue(errors.contains("步骤描述不能为空"));
}
```

### 3.6 配置驱动

**模板规则集中在处理器类中：**

- 列名映射规则：集中在 `getHeadMapping()` 方法
- 验证规则：集中在 `validateData()` 方法
- 处理规则：集中在 `processData()` 方法

修改规则时，只需修改对应的处理器类，无需在业务代码中查找和修改。

### 3.7 向后兼容

**保留默认处理器：**

- 当找不到匹配的模板时，使用默认处理器
- 提供通用的处理逻辑，确保系统稳定运行
- 支持渐进式迁移，无需一次性修改所有代码

## 六、使用示例

### 4.1 在业务代码中使用

```java
// 1. 获取模板处理器
String templateFileName = template.getExcelTemplateFile();
TestCaseTemplateHandler handler = TestCaseTemplateHandlerFactory.getHandler(templateFileName);

// 2. 获取列名映射
Map<String, String> headMapping = handler.getHeadMapping();

// 3. 验证数据
String validationErrors = handler.validateData(excelData);
if (StringUtils.isNotBlank(validationErrors)) {
    // 处理验证错误
}

// 4. 处理数据
handler.processData(excelData);
```

### 4.2 创建新模板处理器

```java
public class TestCaseTemplate4Handler extends AbstractTestCaseTemplateHandler {
    
    @Override
    public String getTemplateFileName() {
        return "testcase-template-4.xlsx";
    }
    
    @Override
    protected Map<String, String> getSpecificHeadMapping() {
        Map<String, String> mapping = new HashMap<>();
        // 添加模板4特有的字段映射
        mapping.put("特殊字段", "内部字段名");
        return mapping;
    }
    
    @Override
    public String getDefaultStepModel() {
        return TestCaseConstants.StepModel.TEXT.name();
    }
    
    @Override
    public String validateData(TestCaseExcelData data) {
        StringBuilder errors = new StringBuilder();
        // 添加模板4特有的验证规则
        if (StringUtils.isBlank(data.getCustomData().get("特殊字段"))) {
            errors.append("特殊字段不能为空;");
        }
        return errors.toString();
    }
    
    @Override
    protected void doProcessData(TestCaseExcelData data) {
        // 添加模板4特有的数据处理逻辑
        // 例如：数据格式转换、默认值填充等
    }
    
    @Override
    public String getDescription() {
        return "用例模板4 - 自定义模板";
    }
}
```

### 4.3 在工厂中注册新处理器

```java
static {
    registerHandler(new TestCaseTemplate1Handler());
    registerHandler(new TestCaseTemplate2Handler());
    registerHandler(new TestCaseTemplate3Handler());
    registerHandler(new TestCaseTemplate4Handler()); // 注册新处理器
}
```

## 七、最佳实践

### 5.1 模板处理器设计原则

1. **单一职责**：每个处理器只负责一个模板的规则
2. **最小化差异**：只实现模板特定的逻辑，公共逻辑交给基类
3. **清晰命名**：处理器类名和模板文件名保持一致
4. **完整文档**：在类注释中说明模板的特点和用途

### 5.2 列名映射设计

1. **通用映射**：基础字段映射放在基类中
2. **特定映射**：模板特有的字段映射放在子类的 `getSpecificHeadMapping()` 方法中
3. **覆盖机制**：特定映射可以覆盖通用映射

### 5.3 验证规则设计

1. **分层验证**：基础验证在业务代码中，模板特定验证在处理器中
2. **错误信息**：提供清晰的错误提示，便于用户理解
3. **非阻塞验证**：收集所有错误，而不是遇到第一个错误就停止

### 5.4 数据处理设计

1. **幂等性**：多次处理同一数据应该得到相同结果
2. **默认值**：合理设置默认值，提高用户体验
3. **格式转换**：在处理器中统一处理数据格式转换

## 八、性能考虑

### 6.1 缓存机制

- 工厂类使用静态缓存存储所有处理器实例
- 处理器实例在类加载时创建，避免重复创建
- 获取处理器的时间复杂度为 O(1)

### 6.2 内存优化

- 列名映射使用不可变Map，避免重复创建
- 处理器实例复用，减少对象创建开销

## 九、模板特定功能实现示例

### 9.1 模板2：所属模块字段自动加上Excel文件名

**需求：** 模板2导入时，所属模块字段前面默认加上Excel文件名（不含扩展名）

**实现方式：** 通过模板处理器上下文机制实现

**实现步骤：**

1. **创建上下文类** `TemplateHandlerContext`
   - 用于传递Excel文件名、项目ID等上下文信息
   - 提供 `getExcelFileNameWithoutExtension()` 方法获取不含扩展名的文件名

2. **在Listener中设置上下文**
   ```java
   // TestCaseNoModelDataListener.java
   // 初始化时创建上下文
   TemplateHandlerContext handlerContext = new TemplateHandlerContext();
   handlerContext.setProjectId(request.getProjectId());
   templateHandler.setContext(handlerContext);
   
   // 设置Excel文件名
   public void setExcelFileName(String excelFileName) {
       if (templateHandler != null && templateHandler instanceof AbstractTestCaseTemplateHandler) {
           TemplateHandlerContext context = ((AbstractTestCaseTemplateHandler) templateHandler).context;
           if (context != null) {
               context.setExcelFileName(excelFileName);
           }
       }
   }
   ```

3. **在业务代码中设置文件名**
   ```java
   // TestCaseService.java
   TestCaseNoModelDataListener easyExcelListener = new TestCaseNoModelDataListener(request, clazz, mergeInfoSet);
   if (multipartFile.getOriginalFilename() != null) {
       easyExcelListener.setExcelFileName(multipartFile.getOriginalFilename());
   }
   ```

4. **在模板2处理器中实现特定逻辑**
   ```java
   // TestCaseTemplate2Handler.java
   @Override
   protected void doProcessData(TestCaseExcelData data) {
       if (context != null && context.getExcelFileName() != null) {
           String nodePath = data.getNodePath();
           String fileName = context.getExcelFileNameWithoutExtension();
           if (StringUtils.isNotBlank(fileName)) {
               if (StringUtils.isNotBlank(nodePath)) {
                   // 规范化路径
                   String normalizedPath = nodePath.startsWith("/") ? nodePath : "/" + nodePath;
                   // 检查路径的第一段是否已经是文件名
                   String firstSegment = normalizedPath.substring(1);
                   int firstSlashIndex = firstSegment.indexOf('/');
                   if (firstSlashIndex > 0) {
                       firstSegment = firstSegment.substring(0, firstSlashIndex);
                   }
                   // 如果第一段不是文件名，则在前面加上文件名
                   if (!fileName.equals(firstSegment)) {
                       data.setNodePath("/" + fileName + normalizedPath);
                   }
               } else {
                   // 如果所属模块为空，则设置为文件名
                   data.setNodePath("/" + fileName);
               }
           }
       }
   }
   ```

**处理示例：**
- Excel文件名：`测试用例.xlsx`
- 所属模块为空 → 设置为 `/测试用例`
- 所属模块为 `/模块1` → 设置为 `/测试用例/模块1`
- 所属模块为 `/测试用例/模块1` → 保持不变（避免重复添加）

**优势：**
- ✅ 模板特定逻辑完全封装在模板处理器中
- ✅ 业务代码只需设置文件名，无需关心具体处理逻辑
- ✅ 其他模板不受影响，只有模板2会执行该逻辑
- ✅ 易于扩展：其他模板也可以使用上下文实现特定功能

## 十、总结

模板驱动设计通过将模板规则封装在独立的处理器类中，实现了：

✅ **代码清晰**：业务代码简洁，职责分明  
✅ **易于扩展**：添加新模板只需创建新处理器类  
✅ **易于维护**：修改模板规则只需修改对应处理器  
✅ **减少重复**：公共逻辑提取到基类  
✅ **提高可测性**：每个处理器可以独立测试  
✅ **配置驱动**：规则集中在处理器类中  
✅ **上下文支持**：通过上下文机制支持模板特定的复杂需求  

这种设计模式特别适合需要支持多种模板格式的场景，是**策略模式**和**模板方法模式**的完美结合。

