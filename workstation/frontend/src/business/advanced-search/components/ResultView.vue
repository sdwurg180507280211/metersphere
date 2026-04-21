<template>
  <div class="result-view">
    <!-- 列表视图 -->
    <list-view
      v-if="viewMode === 'list'"
      :data="data"
      :total="total"
      :loading="loading"
      @page-change="handlePageChange"
    />
    
    <!-- 分屏详情视图 -->
    <split-view
      v-else
      :data="data"
      :total="total"
      :loading="loading"
      @page-change="handlePageChange"
      @item-select="handleItemSelect"
    />
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';
import ListView from './ListView.vue';
import SplitView from './SplitView.vue';

export default {
  name: 'ResultView',
  components: {
    ListView,
    SplitView
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
  computed: {
    viewMode() {
      return this.store.viewMode;
    }
  },
  methods: {
    handlePageChange(pageNum, pageSize) {
      this.$emit('page-change', pageNum, pageSize);
    },
    
    handleItemSelect(item) {
      this.$emit('item-select', item);
    }
  }
};
</script>

<style scoped>
.result-view {
  flex: 1;
  background-color: #fff;
  border-radius: 4px;
  overflow: hidden;
}
</style>
