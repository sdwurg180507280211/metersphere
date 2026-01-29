<template>
  <el-popover
    placement="right"
    width="500"
    trigger="hover"
    popper-class="issues-popover"
    @show="handlePopoverShow">
    <!-- 懒加载：只有在popover显示时才渲染MsMarkDownText组件 -->
    <ms-mark-down-text 
      v-if="contentLoaded" 
      :prop="prop" 
      :data="data" 
      :disabled="true"/>
    <!-- 未加载时显示占位提示 -->
    <div v-else class="lazy-loading-placeholder">
      <i class="el-icon-loading"></i>
      <span style="margin-left: 8px;">{{ $t('commons.loading') }}</span>
    </div>
    <el-button slot="reference" type="text">{{ $t('test_track.issue.preview') }}</el-button>
  </el-popover>
</template>

<script>
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import MsMarkDownText from "metersphere-frontend/src/components/MsMarkDownText";

export default {
  name: "MsReviewTableItemLazy",
  components: {MsMarkDownText, MsTableColumn},
  props: {
    data: Object,
    prop: String,
  },
  data() {
    return {
      // 控制内容是否已加载的标记
      contentLoaded: false
    }
  },
  methods: {
    /**
     * 我在做：监听popover显示事件，首次显示时才加载内容
     * 目的是：实现懒加载，避免列表渲染时加载所有缺陷描述中的图片
     * 如果不这样做，就无法实现：列表页面会一次性加载所有缺陷的描述图片，导致性能问题
     */
    handlePopoverShow() {
      if (!this.contentLoaded) {
        // 使用nextTick确保DOM更新后再加载内容
        this.$nextTick(() => {
          this.contentLoaded = true;
        });
      }
    }
  }
}
</script>

<style scoped>
.lazy-loading-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #909399;
  font-size: 14px;
}
</style>
