<template>
  <div style="float: right;">
    <ms-table-search-bar
      v-if="showBaseSearch"
      :condition.sync="condition"
      :style="{width: baseSearchWidth + 'px'}"
      :tip="baseSearchTip"
      @change="baseSearch"/>
    <ms-table-adv-search-bar
      :show-link="showAdvSearchLink"
      :condition.sync="condition"
      :module-key="moduleKey"
      :project-id="projectId"
      @search="advancedSearch"
      class="ms-adv-search"
      ref="advSearch"/>
  </div>
</template>

<script>
import MsTableSearchBar from "../MsTableSearchBar";
import MsTableAdvSearchBar from "./MsTableAdvSearchBar";

export default {
  name: "MsSearch",
  components: {
    MsTableSearchBar,
    MsTableAdvSearchBar
  },
  props: {
    condition: {
      type: Object,
      default() {
        return {};
      }
    },
    baseSearchTip: {
      type: String,
      default() {
        return this.$t('commons.search_by_name_or_id');
      }
    },
    showBaseSearch: {
      type: Boolean,
      default() {
        return true;
      }
    },
    baseSearchWidth: {
      type: Number,
      default() {
        return 240;
      }
    },
    // 模块标识符，透传给 MsTableAdvSearchBar 启用搜索记忆功能
    moduleKey: {
      type: String,
      default: '',
    },
    // 项目ID，透传给 MsTableAdvSearchBar 实现项目隔离
    projectId: {
      type: String,
      default: '',
    },
  },
  computed: {
    showAdvSearchLink() {
      return this.condition.components !== undefined && this.condition.components.length > 0;
    },
  },
  methods: {
    baseSearch() {
      this.$emit("search", {source: "base"});
    },
    advancedSearch(condition) {
      this.$emit("search", {
        source: "advanced",
        action: condition === undefined ? "reset" : "search"
      });
    },
    resetAdvSearch() {
      if (this.$refs.advSearch) {
        this.$refs.advSearch.reset();
      }
    }
  }
}
</script>

<style scoped>
.ms-adv-search {
  margin-left: 5px;
  margin-right: 5px;
}
</style>
