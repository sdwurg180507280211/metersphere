/**
 * 国际化配置
 * 
 * 技术栈：vue-i18n 8.x（适配 Vue 2）
 * 
 * 功能说明：
 * - 支持中文简体、中文繁体、英文三种语言
 * - 自动检测浏览器语言
 * - 支持动态加载语言文件
 * - 集成 Element UI 和 metersphere-frontend 的翻译
 */
import Vue from "vue";
import VueI18n from "vue-i18n";

Vue.use(VueI18n);

// 直接加载的语言文件列表
const LOADED_LANGUAGES = ["zh-CN", "zh-TW", "en-US"];

// 自动加载 lang 目录下的语言文件
const LANG_FILES = require.context("./lang", true, /\.js$/);

// 将语言文件转换为 messages 对象
// 原理：require.context 返回一个函数，可以获取匹配的模块
// keys() 返回所有匹配的文件路径，如 './zh-CN.js'
const messages = LANG_FILES.keys().reduce((messages, path) => {
  const value = LANG_FILES(path);
  // 从路径中提取语言代码，如 './zh-CN.js' -> 'zh-CN'
  const lang = path.replace(/^\.\/(.*)\.\w+$/, "$1");
  if (LOADED_LANGUAGES.includes(lang)) {
    messages[lang] = value.default;
  }
  return messages;
}, {});

/**
 * 获取当前语言
 * 优先级：localStorage > 浏览器语言
 */
export const getLanguage = () => {
  let language = localStorage.getItem("language");
  if (!language) {
    language = navigator.language || navigator.browserLanguage;
  }
  return language;
};

// 创建 i18n 实例
const i18n = new VueI18n({
  locale: getLanguage(),
  messages,
});

/**
 * 动态导入语言文件
 * 用于按需加载未预加载的语言
 */
const importLanguage = (lang) => {
  if (!LOADED_LANGUAGES.includes(lang)) {
    return import(`./lang/${lang}`).then((response) => {
      i18n.mergeLocaleMessage(lang, response.default);
      LOADED_LANGUAGES.push(lang);
      return Promise.resolve(lang);
    });
  }
  return Promise.resolve(lang);
};

/**
 * 设置语言（内部方法）
 */
const setLang = (lang) => {
  localStorage.setItem("language", lang);
  i18n.locale = lang;
};

/**
 * 设置语言（公开方法）
 * 会自动处理语言代码格式（如 zh_CN -> zh-CN）
 */
export const setLanguage = (lang) => {
  if (lang) {
    lang = lang.replace("_", "-");
  }
  if (i18n.locale !== lang) {
    importLanguage(lang).then(setLang);
  }
};

// 组合翻译，例如 key 为 '请输入{0}'，keys 为 login.username
// 则自动将 keys 翻译并替换到 {0} {1}...
Vue.prototype.$tm = function (key, ...keys) {
  let values = [];
  for (const k of keys) {
    values.push(i18n.t(k));
  }
  return i18n.t(key, values);
};

// 忽略警告，即：不存在 Key 直接返回 Key
Vue.prototype.$tk = function (key) {
  const hasKey = i18n.te(key);
  if (hasKey) {
    return i18n.t(key);
  }
  return key;
};

// 设置当前语言的快捷方法
Vue.prototype.$setLang = setLanguage;

export default i18n;
