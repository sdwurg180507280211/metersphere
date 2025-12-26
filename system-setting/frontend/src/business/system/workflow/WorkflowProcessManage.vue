<template>
  <div>
    <el-card>
      <template v-slot:header>
        <span>{{ $t('workflow.process_manage') }}</span>
      </template>

      <el-form label-width="140px" size="small" @submit.native.prevent>
        <el-form-item :label="$t('workflow.deploy_bpmn')">
          <el-upload
            :show-file-list="false"
            action=""
            :http-request="handleDeploy"
          >
            <el-button type="primary" size="small">{{ $t('workflow.upload_and_deploy') }}</el-button>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="deployResult" :label="$t('workflow.deploy_result')">
          <el-input type="textarea" :rows="4" :value="JSON.stringify(deployResult, null, 2)" readonly />
        </el-form-item>

        <el-divider />

        <el-form-item :label="$t('workflow.process_definition_key')">
          <el-input v-model="startForm.processDefinitionKey" />
        </el-form-item>
        <el-form-item :label="$t('workflow.business_key')">
          <el-input v-model="startForm.businessKey" />
        </el-form-item>
        <el-form-item :label="$t('workflow.variables_json')">
          <el-input v-model="startForm.variablesJson" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="handleStart">{{ $t('workflow.start_process') }}</el-button>
        </el-form-item>

        <el-form-item v-if="startResult" :label="$t('workflow.start_result')">
          <el-input type="textarea" :rows="4" :value="JSON.stringify(startResult, null, 2)" readonly />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { deployProcess, startProcess } from '@/api/workflow';

export default {
  name: 'WorkflowProcessManage',
  data() {
    return {
      deployResult: null,
      startResult: null,
      startForm: {
        processDefinitionKey: '',
        businessKey: '',
        variablesJson: '{\n  "initiator": "admin"\n}',
      },
    };
  },
  methods: {
    async handleDeploy(option) {
      try {
        const res = await deployProcess(option.file, option.file && option.file.name);
        if (res && res.success) {
          this.deployResult = res.data;
          this.$success(this.$t('commons.save_success'));
        } else {
          this.$error((res && res.message) || 'deploy failed');
        }
        option.onSuccess();
      } catch (e) {
        this.$error((e && e.message) || 'deploy failed');
        option.onError(e);
      }
    },
    async handleStart() {
      let variables;
      try {
        variables = this.startForm.variablesJson ? JSON.parse(this.startForm.variablesJson) : undefined;
      } catch (e) {
        this.$error(this.$t('workflow.variables_json_invalid'));
        return;
      }

      const req = {
        processDefinitionKey: this.startForm.processDefinitionKey,
        businessKey: this.startForm.businessKey,
        variables,
      };

      const res = await startProcess(req);
      if (res && res.success) {
        this.startResult = res.data;
        this.$success(this.$t('commons.save_success'));
      } else {
        this.$error((res && res.message) || 'start failed');
      }
    },
  },
};
</script>

<style scoped>
</style>
