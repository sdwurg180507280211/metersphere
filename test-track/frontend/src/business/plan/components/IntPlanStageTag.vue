<template>
  <div class="int-stage-cell">
    <ms-tag :type="meta.type" :content="meta.label"/>
    <span v-if="waitingApproval" class="approval-note">等待审批</span>
    <el-tooltip
        v-else-if="approvalRejected"
        :content="plan.requirementApprovalComment || '审批已驳回，请修改计划后重新提交'"
        placement="top"
    >
      <el-button
          v-if="hasEditPermission"
          type="text"
          size="mini"
          class="approval-action"
          @click.stop="submitApproval"
      >重新提交审批</el-button>
    </el-tooltip>
    <el-button
        v-else-if="canSubmitApproval"
        type="text"
        size="mini"
        class="approval-action"
        @click.stop="submitApproval"
    >提交审批</el-button>
  </div>
</template>

<script>
import MsTag from "metersphere-frontend/src/components/MsTag";
import {hasPermission} from "metersphere-frontend/src/utils/permission";
import {submitRequirementPlanApproval} from "@/api/requirement-plan-workflow";

const STAGE_META = {
  RECEIVED: {label: "需求已接收", type: "info"},
  TEST_PLAN: {label: "测试计划", type: "primary"},
  PREPARATION: {label: "测试准备", type: "warning"},
  TEST_PREPARATION: {label: "测试准备", type: "warning"},
  EXECUTION: {label: "测试执行", type: "primary"},
  TEST_EXECUTION: {label: "测试执行", type: "primary"},
  DONE: {label: "办结", type: "success"},
  COMPLETED: {label: "办结", type: "success"},
  CANCELLED: {label: "已取消", type: "danger"},
  ARCHIVED: {label: "已归档", type: "info"},
};

export default {
  name: "IntPlanStageTag",
  components: {MsTag},
  props: {
    plan: {
      type: Object,
      required: true,
    },
  },
  computed: {
    meta() {
      const plan = this.plan || {};

      if (plan.status === "Cancelled") {
        return STAGE_META.CANCELLED;
      }
      if (plan.status === "Archived") {
        return STAGE_META.ARCHIVED;
      }

      const explicitStage = plan.intStage || plan.requirementFlowStage || plan.workflowStage;
      if (explicitStage && STAGE_META[explicitStage]) {
        return STAGE_META[explicitStage];
      }

      if (plan.status === "Underway") {
        return STAGE_META.TEST_EXECUTION;
      }
      if (plan.status === "Completed" || plan.status === "Finished") {
        return STAGE_META.DONE;
      }
      if (plan.requirementApprovalStatus === "APPROVED") {
        return STAGE_META.TEST_PREPARATION;
      }
      return STAGE_META.TEST_PLAN;
    },
    hasEditPermission() {
      return hasPermission("PROJECT_TRACK_PLAN:READ+EDIT");
    },
    isPlanStage() {
      return this.meta.label === "测试计划";
    },
    waitingApproval() {
      return this.isPlanStage && this.plan.requirementApprovalStatus === "SUBMITTED";
    },
    approvalRejected() {
      return this.isPlanStage && this.plan.requirementApprovalStatus === "REJECTED";
    },
    canSubmitApproval() {
      const approvalStatus = this.plan.requirementApprovalStatus;
      return this.hasEditPermission
          && this.isPlanStage
          && !!this.plan.requirementNumber
          && (!approvalStatus || approvalStatus === "NONE");
    },
  },
  methods: {
    submitApproval() {
      this.$confirm(
          "确认将当前测试计划提交到全流程平台审批？",
          "提交审批",
          {
            confirmButtonText: "提交",
            cancelButtonText: "取消",
            type: "warning",
          }
      ).then(() => {
        submitRequirementPlanApproval(this.plan.id).then(() => {
          this.$set(this.plan, "requirementApprovalStatus", "SUBMITTED");
          this.$set(this.plan, "requirementApprovalComment", null);
          this.$success("测试计划已提交审批");
        });
      }).catch(() => {});
    },
  },
};
</script>

<style scoped>
.int-stage-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.approval-note {
  color: #909399;
  font-size: 12px;
}

.approval-action {
  padding: 0;
  font-size: 12px;
}
</style>
