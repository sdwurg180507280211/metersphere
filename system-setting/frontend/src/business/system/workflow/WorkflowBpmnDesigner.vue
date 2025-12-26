<template>
  <div class="workflow-bpmn-designer">
    <div class="toolbar">
      <el-button size="small" @click="createNew">{{ $t('workflow.designer_new') }}</el-button>

      <el-upload
        class="import-upload"
        action=""
        :show-file-list="false"
        :http-request="handleImport"
      >
        <el-button size="small">{{ $t('workflow.designer_import') }}</el-button>
      </el-upload>

      <el-divider direction="vertical" />

      <div class="spacer"></div>

      <el-input
        v-model="deployFileName"
        size="small"
        class="file-name"
        :placeholder="$t('workflow.designer_file_name')"
      />

      <el-button size="small" @click="exportXml">{{ $t('workflow.designer_export') }}</el-button>
      <el-button type="primary" size="small" :loading="deploying" @click="deploy">{{ $t('workflow.designer_deploy') }}</el-button>
    </div>

    <div class="body">
      <bpmn-modeler
        ref="bpmnModeler"
        :xml="xml"
        :users="users"
        :groups="groups"
        :categorys="categorys"
        :is-view="false"
        @save="handleSave"
      />
    </div>

    <el-dialog :title="$t('workflow.designer_xml')" :visible.sync="xmlDialog.visible" width="860px" destroy-on-close>
      <el-input type="textarea" :rows="18" v-model="xmlDialog.xml" />
      <template v-slot:footer>
        <el-button size="small" @click="xmlDialog.visible = false">{{ $t('commons.cancel') }}</el-button>
        <el-button type="primary" size="small" @click="downloadXml">{{ $t('workflow.designer_download') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import bpmnModeler from 'workflow-bpmn-modeler';
import { deployProcessXml } from '@/api/workflow';

const EMPTY_DIAGRAM = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  targetNamespace="http://metersphere.io/bpmn">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="120" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

export default {
  name: 'WorkflowBpmnDesigner',
  components: {
    bpmnModeler,
  },
  data() {
    return {
      xml: EMPTY_DIAGRAM,
      users: [],
      groups: [],
      categorys: [],
      deploying: false,
      deployFileName: 'process.bpmn20.xml',
      xmlDialog: {
        visible: false,
        xml: '',
      },
    };
  },
  mounted() {
  },
  beforeDestroy() {
  },
  methods: {
    getLatestData() {
      if (this.$refs.bpmnModeler && this.$refs.bpmnModeler.save) {
        return this.$refs.bpmnModeler.save();
      }
      return {
        xml: this.xml,
        svg: '',
        process: null,
      };
    },
    handleSave(data) {
      if (data && data.xml) {
        this.xml = data.xml;
      }
    },
    async createNew() {
      this.xml = EMPTY_DIAGRAM;
    },
    async handleImport(option) {
      try {
        const text = await this.readFileAsText(option.file);
        this.xml = text;
        option.onSuccess();
      } catch (e) {
        option.onError(e);
      }
    },
    readFileAsText(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsText(file);
      });
    },
    async exportXml() {
      try {
        const data = this.getLatestData();
        this.xmlDialog.xml = data && data.xml ? data.xml : '';
        this.xmlDialog.visible = true;
      } catch (e) {
        this.$error((e && e.message) || this.$t('workflow.designer_export_failed'));
      }
    },
    downloadXml() {
      const xml = this.xmlDialog.xml || '';
      const blob = new Blob([xml], { type: 'application/xml' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = this.deployFileName || 'process.bpmn20.xml';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    },
    async deploy() {
      this.deploying = true;
      try {
        const data = this.getLatestData();
        const xml = data && data.xml ? data.xml : '';
        const name = this.deployFileName || 'process.bpmn20.xml';
        const res = await deployProcessXml(xml, name);
        if (res && res.success) {
          this.$success(this.$t('commons.save_success'));
        } else {
          this.$error((res && res.message) || this.$t('workflow.designer_deploy_failed'));
        }
      } catch (e) {
        this.$error((e && e.message) || this.$t('workflow.designer_deploy_failed'));
      } finally {
        this.deploying = false;
      }
    },
  },
};
</script>

<style scoped>
.workflow-bpmn-designer {
  height: calc(100vh - 260px);
  min-height: 560px;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.spacer {
  flex: 1;
}

.file-name {
  width: 260px;
}

.import-upload {
  display: inline-block;
}

.body {
  flex: 1;
  display: flex;
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
</style>
