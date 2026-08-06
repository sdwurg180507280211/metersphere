# 接口自动化 Python(2.7) / JavaScript(ES5) 脚本速查（MeterSphere v2.10-lts）

> 适用：接口自动化场景「脚本」步骤选 **python** / **javascript** 语言时。
> - python 实际是 **Jython 2.7.0**（Python 2 语法，**不是 3.x**）
> - javascript 实际是 **Rhino 1.7.14**（ECMAScript 5.1，**不是 ES6+**）
> 两者都跑在 Java 17 JVM 内，运行时由 JMeter `ScriptEngineManager` 加载引擎。
>
> 注入变量 `vars` / `log` / `prev` / `ctx` / `props` / `sampler` / `OUT` 与
> 《接口自动化脚本速查.md》（Groovy+BeanShell）**完全一致**，本笔记只讲这两门语言自身语法与写法。
> Groovy/BeanShell 写法见同目录《接口自动化脚本速查.md》。

## 一、Python（Jython 2.7）

### 语法要点
- Python 2.7 语法：**缩进**决定代码块（没有 `{}`），行尾不用分号。
- 打印：`print "xxx"`（Python2 语句写法），`print("xxx")` 也能用。
- 注释放 `#`；字符串无 f-string（`"{}".format(x)` 可用，`f"..."` 不行）。
- 集合：`[1,2,3]` 列表、`{k:v}` 字典、`(1,2)` 元组——都原生支持（比 BeanShell 强）。
- 遍历：`for x in lst:`、`for k in d:`；有 `lambda x: x*2`（仅单行表达式）。
- 导入 Java 类：`from java.security import MessageDigest`、`import java.util.ArrayList as AL`。
- 调 Java API：Jython 自动在 Python 与 Java 对象间转换（如 `vars.put("k", py_str)` 可直接传）。

### 常用写法
```python
# 1. 读写变量
token = vars.get("token")
vars.put("newVar", "value")
vars.putObject("user", userObj)    # 对象仍用 putObject

# 2. 解析 JSON（Jython 自带 json 标准库）
import json
resp = prev.getResponseDataAsString()
data = json.loads(resp)
id = data["data"]["id"]
vars.put("id", str(id))
print "解析到 id = " + str(id)

# 3. 断言（抛异常 = 步骤失败）
if prev.getResponseCode() != "200":
    raise Exception("响应码非200: " + prev.getResponseCode())

# 4. MD5 签名（Python hashlib，或走 Java API）
import hashlib
m = hashlib.md5()
m.update(("hello" + token).encode("utf-8"))
vars.put("sign", m.hexdigest())

# 走 Java MessageDigest（更可控，返回 Java byte[]）：
from java.security import MessageDigest
b = MessageDigest.getInstance("MD5").digest(("hello" + token).encode("utf-8"))
# b 是 Java byte[]，需手动转十六进制（略）

# 5. 文件读写（Python open）
with open("/opt/metersphere/data/tmp/in.txt", "r") as f:
    text = f.read()
print text

# 6. 循环
for i in range(3):
    print i
lst = ["a", "b", "c"]
for x in lst:
    print x
```
> ⚠️ Jython 2.7 = Python 2：没有 f-string、没有强制 `print()` 函数式、没有 Python3 的 unicode 默认。写法按 Python 2 风格。

## 二、JavaScript（Rhino / ES5.1）

### 语法要点
- **ES5 语法**：用 `var` 声明（**没有 `let`/`const`**），函数用 `function`，**没有箭头函数 `=>`、没有 class、没有模板字符串 `` ` ``、没有解构**。
- 打印：`print("xxx")`（Rhino 内置）或用注入的 `log.info("...")`。
- 访问 Java 类：通过 `java` 顶层包，如 `java.security.MessageDigest.getInstance("MD5")`；也可用 `importPackage(java.security)`。
- `JSON` 对象属于 ES5，**`JSON.parse()` / `JSON.stringify()` 可用**。
- 遍历：`for (var i=0;...)`、`for (var k in obj)`、`arr.forEach`（ES5 有 forEach）。

### 常用写法
```javascript
// 1. 读写变量
var token = vars.get("token");
vars.put("newVar", "value");
vars.putObject("user", userObj);    // 对象用 putObject

// 2. 解析 JSON（ES5 自带 JSON）
var resp = prev.getResponseDataAsString();
var data = JSON.parse(resp);
var id = data.data.id;
vars.put("id", String(id));
print("解析到 id = " + id);

// 3. 断言（抛异常 = 步骤失败）
if (prev.getResponseCode() != "200") {
    throw new Error("响应码非200: " + prev.getResponseCode());
}

// 4. MD5 签名（ES5 无内置 crypto，走 Java API）
var md = java.security.MessageDigest.getInstance("MD5");
md.update(("hello" + token).getBytes("UTF-8"));
var bytes = md.digest();
var sb = "";
for (var i = 0; i < bytes.length; i++) {
    var h = (bytes[i] & 0xff).toString(16);
    sb += (h.length == 1 ? "0" : "") + h;
}
vars.put("sign", sb);

// 5. 文件读写（用 Java NIO，经 java 包）
var Files = java.nio.file.Files;
var Paths = java.nio.file.Paths;
var charset = java.nio.charset.StandardCharsets.UTF_8;
var text = Files.readString(Paths.get("/opt/metersphere/data/tmp/in.txt"), charset);
print(text);

// 6. 循环
var arr = ["a", "b", "c"];
for (var i = 0; i < arr.length; i++) { print(arr[i]); }
arr.forEach(function(x){ print(x); });
```
> ⚠️ javascript 选项 = Rhino ES5.1：**不要写 `let`/`const`/箭头函数/模板字符串/class**，会报语法错误。需要 hash/crypto 时走 `java.*` 包。

## 三、四语言速选建议
- 新脚本优先 **Groovy**（语法糖多、可编译加速、最常用）。
- **BeanShell**：历史 / 简单取值。
- **Python**：仅当你熟悉 Python 2 且逻辑偏数据处理；注意是 2.7 不是 3。
- **JavaScript**：仅当你熟悉 JS 且逻辑简单；注意是 ES5 不是 ES6。

> 四语言版本汇总（前端写死 `Jsr233ProcessorContent.vue:127`）：
> beanshell 2.0b6 / groovy 3.0.11 / python(Jython) 2.7.0 / javascript(Rhino) 1.7.14(ES5.1)。
> GraalVM JS 在 pom 声明(22.3.1)但未引入依赖，实际未用。
