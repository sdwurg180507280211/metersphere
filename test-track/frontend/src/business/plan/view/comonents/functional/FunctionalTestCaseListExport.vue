<script>
import FunctionalTestCaseList from "./FunctionalTestCaseList";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";
import {fileDownloadPost} from "@/business/utils/sdk-utils";
import {useStore} from "@/store";

const store = useStore();

export default {
  name: "FunctionalTestCaseListExport",
  extends: FunctionalTestCaseList,
  computed: {
    buttons() {
      const exportButton = {
        name: this.$t("test_track.case.import.case_export"),
        permissions: ["PROJECT_TRACK_CASE:READ+EXPORT"],
        children: [
          {
            name: this.$t("test_track.case.export.export_to_excel"),
            tips: this.$t("test_track.case.export.export_to_excel_tips"),
            handleClick: this.handleBatchExportToExcel,
            permissions: ["PROJECT_TRACK_CASE:READ+EXPORT"],
          },
          {
            name: this.$t("test_track.case.export.export_to_xmind"),
            tips: this.$t("test_track.case.export.export_to_xmind_tips"),
            handleClick: this.handleBatchExportToXmind,
            permissions: ["PROJECT_TRACK_CASE:READ+EXPORT"],
          },
        ],
      };

      if (this.planStatus === 'Archived') {
        return [
          exportButton,
          {
            name: this.$t('test_track.case.batch_edit_case'), handleClick: this.handleBatchEdit,
            isDisable: true,
            permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_EDIT']
          },
          {
            name: this.$t('test_track.case.batch_unlink'), handleClick: this.handleDeleteBatch,
            isDisable: true,
            permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_DELETE']
          }
        ]
      } else {
        return [
          exportButton,
          {
            name: this.$t('test_track.case.batch_edit_case'), handleClick: this.handleBatchEdit,
            permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_EDIT']
          },
          {
            name: this.$t('test_track.case.batch_unlink'), handleClick: this.handleDeleteBatch,
            permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_DELETE']
          }
        ]
      }
    },
  },
  methods: {
    getSelectedTestCaseRows() {
      if (!this.$refs.table || !this.$refs.table.selectRows) {
        return [];
      }

      const selectRows = this.$refs.table.selectRows;
      if (Array.isArray(selectRows)) {
        return selectRows;
      }
      if (typeof selectRows.values === "function") {
        return Array.from(selectRows.values());
      }
      return [];
    },

    getSelectedCaseIds() {
      const rows = this.getSelectedTestCaseRows();
      return [...new Set(rows.map(row => row && row.caseId).filter(id => !!id))];
    },

    getDefaultExcelExportFieldParam() {
      return {
        baseHeaders: [
          {id: 'ID', name: 'ID'},
          {id: 'name', name: this.$t("test_track.case.name")},
          {id: 'nodeId', name: this.$t("test_track.case.module")},
          {id: 'prerequisite', name: this.$t("test_track.case.prerequisite")},
          {id: 'remark', name: this.$t("commons.remark")},
          {id: 'stepDesc', name: this.$t("test_track.case.step_describe")},
          {id: 'stepResult', name: this.$t("test_track.case.expected_results")},
          {id: 'stepModel', name: this.$t("test_track.case.step_model")},
          {id: 'tags', name: this.$t("commons.tag")},
        ],
        customHeaders: this.getDefaultCustomExportHeaders(),
        otherHeaders: [],
      }
    },

    getDefaultCustomExportHeaders() {
      if (!this.testCaseTemplate || !Array.isArray(this.testCaseTemplate.customFields)) {
        return [];
      }
      return this.testCaseTemplate.customFields
        .filter(item => item && item.id)
        .map(item => ({
          id: item.id,
          name: item.name,
        }));
    },

    buildExportTestCaseParam(fieldParam = {}) {
      const caseIds = this.getSelectedCaseIds();
      if (caseIds.length < 1) {
        this.$warning(this.$t("test_track.case.check_select"));
        return null;
      }

      const projectId = getCurrentProjectID();
      if (!projectId) {
        this.$warning(this.$t("commons.check_project_tip"));
        return null;
      }

      let param = Object.assign({}, fieldParam || {});
      param.ids = caseIds;
      param.projectId = projectId;
      param.selectAll = false;
      param.exportAll = false;
      param.condition = Object.assign({}, param.condition || {}, {
        ids: caseIds,
        projectId: projectId,
        selectAll: false,
      });

      return param;
    },

    exportTestCase(exportType, fieldParam) {
      const param = this.buildExportTestCaseParam(fieldParam);
      if (!param) {
        return;
      }

      let fileNameSuffix;
      let url;
      if (exportType === "xmind") {
        url = "/test/case/export/testcase/xmind";
        fileNameSuffix = ".xmind";
      } else {
        url = "/test/case/export/testcase";
        fileNameSuffix = param.ids.length > 1000 ? ".zip" : ".xlsx";
      }

      const planName = this.testPlan && this.testPlan.name ? this.testPlan.name : this.planId;
      this.loading = true;
      store.isTestCaseExporting = true;
      fileDownloadPost(
        url,
        param,
        "Metersphere_case_" + planName + fileNameSuffix
      )
        .then(() => {
          this.loading = false;
          store.isTestCaseExporting = false;
        })
        .catch(() => {
          this.loading = false;
          store.isTestCaseExporting = false;
        });
    },

    handleBatchExportToExcel() {
      this.exportTestCase("excel", this.getDefaultExcelExportFieldParam());
    },

    handleBatchExportToXmind() {
      this.exportTestCase("xmind", {exportAll: false});
    },
  }
};
</script>
