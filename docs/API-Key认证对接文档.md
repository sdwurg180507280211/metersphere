# API Key 认证对接文档

> **版本**：V1.0
> **日期**：2026-06-02
> **适用范围**：测试跟踪模块统计接口 & 后续所有需 API Key 认证的外部调用

---

## 一、认证机制概述

MeterSphere 为外部系统提供基于 **API Key + AES 签名** 的无 Session 认证方式。调用方无需登录获取 Cookie，只需在 HTTP Header 中携带 `accessKey` 和实时生成的 `signature` 即可。

### 认证流程

```
外部系统                              MeterSphere
    │                                     │
    │  1. 生成签名                        │
    │     plainText = accessKey|timestamp  │
    │     signature = AES(secretKey, IV=accessKey)
    │                                     │
    │  2. 发起请求 (Headers)               │
    │  ──────────────────────────────────> │
    │     accessKey: yJHyHfe7aqxRfLkQ     │
    │     signature: base64Encoded        │
    │                                     │
    │                                     │  3. ApiKeyFilter 拦截
    │                                     │  4. ApiKeyHandler.getUser()
    │                                     │     a. 查 UserKey by accessKey
    │                                     │     b. AES/CBC 解密 signature
    │                                     │     c. 校验 accessKey 匹配
    │                                     │     d. 校验 timestamp ±30min
    │                                     │     e. 返回 userId → Shiro 登录
    │                                     │
    │                                     │  5. 权限校验 (PROJECT_TRACK_HOME:READ)
    │                                     │
    │  6. 返回 JSON                        │
    │  <────────────────────────────────── │
```

### 关键类文件

| 文件 | 作用 |
|------|------|
| `framework/sdk-parent/sdk/src/main/java/io/metersphere/security/ApiKeyFilter.java` | Shiro Filter，拦截 API Key 请求 |
| `framework/sdk-parent/sdk/src/main/java/io/metersphere/security/ApiKeyHandler.java` | 签名验证、时间戳校验、用户解析 |
| `framework/sdk-parent/sdk/src/main/java/io/metersphere/commons/utils/CodingUtil.java` | `aesEncrypt` / `aesDecrypt` 工具方法 |
| `framework/sdk-parent/domain/src/main/java/io/metersphere/base/domain/UserKey.java` | API Key 实体：`accessKey` + `secretKey` |
| `framework/gateway/src/main/java/io/metersphere/gateway/filter/LoginFilter.java:62-69` | 网关放行：检测到 accessKey+signature 直接放行 |

---

## 二、API Key 管理

### 2.1 生成 Key

管理员在系统中调用 `GET /user/key/generate` 生成一对 Key：

| 字段 | 长度 | 说明 |
|------|------|------|
| `accessKey` | 16 位 | 随机字符串，公开携带在 Header 中 |
| `secretKey` | 16 位 | 随机字符串，**仅展示一次**，用于签名加密 |

### 2.2 约束

- 每个用户最多 **5 个**有效 API Key
- `secretKey` 生成后**仅展示一次**，不可再次查看
- 支持对单个 Key 执行**启用 / 禁用 / 删除**操作
- `accessKey` 状态为 `ACTIVE` 才可用

### 2.3 数据库存储

```sql
-- user_key 表结构
SELECT id, user_id, access_key, secret_key, status, create_time
FROM user_key
WHERE status = 'ACTIVE';
```

> `secret_key` 以**明文**存储在数据库中，生成时通过 `CodingUtil.secretKey()` 方法随机生成。

---

## 三、签名生成算法

### 3.1 算法参数

| 参数 | 值 |
|------|-----|
| 加密算法 | **AES/CBC/PKCS5Padding** |
| 密钥长度 | 128-bit（16 字节） |
| 密钥 (Key) | `secretKey` |
| 初始向量 (IV) | `accessKey` |
| 明文格式 | `{accessKey}\|{timestamp}`（管道符分隔） |
| 时间戳 | 毫秒值 `System.currentTimeMillis()` |
| 密文编码 | Base64 |

### 3.2 生成步骤

```
1. timestamp = 当前毫秒时间戳
2. plainText = accessKey + "|" + timestamp
3. AES/CBC/PKCS5Padding 加密:
   - Key  = secretKey.getBytes(UTF-8)
   - IV   = accessKey.getBytes(UTF-8)
4. Base64 编码密文 → signature
5. HTTP Header:
   - accessKey: <原始 accessKey>
   - signature: <Base64 字符串>
```

### 3.3 服务端校验逻辑

```java
// ApiKeyHandler.java
public static String getUser(String accessKey, String signature) {
    // 1. 查 UserKey
    UserKey userKey = userKeyService.getUserKey(accessKey);
    if (userKey == null) throw new RuntimeException("invalid accessKey");

    // 2. AES 解密
    String decrypt = CodingUtil.aesDecrypt(signature, userKey.getSecretKey(), accessKey);
    // 解密失败: BadPaddingException → 返回原始 signature → split("|") 长度 < 2 → invalid signature

    // 3. 校验 accessKey 匹配
    String[] arr = decrypt.split("\\|");
    if (!accessKey.equals(arr[0])) throw new RuntimeException("invalid signature");

    // 4. 校验时间戳 ±30 分钟
    long sigTime = Long.parseLong(arr[arr.length - 1]);
    if (Math.abs(System.currentTimeMillis() - sigTime) > 1_800_000) {
        throw new RuntimeException("expired signature");
    }

    // 5. 返回 userId → Shiro 执行登录
    return userKey.getUserId();
}
```

### 3.4 签名有效期

**30 分钟**（`ApiKeyHandler.java:58`）。每次请求前需要**实时生成**新签名。

### 3.5 多语言示例

#### Java

```java
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class ApiKeySigner {
    public static String generate(String accessKey, String secretKey) {
        long timestamp = System.currentTimeMillis();
        String plainText = accessKey + "|" + timestamp;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes("UTF-8"), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(
                accessKey.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

#### Python

```python
import time, base64
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

def generate_signature(access_key: str, secret_key: str) -> str:
    plain_text = f"{access_key}|{int(time.time() * 1000)}"
    cipher = Cipher(
        algorithms.AES(secret_key.encode()),
        modes.CBC(access_key.encode()),
    )
    encryptor = cipher.encryptor()
    block_size = 16
    pad = block_size - len(plain_text) % block_size
    padded = plain_text + chr(pad) * pad
    encrypted = encryptor.update(padded.encode()) + encryptor.finalize()
    return base64.b64encode(encrypted).decode()
```

#### JavaScript / Node.js

```javascript
const crypto = require('crypto');

function generateSignature(accessKey, secretKey) {
    const plainText = `${accessKey}|${Date.now()}`;
    const blockSize = 16;
    const pad = blockSize - (plainText.length % blockSize);
    const padded = plainText + String.fromCharCode(pad).repeat(pad);
    const cipher = crypto.createCipheriv(
        'aes-128-cbc',
        Buffer.from(secretKey, 'utf-8'),
        Buffer.from(accessKey, 'utf-8')
    );
    cipher.setAutoPadding(false);
    return Buffer.concat([
        cipher.update(padded, 'utf-8'),
        cipher.final()
    ]).toString('base64');
}
```

#### Shell 一键生成（调用 Python）

```bash
python3 -c "
import time, base64
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
ak='yJHyHfe7aqxRfLkQ'; sk='HzBftUJz3rlNCinf'
pt=f'{ak}|{int(time.time()*1000)}'
c=Cipher(algorithms.AES(sk.encode()),modes.CBC(ak.encode())).encryptor()
pad=16-len(pt)%16;p=pt+chr(pad)*pad
print(base64.b64encode(c.update(p.encode())+c.finalize()).decode())
"
```

---

## 四、接口列表

所有接口路径前缀 `/track`，需 Header 携带 `accessKey` + `signature`。

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | GET | `/track/count/{projectId}` | 用例数量统计 |
| 2 | GET | `/track/relevance/count/{projectId}` | 关联用例覆盖率 |
| 3 | GET | `/track/bug/count/{projectId}` | 遗留缺陷统计 |
| 4 | GET | `/track/case/bar/{projectId}` | 用例责任人分布 |
| 5 | GET | `/track/failure/case/about/plan/{projectId}/{versionId}/{pageSize}/{goPage}` | 失败用例排行 |

---

## 五、测试环境信息

| 项目 | 值 |
|------|-----|
| 服务地址 | `http://192.168.8.101:8005`（test-track 直连） |
| 数据库 | `192.168.8.101:3306 / metersphere_dev` |
| 测试 AccessKey | `yJHyHfe7aqxRfLkQ` |
| 测试 SecretKey | `HzBftUJz3rlNCinf` |
| 测试项目 ID | `698ab521-d1a4-11f0-a2f8-cead5f5242ae`（默认项目） |
| 签名有效期 | 30 分钟 |

---

## 六、接口调用示例与响应

### 6.1 用例数量统计

```bash
curl -s "http://192.168.8.101:8005/track/count/698ab521-d1a4-11f0-a2f8-cead5f5242ae" \
  -H "accessKey: yJHyHfe7aqxRfLkQ" \
  -H "signature: <实时生成>"
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `allCaseCountNumber` | long | 用例总数 |
| `p0CountNumber` ~ `p3CountNumber` | long | P0~P3 各级别用例数 |
| `p0CountStr` ~ `p3CountStr` | String | 带格式文本（大屏用） |
| `passCount` | long | 评审通过数 |
| `unPassCount` | long | 评审未通过数 |
| `prepareCount` | long | 未评审数 |
| `thisWeekAddedCount` | long | 本周新增用例数 |
| `reviewRage` | String | 评审覆盖率 |
| `reviewPassRage` | String | 评审通过率 |
| `chartData` | Map | P0~P3 饼图数据 |

**实际响应：**

```json
{
    "success": true,
    "data": {
        "allCaseCountNumber": 269,
        "p1CaseCountNumber": 1,
        "p2CaseCountNumber": 268,
        "passCount": 0,
        "unPassCount": 0,
        "prepareCount": 269,
        "reviewRage": "0.0%",
        "reviewPassRage": " 0%",
        "chartData": { "P1": 1, "P2": 268 }
    }
}
```

### 6.2 关联用例覆盖率

```bash
curl -s "http://192.168.8.101:8005/track/relevance/count/698ab521-d1a4-11f0-a2f8-cead5f5242ae" \
  -H "accessKey: yJHyHfe7aqxRfLkQ" \
  -H "signature: <实时生成>"
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `apiCaseCount` | long | API 接口用例关联数 |
| `scenarioCaseCount` | long | 场景用例关联数 |
| `performanceCaseCount` | long | 性能用例关联数 |
| `uiScenarioCaseCount` | long | UI 场景用例关联数 |
| `coverageCount` | long | 已覆盖数 |
| `uncoverageCount` | long | 未覆盖数 |
| `coverageRage` | String | 覆盖率 |
| `chartData` | Map | 各类型关联饼图 |

**实际响应：**

```json
{
    "success": true,
    "data": {
        "apiCaseCount": 0,
        "scenarioCaseCount": 0,
        "performanceCaseCount": 0,
        "coverageCount": 0,
        "uncoverageCount": 269,
        "coverageRage": "0.0%",
        "chartData": {
            "接口用例": 0,
            "场景用例": 0,
            "性能用例": 0
        }
    }
}
```

### 6.3 遗留缺陷统计

```bash
curl -s "http://192.168.8.101:8005/track/bug/count/698ab521-d1a4-11f0-a2f8-cead5f5242ae" \
  -H "accessKey: yJHyHfe7aqxRfLkQ" \
  -H "signature: <实时生成>"
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `bugUnclosedCount` | long | 未关闭缺陷数 |
| `bugTotalCount` | long | 缺陷总数 |
| `caseTotalCount` | long | 用例总数 |
| `unClosedRage` | String | 未关闭率 |
| `chartData` | Map | 各状态缺陷分布 |
| `list` | Array | 各测试计划缺陷详情 |

**实际响应：**

```json
{
    "success": true,
    "data": {
        "bugUnclosedCount": 2,
        "bugTotalCount": 2,
        "caseTotalCount": 0,
        "unClosedRage": "100.0%",
        "list": [],
        "chartData": { "新建": 2 }
    }
}
```

### 6.4 用例责任人分布

```bash
curl -s "http://192.168.8.101:8005/track/case/bar/698ab521-d1a4-11f0-a2f8-cead5f5242ae" \
  -H "accessKey: yJHyHfe7aqxRfLkQ" \
  -H "signature: <实时生成>"
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `xAxis` | String | 人员姓名 |
| `yAxis` | BigDecimal | 用例数量 |
| `groupName` | String | `FUNCTIONCASE` 或 `RELEVANCECASE` |

**实际响应：**

```json
{
    "success": true,
    "data": [
        {
            "xAxis": "test01",
            "yAxis": 1,
            "groupName": "FUNCTIONCASE"
        }
    ]
}
```

### 6.5 失败用例排行

```bash
curl -s "http://192.168.8.101:8005/track/failure/case/about/plan/698ab521-d1a4-11f0-a2f8-cead5f5242ae/default/20/0" \
  -H "accessKey: yJHyHfe7aqxRfLkQ" \
  -H "signature: <实时生成>"
```

**参数说明：**

| 参数 | 说明 |
|------|------|
| `projectId` | 项目 ID |
| `versionId` | 版本 ID，传 `default` 表示不过滤 |
| `pageSize` | 每页条数 |
| `goPage` | 页码，从 0 开始 |

**实际响应：**

```json
{
    "success": true,
    "data": {
        "listObject": [],
        "itemCount": 0,
        "pageCount": 0
    }
}
```

---

## 七、认证测试结果

本次测试使用项目 ID `698ab521-d1a4-11f0-a2f8-cead5f5242ae`（默认项目），对 192.168.8.101:8005 的 test-track 服务进行了完整的 API Key 认证验证。

| 测试场景 | HTTP 状态码 | 说明 |
|---------|------------|------|
| 正确 accessKey + 正确签名 | **200** | 认证通过，返回统计数据 ✅ |
| 正确 accessKey + 错误签名 | **500** | `RuntimeException: invalid signature`（AES 解密失败，BadPaddingException） |
| 错误 accessKey + 任意签名 | **500** | `RuntimeException: invalid accessKey`（数据库查不到） |
| 签名过期（>30分钟） | **401** | `expired signature`（时间戳校验失败） |

### 错误响应示例

**错误签名：**
```html
HTTP 500 - jakarta.servlet.ServletException: 
  java.lang.RuntimeException: invalid signature
  at ApiKeyHandler.getUser(ApiKeyHandler.java:47)
```

**错误 accessKey：**
```html
HTTP 500 - jakarta.servlet.ServletException: 
  java.lang.RuntimeException: invalid accessKey
  at ApiKeyHandler.getUser(ApiKeyHandler.java:37)
```

---

## 八、与全研发（数据大屏）对接规范

### 8.1 调用方需要做的事情

1. 从 MeterSphere 管理员获取 `accessKey` 和 `secretKey`
2. 实现签名生成算法（参考第三章多语言示例）
3. 设置定时轮询（建议 **5-10 分钟**一次）
4. 每次请求前**实时生成新签名**

### 8.2 建议轮询策略

```
每 5~10 分钟执行：
  for each projectId:
    1. 生成新签名
    2. GET /track/count/{projectId}
    3. GET /track/relevance/count/{projectId}
    4. GET /track/bug/count/{projectId}
    5. GET /track/case/bar/{projectId}
    6. GET /track/failure/case/about/plan/{projectId}/default/20/0
    7. 缓存数据 → 渲染大屏
```

### 8.3 注意事项

- **签名绝不能硬编码**：时间戳在变，每次请求都要实时生成
- **签名不能有空格/换行**：Base64 字符串复制时注意不要引入额外字符
- **服务器时间同步**：调用方服务器时间与 MeterSphere 偏差超过 30 分钟会认证失败
- **权限前置**：确认 API Key 所属用户拥有目标项目的 `PROJECT_TRACK_HOME:READ` 权限

---

## 九、错误码汇总

| HTTP 状态码 | 场景 | 关键日志 |
|-------------|------|---------|
| 200 | 正常 | — |
| 500 | `accessKey` 不存在 | `invalid accessKey` at `ApiKeyHandler.java:37` |
| 500 | 签名解密失败 | `invalid signature` at `ApiKeyHandler.java:47`（BadPaddingException → split 长度<2） |
| 500 | 签名中 accessKey 不匹配 | `invalid signature` at `ApiKeyHandler.java:50` |
| 401 | 时间戳超过 30 分钟 | `expired signature` at `ApiKeyHandler.java:60` |
| 404 | 项目不存在 | Spring Boot 默认 404 |

---

## 十、FAQ

**Q: 签名生成后能用多久？**
A: 30 分钟。`ApiKeyHandler.java:58` 校验 `System.currentTimeMillis() - sigTime > 1800000`。

**Q: secretKey 忘记了怎么办？**
A: 无法找回，只能重新生成一对新的 Key，并禁用旧的 accessKey。

**Q: 支持哪些 HTTP 方法？**
A: 签名认证不限制方法。现有 `/track` 统计接口均为 GET，POST/PUT 同样支持。

**Q: 网关层是否需要特殊配置？**
A: 不需要。`LoginFilter.java:62-69` 检测到 `accessKey` + `signature` Header 时直接放行，不校验 Session/Cookie。

**Q: 认证失败最常见的原因？**
A: 1) signature 手工输入导致字符错位 2) 签名过期超过 30 分钟 3) accessKey/secretKey 不匹配。
