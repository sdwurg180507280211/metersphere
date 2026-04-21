<template>
  <div class="list-view">
    <el-table
      :data="data"
      v-loading="loading"
      height="100%"
      stripe
      @row-click="handleRowClick"
    >
      <el-table-column type="index" width="50" />
      
      <el-table-column
        v-for="column in visibleColumns"
        :key="column.field"
        :prop="column.field"
        :label="column.label"
        :min-width="column.width || 120"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span v-if="column.type === 'status'">
            <el-tag :type="getStatusType(row[column.field])">
              {{ row[column.field] }}
            </el-tag>
          </span>
          <span v-else>
            {{ row[column.field] }}
          </span>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';

export default {
  name: 'ListView',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    total: {
      type: Number,
      default: 0
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['page-change'],
  setup() {
    const store = useAdvancedSearchStore();
    return { store };
  },
  data() {
    return {
      currentPage: 1,
      pageSize: 20
    };
  },
  computed: {
    visibleColumns() {
      const selectedFields = this.store.currentColumnConfig;
      const allFields = [
        ...this.store.fieldMetadata.systemFields,
        ...this.store.fieldMetadata.customFields
      ];
      
      return allFields.filter(f => selectedFields.includes(f.field));
    }
  },
  methods: {
    handleRowClick(row) {
      // 高亮选中行
      this.$emit('row-click', row);
    },
    
    handleSizeChange(size) {
      this.pageSize = size;
      this.$emit('page-change', this.currentPage, size);
    },
    
    handleCurrentChange(page) {
      this.currentPage = page;
      this.$emit('page-change', page, this.pageSize);
    },
    
    getStatusType(status) {
      const statusMap = {
        'Pass': 'success',
        'Failure': 'danger',
        'Prepare': 'info',
        'Underway': 'warning'
      };
      return statusMap[status] || 'info';
    }
  }
};
</script>

<style scoped>
.list-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.pagination-container {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #e4e7ed;
}
</style>
