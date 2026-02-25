<template>
  <div class="data-dictionary">
    <el-card shadow="never">
      <!-- 卡片头部 -->
      <template #header>
        <div class="header">
          <span>{{ t('analytics.data_dictionary') }}</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">
            {{ t('analytics.add_dictionary') }}
          </el-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table :data="dictionaryList" border style="width: 100%">
        <el-table-column prop="dictType" :label="t('analytics.dict_type')" width="150" />
        <el-table-column prop="dictCode" :label="t('analytics.dict_code')" width="150" />
        <el-table-column prop="dictLabel" :label="t('analytics.dict_label')" width="150" />
        <el-table-column prop="dictValue" :label="t('analytics.dict_value')" />
        <el-table-column prop="sortOrder" :label="t('analytics.sort_order')" width="80" />
        <el-table-column :label="t('analytics.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? t('analytics.enabled') : t('analytics.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('commons.operating')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              {{ t('analytics.edit') }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              {{ t('analytics.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="dictionaryList.length === 0" :description="t('analytics.no_data')" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 数据字典页面
 *
 * 功能：
 * 1. 展示数据字典列表
 * 2. 支持新增、编辑、删除操作（TODO: 接入后端 API）
 */
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const { t } = useI18n()

/** 字典项类型 */
interface DictionaryItem {
  id: string
  dictType: string
  dictCode: string
  dictLabel: string
  dictValue: string
  sortOrder: number
  enabled: boolean
}

/** 字典列表数据 */
const dictionaryList = ref<DictionaryItem[]>([])

/** 新增字典 */
function handleAdd() {
  ElMessage.info(t('analytics.feature_in_development'))
}

/** 编辑字典 */
function handleEdit(row: DictionaryItem) {
  console.log('编辑:', row)
  ElMessage.info(t('analytics.feature_in_development'))
}

/** 删除字典 */
function handleDelete(row: DictionaryItem) {
  console.log('删除:', row)
  ElMessage.info(t('analytics.feature_in_development'))
}
</script>

<style scoped>
.data-dictionary {
  width: 100%;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
