<template>
  <div class="split-view">
    <!-- 左侧列表 -->
    <div class="item-list">
      <div
        v-for="item in data"
        :key="item.id"
        class="item"
        :class="{ active: selectedItem && selectedItem.id === item.id }"
        @click="handleItemClick(item)"
      >
        <div class="item-title">{{ item.name || item.title }}</div>
        <div class="item-meta">
          <span>{{ item.num }}</span>
          <span>{{ item.createUser }}</span>
          <span>{{ formatDate(item.createTime) }}</span>
        </div>
      </div>
      
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          small
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    
    <!-- 右侧详情 -->
    <div class="detail-panel">
      <detail-panel 
        v-if="selectedItem"
        :item="selectedItem"
      />
      <div v-else class="empty-detail">
        <i class="el-icon-document"></i>
        <span>{{ $t('advanced_search.select_item_to_view') }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';
import DetailPanel from './DetailPanel.vue';

export default {
  name: 'SplitView',
  components: {
    DetailPanel
  },
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
  emits: ['page-change', 'item-select'],
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
    selectedItem() {
      return this.store.selectedItem;
    }
  },
  watch: {
    data: {
      handler(newData) {
        // 当数据变化时，自动选中第一条
        if (newData && newData.length > 0 && !this.selectedItem) {
          this.handleItemClick(newData[0]);
        }
      },
      immediate: true
    }
  },
  methods: {
    handleItemClick(item) {
      this.$emit('item-select', item);
    },
    
    handleCurrentChange(page) {
      this.currentPage = page;
      this.$emit('page-change', page, this.pageSize);
    },
    
    formatDate(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      return date.toLocaleDateString();
    }
  }
};
</script>

<style scoped>
.split-view {
  display: flex;
  height: 100%;
}

.item-list {
  width: 300px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.item:hover {
  background-color: #f5f7fa;
}

.item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.item-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 12px;
}

.pagination-container {
  padding: 16px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #e4e7ed;
}

.detail-panel {
  flex: 1;
  overflow-y: auto;
}

.empty-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #c0c4cc;
}

.empty-detail i {
  font-size: 64px;
  margin-bottom: 16px;
}
</style>
