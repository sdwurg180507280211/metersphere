<template>
  <ms-tag :type="meta.type" :content="meta.label"/>
</template>

<script>
import MsTag from "metersphere-frontend/src/components/MsTag";

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
      const explicitStage = plan.intStage || plan.requirementFlowStage || plan.workflowStage;
      if (explicitStage && STAGE_META[explicitStage]) {
        return STAGE_META[explicitStage];
      }

      // 兼容当前 master 已有数据：只映射能够从原测试计划状态明确判断的阶段。
      // “测试准备”必须由全流程平台审批结果或后续独立业务阶段字段明确给出，不做猜测。
      if (plan.status === "Cancelled") {
        return STAGE_META.CANCELLED;
      }
      if (plan.status === "Archived") {
        return STAGE_META.ARCHIVED;
      }
      if (plan.status === "Completed" || plan.status === "Finished") {
        return STAGE_META.DONE;
      }
      if (plan.status === "Underway") {
        return STAGE_META.TEST_EXECUTION;
      }
      if (plan.requirementApprovalStatus === "APPROVED") {
        return STAGE_META.TEST_PREPARATION;
      }
      return STAGE_META.TEST_PLAN;
    },
  },
};
</script>
