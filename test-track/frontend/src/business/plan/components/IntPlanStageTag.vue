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

      // 取消和归档是明确的终止状态，优先于业务阶段显示。
      if (plan.status === "Cancelled") {
        return STAGE_META.CANCELLED;
      }
      if (plan.status === "Archived") {
        return STAGE_META.ARCHIVED;
      }

      // 全流程平台驱动的明确业务阶段优先，不能再被旧执行状态覆盖。
      const explicitStage = plan.intStage || plan.requirementFlowStage || plan.workflowStage;
      if (explicitStage && STAGE_META[explicitStage]) {
        return STAGE_META[explicitStage];
      }

      // 兼容历史数据：只有没有明确INT阶段时，才用原执行状态兜底。
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
  },
};
</script>
