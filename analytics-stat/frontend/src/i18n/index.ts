/**
 * 国际化配置
 *
 * 技术栈：vue-i18n 9.x（适配 Vue 3）
 *
 * 与 Vue 2 版本的差异：
 * - 使用 createI18n() 替代 new VueI18n()
 * - 不再合并 metersphere-frontend / element-ui / fit2cloud-ui 的翻译
 *   （Element Plus 有内置 i18n，通过 locale 属性配置）
 * - 只保留 analytics 模块自身的翻译
 */
import { createI18n } from 'vue-i18n'
import zhCN from './lang/zh-CN'
import enUS from './lang/en-US'
import zhTW from './lang/zh-TW'

/** 获取当前语言，优先级：localStorage > 浏览器语言 */
function getLanguage(): string {
  let language = localStorage.getItem('language')
  if (!language) {
    language = navigator.language
  }
  return language || 'zh-CN'
}

export const i18n = createI18n({
  locale: getLanguage(),
  fallbackLocale: 'zh-CN',
  // 使用 legacy: false 启用 Composition API 模式
  legacy: false,
  // 关闭缺失翻译的警告（开发阶段可能有未翻译的 key）
  missingWarn: false,
  fallbackWarn: false,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'zh-TW': zhTW,
  },
})
