<template>
  <el-dialog class="test-plan-export"
             v-loading="loading"
             :title="$t('test_track.plan.export_test_plan')"
             :visible.sync="dialogVisible"
             @close="close">
    <div class="export-tip">
      {{ $t('test_track.plan.export_test_plan_tip') }}
    </div>

    <span slot="footer" class="dialog-footer">
    <el-button size="mini" @click="dialogVisible = false">{{ $t('commons.cancel') }}</el-button>
    <el-button type="primary" size="mini" @click="exportTestPlan">{{ $t('commons.export') }}</el-button>
  </span>
  </el-dialog>
</template>

<script>
import {listenGoBack, removeGoBackListener} from "metersphere-frontend/src/utils";

export default {
  name: "TestPlanExport",
  data() {
    return {
      dialogVisible: false,
      loading: false
    }
  },
  methods: {
    open() {
      listenGoBack(this.close);
      this.dialogVisible = true;
      this.loading = false;
    },
    close() {
      removeGoBackListener(this.close);
      this.dialogVisible = false;
      this.loading = false;
    },
    exportTestPlan() {
      this.close();
      this.$emit('export');
    }
  }
}
</script>

<style scoped>
.test-plan-export :deep(.el-dialog) {
  width: 500px;
}

.test-plan-export :deep(.el-dialog .el-dialog__title) {
  font-weight: bold;
}

.test-plan-export :deep(.el-dialog .el-dialog__body) {
  padding: 20px;
}

.export-tip {
  font-size: 14px;
  color: #606266;
}
</style>
