<template>
  <div>
    <el-card>
      <template v-slot:header>
        <span>{{ $t('workflow.todo_task') }}</span>
      </template>

      <el-form inline size="small" @submit.native.prevent>
        <el-form-item :label="$t('workflow.assignee')">
          <el-input v-model="query.assignee" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="loadTasks">{{ $t('commons.search') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table border :data="tasks" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" :label="$t('commons.id')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="name" :label="$t('commons.name')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="assignee" :label="$t('workflow.assignee')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="processInstanceId" :label="$t('workflow.process_instance_id')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="$t('commons.operating')" min-width="120">
          <template v-slot:default="scope">
            <el-button type="primary" size="mini" @click="openComplete(scope.row)">{{ $t('workflow.complete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog :title="$t('workflow.complete_task')" :visible.sync="completeDialog.visible" width="720px" destroy-on-close>
        <el-form label-width="140px" size="small" @submit.native.prevent>
          <el-form-item :label="$t('workflow.task_id')">
            <el-input :value="completeDialog.taskId" readonly />
          </el-form-item>
          <el-form-item :label="$t('workflow.variables_json')">
            <el-input v-model="completeDialog.variablesJson" type="textarea" :rows="8" />
          </el-form-item>
        </el-form>
        <template v-slot:footer>
          <el-button size="small" @click="completeDialog.visible = false">{{ $t('commons.cancel') }}</el-button>
          <el-button type="primary" size="small" @click="submitComplete">{{ $t('commons.confirm') }}</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { completeTask, queryTasks } from '@/api/workflow';

export default {
  name: 'WorkflowTodoTask',
  data() {
    return {
      loading: false,
      tasks: [],
      query: {
        assignee: '',
      },
      completeDialog: {
        visible: false,
        taskId: '',
        variablesJson: '{\n  "approved": true\n}',
      },
    };
  },
  methods: {
    async loadTasks() {
      this.loading = true;
      try {
        const res = await queryTasks({ assignee: this.query.assignee });
        if (res && res.success) {
          this.tasks = res.data || [];
        } else {
          this.$error((res && res.message) || 'query failed');
        }
      } finally {
        this.loading = false;
      }
    },
    openComplete(task) {
      this.completeDialog.taskId = task.id;
      this.completeDialog.visible = true;
    },
    async submitComplete() {
      let variables;
      try {
        variables = this.completeDialog.variablesJson
          ? JSON.parse(this.completeDialog.variablesJson)
          : undefined;
      } catch (e) {
        this.$error(this.$t('workflow.variables_json_invalid'));
        return;
      }

      const res = await completeTask(this.completeDialog.taskId, { variables });
      if (res && res.success) {
        this.$success(this.$t('commons.save_success'));
        this.completeDialog.visible = false;
        await this.loadTasks();
      } else {
        this.$error((res && res.message) || 'complete failed');
      }
    },
  },
};
</script>

<style scoped>
</style>
