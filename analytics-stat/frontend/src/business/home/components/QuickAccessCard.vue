<template>
  <el-card>
    <div slot="header" style="padding: 24px;">
      <span class="table-title">快捷入口</span>
    </div>
    <div style="padding: 0 24px 24px 24px;">
      <el-row :gutter="20">
        <el-col 
          v-for="item in items" 
          :key="item.path" 
          :span="8"
          style="margin-bottom: 16px;">
          <div 
            class="quick-access-item"
            @click="handleClick(item)">
            <i :class="item.icon" style="font-size: 32px; color: #409EFF;"></i>
            <div style="margin-top: 12px; font-size: 16px; font-weight: 500; color: #1f2329;">
              {{ item.name }}
            </div>
            <div style="margin-top: 8px; font-size: 14px; color: #8f959e;">
              {{ item.description }}
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </el-card>
</template>

<script>
/**
 * 快捷入口卡片
 * 
 * 功能：
 * 1. 展示功能入口列表
 * 2. 点击跳转到对应页面
 */
export default {
  name: "QuickAccessCard",
  
  props: {
    // 快捷入口列表
    items: {
      type: Array,
      default: () => []
    }
  },
  
  methods: {
    /**
     * 处理点击事件
     */
    handleClick(item) {
      if (item.path) {
        this.$router.push(item.path);
      }
    }
  }
};
</script>

<style scoped>
.quick-access-item {
  padding: 24px;
  text-align: center;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.quick-access-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
</style>
