<template>
  <div class="column-config">
    <div class="config-header">
      <span>{{ $t('advanced_search.select_columns') }}</span>
      <el-button type="text" size="small" @click="resetDefault">
        {{ $t('advanced_search.reset_default') }}
      </el-button>
    </div>
    
    <div class="column-groups">
      <div 
        v-for="group in columnGroups"
        :key="group.key"
        class="column-group"
      >
        <div class="group-title">{{ $t(group.label) }}</div>
        <el-checkbox-group v-model="selectedColumns">
          <el-checkbox
            v-for="column in group.columns"
            :key="column.field"
            :label="column.field"
          >
            {{ column.label }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';

export default {
  name: 'ColumnConfig',
  setup() {
    const store = useAdvancedSearchStore();
    return { store };
  },
  data() {
    return {
      selectedColumns: [],
      columnGroups: []
    };
  },
  watch: {
    selectedColumns(newVal) {
      this.store.updateColumnConfig(newVal);
    }
  },
  mounted() {
    this.loadColumns();
  },
  methods: {
    loadColumns() {
      // 根据当前模块加载可用列
      const allFields = [
        ...this.store.fieldMetadata.systemFields,
        ...this.store.fieldMetadata.customFields
      ];
      
      // 按分组组织列
      const groups = this.store.fieldMetadata.fieldGroups || [];
      this.columnGroups = groups.map(group => ({
        ...group,
        columns: allFields.filter(f => f.group === group.key)
      }));
      
      // 加载已保存的列配置
      this.selectedColumns = this.store.currentColumnConfig;
      
      // 如果没有配置，使用默认列
      if (this.selectedColumns.length === 0) {
        this.resetDefault();
      }
    },
    
    resetDefault() {
      // 默认显示基础信息组的所有列
      const basicFields = this.store.fieldMetadata.systemFields
        .filter(f => f.group === 'basic')
        .map(f => f.field);
      
      this.selectedColumns = basicFields;
    }
  }
};
</script>

<style scoped>
.column-config {
  max-height: 400px;
  overflow-y: auto;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 12px;
}

.column-groups {
  padding: 8px 0;
}

.column-group {
  margin-bottom: 16px;
}

.group-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 500;
}

.el-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
