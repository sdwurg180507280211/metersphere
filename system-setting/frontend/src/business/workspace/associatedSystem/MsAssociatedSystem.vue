<template>
  <div v-loading="loading">
    <el-card class="table-card">
      <template v-slot:header>
        <div class="search-header-container">
          <div class="title">
            {{ $t('organization.associated_system') }}
          </div>
          <div class="search-container">
            <el-select v-model="searchType" style="width: 150px; margin-right: 10px;" size="small">
              <el-option :label="$t('commons.name')" value="name"></el-option>
              <el-option :label="$t('organization.associated_system_code')" value="description"></el-option>
            </el-select>
            <el-input
              v-model="searchContent"
              :placeholder="searchPlaceholder"
              style="width: 300px; margin-right: 10px;"
              size="small"
              clearable
              @clear="search"
              @keyup.enter.native="search">
              <i slot="suffix" class="el-input__icon el-icon-search" @click="search" style="cursor: pointer;"></i>
            </el-input>
            <ms-table-button
              v-permission="['WORKSPACE_PROJECT_MANAGER:READ+CREATE']"
              icon="el-icon-circle-plus-outline"
              :content="btnTips"
              @click="create"/>
          </div>
        </div>
      </template>
      <el-table border class="adjust-table" :data="items" style="width: 100%"
                @sort-change="sort"
                @filter-change="filter"
                :height="screenHeight"
      >
        <el-table-column prop="name" :label="$t('commons.name')" min-width="100" show-overflow-tooltip>
<!--          <template v-slot:default="scope">-->
<!--            <el-link type="primary" class="member-size" @click="jumpPage(scope.row)">-->
<!--              {{ scope.row.name }}-->
<!--            </el-link>-->
<!--          </template>-->
        </el-table-column>
        <el-table-column prop="description" :label="$t('organization.associated_system_code')" show-overflow-tooltip>
          <template v-slot:default="scope">
            <pre>{{ scope.row.description }}</pre>
          </template>
        </el-table-column>
        <el-table-column min-width="100"
                         sortable
                         prop="createTime"
                         :label="$t('commons.create_time')"
                         show-overflow-tooltip>
          <template v-slot:default="scope">
            <span>{{ scope.row.createTime | datetimeFormat }}</span>
          </template>
        </el-table-column>
        <el-table-column min-width="100"
                         sortable
                         prop="updateTime"
                         :label="$t('commons.update_time')"
                         show-overflow-tooltip>
          <template v-slot:default="scope">
            <span>{{ scope.row.updateTime | datetimeFormat }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('commons.operating')" width="180">
          <template v-slot:default="scope">
            <div>
              <ms-table-operator
                :edit-permission="['WORKSPACE_PROJECT_MANAGER:READ+EDIT']"
                :delete-permission="['WORKSPACE_PROJECT_MANAGER:READ+DELETE']"
                :show-delete="projectId !== scope.row.id"
                @editClick="edit(scope.row)"
                @deleteClick="handleDelete(scope.row)">
              </ms-table-operator>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <ms-table-pagination :change="list" :current-page.sync="currentPage" :page-size.sync="pageSize"
                           :total="total"/>
    </el-card>

    <edit-associated-system ref="EditAssociatedSystem"/>

    <ms-delete-confirm title="删除所属系统" @delete="_handleDelete" ref="deleteConfirm"/>

  </div>
</template>

<script>
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import MsTableOperator from "metersphere-frontend/src/components/MsTableOperator";
import MsDialogFooter from "metersphere-frontend/src/components/MsDialogFooter";
import {
  fullScreenLoading,
  operationConfirm,
  removeGoBackListener,
  stopFullScreenLoading
} from "metersphere-frontend/src/utils";
import {
  getCurrentProjectID,
  getCurrentUser,
  getCurrentUserId,
  getCurrentWorkspaceId
} from "metersphere-frontend/src/utils/token";
import MsDeleteConfirm from "metersphere-frontend/src/components/MsDeleteConfirm";
import MsTableOperatorButton from "metersphere-frontend/src/components/MsTableOperatorButton";
import {GROUP_PROJECT, PROJECT_ID} from "metersphere-frontend/src/utils/constants";
import MsTableButton from "metersphere-frontend/src/components/MsTableButton";
import {_filter, _sort} from "metersphere-frontend/src/utils/tableUtils";
import {PROJECT_MANAGE_CONFIGS} from "metersphere-frontend/src/components/search/search-components";
import MsRolesTag from "metersphere-frontend/src/components/MsRolesTag";
import MsInstructionsIcon from "metersphere-frontend/src/components/MsInstructionsIcon";
import AddMember from "../../common/AddMember";
import {isSuperUser} from "metersphere-frontend/src/api/user.js";
import {
  addProjectMember,
  delProjectMember,
  getCurrentWorkspaceMemberSpecial,
  getProjectMemberPages,
  updateCurrentUser
} from "../../../api/user";
// import {delProjectById, getProjectPages, modifyProjectMember} from "../../../api/project";
import {getAssociatedSystemPages,delAssociatedSystemById} from "../../../api/associated-system"
import {getProjectMemberGroup, getUserGroupList} from "../../../api/user-group";
import EditAssociatedSystem from "./EditAssociatedSystem.vue";
import ApiEnvironmentConfig from "metersphere-frontend/src/components/environment/ApiEnvironmentConfig";
import {switchProject} from "metersphere-frontend/src/api/project";



export default {
  name: "MsAssociatedSystem",
  components: {
    MsInstructionsIcon,
    MsTableButton,
    MsTableOperatorButton,
    MsDeleteConfirm,
    MsRolesTag,
    EditAssociatedSystem,
    MsTableOperator,
    MsTablePagination,
    MsDialogFooter,
    AddMember,
    ApiEnvironmentConfig
  },
  inject: [
    'reload',
    'reloadTopMenus'
  ],
  data() {
    return {
      updateVisible: false,
      dialogMemberVisible: false,
      loading: false,
      memberDialogLoading: false,
      memberTableLoading: false,
      btnTips: this.$t('organization.add_associated_system'),
      title: this.$t('organization.add_associated_system'),
      condition: {},
      searchType: 'name',
      searchContent: '',
      items: [],
      form: {},
      currentPage: 1,
      pageSize: 10,
      total: 0,
      userFilters: [],
      rules: {
        name: [
          {required: true, message: this.$t('organization.associated_system_input_name'), trigger: 'blur'},
          {min: 2, max: 60, message: this.$t('commons.input_limit', [2, 60]), trigger: 'blur'}
        ],
        description: [
          {max: 250, message: this.$t('commons.input_limit', [0, 250]), trigger: 'blur'}
        ],
      },
      screenHeight: 'calc(100vh - 155px)',
      dialogCondition: {},
      memberVisible: false,
      memberLineData: [],
      memberForm: {},
      dialogCurrentPage: 1,
      dialogPageSize: 5,
      dialogTotal: 0,
      currentProjectId: "",
      userList: [],
      labelWidth: '150px',
      rowProjectId: ""
    };
  },
  props: {
    baseUrl: {
      type: String
    }
  },
  // mounted() {
  //   if (this.$route.path.split('/')[2] === 'project' &&
  //     this.$route.path.split('/')[3] === 'create') {
  //     this.$router.replace('/setting/project/all');
  //     setTimeout(() => {
  //       this.create();
  //     }, 200)
  //   }
  //   this.list();
  //   this.getMaintainerOptions();
  // },
  activated() {
    this.list();
  },
  computed: {
    projectId() {
      return getCurrentProjectID();
    },
    workspaceId() {
      return getCurrentWorkspaceId();
    },
    searchPlaceholder() {
      if (this.searchType === 'name') {
        return "请输入系统名称";
      } else {
        return "请输入系统简称"
      }
    }
  },
  methods: {
    jumpPage(row) {
      this.currentWorkspaceRow = row;
      this.currentProjectId = row.id;
      let param = {
        projectId: row.id
      };
      this.loading = getProjectMemberPages(1, 10000, row.workspaceId, param).then(res => {
        let {listObject} = res.data;
        this.memberLineData = listObject;
        let arr = this.memberLineData.filter(item => item.id === getCurrentUserId());
        if (arr.length > 0) {
          this.doJump(row);
        } else {
          isSuperUser(getCurrentUserId()).then(r => {
            if (r && r.data) {
              this.doJump(row);
            } else {
              this.$warning(this.$t("commons.project_permission"));
            }
          });
        }
      });
    },
    doJump(row) {
      // 跳转的时候更新用户的last_project_id
      sessionStorage.setItem(PROJECT_ID, row.id);
      const loading = fullScreenLoading(this);
      switchProject({id: getCurrentUserId(), lastProjectId: row.id}).then(() => {
        this.$router.push('/track/home').then(() => {
          location.reload();
          stopFullScreenLoading(loading);
        });
      })
    },

    create() {
      let workspaceId = getCurrentWorkspaceId();
      if (!workspaceId) {
        this.$warning(this.$t('project.please_choose_workspace'));
        return false;
      }
      this.title = this.$t('organization.add_associated_system');
      // listenGoBack(this.handleClose);
      this.form = {};
      this.$refs.EditAssociatedSystem.edit();
    },
    edit(row) {
      this.$refs.EditAssociatedSystem.edit(row);
    },

    handleDelete(project) {
      this.$refs.deleteConfirm.open(project);
    },
    _handleDelete(project) {
      operationConfirm(this, this.$t('organization.associated_system_delete_tip'), () => {
        delAssociatedSystemById(project.id).then(() => {
          // if (project.id === getCurrentProjectID()) {
          //   localStorage.removeItem(PROJECT_ID);
          //   updateCurrentUser({id: getCurrentUser().id, lastProjectId: ''});
          // }
          this.$success(this.$t('commons.delete_success'));
          this.list();
        });
      }, () => {
        this.$info(this.$t('commons.delete_cancelled'));
      })
    },
    handleClose() {
      removeGoBackListener(this.handleClose);
    },
    search() {
      this.currentPage = 1;
      if (this.searchType === 'name') {
        this.condition.name = this.searchContent;
        delete this.condition.description;
      } else {
        this.condition.description = this.searchContent;
        delete this.condition.name;
      }
      this.list();
    },
    list() {
      this.condition.workspaceId = getCurrentWorkspaceId();
      this.loading = getAssociatedSystemPages(this.currentPage, this.pageSize, this.condition).then(res => {
        let data = res.data;
        let {listObject, itemCount} = data;
        this.items = listObject;
        this.total = itemCount;
        for (let i = 0; i < this.items.length; i++) {
          let param = {
            projectId: this.items[i].id
          };
          // getProjectMemberPages(1, 10000, this.condition.workspaceId, param).then(res => {
          //   let {listObject} = res.data
          //   this.$set(this.items[i], "memberSize", listObject.length);
          // });
        }
      });
    },
    sort(column) {
      _sort(column, this.condition);
      this.list();
    },
    filter(filters) {
      _filter(filters, this.condition);
      this.list();
    },

    close: function () {
      this.memberVisible = false;
      this.memberLineData = [];
      this.list();
    },


    open() {
      this.$refs.addMember.open();
    },

    querySearch(queryString, cb) {
      let userList = this.userList;
      let results = queryString ? userList.filter(this.createFilter(queryString)) : userList;
      // 调用 callback 返回建议列表的数据
      cb(results);
    },
    createFilter(queryString) {
      return (user) => {
        return (user.email.indexOf(queryString.toLowerCase()) === 0 || user.id.indexOf(queryString.toLowerCase()) === 0);
      };
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

.select-width {
  width: 100%;
}

.workspace-member-name {
  float: left;
}

.workspace-member-email {
  float: right;
  color: #8492a6;
  font-size: 13px;
}

.el-input, .el-textarea {
  width: 80%;
}

.search-header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-header-container .title {
  font-size: 16px;
  font-weight: 500;
}

.search-container {
  display: flex;
  align-items: center;
}
</style>
