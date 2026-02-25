<template>
  <!-- 快捷入口卡片 -->
  <el-card shadow="never">
    <template #header>
      <span class="card-title">{{ t('analytics.quick_access') }}</span>
    </template>
    <el-row :gutter="20">
      <el-col
        v-for="item in items"
        :key="item.path"
        :span="8"
        style="margin-bottom: 16px;"
      >
        <div class="quick-access-item" @click="handleClick(item)">
          <el-icon :size="32" color="#409EFF"><component :is="item.icon" /></el-icon>
          <div class="item-title">{{ t(item.i18nKey) }}</div>
          <div class="item-desc">{{ t(item.descKey) }}</div>
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
/**
 * 快捷入口卡片
 *
 * 功能：
 * 1. 展示功能入口列表（图标 + 标题 + 描述）
 * 2. 点击跳转到对应页面
 */
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { Component } from 'vue'

const router = useRouter()
const { t } = useI18n()

/** 快捷入口项类型 */
export interface QuickAccessItem {
  i18nKey: string
  icon: Component
  path: string
  descKey: string
}

defineProps<{
  items: QuickAccessItem[]
}>()

/** 点击跳转 */
function handleClick(item: QuickAccessItem) {
  if (item.path) {
    router.push(item.path)
  }
}
</script>

<style scoped>
.card-title {
  font-size: 18px;
  font-weight: 500;
  color: #1f2329;
}

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

.item-title {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 500;
  color: #1f2329;
}

.item-desc {
  margin-top: 8px;
  font-size: 14px;
  color: #8f959e;
}
</style>
