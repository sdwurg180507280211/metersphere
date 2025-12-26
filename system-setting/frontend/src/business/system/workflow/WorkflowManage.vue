<template>
  <div>
    <el-card>
      <template v-slot:header>
        <span>{{ $t('workflow.management') }}</span>
      </template>

      <el-tabs v-model="active" @tab-click="onTabClick">
        <el-tab-pane :label="$t('workflow.process_manage')" name="process"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.todo_task')" name="todo"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.done_task')" name="done"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.model_manage')" name="models"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.deployment_manage')" name="deployment"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.definition_manage')" name="definition"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.instance_manage')" name="instance"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.form_manage')" name="forms"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.process_design')" name="design"></el-tab-pane>
        <el-tab-pane :label="$t('workflow.form_design')" name="form-design"></el-tab-pane>
      </el-tabs>

      <workflow-process-manage v-if="active === 'process'" />
      <workflow-todo-task v-else-if="active === 'todo'" />
      <workflow-bpmn-designer v-else-if="active === 'design'" />
      <workflow-placeholder v-else :name="active" />
    </el-card>
  </div>
</template>

<script>
import WorkflowProcessManage from './WorkflowProcessManage';
import WorkflowTodoTask from './WorkflowTodoTask';
import WorkflowBpmnDesigner from './WorkflowBpmnDesigner';
import WorkflowPlaceholder from './WorkflowPlaceholder';

const TAB_TO_PATH = {
  process: '/setting/workflow/process',
  todo: '/setting/workflow/todo',
  done: '/setting/workflow/done',
  models: '/setting/workflow/models',
  deployment: '/setting/workflow/deployment',
  definition: '/setting/workflow/definition',
  instance: '/setting/workflow/instance',
  forms: '/setting/workflow/forms',
  design: '/setting/workflow/design',
  'form-design': '/setting/workflow/form-design',
};

export default {
  name: 'WorkflowManage',
  components: {
    WorkflowPlaceholder,
    WorkflowBpmnDesigner,
    WorkflowProcessManage,
    WorkflowTodoTask,
  },
  data() {
    return {
      active: 'process',
    };
  },
  created() {
    this.active = this.resolveActiveByPath(this.$route.path);
  },
  watch: {
    '$route.path'(val) {
      this.active = this.resolveActiveByPath(val);
    },
  },
  methods: {
    resolveActiveByPath(path) {
      if (path && path.indexOf('/setting/workflow/todo') === 0) return 'todo';
      if (path && path.indexOf('/setting/workflow/done') === 0) return 'done';
      if (path && path.indexOf('/setting/workflow/models') === 0) return 'models';
      if (path && path.indexOf('/setting/workflow/deployment') === 0) return 'deployment';
      if (path && path.indexOf('/setting/workflow/definition') === 0) return 'definition';
      if (path && path.indexOf('/setting/workflow/instance') === 0) return 'instance';
      if (path && path.indexOf('/setting/workflow/forms') === 0) return 'forms';
      if (path && path.indexOf('/setting/workflow/design') === 0) return 'design';
      if (path && path.indexOf('/setting/workflow/form-design') === 0) return 'form-design';
      return 'process';
    },
    onTabClick(tab) {
      const name = tab && tab.name;
      const path = TAB_TO_PATH[name] || TAB_TO_PATH.process;
      if (this.$route.path !== path) {
        this.$router.push(path);
      }
    },
  },
};
</script>

<style scoped>
</style>
