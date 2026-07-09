<script>
import Vue from "vue";
import FunctionalTestCaseList from "./FunctionalTestCaseList";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";
import {fileDownloadPost} from "@/business/utils/sdk-utils";
import {useStore} from "@/store";
import TestCaseExportToExcel from "@/business/case/components/export/TestCaseExportToExcel";

const store = useStore();

export default {
  name: "FunctionalTestCaseListExport",
  extends: FunctionalTestCaseList,
  data() {
    return {
      exportExcelDialog: null,
    }
  },
  beforeDestroy() {
    this.destroyExportExcelDialog();
  },
  computed: {
    buttons() {
      const exportExcelButton = {
        name: this.$t("test_track.case.export.export_to_excel"),
        tips: this.$t("test_track.case.export.export_to_excel_tips"),
        handleClick: this.handleBatchExportToExcel,
        permissions: ["PROJECT_TRACK_CASE:READ+EXPORT"],
      };

      const exportXmindButton = {
        name: this.$t("test_track.case.export.export_to_xmind"),
        tips: this.$t("test_track.case.export.export_to_xmind_tips"),
        handleClick: this.handleBatchExportToXmind,
        permissions: ["PROJECT_TRACK_CASE:READ+EXPORT"],
      };

      const batchEditButton = {
        name: this.$t('test_track.case.batch_edit_case'), handleClick: this.handleBatchEdit,
        permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_EDIT']
      };

      const batchUnlinkButton = {
        name: this.$t('test_track.case.batch_unlink'), handleClick: this.handleDeleteBatch,
        permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_DELETE']
      };

      if (this.planStatus === 'Archived') {
        batchEditButton.isDisable = true;
        batchUnlinkButton.isDisable = true;
      }

      return [
        exportExcelButton,
        exportXmindButton,
        batchEditButton,
        batchUnlinkButton,
      ]
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

    getExportExcelDialog() {
      if (this.exportExcelDialog) {
        return this.exportExcelDialog;
      }

      const ExportExcelDialog = Vue.extend(TestCaseExportToExcel);
      this.exportExcelDialog = new ExportExcelDialog({parent: this});
      this.exportExcelDialog.$on('exportTestCase', this.exportTestCase);
      this.exportExcelDialog.$mount();
      document.body.appendChild(this.exportExcelDialog.$el);
      return this.exportExcelDialog;
    },

    destroyExportExcelDialog() {
      if (!this.exportExcelDialog) {
        return;
      }
      this.exportExcelDialog.$off('exportTestCase', this.exportTestCase);
      this.exportExcelDialog.$destroy();
      if (this.exportExcelDialog.$el && this.exportExcelDialog.$el.parentNode) {
        this.exportExcelDialog.$el.parentNode.removeChild(this.exportExcelDialog.$el);
      }
      this.exportExcelDialog = null;
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
      const caseIds = this.getSelectedCaseIds();
      if (caseIds.length < 1) {
        this.$warning(this.$t("test_track.case.check_select"));
        return;
      }
      this.getExportExcelDialog().open(caseIds.length, false);
    },

    handleBatchExportToXmind() {
      this.exportTestCase("xmind", {exportAll: false});
    },
  }
};
</script>
