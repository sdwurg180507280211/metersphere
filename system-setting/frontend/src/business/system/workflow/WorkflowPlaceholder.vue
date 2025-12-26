<template>
  <div class="workflow-placeholder">
    <el-empty :description="description" />
  </div>
</template>

<script>
export default {
  name: 'WorkflowPlaceholder',
  props: {
    name: {
      type: String,
      default: '',
    },
  },
  computed: {
    description() {
      return `${this.$t('workflow.placeholder_tips')} (${this.name})`;
    },
  },
};
</script>

<style scoped>
.workflow-placeholder {
  padding: 16px;
}
</style>
