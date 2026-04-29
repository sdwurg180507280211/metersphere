<template>
  <ms-container>
    <ms-main-container>
      <el-card class="table-card" v-loading="page.loading">
        <template v-slot:header>
          <ms-table-header
              :condition.sync="page.condition"
              @search="search"
              @create="handleCreateRequirement"
              create-tip="创建需求"
              module-key="REQUIREMENT_POOL_LIST"
          >
          </ms-table-header>
        </template>

        <ms-table
            v-loading="page.loading"
            operator-width="160px"
            row-key="dmpNum"
            :data="page.data"
            :condition="page.condition"
            :total="page.total"
            :page-size.sync="page.pageSize"
            :reserve-option="true"
            :page-refresh="pageRefresh"
            :operators="operators"
            :screen-height="screenHeight"
            :remember-order="true"
            :fields.sync="fields"
            :field-key="tableHeaderKey"
            @handlePageChange="handlePageChange"
            @order="initTableData"
            ref="requirementPoolTable"
            @filter="filter"
            class="requirement-pool-table"
        >
        <span v-for="item in fields" :key="item.key">
          <ms-table-column
              v-if="item.id === 'dmpNum'"
              prop="dmpNum"
              sortable
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'requirementName'"
              prop="requirementName"
              sortable
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'poolStatus'"
              prop="poolStatus"
              :field="item"
              :fields-width="fieldsWidth"
              :filters="item.filters"
              :column-key="item.columnKey"
              :label="item.label"
              :min-width="item.minWidth"
          >
            <template v-slot:default="scope">
              <el-tag :type="getStatusType(scope.row.poolStatus)" size="mini">
                {{ getStatusText(scope.row.poolStatus) }}
              </el-tag>
            </template>
          </ms-table-column>
          <ms-table-column
              v-if="item.id === 'systemName'"
              prop="systemName"
              sortable
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'reqManagerName'"
              prop="reqManagerName"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'reqFatherClass'"
              prop="reqFatherClass"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'reqSonClass'"
              prop="reqSonClass"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'actName'"
              prop="actName"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'parentWfinstCode'"
              prop="parentWfinstCode"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'operationType'"
              prop="operationType"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'assigneeName'"
              prop="assigneeName"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'createdDept'"
              prop="createdDept"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'createUser1'"
              prop="createUser1"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'deptName'"
              prop="deptName"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'startUserName'"
              prop="startUserName"
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          />
          <ms-table-column
              v-if="item.id === 'upTime'"
              prop="upTime"
              sortable
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          >
            <template v-slot:default="scope">
              {{ scope.row.upTime | datetimeFormat }}
            </template>
          </ms-table-column>
          <ms-table-column
              v-if="item.id === 'createTime'"
              prop="createTime"
              sortable
              :field="item"
              :fields-width="fieldsWidth"
              :label="item.label"
              :min-width="item.minWidth"
          >
            <template v-slot:default="scope">
              {{ scope.row.createTime | datetimeFormat }}
            </template>
          </ms-table-column>
        </span>
    </ms-table>

    <ms-table-pagination
      :change-current="handlePageChange"
      :change-size="handlePageSizeChange"
      :current-page.sync="page.currentPage"
      :page-size.sync="page.pageSize"
      :total="page.total"
    />
  </el-card>
  </ms-main-container>

  <test-plan-edit
      ref="testPlanEdit"
      @refresh="initTableData"
  />
  <create-requirement-dialog
      ref="createRequirementDialog"
      @refresh="search"
  />
</ms-container>
</template>

<script>
import {getRequirementPoolList, rollbackTestPlan} from '@/api/requirement-pool';
import {getCustomTableHeader, getCustomTableWidth, getPageInfo, _filter} from "metersphere-frontend/src/utils/tableUtils";
import {REQUIREMENT_POOL_LIST} from "metersphere-frontend/src/components/search/search-components";
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import MsTableHeader from "metersphere-frontend/src/components/MsTableHeader";
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsMainContainer from "metersphere-frontend/src/components/MsMainContainer";
import TestPlanEdit from "@/business/plan/components/TestPlanEdit";
import CreateRequirementDialog from "@/business/requirement-pool/components/CreateRequirementDialog";

export default {
  name: 'RequirementPoolList',
  components: {
    MsTable,
    MsTableColumn,
    MsTableHeader,
    MsTablePagination,
    MsContainer,
    MsMainContainer,
    TestPlanEdit,
    CreateRequirementDialog
  },
  data() {
    return {
      page: getPageInfo({
        components: REQUIREMENT_POOL_LIST,
        custom: false,
      }),
      fields: getCustomTableHeader('REQUIREMENT_POOL_LIST'),
      fieldsWidth: getCustomTableWidth('REQUIREMENT_POOL_LIST'),
      tableHeaderKey: 'REQUIREMENT_POOL_LIST',
      screenHeight: 'calc(100vh - 160px)',
      pageRefresh: false,
      operators: [
        {
          tip: '创建测试计划',
          icon: 'el-icon-circle-plus-outline',
          exec: this.handleCreatePlan,
          isDisable: this.isCreateDisabled,
          permissions: ['PROJECT_TRACK_PLAN:READ+CREATE']
        },
        {
          tip: '回退',
          icon: 'el-icon-refresh-left',
          exec: this.handleRollback,
          isDisable: this.isRollbackDisabled,
          permissions: ['PROJECT_TRACK_PLAN:READ+DELETE']
        }
      ]
    };
  },
  activated() {
    this.page.loading = true;
    this.initTableData();
  },
  methods: {
    handleCreateRequirement() {
      this.$refs.createRequirementDialog.open();
    },
    // 初始化表格数据
    initTableData() {
      this.page.loading = true;
      getRequirementPoolList(this.page.currentPage, this.page.pageSize, this.page.condition).then(response => {
        this.page.loading = false;
        if (response.data) {
          this.page.data = response.data.listObject;
          this.page.total = response.data.itemCount;
        }
      }).catch(error => {
        this.page.loading = false;
        console.error('获取需求池列表失败:', error);
        this.$message.error('获取需求池列表失败');
      });
    },
    // 搜索
    search() {
      this.page.currentPage = 1;
      this.initTableData();
    },
    // 列头筛选
    filter(filters) {
      _filter(filters, this.page.condition);
      this.search();
    },
    // 分页
    handlePageChange(page) {
      this.page.currentPage = page;
      this.initTableData();
    },
    // 每页大小
    handlePageSizeChange(size) {
      this.page.pageSize = size;
      this.page.currentPage = 1;
      this.initTableData();
    },
    // 从需求池创建测试计划
    handleCreatePlan(row) {
      if (row.poolStatus !== 'PENDING') {
        this.$warning('只能为未创建状态的需求创建测试计划');
        return;
      }
      // 打开测试计划创建弹窗，传入需求信息
      this.$refs.testPlanEdit.openFromRequirement({
        dmpNum: row.dmpNum,
        requirementName: row.requirementName
      });
    },
    // 判断创建按钮是否禁用
    isCreateDisabled(row) {
      return row.poolStatus !== 'PENDING';
    },
    // 回退测试计划
    handleRollback(row) {
      if (row.poolStatus !== 'CREATED') {
        this.$warning('只有已创建状态的需求才能回退');
        return;
      }
      this.$confirm('回退将删除关联的测试计划及自动创建的模块节点，需求状态恢复为未创建，是否继续？', '回退确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        rollbackTestPlan(row.dmpNum).then(() => {
          this.$success('回退成功');
          this.initTableData();
        }).catch(() => {
          this.$error('回退失败');
        });
      }).catch(() => {});
    },
    // 判断回退按钮是否禁用
    isRollbackDisabled(row) {
      return row.poolStatus !== 'CREATED';
    },
    // 获取状态类型
    getStatusType(status) {
      switch (status) {
        case 'PENDING':
          return '';
        case 'CREATED':
          return 'success';
        case 'CANCELLED':
          return 'info';
        default:
          return '';
      }
    },
    // 获取状态文本
    getStatusText(status) {
      switch (status) {
        case 'PENDING':
          return '未创建';
        case 'CREATED':
          return '已创建';
        case 'CANCELLED':
          return '已取消';
        default:
          return status;
      }
    }
  }
};
</script>

<style scoped>
.table-card {
  margin: 10px;
}

.requirement-pool-table {
  width: 100%;
}
</style>
