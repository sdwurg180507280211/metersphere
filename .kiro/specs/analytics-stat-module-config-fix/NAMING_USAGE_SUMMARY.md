# 统计分析服务命名使用情况总结

## 一、当前使用情况

| 配置项 | 当前值 | 原因 |
|-------|-------|------|
| Spring服务名 | `analytics-stat` | Eureka注册标识,不是模块key |
| SessionFilter | `/analytics-stat` | URL路径前缀,不是模块key |
| WebMvcConfig | `/analytics-stat/**` | 静态资源映射路径 |
| 前端路由path | `/analytics-stat` | URL路径,不是模块key |
| 菜单index | `/analytics-stat` | 路由路径,不是模块key |
| 图标名称 | `analytics-stat` | SVG文件名,不是模块key |
| 内部路由 | `/analytics-stat/xxx` | 页面访问路径 |
| 健康检查 | `analytics-stat` | 服务标识 |
| **数据库key** | `analyticsStat` | **模块配置key(驼峰)** |

---

## 二、核心概念区分

### 1. 模块key (本次修改) → `analyticsStat` (驼峰)
- 用途: 数据库配置,控制模块启用/禁用
- 位置: `system_parameter.param_key`

### 2. 服务名 (不修改) → `analytics-stat` (短横线)
- 用途: Eureka注册,服务发现
- 位置: `spring.application.name`

### 3. 路由路径 (不修改) → `/analytics-stat` (短横线)
- 用途: URL访问,前端路由
- 位置: Vue Router配置

### 4. 图标名称 (不修改) → `analytics-stat` (短横线)
- 用途: SVG图标文件名
- 位置: `iconClass`

---

## 三、其他模块对比

| 模块 | 服务名 | 数据库key | 路由路径 | 规律 |
|-----|-------|----------|---------|------|
| report-stat | report | report | /report | 简化命名 |
| api-test | api | api | /api | 简化命名 |
| test-track | track | track | /track | 简化命名 |
| system-setting | setting | setting | /setting | 简化命名 |
| project-management | project | project | /project | 简化命名 |
| performance-test | performance | performance | /performance | 简化命名 |
| workstation | workstation | workstation | /workstation | 完整命名 |
| **analytics-stat** | **analytics-stat** | **analyticsStat** | **/analytics-stat** | **不一致!** |

---

## 四、问题根源

```javascript
// check() 方法需要两个条件都满足:
check(key) {
  // key = "analytics-stat"
  return this.modules[key] === 'ENABLE'  // modules["analytics-stat"] = undefined ❌
      && microApps[key];                  // microApps["analytics-stat"] = true ✅
}

// 实际数据:
sessionStorage.micro_apps = {"analytics-stat": true}      // ← 短横线
localStorage.modules = {"analyticsStat": "ENABLE"}        // ← 驼峰

// 结果: "analytics-stat" !== "analyticsStat" → 菜单不显示
```

---

## 五、推荐方案: 简化服务名为 `analytics`

| 对比项 | 当前 | 推荐 |
|-------|------|------|
| 服务名 | analytics-stat | **analytics** |
| 数据库key | analyticsStat | **analytics** |
| 路由路径 | /analytics-stat | **/analytics** |
| Eureka | ANALYTICS-STAT | **ANALYTICS** |

### 优势
1. 与其他模块保持一致 (report, api, track)
2. 避免短横线和驼峰的转换问题
3. 简洁明了,易于维护

---

## 六、修改清单

| 类别 | 文件路径 | 修改内容 |
|-----|---------|---------|
| **后端** | `analytics-stat/backend/src/main/resources/application.properties` | `spring.application.name=analytics` |
| **后端** | `analytics-stat/backend/src/main/resources/db/migration/V1__add_analytics_stat_module.sql` | `metersphere.module.analytics` |
| **后端** | `analytics-stat/backend/src/main/java/io/metersphere/analyticsstat/config/WebMvcConfig.java` | `/analytics/**` |
| **后端** | `system-setting/backend/src/main/java/io/metersphere/service/ModuleService.java` | `case "analytics"` |
| **Gateway** | `framework/gateway/src/main/resources/application.properties` | `name=analytics` |
| **Gateway** | `framework/gateway/src/main/java/io/metersphere/gateway/filter/SessionFilter.java` | `/analytics` |
| **主框架** | `system-setting/frontend/src/business/system/setting/MsModule.vue` | `analytics: "ENABLE"` |
| **主框架** | `framework/sdk-parent/frontend/src/components/layout/AsideMenus.vue` | `check('analytics')`, `index="/analytics"` |
| **模块前端** | `analytics-stat/frontend/src/router/modules/analytics.js` | `path: "/analytics"` |
| **模块前端** | `analytics-stat/frontend/src/router/index.js` | `redirect: "/analytics/home"` |
| **模块前端** | `analytics-stat/frontend/src/business/head/AnalyticsStatHeaderMenus.vue` | `/analytics/*` |
| **模块前端** | `analytics-stat/frontend/src/business/AnalyticsStatMenu.vue` | `/analytics/*` |
| **模块前端** | `analytics-stat/frontend/src/business/home/AnalyticsStatHome.vue` | `/analytics/*` |
| **数据库** | SQL | `UPDATE system_parameter SET param_key='metersphere.module.analytics' WHERE param_key='metersphere.module.analyticsStat'` |

---

## 七、验证清单

| 验证项 | 预期结果 |
|-------|---------|
| Eureka服务名 | `ANALYTICS` |
| `/services` 接口 | `{"serviceId": "analytics", "port": 8009}` |
| sessionStorage | `{"analytics": true}` |
| localStorage | `{"analytics": "ENABLE"}` |
| check("analytics") | `true && true = true` ✅ |
| 左侧菜单 | 显示"分析统计" |
| 路由跳转 | `/#/analytics` 正常加载 |
