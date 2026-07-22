<template>
  <el-card class="requirement-workflow-workbench" shadow="never" v-loading="loading">
    <div slot="header" class="workbench-header">
      <div class="workbench-title-wrap">
        <span class="workbench-title">INT 测试流程工作台</span>
        <el-tag size="mini" type="warning" effect="plain">前端方案预览</el-tag>
        <span class="workbench-subtitle">全流程平台驱动测试计划创建、评审、准备、执行与交付</span>
      </div>
      <div class="workbench-actions">
        <el-button type="text" icon="el-icon-refresh" @click="refresh">刷新</el-button>
        <el-button type="text" :icon="collapsed ? 'el-icon-arrow-down' : 'el-icon-arrow-up'" @click="collapsed = !collapsed">
          {{ collapsed ? "展开" : "收起" }}
        </el-button>
      </div>
    </div>

    <div v-show="!collapsed">
      <div class="overview-cards">
        <div v-for="item in overviewCards" :key="item.key" class="overview-card">
          <div class="overview-card-icon" :class="'is-' + item.key">
            <i :class="item.icon"></i>
          </div>
          <div>
            <div class="overview-card-value">{{ item.value }}</div>
            <div class="overview-card-label">{{ item.label }}</div>
          </div>
        </div>
      </div>

      <div class="workflow-strip">
        <div
          v-for="(step, index) in workflowSteps"
          :key="step.code"
          class="workflow-step"
        >
          <div class="workflow-step-main">
            <span class="workflow-step-index">{{ index + 1 }}</span>
            <div>
              <div class="workflow-step-name">{{ step.name }}</div>
              <div class="workflow-step-count">{{ stepCount(step.code) }} 项</div>
            </div>
          </div>
          <i v-if="index < workflowSteps.length - 1" class="el-icon-arrow-right workflow-step-arrow"></i>
        </div>
      </div>

      <div class="linked-plan-header">
        <div>
          <span class="linked-plan-title">近期对接测试计划</span>
          <span class="linked-plan-tip">状态可点击查看当前环节，操作入口按最新版对接需求集中展示</span>
        </div>
        <el-tag size="mini" effect="plain">共 {{ rows.length }} 条</el-tag>
      </div>

      <el-table
        :data="rows"
        size="mini"
        border
        class="linked-plan-table"
        :empty-text="emptyText"
      >
        <el-table-column label="需求编号" min-width="155" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-tag v-if="scope.row.requirementNumber" size="mini" effect="plain">
              {{ scope.row.requirementNumber }}
            </el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="需求主题 / 测试计划" min-width="210" show-overflow-tooltip />
        <el-table-column label="所属系统" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.systemName || scope.row.projectName || "--" }}
          </template>
        </el-table-column>
        <el-table-column label="需求规格" width="92" align="center">
          <template slot-scope="scope">
            <el-link
              v-if="scope.row.requirementDocUrl"
              type="primary"
              :underline="false"
              @click.stop="openDocument(scope.row.requirementDocUrl)"
            >
              查看
            </el-link>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="当前进度" min-width="125" align="center">
          <template slot-scope="scope">
            <el-tag
              class="workflow-status-tag"
              size="mini"
              :type="statusMeta(scope.row).type"
              effect="plain"
              @click.stop="openProgress(scope.row)"
            >
              {{ statusMeta(scope.row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="计划周期" min-width="185">
          <template slot-scope="scope">
            <div class="time-line">{{ formatDate(scope.row.plannedStartTime) }}</div>
            <div class="time-line muted">至 {{ formatDate(scope.row.plannedEndTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="责任人" min-width="120" show-overflow-tooltip>
          <template slot-scope="scope">
            {{ scope.row.principalName || "--" }}
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="90" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.passRate || "0%" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="业务操作" width="275" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click.stop="openPlanDialog(scope.row)">计划编制</el-button>
            <el-button type="text" size="mini" @click.stop="openPreparationDialog(scope.row)">测试准备</el-button>
            <el-dropdown size="mini" trigger="click" @command="handleBusinessCommand($event, scope.row)">
              <el-button type="text" size="mini">
                更多 <i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="move">移动</el-dropdown-item>
                <el-dropdown-item command="review">评审用例（只读）</el-dropdown-item>
                <el-dropdown-item command="smoke">冒烟结论</el-dropdown-item>
                <el-dropdown-item command="execute">执行用例</el-dropdown-item>
                <el-dropdown-item command="defect">缺陷</el-dropdown-item>
                <el-dropdown-item command="report">报告</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      :visible.sync="progressDialogVisible"
      append-to-body
      width="760px"
      title="INT 测试当前进度"
    >
      <div v-if="activeRow" class="progress-dialog-content">
        <div class="requirement-summary">
          <div class="summary-item">
            <span class="summary-label">需求编号</span>
            <span>{{ activeRow.requirementNumber || "--" }}</span>
          </div>
          <div class="summary-item summary-item-wide">
            <span class="summary-label">需求主题</span>
            <span>{{ activeRow.name || "--" }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">责任人</span>
            <span>{{ activeRow.principalName || "--" }}</span>
          </div>
        </div>
        <el-steps :active="activeStepIndex" align-center finish-status="success">
          <el-step v-for="step in workflowSteps" :key="step.code" :title="step.name" />
        </el-steps>
        <div class="progress-note">
          <i class="el-icon-info"></i>
          当前为“{{ statusMeta(activeRow).label }}”。计划评审由全流程平台推进，测试平台接收审批结果并控制后续页面入口。
        </div>
      </div>
      <span slot="footer">
        <el-button @click="progressDialogVisible = false">关闭</el-button>
        <el-button v-if="activeRow" type="primary" @click="openCurrentStage(activeRow)">进入当前环节</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :visible.sync="planDialogVisible"
      append-to-body
      width="720px"
      title="测试计划编制"
      :close-on-click-modal="false"
    >
      <el-alert
        title="提交后状态进入“计划评审”，计划时间与责任人将由后端接口同步至全流程平台"
        type="info"
        :closable="false"
        show-icon
        class="dialog-alert"
      />
      <el-form :model="planForm" label-width="130px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="需求编号">
              <el-input v-model="planForm.requirementNumber" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求主题">
              <el-input v-model="planForm.name" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-section-title">计划准备</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="准备开始时间">
              <el-date-picker v-model="planForm.preparationStartTime" type="datetime" value-format="timestamp" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="准备结束时间">
              <el-date-picker v-model="planForm.preparationEndTime" type="datetime" value-format="timestamp" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="准备责任人">
          <el-select v-model="planForm.preparationPrincipals" multiple filterable allow-create default-first-option style="width: 100%" placeholder="可选择或输入多人">
            <el-option v-for="name in principalOptions" :key="'prepare-' + name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <div class="form-section-title">计划执行</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="执行开始时间">
              <el-date-picker v-model="planForm.executionStartTime" type="datetime" value-format="timestamp" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行结束时间">
              <el-date-picker v-model="planForm.executionEndTime" type="datetime" value-format="timestamp" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="执行责任人">
          <el-select v-model="planForm.executionPrincipals" multiple filterable allow-create default-first-option style="width: 100%" placeholder="可选择或输入多人">
            <el-option v-for="name in principalOptions" :key="'execute-' + name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPlanPreview">提交计划评审</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :visible.sync="preparationDialogVisible"
      append-to-body
      width="680px"
      title="测试准备"
      :close-on-click-modal="false"
    >
      <el-form :model="preparationForm" label-width="130px" size="small">
        <el-form-item label="是否需要冒烟测试">
          <el-radio-group v-model="preparationForm.requireSmokeTest">
            <el-radio :label="true">是</el-radio>
            <el-radio :label="false">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评审类型">
          <el-radio-group v-model="preparationForm.reviewType">
            <el-radio label="SELF">自评</el-radio>
            <el-radio label="OTHER">他评</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评审人员">
          <el-select v-model="preparationForm.reviewers" multiple filterable allow-create default-first-option style="width: 100%" placeholder="可输入多人姓名">
            <el-option v-for="name in principalOptions" :key="'review-' + name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="评审用例上传">
          <el-upload
            action="#"
            :auto-upload="false"
            :file-list="reviewFiles"
            :on-change="handleReviewFileChange"
            :on-remove="handleReviewFileRemove"
          >
            <el-button size="small" icon="el-icon-upload2">选择评审用例</el-button>
            <div slot="tip" class="el-upload__tip">支持展示上传交互，文件落库接口后续联调</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="评审用例数">
          <el-input-number v-model="preparationForm.reviewCaseCount" :min="0" :max="99999" controls-position="right" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="preparationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPreparationPreview">完成测试准备</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :visible.sync="smokeDialogVisible"
      append-to-body
      width="560px"
      title="冒烟测试结论"
      :close-on-click-modal="false"
    >
      <el-form :model="smokeForm" label-width="105px" size="small">
        <el-form-item label="冒烟结论">
          <el-radio-group v-model="smokeForm.result">
            <el-radio label="PASSED">通过</el-radio>
            <el-radio label="FAILED">不通过</el-radio>
            <el-radio label="CANCELLED">取消</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="结论说明">
          <el-input v-model="smokeForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-alert
          v-if="smokeForm.result === 'FAILED'"
          title="提交后进入“冒烟不通过”，并触发开发人员通知；开发处理完成后重新进入冒烟测试"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <span slot="footer">
        <el-button @click="smokeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSmokePreview">提交结论</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import { testPlanList, testPlanMetric } from "@/api/remote/plan/test-plan";
import { getCurrentProjectID } from "metersphere-frontend/src/utils/token";

const WORKFLOW_META = {
  PLAN: { label: "INT测试计划", type: "info", step: 0 },
  PLAN_REVIEW: { label: "计划评审", type: "warning", step: 1 },
  PLAN_REVIEW_FAILED: { label: "计划评审失败", type: "danger", step: 1 },
  TEST_PREPARATION: { label: "测试准备", type: "warning", step: 2 },
  SMOKE_TEST: { label: "冒烟测试", type: "warning", step: 3 },
  SMOKE_FAILED: { label: "冒烟不通过", type: "danger", step: 3 },
  TEST_EXECUTION: { label: "测试执行", type: "primary", step: 4 },
  COMPLETED: { label: "已完成", type: "success", step: 5 },
  CANCELLED: { label: "已取消", type: "info", step: 0 },
};

export default {
  name: "RequirementWorkflowWorkbench",
  data() {
    return {
      loading: false,
      collapsed: false,
      rows: [],
      activeRow: null,
      progressDialogVisible: false,
      planDialogVisible: false,
      preparationDialogVisible: false,
      smokeDialogVisible: false,
      reviewFiles: [],
      workflowSteps: [
        { code: "PLAN", name: "INT测试计划" },
        { code: "PLAN_REVIEW", name: "计划评审" },
        { code: "TEST_PREPARATION", name: "测试准备" },
        { code: "SMOKE_TEST", name: "冒烟测试" },
        { code: "TEST_EXECUTION", name: "测试执行" },
        { code: "COMPLETED", name: "已完成" },
      ],
      planForm: {
        requirementNumber: "",
        name: "",
        preparationStartTime: null,
        preparationEndTime: null,
        preparationPrincipals: [],
        executionStartTime: null,
        executionEndTime: null,
        executionPrincipals: [],
      },
      preparationForm: {
        requireSmokeTest: true,
        reviewType: "SELF",
        reviewers: [],
        reviewCaseCount: 0,
      },
      smokeForm: {
        result: "PASSED",
        comment: "",
      },
    };
  },
  computed: {
    emptyText() {
      return getCurrentProjectID() ? "暂无已关联需求的测试计划" : "请先选择项目";
    },
    overviewCards() {
      const reviewCount = this.rows.filter((row) => {
        const code = this.workflowCode(row);
        return code === "PLAN_REVIEW" || code === "PLAN_REVIEW_FAILED";
      }).length;
      const preparationCount = this.rows.filter((row) => {
        const code = this.workflowCode(row);
        return code === "TEST_PREPARATION" || code === "SMOKE_TEST" || code === "SMOKE_FAILED";
      }).length;
      return [
        { key: "all", label: "对接计划", value: this.rows.length, icon: "el-icon-connection" },
        { key: "review", label: "待计划评审", value: reviewCount, icon: "el-icon-document-checked" },
        { key: "prepare", label: "测试准备中", value: preparationCount, icon: "el-icon-set-up" },
        { key: "running", label: "测试执行中", value: this.countByCode("TEST_EXECUTION"), icon: "el-icon-video-play" },
        { key: "done", label: "已完成", value: this.countByCode("COMPLETED"), icon: "el-icon-circle-check" },
      ];
    },
    principalOptions() {
      if (!this.activeRow || !this.activeRow.principalUsers) {
        return this.activeRow && this.activeRow.principalName ? this.activeRow.principalName.split("、") : [];
      }
      return this.activeRow.principalUsers.map((user) => user.name).filter(Boolean);
    },
    activeStepIndex() {
      if (!this.activeRow) {
        return 0;
      }
      return this.statusMeta(this.activeRow).step;
    },
  },
  mounted() {
    this.loadData();
  },
  methods: {
    refresh() {
      this.loadData();
    },
    loadData() {
      const projectId = getCurrentProjectID();
      if (!projectId) {
        this.rows = [];
        return;
      }
      this.loading = true;
      const condition = { projectId };
      testPlanList({ pageNum: 1, pageSize: 50 }, condition)
        .then((response) => {
          const data = response.data || {};
          const sourceRows = data.listObject || data.list || [];
          const normalizedRows = sourceRows.map((row) => this.normalizeRow(row));
          const linkedRows = normalizedRows.filter((row) => row.requirementNumber);
          this.rows = (linkedRows.length > 0 ? linkedRows : normalizedRows).slice(0, 8);
          this.loadMetric();
        })
        .catch(() => {
          this.rows = [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadMetric() {
      const ids = this.rows.map((row) => row.id).filter(Boolean);
      if (ids.length === 0) {
        return;
      }
      testPlanMetric(ids).then((response) => {
        const metrics = response.data || [];
        const metricMap = {};
        metrics.forEach((item) => {
          metricMap[item.id] = item;
        });
        this.rows.forEach((row) => {
          const metric = metricMap[row.id];
          if (metric) {
            this.$set(row, "passRate", (metric.passRate || 0) + "%");
            this.$set(row, "testRate", metric.testRate || 0);
          }
        });
      }).catch(() => {});
    },
    normalizeRow(source) {
      const row = Object.assign({}, source);
      row.requirementDocUrl = row.requirementDocUrl || row.requirement_doc_url || row.docUrl || "";
      row.systemName = row.systemName || row.requirementSystemName || "";
      if (row.principalUsers && row.principalUsers.length > 0) {
        row.principalName = row.principalUsers.map((user) => user.name).filter(Boolean).join("、");
      } else {
        row.principalName = row.principalName || "";
      }
      row._workflowStatus = this.workflowCode(row);
      return row;
    },
    workflowCode(row) {
      if (!row) {
        return "PLAN";
      }
      const rawStatus = row._workflowStatus || row.requirementWorkflowStatus || row.workflowStatus || row.integrationStatus || row.planStatus;
      const normalized = rawStatus ? String(rawStatus).toUpperCase() : "";
      const aliases = {
        "INT测试计划": "PLAN",
        "测试计划": "PLAN",
        "计划评审": "PLAN_REVIEW",
        "计划评审失败": "PLAN_REVIEW_FAILED",
        "测试准备": "TEST_PREPARATION",
        "冒烟测试": "SMOKE_TEST",
        "冒烟不通过": "SMOKE_FAILED",
        "测试执行": "TEST_EXECUTION",
        "已完成": "COMPLETED",
        PREPARE: "PLAN",
        PENDING: "PLAN",
        UNDERWAY: "TEST_EXECUTION",
        RUNNING: "TEST_EXECUTION",
        FINISHED: "COMPLETED",
        ARCHIVED: "COMPLETED",
        CANCELLED: "CANCELLED",
      };
      if (WORKFLOW_META[normalized]) {
        return normalized;
      }
      if (aliases[normalized]) {
        return aliases[normalized];
      }
      if (row.status && aliases[String(row.status).toUpperCase()]) {
        return aliases[String(row.status).toUpperCase()];
      }
      return "PLAN";
    },
    statusMeta(row) {
      return WORKFLOW_META[this.workflowCode(row)] || WORKFLOW_META.PLAN;
    },
    countByCode(code) {
      return this.rows.filter((row) => this.workflowCode(row) === code).length;
    },
    stepCount(code) {
      if (code === "PLAN_REVIEW") {
        return this.rows.filter((row) => ["PLAN_REVIEW", "PLAN_REVIEW_FAILED"].indexOf(this.workflowCode(row)) >= 0).length;
      }
      if (code === "SMOKE_TEST") {
        return this.rows.filter((row) => ["SMOKE_TEST", "SMOKE_FAILED"].indexOf(this.workflowCode(row)) >= 0).length;
      }
      return this.countByCode(code);
    },
    formatDate(value) {
      if (!value) {
        return "--";
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return "--";
      }
      const pad = (num) => String(num).padStart(2, "0");
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    },
    openDocument(url) {
      if (url) {
        window.open(url, "_blank", "noopener,noreferrer");
      }
    },
    openProgress(row) {
      this.activeRow = row;
      this.progressDialogVisible = true;
    },
    openCurrentStage(row) {
      const code = this.workflowCode(row);
      this.progressDialogVisible = false;
      if (code === "PLAN" || code === "PLAN_REVIEW_FAILED") {
        this.openPlanDialog(row);
      } else if (code === "TEST_PREPARATION") {
        this.openPreparationDialog(row);
      } else if (code === "SMOKE_TEST" || code === "SMOKE_FAILED") {
        this.openSmokeDialog(row);
      } else {
        this.goPlan(row, "execute");
      }
    },
    openPlanDialog(row) {
      this.activeRow = row;
      const names = row.principalName ? row.principalName.split("、") : [];
      this.planForm = {
        requirementNumber: row.requirementNumber || "",
        name: row.name || "",
        preparationStartTime: row.preparationStartTime || row.planPreparationStartTime || null,
        preparationEndTime: row.preparationEndTime || row.planPreparationEndTime || null,
        preparationPrincipals: row.preparationPrincipals || names.slice(),
        executionStartTime: row.executionStartTime || row.plannedStartTime || null,
        executionEndTime: row.executionEndTime || row.plannedEndTime || null,
        executionPrincipals: row.executionPrincipals || names.slice(),
      };
      this.planDialogVisible = true;
    },
    submitPlanPreview() {
      if (!this.activeRow) {
        return;
      }
      if (this.planForm.preparationStartTime && this.planForm.preparationEndTime && this.planForm.preparationStartTime > this.planForm.preparationEndTime) {
        this.$warning("计划准备开始时间不能晚于结束时间");
        return;
      }
      if (this.planForm.executionStartTime && this.planForm.executionEndTime && this.planForm.executionStartTime > this.planForm.executionEndTime) {
        this.$warning("计划执行开始时间不能晚于结束时间");
        return;
      }
      this.$set(this.activeRow, "preparationStartTime", this.planForm.preparationStartTime);
      this.$set(this.activeRow, "preparationEndTime", this.planForm.preparationEndTime);
      this.$set(this.activeRow, "preparationPrincipals", this.planForm.preparationPrincipals);
      this.$set(this.activeRow, "plannedStartTime", this.planForm.executionStartTime);
      this.$set(this.activeRow, "plannedEndTime", this.planForm.executionEndTime);
      this.$set(this.activeRow, "executionPrincipals", this.planForm.executionPrincipals);
      this.$set(this.activeRow, "_workflowStatus", "PLAN_REVIEW");
      this.planDialogVisible = false;
      this.$success("前端方案已提交至“计划评审”，后端接口接入后将同步全流程平台");
    },
    openPreparationDialog(row) {
      this.activeRow = row;
      this.preparationForm = {
        requireSmokeTest: row.requireSmokeTest !== false,
        reviewType: row.reviewType || "SELF",
        reviewers: row.reviewers || (row.principalName ? row.principalName.split("、") : []),
        reviewCaseCount: row.reviewCaseCount || 0,
      };
      this.reviewFiles = row.reviewFiles || [];
      this.preparationDialogVisible = true;
    },
    handleReviewFileChange(file, fileList) {
      this.reviewFiles = fileList;
    },
    handleReviewFileRemove(file, fileList) {
      this.reviewFiles = fileList;
    },
    submitPreparationPreview() {
      if (!this.activeRow) {
        return;
      }
      this.$set(this.activeRow, "requireSmokeTest", this.preparationForm.requireSmokeTest);
      this.$set(this.activeRow, "reviewType", this.preparationForm.reviewType);
      this.$set(this.activeRow, "reviewers", this.preparationForm.reviewers);
      this.$set(this.activeRow, "reviewCaseCount", this.preparationForm.reviewCaseCount);
      this.$set(this.activeRow, "reviewFiles", this.reviewFiles);
      this.$set(this.activeRow, "_workflowStatus", this.preparationForm.requireSmokeTest ? "SMOKE_TEST" : "TEST_EXECUTION");
      this.preparationDialogVisible = false;
      this.$success(this.preparationForm.requireSmokeTest ? "测试准备完成，已进入冒烟测试" : "测试准备完成，已进入测试执行");
    },
    openSmokeDialog(row) {
      this.activeRow = row;
      this.smokeForm = {
        result: "PASSED",
        comment: row.smokeComment || "",
      };
      this.smokeDialogVisible = true;
    },
    submitSmokePreview() {
      if (!this.activeRow) {
        return;
      }
      const resultStatusMap = {
        PASSED: "TEST_EXECUTION",
        FAILED: "SMOKE_FAILED",
        CANCELLED: "TEST_PREPARATION",
      };
      this.$set(this.activeRow, "smokeResult", this.smokeForm.result);
      this.$set(this.activeRow, "smokeComment", this.smokeForm.comment);
      this.$set(this.activeRow, "_workflowStatus", resultStatusMap[this.smokeForm.result]);
      this.smokeDialogVisible = false;
      if (this.smokeForm.result === "FAILED") {
        this.$warning("冒烟测试不通过，页面已展示开发通知与重新冒烟流程");
      } else if (this.smokeForm.result === "PASSED") {
        this.$success("冒烟测试通过，已进入测试执行");
      } else {
        this.$info("已取消本次冒烟结论");
      }
    },
    handleBusinessCommand(command, row) {
      const handlers = {
        move: () => this.$info("移动能力沿用测试计划原有模块树操作"),
        review: () => this.goPlan(row, "review"),
        smoke: () => this.openSmokeDialog(row),
        execute: () => this.goPlan(row, "execute"),
        defect: () => this.goPlan(row, "defect"),
        report: () => this.$emit("open-report", row),
      };
      if (handlers[command]) {
        handlers[command]();
      }
    },
    goPlan(row, business) {
      if (!row || !row.id) {
        return;
      }
      this.$router.push({
        path: "/track/plan/view/" + row.id,
        query: { business },
      });
    },
  },
};
</script>

<style scoped>
.requirement-workflow-workbench {
  margin-bottom: 12px;
  border-color: #ebeef5;
}

.workbench-header,
.workbench-title-wrap,
.workbench-actions,
.linked-plan-header,
.workflow-step,
.workflow-step-main {
  display: flex;
  align-items: center;
}

.workbench-header,
.linked-plan-header {
  justify-content: space-between;
}

.workbench-title-wrap {
  gap: 10px;
  min-width: 0;
}

.workbench-title {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.workbench-subtitle,
.linked-plan-tip {
  color: #909399;
  font-size: 12px;
}

.workbench-actions {
  gap: 8px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(5, minmax(130px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 14px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.overview-card-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #783887;
  background: #f4ecf6;
  font-size: 18px;
}

.overview-card-icon.is-review,
.overview-card-icon.is-prepare {
  color: #e6a23c;
  background: #fdf6ec;
}

.overview-card-icon.is-running {
  color: #409eff;
  background: #ecf5ff;
}

.overview-card-icon.is-done {
  color: #67c23a;
  background: #f0f9eb;
}

.overview-card-value {
  color: #303133;
  font-size: 22px;
  line-height: 1.1;
  font-weight: 600;
}

.overview-card-label {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.workflow-strip {
  display: flex;
  align-items: stretch;
  padding: 12px 14px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: linear-gradient(90deg, #fbf8fc 0%, #f7f9fc 100%);
}

.workflow-step {
  flex: 1;
  min-width: 0;
  justify-content: space-between;
}

.workflow-step-main {
  min-width: 0;
}

.workflow-step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-right: 8px;
  color: #fff;
  background: #783887;
  border-radius: 50%;
  font-size: 12px;
}

.workflow-step-name {
  color: #303133;
  font-size: 13px;
  white-space: nowrap;
}

.workflow-step-count {
  margin-top: 2px;
  color: #909399;
  font-size: 11px;
}

.workflow-step-arrow {
  margin: 0 8px;
  color: #c0c4cc;
}

.linked-plan-header {
  margin-bottom: 10px;
}

.linked-plan-title {
  margin-right: 10px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.linked-plan-table {
  width: 100%;
}

.workflow-status-tag {
  cursor: pointer;
}

.time-line {
  line-height: 18px;
  font-size: 12px;
}

.muted {
  color: #909399;
}

.progress-dialog-content {
  padding: 0 8px;
}

.requirement-summary {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;
  gap: 10px;
  padding: 12px;
  margin-bottom: 24px;
  background: #f7f8fa;
  border-radius: 6px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.summary-item-wide span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-label {
  color: #909399;
  font-size: 12px;
}

.progress-note {
  padding: 10px 12px;
  margin-top: 24px;
  color: #606266;
  background: #f4f4f5;
  border-radius: 4px;
  font-size: 13px;
}

.progress-note i {
  margin-right: 6px;
  color: #909399;
}

.dialog-alert {
  margin-bottom: 18px;
}

.form-section-title {
  padding-left: 9px;
  margin: 4px 0 14px;
  color: #303133;
  border-left: 3px solid #783887;
  font-weight: 600;
}

@media screen and (max-width: 1366px) {
  .overview-cards {
    grid-template-columns: repeat(3, minmax(130px, 1fr));
  }

  .workflow-step-name {
    font-size: 12px;
  }

  .workbench-subtitle,
  .linked-plan-tip {
    display: none;
  }
}
</style>
