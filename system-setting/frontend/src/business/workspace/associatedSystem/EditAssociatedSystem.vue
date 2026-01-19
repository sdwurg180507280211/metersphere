<template>
  <div>
    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="createVisible" v-if="createVisible"
               @close="handleClose">
      <el-form v-loading="loading" :model="form" :rules="rules" ref="form" label-position="right" label-width="80px"
               size="small">
        <el-form-item :label-width="labelWidth" :label="$t('commons.name')" prop="name">
          <el-input v-model="form.name" autocomplete="off"></el-input>
        </el-form-item>


        <el-form-item :label-width="labelWidth" :label="$t('organization.associated_system_code')" prop="description">
<!--          <el-input :autosize="{ minRows: 2, maxRows: 4}" type="textarea" v-model="form.description"></el-input>-->
          <el-input v-model="form.description" autocomplete="off"></el-input>
        </el-form-item>



      </el-form>
      <template v-slot:footer>
        <div class="dialog-footer">
          <ms-dialog-footer
            @cancel="createVisible = false"
            @confirm="submit('form')"/>
        </div>
      </template>
    </el-dialog>

    <ms-delete-confirm :title="$t('project.delete')" @delete="_handleDelete" ref="deleteConfirm"/>
  </div>
</template>

<script>

import {listenGoBack, removeGoBackListener} from "metersphere-frontend/src/utils";
import {operationConfirm} from "metersphere-frontend/src/utils";
import {
  getCurrentProjectID,
  getCurrentUser,
  getCurrentUserId,
  getCurrentWorkspaceId
} from "metersphere-frontend/src/utils/token";
import {AZURE_DEVOPS, PROJECT_ID, TAPD, ZEN_TAO} from "metersphere-frontend/src/utils/constants";
import {PROJECT_CONFIGS} from "metersphere-frontend/src/components/search/search-components";
import MsInstructionsIcon from "metersphere-frontend/src/components/MsInstructionsIcon";

import MsTableButton from "metersphere-frontend/src/components/MsTableButton";
import MsTableOperatorButton from "metersphere-frontend/src/components/MsTableOperatorButton";
import MsDeleteConfirm from "metersphere-frontend/src/components/MsDeleteConfirm";
import MsTableOperator from "metersphere-frontend/src/components/MsTableOperator";
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import MsTableHeader from "metersphere-frontend/src/components/MsTableHeader";
import MsDialogFooter from "metersphere-frontend/src/components/MsDialogFooter";
import {ISSUE_PLATFORM_OPTION} from "metersphere-frontend/src/utils/table-constants";
import {
  getAllServiceIntegration,
  checkThirdPlatformProject,
  deleteProjectById,
  modifyProject,
  saveProject
} from "../../../api/project";
import {updateInfo} from "metersphere-frontend/src/api/user";
import {getPlatformOption, getPlatformProjectInfo, getThirdPartTemplateSupportPlatform} from "@/api/platform-plugin";
import ProjectPlatformConfig from "@/business/workspace/project/ProjectPlatformConfig";
import {createAssociatedSystem, updateAssociatedSystem} from "@/api/associated-system";

export default {
  name: "EditAssociatedSystem",
  components: {
    ProjectPlatformConfig,
    MsInstructionsIcon,
    MsTableButton,
    MsTableOperatorButton,
    MsDeleteConfirm,
    MsTableOperator,
    MsTablePagination,
    MsTableHeader,
    MsDialogFooter
  },
  data() {
    return {
      screenHeight: 'calc(100vh - 155px)',
      labelWidth: '150px',
      createVisible: false,
      loading: false,
      jiraResult: {
        loading: false
      },
      platformProjectConfigs: [],
      btnTips: this.$t('project.create'),
      title: this.$t('project.create'),
      condition: {components: PROJECT_CONFIGS},
      items: [],
      form: {},
      currentPage: 1,
      pageSize: 10,
      total: 0,
      userFilters: [],
      rules: {
        name: [
          {required: true, message: this.$t('project.input_name'), trigger: 'blur'},
          {min: 2, max: 250, message: this.$t('commons.input_limit', [2, 250]), trigger: 'blur'}
        ],
        description: [
          {max: 250, message: this.$t('commons.input_limit', [0, 250]), trigger: 'blur'}
        ],
      },
      platformOptions: [],
      issueTemplateId: "",
      ableEdit: true,
      platformConfig: {},
      thirdPartTemplateSupportPlatforms: []
    };
  },
  props: {
    baseUrl: {
      type: String
    },
    isShowApp: {
      type: Boolean,
      default: true
    }
  },
  // computed: {
  //   currentUser: () => {
  //     return getCurrentUser();
  //   },
  // },
  inject: ['reload'],
  destroyed() {
    this.createVisible = false;
  },
  methods: {
    showPlatform(platform) {
      return this.form.platform === platform
        && this.platformOptions.map(i => i.value).indexOf(platform) > -1;
    },
    check() {
      if (!this.form.id) {
        this.$warning(this.$t("test_track.issue.save_project_first"));
        return;
      }
      checkThirdPlatformProject(this.form).then(() => {
        this.$success(this.$t("system.check_third_project_success"));
      });
    },
    getOptions() {
      if (this.$refs.issueTemplate) {
        this.$refs.issueTemplate.getTemplateOptions();
      }
      if (this.$refs.caseTemplate) {
        this.$refs.caseTemplate.getTemplateOptions();
      }
      if (this.$refs.apiTemplate) {
        this.$refs.apiTemplate.getTemplateOptions();
      }
      getThirdPartTemplateSupportPlatform()
        .then((r) => {
          this.thirdPartTemplateSupportPlatforms = r.data;
        });
    },

    edit(row) {
      this.form = {};
      //this.getOptions();
      this.createVisible = true;
      listenGoBack(this.handleClose);
      if (row) {
        this.title = this.$t('project.edit');
        //this.platformConfig = row.issueConfig ? JSON.parse(row.issueConfig) : {};
        this.form = Object.assign({}, row);
        this.issueTemplateId = row.issueTemplateId;
      }

      //this.platformOptions = [];
      //this.platformOptions.push(...ISSUE_PLATFORM_OPTION);
      // getPlatformOption()
      //   .then((r) => {
      //     this.platformOptions.push(...r.data);
      //     this.loading = getAllServiceIntegration().then(res => {
      //       let data = res.data;
      //       let platforms = data.map(d => d.platform);
      //       this.filterPlatformOptions(platforms, TAPD);
      //       this.filterPlatformOptions(platforms, ZEN_TAO);
      //       this.filterPlatformOptions(platforms, AZURE_DEVOPS);
      //     }).catch(() => {
      //       this.ableEdit = false;
      //     })
      //   });
    },
    getPlatformProjectInfo() {
      getPlatformProjectInfo()
        .then((r) => {
          this.platformProjectConfigs = r.data;
        });
    },
    filterPlatformOptions(platforms, platform) {
      if (platforms.indexOf(platform) === -1) {
        for (let i = 0; i < this.platformOptions.length; i++) {
          if (this.platformOptions[i].value === platform) {
            this.platformOptions.splice(i, 1);
            break;
          }
        }
      }
    },
    submit(formName) {
      this.$refs[formName].validate((valid) => {
        if (!valid || !this.ableEdit) {
          return false;
        }
        let projectConfig = this.$refs.platformConfig;
        if (projectConfig) {
          projectConfig.validate()
            .then(() => {
              this.form.issueConfig = JSON.stringify(projectConfig.form);
              this.handleSave()
            });
        } else {
          this.handleSave();
        }
      });
    },
    handleSave() {
      // let protocol = document.location.protocol;
      // protocol = protocol.substring(0, protocol.indexOf(":"));
      // this.form.protocal = protocol;
      this.form.workspaceId = getCurrentWorkspaceId();
      this.form.createUser = getCurrentUserId();
      // if (this.issueTemplateId !== this.form.issueTemplateId) {
      //   // 更换缺陷模版移除字段
      //   localStorage.removeItem("ISSUE_LIST");
      // }

      if (this.form.id) {
        this.loading = updateAssociatedSystem(this.form).then(() => {
          this.createVisible = false;
          this.$success(this.$t('commons.save_success'));
          this.reload();
        });
      } else {
        this.loading = createAssociatedSystem(this.form).then(() => {
          this.createVisible = false;
          this.$success(this.$t('commons.save_success'));
          setTimeout(() => {
            location.reload();
          }, 1000);
        });
      }
    },
    handleDelete(project) {
      this.$refs.deleteConfirm.open(project);
    },
    _handleDelete(project) {
      operationConfirm(this, this.$t('project.delete_tip'), () => {
        deleteProjectById(project.id).then(() => {
          if (project.id === getCurrentProjectID()) {
            localStorage.removeItem(PROJECT_ID);
            updateInfo({id: getCurrentUser().id, lastProjectId: ''});
          }
          this.$success(this.$t('commons.delete_success'));
          this.list();
        });
      }, () => {
        this.$info(this.$t('commons.delete_cancelled'));
      })
    },
    handleClose() {
      removeGoBackListener(this.handleClose);
      this.createVisible = false;
    },
  },
  created() {
    document.addEventListener('keydown', this.handleEvent);
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.handleEvent);
  },
};
</script>

<style scoped>
pre {
  margin: 0 0;
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", Arial, sans-serif;
}

.el-input, .el-textarea {
  width: 80%;
}

.checkButton {
  margin-left: 5px;
}
</style>
