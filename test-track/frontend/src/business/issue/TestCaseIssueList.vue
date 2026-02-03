<template>
  <div>
    <el-button type="primary" @click="relateTestCase">{{
      $t("test_track.review_view.relevance_case")
    }}</el-button>

    <ms-table
      v-loading="result.loading"
      :enable-selection="false"
      :operators="operators"
      :data="tableData"
      :screen-height="null"
      @refresh="initTableData"
      @order="initTableData"
      ref="table"
    >
      <ms-table-column :label="$t('commons.id')" prop="num">
        <template v-slot:default="scope">
          <el-link @click="openTestCaseDetail(scope.row)" type="primary">
            <span>{{ scope.row.num }}</span>
          </el-link>
        </template>
      </ms-table-column>

      <ms-table-column :label="$t('commons.name')" prop="name">
      </ms-table-column>

      <ms-table-column :label="$t('test_track.case.priority')" prop="name">
        <template v-slot:default="scope">
          <priority-table-item :value="scope.row.priority" ref="priority" />
        </template>
      </ms-table-column>

      <ms-table-column :label="$t('test_track.case.type')" prop="type">
        <template v-slot:default="scope">
          <type-table-item :value="scope.row.type" />
        </template>
      </ms-table-column>

      <ms-table-column
        :label="$t('commons.tag')"
        prop="tags"
        sortable="custom"
        min-width="120"
        :show-overflow-tooltip="false"
      >
        <template v-slot:default="scope">
          <el-tooltip class="item" effect="dark" placement="top">
            <div v-html="getTagToolTips(scope.row.tags)" slot="content"></div>
            <div class="oneLine">
              <ms-tag
                v-for="(itemName, index) in scope.row.tags"
                :key="index"
                type="success"
                effect="plain"
                :show-tooltip="scope.row.tags && scope.row.tags.length === 1 && itemName.length * 12 <= 100"
                :content="itemName"
                style="margin-left: 0px; margin-right: 2px"
              />
            </div>
          </el-tooltip>
        </template>
      </ms-table-column>

      <ms-table-column :label="$t('test_track.case.module')" prop="nodePath">
      </ms-table-column>

      <ms-table-column
        :label="$t('test_track.plan.plan_project')"
        prop="projectName"
      >
      </ms-table-column>
    </ms-table>

    <test-case-relate-list
      :test-case-contain-ids="testCaseContainIds"
      @refresh="initTableData"
      @save="handleRelate"
      ref="testCaseRelevance"
    />
  </div>
</template>

<script>
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import PriorityTableItem from "@/business/common/tableItems/planview/PriorityTableItem";
import TypeTableItem from "@/business/common/tableItems/planview/TypeTableItem";
import TestCaseRelateList from "@/business/issue/TestCaseRelateList";
import {getTestCaseIssueList} from "@/api/testCase";
import {getUUID} from "metersphere-frontend/src/utils";
import { getCurrentProjectID } from "metersphere-frontend/src/utils/token";
import MsTag from "metersphere-frontend/src/components/MsTag";

export default {
  name: "TestCaseIssueList",
  components: {
    TestCaseRelateList,
    TypeTableItem,
    PriorityTableItem,
    MsTableColumn,
    MsTable,
    MsTag,
  },
  data() {
    return {
      result: {
        loading: false,
      },
      tableData: [],
      deleteIds: new Set(),
      addIds: new Set(),
      testCaseContainIds: new Set(),
      operators: [
        {
          tip: this.$t("commons.delete"),
          icon: "el-icon-delete",
          type: "danger",
          exec: this.handleDelete,
        },
      ],
      cacheAddRows: [], // 缓存关联用例信息
    };
  },
  props: {
    issuesId: String,
  },
  watch: {
    issuesId: {
      handler(val, oldVal) {
        // 当 issuesId 从一个值变为另一个值时（切换不同的缺陷），清空缓存
        // 注意：从 undefined 变为有值时不清空，因为 clear() 已在 open() 中被调用
        if (oldVal && val && oldVal !== val) {
          this.cacheAddRows = [];
          this.addIds.clear();
          this.deleteIds.clear();
        }
        if (val) {
          this.initTableData();
        }
      },
      immediate: true,
    },
  },
  methods: {
    handleDelete(item, index) {
      this.testCaseContainIds.delete(item.id);
      this.tableData.splice(index, 1);
      this.deleteIds.add(item.id);
      // 根据ID从 cacheAddRows 中删除，而不是根据索引（因为顺序可能不一致）
      const cacheIndex = this.cacheAddRows.findIndex(cacheItem => cacheItem.id === item.id);
      if (cacheIndex !== -1) {
        this.cacheAddRows.splice(cacheIndex, 1);
      }
    },
    clear() {
      this.addIds.clear();
      this.deleteIds.clear();
      this.cacheAddRows = []; // 清空缓存，避免新建和编辑缺陷时缓存混乱
    },
    initTableData() {
      this.tableData = [];
      let condition = {
        issuesId: this.issuesId,
      };
      if (this.issuesId) {
        this.result.loading = true;
        getTestCaseIssueList(condition).then((response) => {
          this.tableData = response.data;
          this.tableData.forEach((item) => {
            this.testCaseContainIds.add(item.id);
            // 解析 tags JSON 字符串为数组
            if (item.tags && typeof item.tags === 'string') {
              try {
                item.tags = JSON.parse(item.tags);
              } catch (e) {
                item.tags = [];
              }
            }
          });

          // 合并未保存的关联用例（cacheAddRows）到 tableData 中
          this.mergeUnsavedCases();

          this.$refs.table.reloadTable();
          this.result.loading = false;
        });
      } else {
        // 新建缺陷时（issuesId 为空），直接显示 cacheAddRows 中已关联的用例
        this.mergeUnsavedCases();
        this.$refs.table.reloadTable();
      }
    },
    mergeUnsavedCases() {
      // 合并未保存的关联用例（cacheAddRows）到 tableData 中
      if (this.cacheAddRows && this.cacheAddRows.length > 0) {
        // 获取已保存用例的ID集合，用于去重
        const savedCaseIds = new Set(this.tableData.map(item => item.id));

        // 过滤出未保存且未被删除的关联用例
        const unsavedCases = this.cacheAddRows.filter(caseItem => {
          // 排除已在服务器数据中存在的用例（避免重复）
          // 排除在删除列表中的用例
          return caseItem.id &&
                 !savedCaseIds.has(caseItem.id) &&
                 !this.deleteIds.has(caseItem.id);
        });

        // 将未保存的关联用例合并到 tableData
        if (unsavedCases.length > 0) {
          this.tableData.push(...unsavedCases);
          // 更新 testCaseContainIds，确保这些用例的ID也被记录
          unsavedCases.forEach((item) => {
            if (item.id) {
              this.testCaseContainIds.add(item.id);
            }
          });
        }
      }
    },
    relateTestCase() {
      this.$refs.testCaseRelevance.open();
    },
    handleRelate(selectRows) {
      let selectData = Array.from(selectRows);
      selectRows.forEach((i) => {
        if (i.id) {
          this.testCaseContainIds.add(i.id);
        }
        this.deleteIds.delete(i.id);
        this.addIds.add(i.id);
        // 解析 tags JSON 字符串为数组（从弹窗选择的数据已在 TestCaseRelateList 中解析）
        // 这里做兜底处理
        if (i.tags && typeof i.tags === 'string') {
          try {
            i.tags = JSON.parse(i.tags);
          } catch (e) {
            i.tags = [];
          }
        }
      });
      this.tableData.push(...selectData);
      this.cacheAddRows.push(...selectData);
    },
    openTestCaseDetail(row) {
      // 打开测试用例详情页
      const projectId = row.projectId || getCurrentProjectID();
      let routeUrl = this.$router.resolve({
        path: `/track/case/edit/${row.id}`,
        query: {
          projectId: projectId,
        },
      });
      window.open(routeUrl.href, '_blank');
    },
    getTagToolTips(tags) {
      try {
        let showTips = '';
        if (tags) {
          tags.forEach((item) => {
            showTips += item + ',';
          });
          return showTips.substr(0, showTips.length - 1);
        }
        return '';
      } catch (e) {
        return '';
      }
    },
  },
};
</script>

<style scoped>
.oneLine {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
