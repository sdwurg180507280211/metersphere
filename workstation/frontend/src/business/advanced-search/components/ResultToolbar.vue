<template>
  <div class="result-toolbar">
    <div class="left-section">
      <span class="result-count">
        {{ $t('advanced_search.total_results', { count: total }) }}
      </span>
    </div>
    
    <div class="right-section">
      <!-- 视图模式切换 -->
      <el-radio-group v-model="viewMode" size="small" @change="handleViewModeChange">
        <el-radio-button label="list">
          <i class="el-icon-s-grid"></i>
          {{ $t('advanced_search.list_view') }}
        </el-radio-button>
        <el-radio-button label="split">
          <i class="el-icon-s-unfold"></i>
          {{ $t('advanced_search.split_view') }}
        </el-radio-button>
      </el-radio-group>
      
      <!-- 列配置 -->
      <el-popover
        placement="bottom-end"
        width="300"
        trigger="click"
      >
        <column-config />
        <template #reference>
          <el-button size="small" icon="el-icon-setting">
            {{ $t('advanced_search.column_config') }}
          </el-button>
        </template>
      </el-popover>
      
      <!-- 导出 -->
      <el-button 
        size="small" 
        icon="el-icon-download"
        @click="handleExport"
      >
        {{ $t('advanced_search.export') }}
      </el-button>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';
import ColumnConfig from './ColumnConfig.vue';

export default {
  name: 'ResultToolbar',
  components: {
    ColumnConfig
  },
  props: {
    total: {
      type: Number,
      default: 0
    }
  },
  emits: ['view-mode-change', 'export'],
  setup() {
    const store = useAdvancedSearchStore();
    return { store };
  },
  computed: {
    viewMode: {
      get() {
        return this.store.viewMode;
      },
      set(value) {
        this.store.viewMode = value;
      }
    }
  },
  methods: {
    handleViewModeChange(mode) {
      this.$emit('view-mode-change', mode);
    },
    
    handleExport() {
      this.$emit('export');
    }
  }
};
</script>

<style scoped>
.result-toolbar {
  background-color: #fff;
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left-section {
  display: flex;
  align-items: center;
}

.result-count {
  font-size: 14px;
  color: #606266;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
