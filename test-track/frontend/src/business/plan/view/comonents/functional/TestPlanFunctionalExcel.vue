<template>
  <div class="onlyoffice-excel">
    <div class="onlyoffice-toolbar">
      <div class="onlyoffice-status">
        <span class="onlyoffice-status__dot" :class="statusClass" />
        <span class="onlyoffice-status__text">{{ statusText }}</span>
      </div>
      <div class="onlyoffice-actions">
        <el-button
          type="primary"
          size="mini"
          icon="el-icon-check"
          :loading="saveLoading"
          :disabled="!canSave"
          @click="saveToCaseLibrary"
        >
          {{ $t("test_track.case.onlyoffice_save") }}
        </el-button>
        <el-button
          size="mini"
          icon="el-icon-refresh"
          :loading="configLoading"
          :disabled="saveLoading"
          @click="loadSession"
        >
          {{ $t("test_track.case.onlyoffice_reload") }}
        </el-button>
        <el-button
          size="mini"
          icon="el-icon-refresh-right"
          :disabled="!sessionId"
          @click="refreshState"
        >
          {{ $t("test_track.case.onlyoffice_sync_state") }}
        </el-button>
        <el-popover
          v-if="hasResultErrors"
          placement="bottom-end"
          width="420"
          trigger="click"
        >
          <div class="onlyoffice-errors">
            <div
              v-for="(error, index) in syncResult.errors"
              :key="index"
              class="onlyoffice-errors__item"
            >
              {{ error }}
            </div>
          </div>
          <el-button slot="reference" size="mini" type="warning" plain>
            {{ $t("test_track.case.onlyoffice_error_detail") }}
          </el-button>
        </el-popover>
      </div>
    </div>

    <div v-if="configLoading" class="onlyoffice-state" v-loading="true" />

    <div v-else-if="errorMessage" class="onlyoffice-state">
      <div class="onlyoffice-state__title">{{ errorMessage }}</div>
      <div class="onlyoffice-state__description">
        {{ $t("test_track.case.onlyoffice_config_hint") }}
      </div>
    </div>

    <div
      v-else
      class="onlyoffice-frame-shell"
      v-loading="loading"
      :element-loading-text="$t('test_track.case.onlyoffice_loading')"
    >
      <div :id="editorId" class="onlyoffice-frame" />

      <div v-if="showLoadWarning" class="onlyoffice-warning">
        <div>
          <div class="onlyoffice-warning__title">
            {{ $t("test_track.case.onlyoffice_load_timeout") }}
          </div>
          <div class="onlyoffice-warning__description">
            {{ $t("test_track.case.onlyoffice_load_timeout_hint") }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  createOnlyOfficeCaseSession,
  getOnlyOfficeCaseSessionState,
  saveOnlyOfficeCaseSession,
} from "@/api/onlyoffice";

export default {
  name: "TestPlanFunctionalExcel",
  props: {
    planId: String,
    projectId: String,
  },
  data() {
    return {
      sessionId: "",
      config: null,
      documentServerUrl: "",
      syncResult: null,
      configLoading: false,
      loading: false,
      showLoadWarning: false,
      errorMessage: "",
      hasUnsavedChanges: false,
      saveLoading: false,
      warningTimer: null,
      stateTimer: null,
      savePollTimer: null,
      editor: null,
    };
  },
  computed: {
    editorId() {
      return `onlyoffice-case-editor-${this._uid}`;
    },
    statusClass() {
      const status = this.syncResult && this.syncResult.status;
      if (status === "saved") {
        return "is-success";
      }
      if (status === "saving") {
        return "is-saving";
      }
      if (status === "partial") {
        return "is-warning";
      }
      if (status === "error") {
        return "is-error";
      }
      if (this.hasUnsavedChanges) {
        return "is-warning";
      }
      return "is-ready";
    },
    statusText() {
      if (this.errorMessage) {
        return this.errorMessage;
      }
      if (this.saveLoading) {
        return this.$t("test_track.case.onlyoffice_saving");
      }
      if (this.hasUnsavedChanges) {
        return this.$t("test_track.case.onlyoffice_dirty");
      }
      if (this.syncResult && this.syncResult.message) {
        return this.syncResult.message;
      }
      return this.$t("test_track.case.onlyoffice_waiting");
    },
    canSave() {
      return !!this.sessionId && !this.configLoading && !this.loading && !this.saveLoading && !this.errorMessage;
    },
    hasResultErrors() {
      return this.syncResult && Array.isArray(this.syncResult.errors) && this.syncResult.errors.length > 0;
    },
  },
  mounted() {
    this.loadSession();
  },
  beforeDestroy() {
    this.destroyEditor();
    this.clearLoadTimer();
    this.clearStateTimer();
    this.clearSavePollTimer();
  },
  watch: {
    planId() {
      this.loadSession();
    },
  },
  methods: {
    loadSession() {
      if (!this.planId) {
        this.errorMessage = this.$t("test_track.case.onlyoffice_plan_required");
        return;
      }
      this.destroyEditor();
      this.clearLoadTimer();
      this.clearStateTimer();
      this.clearSavePollTimer();
      this.errorMessage = "";
      this.configLoading = true;
      this.loading = false;
      this.saveLoading = false;
      this.hasUnsavedChanges = false;
      this.showLoadWarning = false;

      createOnlyOfficeCaseSession({
        planId: this.planId,
        projectId: this.projectId,
      })
        .then((response) => {
          const data = response.data || {};
          this.sessionId = data.sessionId;
          this.documentServerUrl = data.documentServerUrl;
          this.config = data.config;
          this.syncResult = data.syncResult;
          if (!this.documentServerUrl || !this.config) {
            throw new Error(this.$t("test_track.case.onlyoffice_not_configured"));
          }
          return this.loadDocumentScript(this.documentServerUrl);
        })
        .then(() => {
          this.configLoading = false;
          this.loading = true;
          this.$nextTick(this.openEditor);
        })
        .catch((error) => {
          this.configLoading = false;
          this.loading = false;
          this.errorMessage = error && error.message
            ? error.message
            : this.$t("test_track.case.onlyoffice_not_configured");
        });
    },
    loadDocumentScript(documentServerUrl) {
      if (window.DocsAPI) {
        return Promise.resolve();
      }
      const scriptUrl = `${documentServerUrl.replace(/\/$/, "")}/web-apps/apps/api/documents/api.js`;
      const runtimeDocument = window.rawDocument || document;
      const existed = runtimeDocument.querySelector(`script[src="${scriptUrl}"]`);
      if (existed) {
        return new Promise((resolve, reject) => {
          if (window.DocsAPI) {
            resolve();
            return;
          }
          existed.addEventListener("load", resolve, {once: true});
          existed.addEventListener("error", reject, {once: true});
        });
      }
      return new Promise((resolve, reject) => {
        const script = runtimeDocument.createElement("script");
        script.src = scriptUrl;
        script.async = true;
        script.setAttribute("ignore", "true");
        script.setAttribute("data-qiankun-ignore", "true");
        script.onload = resolve;
        script.onerror = () => reject(new Error(this.$t("test_track.case.onlyoffice_script_load_failed")));
        runtimeDocument.head.appendChild(script);
      });
    },
    openEditor() {
      if (!window.DocsAPI || !this.config) {
        this.errorMessage = this.$t("test_track.case.onlyoffice_script_load_failed");
        this.loading = false;
        return;
      }
      const config = Object.assign({}, this.config, {
        events: Object.assign({}, this.config.events || {}, {
          onAppReady: this.handleEditorReady,
          onDocumentStateChange: this.handleDocumentStateChange,
        }),
      });
      this.editor = new window.DocsAPI.DocEditor(this.editorId, config);
      this.startLoadTimer();
      this.startStateTimer();
    },
    destroyEditor() {
      if (this.editor && this.editor.destroyEditor) {
        this.editor.destroyEditor();
      }
      this.editor = null;
    },
    handleEditorReady() {
      this.loading = false;
      this.showLoadWarning = false;
      this.clearLoadTimer();
    },
    handleDocumentStateChange(event) {
      if (event && Object.prototype.hasOwnProperty.call(event, "data")) {
        this.hasUnsavedChanges = !!event.data;
      }
    },
    saveToCaseLibrary() {
      if (!this.sessionId || this.saveLoading) {
        return;
      }
      this.saveLoading = true;
      this.clearSavePollTimer();
      this.syncResult = Object.assign({}, this.syncResult || {}, {
        status: "saving",
        message: this.$t("test_track.case.onlyoffice_save_triggered"),
        errors: [],
      });
      saveOnlyOfficeCaseSession(this.sessionId)
        .then((response) => {
          this.syncResult = response.data || this.syncResult;
          if (this.isTerminalSaveStatus(this.syncResult.status)) {
            this.finishSavePolling();
            return;
          }
          this.pollSaveResult(Date.now());
        })
        .catch((error) => {
          this.saveLoading = false;
          this.syncResult = Object.assign({}, this.syncResult || {}, {
            status: "error",
            message: error && error.message
              ? error.message
              : this.$t("test_track.case.onlyoffice_save_failed"),
          });
        });
    },
    refreshState() {
      if (!this.sessionId) {
        return Promise.resolve();
      }
      return getOnlyOfficeCaseSessionState(this.sessionId)
        .then((response) => {
          this.syncResult = response.data || this.syncResult;
          if (this.syncResult && this.isTerminalSaveStatus(this.syncResult.status)) {
            this.finishSavePolling();
          }
          return this.syncResult;
        })
        .catch(() => {
          this.clearStateTimer();
        });
    },
    pollSaveResult(startedAt) {
      this.clearSavePollTimer();
      this.savePollTimer = window.setTimeout(() => {
        this.refreshState().then((result) => {
          if (result && this.isTerminalSaveStatus(result.status)) {
            this.finishSavePolling();
            return;
          }
          if (Date.now() - startedAt > 70000) {
            this.saveLoading = false;
            this.syncResult = Object.assign({}, this.syncResult || {}, {
              status: "error",
              message: this.$t("test_track.case.onlyoffice_save_timeout"),
            });
            return;
          }
          this.pollSaveResult(startedAt);
        });
      }, 1500);
    },
    finishSavePolling() {
      this.saveLoading = false;
      this.clearSavePollTimer();
      if (this.syncResult && (this.syncResult.status === "saved" || this.syncResult.status === "partial")) {
        this.hasUnsavedChanges = false;
      }
    },
    isTerminalSaveStatus(status) {
      return ["saved", "partial", "error"].includes(status);
    },
    startStateTimer() {
      this.clearStateTimer();
      this.stateTimer = window.setInterval(this.refreshState, 3000);
    },
    clearStateTimer() {
      if (this.stateTimer) {
        window.clearInterval(this.stateTimer);
        this.stateTimer = null;
      }
    },
    startLoadTimer() {
      this.clearLoadTimer();
      this.warningTimer = window.setTimeout(() => {
        this.showLoadWarning = this.loading;
      }, 20000);
    },
    clearLoadTimer() {
      if (this.warningTimer) {
        window.clearTimeout(this.warningTimer);
        this.warningTimer = null;
      }
    },
    clearSavePollTimer() {
      if (this.savePollTimer) {
        window.clearTimeout(this.savePollTimer);
        this.savePollTimer = null;
      }
    },
  },
};
</script>

<style scoped>
.onlyoffice-excel {
  height: calc(100vh - 190px);
  min-height: 520px;
  margin-top: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: grid;
  grid-template-rows: 40px minmax(0, 1fr);
}

.onlyoffice-toolbar {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 12px;
  border-bottom: 1px solid #ebeef5;
  background: #f7f9fb;
}

.onlyoffice-status {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 13px;
}

.onlyoffice-status__dot {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #909399;
}

.onlyoffice-status__dot.is-success {
  background: #67c23a;
}

.onlyoffice-status__dot.is-saving {
  background: #409eff;
}

.onlyoffice-status__dot.is-warning {
  background: #e6a23c;
}

.onlyoffice-status__dot.is-error {
  background: #f56c6c;
}

.onlyoffice-status__dot.is-ready {
  background: #409eff;
}

.onlyoffice-status__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.onlyoffice-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.onlyoffice-errors {
  max-height: 260px;
  overflow: auto;
}

.onlyoffice-errors__item {
  color: #606266;
  font-size: 12px;
  line-height: 18px;
  padding: 4px 0;
  border-bottom: 1px solid #ebeef5;
}

.onlyoffice-errors__item:last-child {
  border-bottom: none;
}

.onlyoffice-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
}

.onlyoffice-state__title {
  color: #303133;
  font-size: 16px;
  line-height: 24px;
  font-weight: 500;
}

.onlyoffice-state__description {
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
  line-height: 22px;
}

.onlyoffice-frame-shell,
.onlyoffice-frame {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.onlyoffice-frame-shell {
  position: relative;
}

.onlyoffice-frame {
  display: block;
  background: #ffffff;
}

.onlyoffice-warning {
  position: absolute;
  right: 20px;
  bottom: 20px;
  left: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(31, 35, 41, 0.08);
}

.onlyoffice-warning__title {
  color: #303133;
  font-size: 14px;
  line-height: 22px;
  font-weight: 500;
}

.onlyoffice-warning__description {
  margin-top: 2px;
  color: #606266;
  font-size: 13px;
  line-height: 20px;
}
</style>
