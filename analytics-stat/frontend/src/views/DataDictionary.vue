<template>
  <div class="data-dictionary">
    <el-card>
      <template #header>
        <div class="header">
          <span>数据字典</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">
            新增字典
          </el-button>
        </div>
      </template>

      <el-table
        :data="dictionaryList"
        border
        style="width: 100%"
      >
        <el-table-column prop="dictType" label="字典类型" width="150" />
        <el-table-column prop="dictCode" label="字典编码" width="150" />
        <el-table-column prop="dictLabel" label="字典标签" width="150" />
        <el-table-column prop="dictValue" label="字典值" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="dictionaryList.length === 0" description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface Dictionary {
  id: string
  dictType: string
  dictCode: string
  dictLabel: string
  dictValue: string
  sortOrder: number
  enabled: boolean
}

const dictionaryList = ref<Dictionary[]>([])

const handleAdd = () => {
  ElMessage.info('新增功能开发中...')
}

const handleEdit = (_row: Dictionary) => {
  ElMessage.info('编辑功能开发中...')
}

const handleDelete = (_row: Dictionary) => {
  ElMessage.info('删除功能开发中...')
}
</script>

<style scoped>
.data-dictionary {
  width: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
