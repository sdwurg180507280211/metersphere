# MeterSphere 路由优化分析报告（修订版）

- 修订日期：2026-03-07
- 基线范围：`framework/sdk-parent` 与 8 个微前端子应用（`api/track/performance/project/setting/workstation/report/analytics`）
- 目标：将原报告调整为“可直接执行、可回归验证”的版本

---

## 1. 执行摘要

本次修订后，建议优先处理 4 个已验证问题：

1. `AsideMenus.vue` 中使用 `window.location.href` 做站内跳转，导致整页刷新（P0）。
2. 系统设置存在 `'/setting/project/:type'` 字面量跳转，参数错误（P1）。
3. `Router.prototype.push` 补丁在多个子应用重复，维护成本高（P1）。
4. 主应用路由守卫跨模块切换时重复登录校验，可做“限频 + 并发合并”优化（P1）。

同时明确 3 条不建议立即推进的事项：

1. 不要“一刀切”替换所有 `window.location.href`（第三方认证外跳必须保留）。
2. 不要按原报告直接做 `[request]` chunk 分析优化（当前代码并未使用该模式）。
3. 不要再新增“404 基础设施”作为优先项（仓库已有错误页路由与兜底规则）。

---

## 2. 本次核查到的关键事实（已验证）

1. 主应用是 Vue 2 + Vue Router 3，存在跨模块守卫逻辑：
   - `framework/sdk-parent/frontend/src/router/index.js`
2. 站内菜单中存在 3 处明确的整页跳转：
   - `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue:131`
   - `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue:148`
   - `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue:152`
3. 系统设置存在错误的动态路由字面量跳转：
   - `system-setting/frontend/src/business/SettingHome.vue:73`
   - `system-setting/frontend/src/business/common/config.js:567`
4. `Router.prototype.push` 兼容补丁在多个应用重复出现：
   - `framework/sdk-parent/frontend/src/router/index.js`
   - `api-test/test-track/performance-test/project-management/system-setting/workstation/report-stat` 各自 `src/router/index.js`
5. 项目已存在错误页模块和兜底路由：
   - `framework/sdk-parent/frontend/src/router/modules/error.js`
   - `framework/sdk-parent/frontend/src/router/index.js:68`
6. 项目已存在 micro-app 预加载能力：
   - `framework/sdk-parent/frontend/src/micro-app-setup.js:125`
7. `analytics` 子应用已是 Vue 3 + Router 4：
   - `analytics-stat/frontend/src/router/index.ts`

---

## 3. 已验证问题清单（按优先级）

## 3.1 P0: 站内菜单使用 `window.location.href` 触发整页刷新

**位置**

- `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue:131`
- `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue:148`
- `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue:152`

**影响**

- 破坏 SPA 路由体验，造成不必要刷新。
- 切模块时会放大“白屏感知”。
- 状态恢复路径复杂（依赖 local/sessionStorage）。

**修复建议（可直接改）**

- 仅替换“站内跳转”这 3 处为 `this.$router.push/replace`。
- 保留第三方认证外跳（见第 4 节）。

**参考实现**

```javascript
// AsideMenus.vue
mounted() {
  if (this.$route.matched.length > 0) {
    this.activeIndex = this.$route.matched[0].path;
    if (!this.check(this.$route.matched[0].name.toLowerCase())) {
      this.$router.replace('/');
    }
  }
  this.registerEvents();
},
methods: {
  active() {
    if (this.activeIndex === '/api') {
      this.$router.push('/api/home').catch(() => {});
    }
  },
  activeAnalyticsStat() {
    this.$router.push('/analytics').catch(() => {});
  },
}
```

---

## 3.2 P1: `'/setting/project/:type'` 字面量跳转导致参数错误

**位置**

- `system-setting/frontend/src/business/SettingHome.vue:73`
- `system-setting/frontend/src/business/common/config.js:567`

**影响**

- `:type` 作为普通字符串进入 URL，路由参数值错误（会变成 `:type`）。
- 行为不确定，依赖页面后续容错。

**修复建议（可直接改）**

- 改为真实参数值（例如 `all`），与已有用法保持一致（`/setting/project/all` 在现网代码已有使用）。

**参考实现**

```javascript
// SettingHome.vue
this.$router.push('/setting/project/all');

// common/config.js
url += '/setting/project/all';
```

---

## 3.3 P1: `Router.prototype.push` 补丁重复定义，维护成本高

**位置**

- 主应用 + 多个子应用 `src/router/index.js` 都存在相同补丁。

**影响**

- 每个应用单独维护同一逻辑，容易漂移。
- 后续升级 Vue Router 时风险放大。

**修复建议（渐进）**

- 抽一个共享函数（例如放到 `metersphere-frontend` 公共包），各应用只调用一次。
- 当前不建议“立刻删除补丁”，应先统一实现再替换。

**参考实现**

```javascript
// shared/router/install-safe-push.js
import Router from 'vue-router';

export function installSafeRouterPush() {
  if (Router.prototype.__msSafePushInstalled) return;

  const rawPush = Router.prototype.push;
  Router.prototype.push = function push(location, onResolve, onReject) {
    if (onResolve || onReject) {
      return rawPush.call(this, location, onResolve, onReject);
    }
    return rawPush.call(this, location).catch((err) => {
      if (err && err.name === 'NavigationDuplicated') return err;
      return Promise.reject(err);
    });
  };

  Router.prototype.__msSafePushInstalled = true;
}
```

---

## 3.4 P1: 跨模块切换登录校验可做“限频 + 并发合并”

**位置**

- `framework/sdk-parent/frontend/src/router/index.js:95-103`

**现状**

- 跨模块切换会触发 `store.getIsLogin()`。
- 功能正确，但在频繁切换时请求冗余。

**修复建议（保守）**

- 保留校验语义，不降低安全性。
- 加一个短 TTL（如 30-60 秒）和并发合并，避免重复请求风暴。

**参考实现**

```javascript
let lastLoginCheckAt = 0;
let loginCheckPromise = null;
const LOGIN_CHECK_TTL = 60 * 1000;

async function ensureLogin(store) {
  const now = Date.now();
  if (now - lastLoginCheckAt < LOGIN_CHECK_TTL) return;

  if (!loginCheckPromise) {
    loginCheckPromise = store.getIsLogin()
      .then(() => {
        lastLoginCheckAt = Date.now();
      })
      .finally(() => {
        loginCheckPromise = null;
      });
  }

  await loginCheckPromise;
}
```

---

## 3.5 P2: 路由硬编码较多，建议“增量治理”

**现状**

- 多处直接拼 path（如 `'/track/plan/view/' + id`）。
- 一次性全量迁移风险高。

**修复建议**

- 新代码强制走“路由常量 + 工具函数”。
- 存量代码按热路径优先，随业务改动顺带迁移。
- 先不做全仓大改。

---

## 4. 明确不建议立即执行的事项（修正原报告）

1. 不建议将所有 `window.location.href` 都替换为 `router.push`。

- 以下场景应保留整页跳转：
  - 登录态初始化后回根路径：`framework/sdk-parent/frontend/src/business/login/index.vue:210`
  - OIDC/CAS/OAuth 外跳：`framework/sdk-parent/frontend/src/business/login/index.vue:223,424`
  - 类似外部认证回调跳转（例如 `MxAuth.vue`、二维码登录组件）。

2. 不建议按原文继续推进“`[request]` chunk 过碎”改造。

- 当前仓库路由懒加载并未使用文中假设的 `[request]` 方案作为现状基线。
- 该项应先产出真实 `webpack-bundle-analyzer` 数据再决策。

3. 不建议把“新增 404 基础设施”作为优先任务。

- 已有错误页模块和兜底路由，当前更高优先级是跳转体验与一致性问题。

---

## 5. 可执行落地计划（建议 2 周）

### 阶段 A（1-2 天，必须做）

1. 修复 `AsideMenus.vue` 3 处站内 `window.location.href`。
2. 修复 `'/setting/project/:type'` 字面量跳转（2 处）。
3. 增加回归用例：模块切换、菜单高亮、系统设置跳转。

**验收标准**

- 菜单切换无整页刷新。
- 系统设置进入项目管理页面参数正确（非 `:type`）。

### 阶段 B（3-4 天，建议做）

1. 抽取并统一 `safe router push` 安装函数。
2. 主应用路由守卫加入“限频 + 并发合并”。
3. 增加守卫相关埋点（校验次数、耗时、失败率）。

**验收标准**

- 各应用 `router/index.js` 不再复制大段补丁。
- 高频切换模块时 `getIsLogin()` 请求次数明显下降。

### 阶段 C（持续迭代）

1. 引入路由常量（先覆盖高频业务路径）。
2. 新增 ESLint 规则，限制新增站内 `window.location.href='/#/'` 用法。
3. 存量逐步迁移，避免大批量高风险改动。

**验收标准**

- 新增代码无新的硬编码热路径。
- 热点页面跳转都可追溯到常量/工具函数。

---

## 6. 关键风险与控制

1. 风险：误改认证外跳，导致 SSO 失败。
- 控制：先建立“允许 `window.location.href` 的白名单文件”。

2. 风险：路由守卫缓存导致登录态不同步。
- 控制：TTL 不超过 60 秒；401 拦截后强制失效缓存。

3. 风险：跨应用改造节奏不一致，出现行为分叉。
- 控制：先在主应用试点，完成后模板化推广到子应用。

---

## 7. 建议修改文件清单（按阶段）

### 阶段 A

- `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue`
- `system-setting/frontend/src/business/SettingHome.vue`
- `system-setting/frontend/src/business/common/config.js`

### 阶段 B

- `framework/sdk-parent/frontend/src/router/index.js`
- `*/frontend/src/router/index.js`（替换为共享 `safe push` 安装函数）
- 公共包新增：`metersphere-frontend/src/router/install-safe-push.js`（示例路径）

### 阶段 C

- 路由常量文件（新增）
- 路由工具文件（新增）
- 重点业务组件（按访问量优先）

---

## 8. 本修订版相对原报告的主要变更

1. 删除了不准确前提（如“缺少 404 基础设施”“当前使用 `[request]`”）。
2. 将“替换所有 `window.location.href`”改为“仅替换站内跳转，保留认证外跳”。
3. 修复了示例代码中会直接运行失败的问题（`this.$confirm` 上下文、错误导入等）。
4. 将建议改为可回归的执行计划，并补充了每阶段验收标准。

