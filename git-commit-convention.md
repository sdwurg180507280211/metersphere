# Git 提交规范

每次提交前必须读取本文件。普通提交、PR 合并提交和 squash 提交均适用。

## 格式

```text
<type>(<scope>): <subject>
```

scope 可选；省略 scope 时写作 `<type>: <subject>`，不保留空括号。

- type 必填，只允许 `feat`、`fix`、`refactor`、`perf`、`style`、`test`、`ci`、`build`、`chore`、`docs`。
- scope 填写中文模块名，例如：工作台、接口测试、测试跟踪、系统设置。
- subject 必填，使用中文，不超过 50 字，结尾不加句号。SQL、API 等必要技术名词可保留，但不得使用整句英文标题。
- 禁止没有 type 的提交，禁止 `tmp`、`修改` 等无意义信息。
- 不使用 GitHub 默认英文合并标题；合并前显式填写符合规范的标题。
- 不为修正已经发布的提交信息而擅自改写历史或强制推送。

## 示例

```text
feat(工作台): 支持SQL多结果查看与本地筛选
fix(测试跟踪): 修复用例导入跨模块重复误判
ci(工作台): 增加前后端回归校验
```
