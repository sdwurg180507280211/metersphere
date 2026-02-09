<template>
  <test-case-relevance-base
    @setProject="setProject"
    @save="save"
    ref="baseRelevance"
  >
    <template v-slot:aside>
      <ms-node-tree
        class="node-tree"
        v-loading="result.loading"
        @nodeSelectEvent="nodeChange"
        :tree-nodes="treeNodes"
        default-label="未规划用例"
        local-suffix="test_case"
        ref="nodeTree"
      />
    </template>

    <el-card>
      <ms-table-header
        :condition="condition"
        @search="search"
        title=""
        :show-create="false"
      />
      <ms-table
        v-loading="result.loading"
        :data="tableData"
        :condition="condition"
        :total="total"
        :page-size.sync="pageSize"
        :show-select-all="false"
        @handlePageChange="initTableData"
        @refresh="initTableData"
        @order="initTableData"
        ref="table"
      >
        <ms-table-column :label="$t('commons.id')" prop="num" sortable="custom">
        </ms-table-column>

        <ms-table-column :label="$t('commons.name')" prop="name">
        </ms-table-column>

        <ms-table-column :label="$t('test_track.case.priority')" prop="priority" sortable>
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
          min-width="180"
          :show-overflow-tooltip="false"
        >
          <template v-slot:default="scope">
            <el-tooltip class="item" effect="dark" placement="top">
              <div v-html="getTagToolTips(scope.row.tags)" slot="content"></div>
              <div class="oneLine">
                <ms-single-tag
                  v-for="(itemName, index) in parseColumnTag(scope.row.tags)"
                  :key="index"
                  type="success"
                  effect="plain"
                  :show-tooltip="
                    scope.row.tags.length === 1 && itemName.length * 12 <= 100
                  "
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

      <ms-table-pagination
        :change="initTableData"
        :current-page.sync="currentPage"
        :page-size.sync="pageSize"
        :total="total"
      />
    </el-card>
  </test-case-relevance-base>
</template>

<script>
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import { CUSTOM_FIELD_LIST } from "metersphere-frontend/src/utils/default-table-header";
import MsTableButton from "metersphere-frontend/src/components/MsTableButton";
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import MsTableHeader from "metersphere-frontend/src/components/MsTableHeader";
import MsEditDialog from "metersphere-frontend/src/components/MsEditDialog";
import TestCaseRelevanceBase from "@/business/plan/view/comonents/base/TestCaseRelevanceBase";
import MsNodeTree from "metersphere-frontend/src/components/module/MsNodeTree";
import PriorityTableItem from "@/business/common/tableItems/planview/PriorityTableItem";
import TypeTableItem from "@/business/common/tableItems/planview/TypeTableItem";
import MsTag from "metersphere-frontend/src/components/MsTag";
import MsSingleTag from "metersphere-frontend/src/components/new-ui/MsSingleTag";
import { getTagToolTips, parseColumnTag } from "@/business/case/test-case";
import { getTestCaseRelateIssue } from "@/api/testCase";
import { testCaseNodeListProject } from "@/api/test-case-node";

export default {
  name: "TestCaseRelateList",
  components: {
    TypeTableItem,
    PriorityTableItem,
    MsNodeTree,
    TestCaseRelevanceBase,
    MsEditDialog,
    MsTableHeader,
    MsTablePagination,
    MsTableButton,
    MsTableColumn,
    MsTable,
    MsTag,
    MsSingleTag,
  },
  data() {
    return {
      tableData: [],
      condition: {},
      visible: false,
      total: 0,
      pageSize: 10,
      currentPage: 1,
      projectId: "",
      result: {},
      treeNodes: [],
      projects: [],
      selectNodeIds: [],
    };
  },
  props: {
    testCaseContainIds: {
      type: Set,
      default: new Set(),
    },
  },
  watch: {
    selectNodeIds() {
      this.initTableData();
    },
    projectId() {
      this.initTableData();
    },
  },
  computed: {
    fields() {
      return CUSTOM_FIELD_LIST;
    },
  },
  methods: {
    search() {
      this.currentPage = 1;
      this.initTableData();
    },
    initTableData() {
      this.condition.projectId = this.projectId;
      if (this.selectNodeIds && this.selectNodeIds.length > 0) {
        this.condition.nodeIds = this.selectNodeIds;
      } else {
        this.condition.nodeIds = [];
      }
      if (this.projectId) {
        this.getProjectNode();
        this.condition.projectId = this.projectId;
        this.condition.testCaseContainIds = Array.from(this.testCaseContainIds);
        getTestCaseRelateIssue(
          this.currentPage,
          this.pageSize,
          this.condition
        ).then((response) => {
          let data = response.data;
          this.total = data.itemCount;
          this.tableData = data.listObject;
          // 解析 tags JSON 字符串为数组
          this.tableData.forEach((item) => {
            if (item.tags && typeof item.tags === 'string') {
              try {
                item.tags = JSON.parse(item.tags);
              } catch (e) {
                item.tags = [];
              }
            }
          });
        });
      }
    },
    nodeChange(node, nodeIds, nodeNames) {
      this.selectNodeIds = nodeIds;
      this.selectNodeNames = nodeNames;
    },
    getProjectNode(projectId) {
      if (projectId) {
        this.projectId = projectId;
      }
      let data = { projectId: this.projectId };
      testCaseNodeListProject(data).then((response) => {
        this.treeNodes = response.data;
      });
    },
    open() {
      this.$refs.baseRelevance.open();
      this.selectNodeIds = [];
      this.initTableData();
    },
    save() {
      this.$emit("save", this.$refs.table.selectRows);
      this.$refs.table.clear();
      this.$refs.baseRelevance.close();
    },
    setProject(projectId) {
      this.projectId = projectId;
    },
    getTagToolTips(tags) {
      return getTagToolTips(tags);
    },
    parseColumnTag(tags) {
      return parseColumnTag(tags);
    },
  },
};
</script>

<style scoped>
.node-tree {
  height: calc(100% - 50px);
  position: relative;
  overflow: hidden;
}

.oneLine {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
