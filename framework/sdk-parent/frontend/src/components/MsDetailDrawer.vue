<template>
  <ms-drawer
    :visible.sync="innerVisible"
    :direction="direction"
    :size="size"
    :mask="mask"
    :mask-closable="maskClosable"
    :title="title"
    :show-full-screen="showFullScreen"
    :is-show-close="isShowClose"
    :show-footer="showFooter"
    @close="handleClose"
  >
    <template #header>
      <div class="ms-detail-drawer-header">
        <div class="ms-detail-drawer-header-left">
          <slot name="titleLeft" :loading="loading" :detail="detail"></slot>
          <slot name="title">
            <span v-if="title" class="ms-detail-drawer-title">{{ title }}</span>
          </slot>
        </div>
        <div class="ms-detail-drawer-header-right">
          <ms-previous-next-button
            v-if="tableData && pagination && pageChange && showPagination"
            :list="tableData"
            :index="detailIndex"
            :page-num="pagination.pageNum || pagination.current || 1"
            :page-size="pagination.pageSize || 10"
            :page-total="pagination.pageTotal || Math.ceil((pagination.total || 0) / (pagination.pageSize || 10))"
            :total="pagination.total || 0"
            :next-page-data="nextPageData"
            :pre-page-data="prePageData"
            @pre="handlePre"
            @next="handleNext"
          />
          <slot name="titleRight" :loading="loading" :detail="detail"></slot>
        </div>
      </div>
    </template>
    <div v-loading="loading" class="ms-detail-drawer-body">
      <slot :loading="loading" :detail="detail"></slot>
    </div>
    <template #footer v-if="showFooter">
      <slot name="footer" :loading="loading" :detail="detail"></slot>
    </template>
  </ms-drawer>
</template>

<script>
import MsDrawer from "./MsDrawer";
import MsPreviousNextButton from "./MsPreviousNextButton";

export default {
  name: "MsDetailDrawer",
  components: {
    MsDrawer,
    MsPreviousNextButton,
  },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    direction: {
      type: String,
      default: "right",
    },
    size: {
      type: Number,
      default: 40,
    },
    mask: {
      type: Boolean,
      default: true,
    },
    maskClosable: {
      type: Boolean,
      default: true,
    },
    title: {
      type: String,
      default: "",
    },
    showFullScreen: {
      type: Boolean,
      default: true,
    },
    isShowClose: {
      type: Boolean,
      default: true,
    },
    showFooter: {
      type: Boolean,
      default: false,
    },
    showPagination: {
      type: Boolean,
      default: true,
    },
    // 详情ID
    detailId: {
      type: String,
      default: "",
    },
    // 详情在列表中的索引
    detailIndex: {
      type: Number,
      default: 0,
    },
    // 表格数据
    tableData: {
      type: Array,
      default() {
        return [];
      },
    },
    // 分页信息
    pagination: {
      type: Object,
      default() {
        return {};
      },
    },
    // 分页变更函数
    pageChange: {
      type: Function,
      default: null,
    },
    // 获取详情的函数
    getDetailFunc: {
      type: Function,
      required: true,
    },
    // 上一页数据
    prePageData: {
      type: Object,
      default: null,
    },
    // 下一页数据
    nextPageData: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      innerVisible: false,
      loading: false,
      detail: {},
      currentDetailId: "",
    };
  },
  watch: {
    visible(val) {
      this.innerVisible = val;
      if (val) {
        this.initDetail();
      }
    },
    innerVisible(val) {
      this.$emit("update:visible", val);
      if (!val) {
        this.detail = {};
        this.currentDetailId = "";
      }
    },
    detailId: {
      immediate: true,
      handler(val) {
        if (val && this.innerVisible) {
          this.initDetail();
        }
      },
    },
  },
  methods: {
    initDetail(id) {
      const targetId = id || this.detailId;
      if (!targetId) {
        return;
      }
      this.currentDetailId = targetId;
      this.loading = true;
      this.getDetailFunc(targetId)
        .then((res) => {
          this.detail = res.data || res;
          this.$emit("loaded", this.detail);
          this.loading = false;
        })
        .catch((error) => {
          console.error("加载详情失败:", error);
          this.loading = false;
          this.$emit("loadError", error);
        });
    },
    handlePre() {
      const currentIndex = this.detailIndex;
      if (currentIndex > 0) {
        // 当前页的前一个
        const prevItem = this.tableData[currentIndex - 1];
        if (prevItem && prevItem.id) {
          this.$emit("pre", prevItem);
          this.initDetail(prevItem.id);
        }
      } else if (this.prePageData) {
        // 上一页的最后一个
        this.$emit("pre", this.prePageData);
        if (this.pageChange) {
          const currentPage = this.pagination.pageNum || this.pagination.current || 1;
          this.pageChange(currentPage - 1).then(() => {
            if (this.tableData && this.tableData.length > 0) {
              const lastItem = this.tableData[this.tableData.length - 1];
              if (lastItem && lastItem.id) {
                this.initDetail(lastItem.id);
              }
            }
          });
        }
      }
    },
    handleNext() {
      const currentIndex = this.detailIndex;
      if (currentIndex < this.tableData.length - 1) {
        // 当前页的下一个
        const nextItem = this.tableData[currentIndex + 1];
        if (nextItem && nextItem.id) {
          this.$emit("next", nextItem);
          this.initDetail(nextItem.id);
        }
      } else if (this.nextPageData) {
        // 下一页的第一个
        this.$emit("next", this.nextPageData);
        if (this.pageChange) {
          const currentPage = this.pagination.pageNum || this.pagination.current || 1;
          this.pageChange(currentPage + 1).then(() => {
            if (this.tableData && this.tableData.length > 0) {
              const firstItem = this.tableData[0];
              if (firstItem && firstItem.id) {
                this.initDetail(firstItem.id);
              }
            }
          });
        }
      }
    },
    handleClose() {
      this.innerVisible = false;
      this.$emit("close");
    },
    // 暴露给父组件的方法
    refresh() {
      this.initDetail();
    },
  },
};
</script>

<style scoped>
.ms-detail-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.ms-detail-drawer-header-left {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.ms-detail-drawer-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ms-detail-drawer-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ms-detail-drawer-body {
  min-height: 200px;
}
</style>

