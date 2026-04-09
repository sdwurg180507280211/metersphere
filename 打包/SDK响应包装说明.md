# SDK 响应自动包装说明

## 机制

MeterSphere SDK 的 `ResultResponseBodyAdvice` 会自动将所有 Controller 返回值包装为统一格式。

## 响应格式

```json
{
  "success": true,
  "message": null,
  "data": <Controller返回的数据>
}
```

## 后端实现

直接返回业务数据即可，无需手动包装：

```java
@GetMapping("/models")
public List<Map<String, String>> listModels() {
    return llmClient.listModels();
}
```

## 前端处理

访问 `response.data.data` 获取实际数据：

```typescript
const response = await knowledgeHttp.get<ApiResponse<ModelInfo[]>>('/knowledge/chat/models')
return response.data.data
```
