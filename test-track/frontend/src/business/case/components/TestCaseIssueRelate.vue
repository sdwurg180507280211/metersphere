<template>
  <div>
    <el-button class="add-btn" v-permission="['PROJECT_TRACK_PLAN:READ+RELEVANCE_OR_CANCEL']" :disabled="readOnly"
               type="primary" size="mini" @click="addIssue">{{ $t('test_track.issue.add_issue') }}
    </el-button>
    <el-button class="add-btn" v-permission="['PROJECT_TRACK_PLAN:READ+RELEVANCE_OR_CANCEL']" :disabled="readOnly"
               type="primary" size="mini" @click="relateIssue">{{ $t('test_track.case.relate_issue') }}
    </el-button>
    <el-tooltip class="item" v-permission="['PROJECT_TRACK_PLAN:READ+RELEVANCE_OR_CANCEL']" effect="dark"
                :content="$t('test_track.issue.platform_tip')"
                placement="right">
      <i class="el-icon-info"/>
    </el-tooltip>

    <ms-table
      v-loading="page.result.loading"
      :show-select-all="false"
      :data="page.data"
      :fields.sync="fields"
      :operators="operators"
      :enable-selection="false"
      ref="table"
      @refresh="getIssues">
      <span v-for="(item) in fields" :key="item.key">
        <ms-table-column
          :label="$t('test_track.issue.id')"
          :field="item"
          prop="id" v-if="false">
        </ms-table-column>
        <ms-table-column
          :field="item"
          :label="$t('test_track.issue.id')"
          prop="num">
        </ms-table-column>

        <ms-table-column
          :field="item"
          :label="$t('test_track.issue.title')"
          prop="title">
        </ms-table-column>

        <issue-platform-status-column  v-if="isThirdPart && item.id === 'platformStatus'" ref="issuePlatformStatus"/>

      <ms-table-column
        v-else
        :field="item"
        :label="$t('test_track.issue.status')"
        prop="status">
        <template v-slot="scope">
          <span>{{ issueStatusMap[scope.row.status] ? issueStatusMap[scope.row.status] : scope.row.status }}</span>
        </template>
      </ms-table-column>

        <span v-for="field in issueTemplate.customFields" :key="field.id">
          <ms-table-column :field="item" :label="field.name" :prop="field.name" v-if="field.name === '状态'">
            <template v-slot="scope">
              <span>
                {{ getIssueStatusDisplay(scope.row, field) }}
              </span>
            </template>

          </ms-table-column>
        </span>

        <ms-table-column
          :field="item"
          :label="$t('test_track.issue.platform')"
          prop="platform">
        </ms-table-column>

        <issue-description-table-item :field="item"/>
      </span>
    </ms-table>

    <test-plan-issue-edit
      :plan-case-id="planCaseId"
      :plan-id="planId"
      :case-id="caseId"
      @refresh="getIssues"
      ref="issueEdit"/>

    <IssueRelateList
      :plan-case-id="planCaseId"
      :case-id="caseId"
      :not-in-ids="notInIds"
      @refresh="getIssues"
      ref="issueRelate"/>
  </div>
</template>

<script>
import TestPlanIssueEdit from "@/business/case/components/TestPlanIssueEdit";
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import IssueDescriptionTableItem from "@/business/issue/IssueDescriptionTableItem";
import {ISSUE_STATUS_MAP} from "metersphere-frontend/src/utils/table-constants";
import IssueRelateList from "@/business/case/components/IssueRelateList";
import {
  closeIssue,
  deleteIssueRelate,
  getIssuePartTemplateWithProject,
  getIssuesByCaseId,
} from "@/api/issue";
import {getCustomFieldValue, getTableHeaderWithCustomFields} from "metersphere-frontend/src/utils/tableUtils";
import {LOCAL} from "metersphere-frontend/src/utils/constants";
import {getCurrentProjectID, getCurrentWorkspaceId} from "metersphere-frontend/src/utils/token";
import {operationConfirm} from "@/business/utils/sdk-utils";
import IssuePlatformStatusColumn from "@/business/issue/IssuePlatformStatusColumn.vue";

export default {
  name: "TestCaseIssueRelate",
  components: {
    IssuePlatformStatusColumn,
    IssueRelateList, IssueDescriptionTableItem, MsTableColumn, MsTable, TestPlanIssueEdit},
  data() {
    return {
      page: {
        data: [],
        result: {},
      },
      isThirdPart: false,
      issueTemplate: {},
      members: [],
      fields: [],
      operators: [
        {
          tip: this.$t('test_track.case.unlink'),
          icon: "el-icon-unlock",
          type: "danger",
          exec: this.deleteIssue
        }
      ],
      issueRelateVisible: false,
    }
  },
  props: {
    planId: String,
    caseId: String,
    planCaseId: String,
    readOnly: Boolean,
    isCopy: Boolean,
  },
  computed: {
    issueStatusMap() {
      return ISSUE_STATUS_MAP;
    },
    notInIds() {
      return this.page.data ? this.page.data.map(i => i.id) : [];
    },
    projectId() {
      return getCurrentProjectID();
    },
  },
  watch: {
    isThirdPart() {
      if (this.isThirdPart) {
        this.$nextTick(() => {
          if (this.$refs.issuePlatformStatus && this.$refs.issuePlatformStatus.length > 0) {
            this.$refs.issuePlatformStatus[0].getPlatformStatus();
          }
        });
      }
    }
  },
  created() {
    getIssuePartTemplateWithProject((template, project) => {
      this.currentProject = project;
      this.issueTemplate = template;
      if (this.issueTemplate.platform === LOCAL) {
        this.isThirdPart = false;
      } else {
        this.isThirdPart = true;
      }
      this.fields = getTableHeaderWithCustomFields('ISSUE_LIST', this.issueTemplate.customFields);
      if (!this.isThirdPart) {
        for (let i = 0; i < this.fields.length; i++) {
          if (this.fields[i].id === 'platformStatus') {
            this.fields.splice(i, 1);
            break;
          }
        }
      }
      if (this.$refs.table) {
        this.$refs.table.reloadTable();
      }
    });
  },
  methods: {
    /**
     * 我在做：对齐“缺陷管理列表页”的状态回显逻辑，确保测试计划关联缺陷表格也能稳定展示状态。
     * 目的是：解决个别缺陷在本页不展示状态的问题。
     * 如果不这样做，就无法实现：当缺陷的状态值/选项与前端模板不完全一致时，本页会出现空白回显。
     */
    getIssueStatusDisplay(row, field) {
      if (row && row.displayValueMap && row.displayValueMap[field && field.name]) {
        return row.displayValueMap[field.name];
      }

      const customVal = getCustomFieldValue(row, field, this.members);
      if (customVal) {
        return customVal;
      }

      if (row && row.status) {
        return this.issueStatusMap[row.status] || row.status;
      }

      return "--";
    },
    getCustomFieldValue(row, field) {
      return getCustomFieldValue(row, field, this.members);
    },
    getIssues() {
      if (!this.isCopy) {
        let result = getIssuesByCaseId(this.planId ? 'PLAN_FUNCTIONAL' : 'FUNCTIONAL', this.getCaseResourceId(), this.page);
        if (result) {
          this.page.result = result;
        }
      }
    },
    getCaseResourceId() {
      return this.planId ? this.planCaseId : this.caseId;
    },
    addIssue() {
      if (!this.caseId || this.isCopy) {
        this.$warning(this.$t('api_test.automation.save_case_info'));
        return;
      }
      this.$refs.issueEdit.open();
    },
    relateIssue() {
      if (!this.caseId || this.isCopy) {
        this.$warning(this.$t('api_test.automation.save_case_info'));
        return;
      }
      this.$refs.issueRelate.open();
    },
    closeIssue(row) {
      if (row.status === 'closed') {
        this.$success(this.$t('test_track.issue.close_success'));
      } else {
        this.page.result.loading = true;
        closeIssue(row.id)
          .then(() => {
            this.page.result.loading = false;
            this.getIssues();
            this.$success(this.$t('test_track.issue.close_success'));
          })
      }
    },
    deleteIssue(row) {
      operationConfirm(this, this.$t('test_track.issue.delete_warning'), () => {
        this.page.result.loading = true;
        deleteIssueRelate({
          id: row.id,
          caseResourceId: this.getCaseResourceId(),
          isPlanEdit: this.planId ? true : false,
          projectId: this.projectId,
          workspaceId: getCurrentWorkspaceId()
        }).then(() => {
          this.page.result.loading = false;
          this.getIssues();
          this.$success(this.$t('commons.delete_success'));
        });
      });
    }
  }
}
</script>

<style scoped>
.add-btn {
  display: inline-block;
  margin-right: 5px;
}

.el-dropdown-link {
  cursor: pointer;
  color: #783887;
}
</style>
