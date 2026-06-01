# 设计文档：数据大屏 API 数据对接

## 概述

本文档描述数据大屏外部系统通过 HTTP API 从 MeterSphere 获取测试跟踪统计数据的对接方案。核心依赖现有 `ApiKeyFilter` / `ApiKeyHandler` 认证机制和 `TrackController` 五个统计接口，外部系统通过定时轮询拉取数据，在本地缓存并渲染大屏展示。

V1 阶段不新增任何后端接口，完全复用现有能力。后续需求测试流程模块上线后，再新增 workflow 维度的统计端点。

**文档版本**：V1.0
**修订日期**：2026年6月1日

---

## 核心设计目标

1. **零侵入**：V1 不新增/不修改任何后端代码，仅提供对接规范和示例
2. **认证复用**：直接使用现有 ApiKeyFilter 链路，无需额外开发
3. **接口复用**：复用 TrackController 现有 5 个统计端点
4. **可扩展**：预留 workflow 统计接口设计，后续按相同认证模式接入
5. **文档化**：提供签名算法多语言示例和接口契约

---

## 架构设计

### 总体架构

```
┌─────────────────────┐         HTTPS + API Key           ┌──────────────────────┐
│   数据大屏           │ ────────────────────────────────> │  MeterSphere          │
│   (外部系统)         │   GET /track/count/{projectId}     │                      │
│                     │   GET /track/relevance/count/...   │  ┌────────────────┐  │
│  ┌───────────────┐  │   GET /track/bug/count/...         │  │ ApiKeyFilter    │  │
│  │ 定时轮询器    │  │   GET /track/case/bar/...           │  │ (accessKey +    │  │
│  │ (5-10min)    │  │   GET /track/failure/case/...       │  │  signature)     │  │
│  └───────────────┘  │                                     │  └───────┬────────┘  │
│                     │   Headers:                          │          │           │
│  ┌───────────────┐  │   - accessKey: AK1a2b3c4d5e6f7g   │  ┌───────▼────────┐  │
│  │ 数据缓存层    │  │   - signature: AES(...)             │  │ ApiKeyHandler   │  │
│  └───────────────┘  │                                     │  │ 验证签名+时间戳 │  │
│                     │                                     │  └───────┬────────┘  │
│  ┌───────────────┐  │                                     │          │           │
│  │ 可视化渲染    │  │                                     │  ┌───────▼────────┐  │
│  └───────────────┘  │                                     │  │ TrackController │  │
└─────────────────────┘                                     │  │ 返回统计数据    │  │
                                                            │  └────────────────┘  │
                                                            └──────────────────────┘
```

### 认证流程

```
外部系统                              MeterSphere
    │                                     │
    │  1. 生成签名                        │
    │  plainText = accessKey|timestamp     │
    │  signature = AES(secretKey, plain)   │
    │                                     │
    │  2. 发起请求 (Headers)               │
    │  ──────────────────────────────────> │
    │  accessKey: AKxxx                   │
    │  signature: base64Encoded           │
    │                                     │
    │                                     │  3. ApiKeyFilter 拦截
    │                                     │  4. ApiKeyHandler.getUser()
    │                                     │     a. 查 UserKey by accessKey
    │                                     │     b. AES 解密 signature
    │                                     │     c. 校验 accessKey 匹配
    │                                     │     d. 校验 timestamp ±30min
    │                                     │     e. 返回 userId
    │                                     │
    │                                     │  5. Shiro 执行登录
    │                                     │     (UsernamePasswordToken)
    │                                     │
    │                                     │  6. 权限校验
    │                                     │     (PROJECT_TRACK_HOME:READ)
    │                                     │
    │                                     │  7. 执行业务逻辑
    │                                     │
    │  8. 返回 JSON 数据                   │
    │  <────────────────────────────────── │
    │                                     │
    │  9. 请求结束，ApiKeyFilter.postHandle│
    │     登出 API Key 用户的 Shiro Session│
    │                                     │
```

---

## API Key 管理

### 生成 API Key

相关文件：

| 文件 | 说明 |
|------|------|
| `sdk/src/main/java/io/metersphere/controller/UserKeysController.java` | API Key CRUD 接口 |
| `sdk/src/main/java/io/metersphere/service/UserKeyService.java` | API Key 业务逻辑 |
| `sdk/src/main/java/io/metersphere/security/ApiKeyHandler.java` | 签名验证 |
| `sdk/src/main/java/io/metersphere/security/ApiKeyFilter.java` | Shiro Filter |
| `sdk/src/main/java/io/metersphere/commons/utils/CodingUtil.java` | AES 加解密工具 |

### 操作步骤

1. MeterSphere 管理员或项目管理员登录系统
2. 调用 `GET /user/key/generate` 生成 API Key
3. 系统返回一对：`accessKey`（16 位随机字符串）和 `secretKey`（16 位随机字符串）
4. **secretKey 仅展示一次**，需要立即复制保存
5. 可对 Key 执行启用/禁用/删除操作
6. 每个用户最多 5 个有效 API Key

### UserKey 实体关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(50) | 主键 |
| user_id | VARCHAR(64) | 关联用户 |
| access_key | VARCHAR(16) | 16 位随机字母数字 |
| secret_key | VARCHAR(16) | 16 位随机字母数字 |
| status | VARCHAR(32) | ACTIVE / DISABLED |
| create_time | BIGINT | 创建时间 |

---

## 签名算法

### 算法参数

| 参数 | 说明 |
|------|------|
| 算法 | AES/CBC/PKCS5Padding |
| 密钥 | `secretKey`（16 字节 = 128-bit AES） |
| IV | `accessKey`（16 字节） |
| 明文格式 | `accessKey|timestamp`（管道符分隔，timestamp 为毫秒值） |
| 密文编码 | Base64 |

### 签名生成步骤

```
1. 获取当前毫秒时间戳: timestamp = System.currentTimeMillis()
2. 拼接明文: plainText = accessKey + "|" + timestamp
3. AES 加密:
   - Key = secretKey.getBytes(UTF-8)   // 16 bytes
   - IV  = accessKey.getBytes(UTF-8)   // 16 bytes
   - Cipher = AES/CBC/PKCS5Padding
4. Base64 编码密文得到 signature
5. 设置 HTTP Header:
   - accessKey: accessKey 原始值
   - signature: Base64 字符串
```

### Java 示例

```java
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class ApiKeySigner {

    public static String generateSignature(String accessKey, String secretKey) {
        long timestamp = System.currentTimeMillis();
        String plainText = accessKey + "|" + timestamp;

        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(accessKey.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    public static void main(String[] args) {
        String accessKey = "AK1a2b3c4d5e6f7g";
        String secretKey = "SK9h8g7f6e5d4c3b";

        String signature = generateSignature(accessKey, secretKey);
        System.out.println("accessKey: " + accessKey);
        System.out.println("signature: " + signature);
    }
}
```

### Python 示例

```python
import time
import base64
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

def generate_signature(access_key: str, secret_key: str) -> str:
    timestamp = int(time.time() * 1000)
    plain_text = f"{access_key}|{timestamp}"

    backend = default_backend()
    cipher = Cipher(
        algorithms.AES(secret_key.encode("utf-8")),
        modes.CBC(access_key.encode("utf-8")),
        backend=backend
    )
    encryptor = cipher.encryptor()

    # PKCS5/PKCS7 padding
    block_size = 16
    padding_len = block_size - len(plain_text) % block_size
    padded = plain_text + chr(padding_len) * padding_len

    encrypted = encryptor.update(padded.encode("utf-8")) + encryptor.finalize()
    return base64.b64encode(encrypted).decode("utf-8")

# Usage
access_key = "AK1a2b3c4d5e6f7g"
secret_key = "SK9h8g7f6e5d4c3b"
signature = generate_signature(access_key, secret_key)
print(f"accessKey: {access_key}")
print(f"signature: {signature}")
```

### JavaScript/Node.js 示例

```javascript
const crypto = require('crypto');

function generateSignature(accessKey, secretKey) {
    const timestamp = Date.now();
    const plainText = `${accessKey}|${timestamp}`;

    // PKCS7 padding
    const blockSize = 16;
    const paddingLen = blockSize - (plainText.length % blockSize);
    const padded = plainText + String.fromCharCode(paddingLen).repeat(paddingLen);

    const cipher = crypto.createCipheriv(
        'aes-128-cbc',
        Buffer.from(secretKey, 'utf-8'),
        Buffer.from(accessKey, 'utf-8')
    );
    cipher.setAutoPadding(false);  // we do manual PKCS7 padding

    let encrypted = cipher.update(padded, 'utf-8');
    encrypted = Buffer.concat([encrypted, cipher.final()]);
    return encrypted.toString('base64');
}

// Usage
const accessKey = 'AK1a2b3c4d5e6f7g';
const secretKey = 'SK9h8g7f6e5d4c3b';
const signature = generateSignature(accessKey, secretKey);
console.log('accessKey:', accessKey);
console.log('signature:', signature);
```

### 调用示例（curl）

```bash
ACCESS_KEY="AK1a2b3c4d5e6f7g"
SECRET_KEY="SK9h8g7f6e5d4c3b"

# 用上述任意语言示例生成 signature
SIGNATURE=$(java -jar signer.jar $ACCESS_KEY $SECRET_KEY)

curl -X GET \
  "http://metersphere-host/track/count/project-123" \
  -H "accessKey: $ACCESS_KEY" \
  -H "signature: $SIGNATURE"
```

---

## 接口详细契约

所有接口前缀为 `/track`，需要 header `accessKey` + `signature`。

### 接口 1: GET /track/count/{projectId}

**请求**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| projectId | Path | String | 是 | 项目 ID |

**响应** - `TrackStatisticsDTO`

```json
{
  "success": true,
  "data": {
    "priorityCounts": {
      "P0": 5,
      "P1": 23,
      "P2": 67,
      "P3": 12
    },
    "reviewStatusCounts": {
      "pass": 56,
      "unpass": 8,
      "prepare": 43
    },
    "thisWeekAddedCount": 15,
    "reviewRage": "87.50",
    "reviewPassRage": "52.34",
    "chartData": {}
  }
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| priorityCounts | Map | P0/P1/P2/P3 各级别用例数量 |
| reviewStatusCounts | Map | pass(通过) / unpass(未通过) / prepare(未评审) |
| thisWeekAddedCount | int | 本周新增用例数 |
| reviewRage | String | 评审覆盖率（百分比字符串） |
| reviewPassRage | String | 评审通过率（百分比字符串） |

---

### 接口 2: GET /track/relevance/count/{projectId}

**请求**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| projectId | Path | String | 是 | 项目 ID |

**响应** - `TrackStatisticsDTO`

```json
{
  "success": true,
  "data": {
    "relevanceCounts": {
      "apiCase": 120,
      "scenarioCase": 45,
      "performanceCase": 8,
      "uiScenarioCase": 3
    },
    "coverageCount": 98,
    "uncoverageCount": 49,
    "coverageRage": "66.67",
    "thisWeekAddedCount": 10,
    "chartData": {}
  }
}
```

---

### 接口 3: GET /track/bug/count/{projectId}

**请求**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| projectId | Path | String | 是 | 项目 ID |

**响应** - `BugStatistics`

```json
{
  "success": true,
  "data": {
    "bugUnclosedCount": 34,
    "bugTotalCount": 89,
    "caseTotalCount": 298,
    "newCount": 12,
    "resolvedCount": 45,
    "rejectedCount": 3,
    "unKnownCount": 7,
    "thisWeekCount": 5,
    "unClosedRage": "38.20",
    "bugCaseRage": "29.87",
    "chartData": {
      "open": 20,
      "in_progress": 14,
      "resolved": 45,
      "closed": 10
    },
    "testPlanBugCounts": [
      {
        "index": 1,
        "planName": "Sprint 12 测试计划",
        "createTime": 1717200000000,
        "status": "进行中",
        "caseSize": 85,
        "bugSize": 8,
        "passRage": "92.50",
        "planId": "plan-001"
      }
    ]
  }
}
```

---

### 接口 4: GET /track/case/bar/{projectId}

**请求**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| projectId | Path | String | 是 | 项目 ID |

**响应** - `List<ChartsData>`

```json
{
  "success": true,
  "data": [
    {
      "xAxis": "张三",
      "yAxis": 45.0,
      "groupName": "FUNCTIONCASE",
      "description": null
    },
    {
      "xAxis": "张三",
      "yAxis": 12.0,
      "groupName": "RELEVANCECASE",
      "description": null
    },
    {
      "xAxis": "李四",
      "yAxis": 32.0,
      "groupName": "FUNCTIONCASE",
      "description": null
    }
  ]
}
```

**字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| xAxis | String | 人员名称 |
| yAxis | BigDecimal | 用例数量 |
| groupName | String | FUNCTIONCASE(功能用例) 或 RELEVANCECASE(关联用例) |

---

### 接口 5: GET /track/failure/case/about/plan/{projectId}/{versionId}/{pageSize}/{goPage}

**请求**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| projectId | Path | String | 是 | 项目 ID |
| versionId | Path | String | 是 | 版本 ID，传 `default` 不过滤 |
| pageSize | Path | int | 是 | 每页条数 |
| goPage | Path | int | 是 | 页码，0-based |

**响应** - `Pager<List<ExecutedCaseInfoDTO>>`

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "sortIndex": 1,
        "caseID": "case-001",
        "caseName": "用户登录功能测试",
        "testPlan": "Sprint 12 回归测试",
        "failureTimes": 3,
        "testPlanId": "plan-001",
        "caseType": "功能用例",
        "protocol": "HTTP"
      }
    ],
    "total": 25,
    "pageSize": 20,
    "currentPage": 0
  }
}
```

---

## 错误处理

### 认证错误

| HTTP 状态码 | 场景 | Header 或 Body 提示 |
|-------------|------|---------------------|
| 401 | accessKey 不存在或已禁用 | `AUTHENTICATION_STATUS: invalid` |
| 401 | 签名验证失败 | `AUTHENTICATION_STATUS: invalid` |
| 401 | 时间戳超过 30 分钟 | `AUTHENTICATION_STATUS: invalid` |

### 业务错误

| HTTP 状态码 | 场景 | 响应体 |
|-------------|------|--------|
| 403 | 无项目权限 | `{"success": false, "message": "无权限"}` |
| 404 | 项目不存在 | `{"success": false, "message": "项目未找到"}` |

---

## 网关层

**文件**: `gateway/src/main/java/io/metersphere/gateway/filter/LoginFilter.java:62-69`

网关 `LoginFilter` 自动检测到 `accessKey` + `signature` header 时会直接放行，不校验 Session/Cookie，因此数据大屏请求可以正常穿透网关到达后端服务。

---

## 后续扩展：需求测试流程统计接口

当需求测试流程（`test_workflow_*`）模块上线后，新增以下统计接口：

### 预留路径

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/requirement-flow/statistics/stage-distribution/{projectId}` | 各阶段需求数量分布 |
| GET | `/requirement-flow/statistics/stage-duration/{projectId}` | 各阶段平均耗时 |
| GET | `/requirement-flow/statistics/plan-deviation/{projectId}` | 计划与实际偏差统计 |
| GET | `/requirement-flow/statistics/defect-density/{projectId}` | 缺陷密度（缺陷数/用例数） |
| GET | `/requirement-flow/statistics/review-pass-rate/{projectId}` | 评审通过率趋势 |

### 认证方式

复用相同的 API Key 认证，在 `ShiroConfig` 中为 `/requirement-flow/statistics/**` 路径配置相同的 filter chain。

### 权限

新增权限点 `PROJECT_WORKFLOW_STATISTICS:READ`，在 API Key 所属用户的角色中授予。

---

## 涉及文件清单

### V1 — 无需新建或修改（纯对接文档 + API Key 生成）

| 事项 | 说明 |
|------|------|
| API Key 生成 | 由 MeterSphere 管理员通过现有 `GET /user/key/generate` 操作 |
| 对接文档 | 本 spec 即为对接规范 |

### V2 — 后续 workflow 统计接口

| 文件 | 说明 |
|------|------|
| `test-track/.../requirement/flow/controller/RequirementFlowStatisticsController.java` | 新建 workflow 统计 Controller |
| `test-track/.../requirement/flow/service/RequirementFlowStatisticsService.java` | 新建统计 Service |
| `test-track/.../requirement/flow/dto/WorkflowStatisticsDTO.java` | 新建统计 DTO |
| `ShiroConfig` 或权限配置 | 新增 `PROJECT_WORKFLOW_STATISTICS:READ` 权限点 |

---

## 风险与取舍

| 风险 | 等级 | 处理方式 |
|------|------|----------|
| API Key 泄露 | 中 | secretKey 仅展示一次；支持随时禁用/删除 Key；时间戳 30 分钟窗口 |
| 接口响应时间超标 | 低 | 现有 5 个接口已有查询优化；大屏端可缓存数据，降低轮询频率 |
| 项目数量多导致轮询请求量大 | 中 | 建议大屏端按需轮询活跃项目，或后续提供跨项目聚合接口 |
| 需求测试流程模块未上线 | - | V1 不包含 workflow 统计，待模块上线后通过 V2 spec 补充 |
