/**
 * Pinia Store 配置
 * 
 * 使用 metersphere-frontend 的 user store 模块
 * 提供用户状态管理（登录状态、用户信息等）
 */
import user from "metersphere-frontend/src/store/modules/user";
import { defineStore } from "pinia";

// 创建用户 store
let useUserStore = defineStore(user);

// 导出 store 工厂函数
const useStore = () => ({
  user: useUserStore()
});

export {
  useUserStore,
  useStore as default
};
