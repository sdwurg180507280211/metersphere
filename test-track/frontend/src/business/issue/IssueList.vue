<template>
  <ms-container>
    <ms-main-container>
      <el-card class="table-card">
        <template v-slot:header>
          <ms-table-header :create-permission="['PROJECT_TRACK_ISSUE:READ+CREATE']" :condition.sync="page.condition"
                           @search="search" @create="handleCreate"
                           :create-tip="$t('test_track.issue.create_issue')"
                           :tip="$t('commons.search_by_name_or_id')">
            <template v-slot:button>

              <span v-if="isThirdPart && hasPermission('PROJECT_TRACK_ISSUE:READ+CREATE')">
                <ms-table-button
                  v-if="hasLicense"
                  :disabled="syncDisable"
                  icon="el-icon-refresh"
                  :content="$t('test_track.issue.sync_bugs')"
                  @click="syncAllIssues"/>
                <ms-table-button
                  v-if="!hasLicense"
                  :disabled="syncDisable"
                  icon="el-icon-refresh"
                  :content="$t('test_track.issue.sync_bugs')"
                  @click="syncIssues"/>
              </span>


              <ms-table-button icon="el-icon-upload2" :content="$t('commons.import')"
                               v-if="hasPermission('PROJECT_TRACK_ISSUE:READ+CREATE')" @click="handleImport"/>
              <ms-table-button icon="el-icon-download" :content="$t('commons.export')"
                               v-if="hasPermission('PROJECT_TRACK_ISSUE:READ')" @click="handleExport"/>
            </template>
          </ms-table-header>
        </template>

        <ms-table
          v-loading="loading"
          row-key="id"
          :data="page.data"
          :condition="page.condition"
          :total="page.total"
          :page-size.sync="page.pageSize"
          :reserve-option="true"
          :page-refresh="pageRefresh"
          :operators="operators"
          :batch-operators="batchButtons"
          :screen-height="screenHeight"
          :remember-order="true"
          :fields.sync="fields"
          :field-key="tableHeaderKey"
          :custom-fields="issueTemplate.customFields"
          @headChange="handleHeadChange"
          @filter="search"
          @order="getIssues"
          @handlePageChange="handlePageChange"
          ref="table">

          <ms-table-column
            v-for="(item) in fields" :key="item.key"
            :label="item.label"
            :prop="item.id"
            :field="item"
            :sortable="item.sortable ? 'custom' : item.sortable"
            :min-width="item.minWidth"
            :column-key="item.columnKey"
            :width="item.width"
            :fields-width="fieldsWidth"
            :filters="item.filters"
            :show-header-tooltip="item.id === 'creatorName'"
          >
            <template v-slot="scope">

              <span v-if="item.id === 'platformStatus'">
                <span v-if="scope.row.platform === 'Tapd'">
                  {{ scope.row.platformStatus ? tapdIssueStatusMap[scope.row.platformStatus] : '--' }}
                </span>
                <span v-else-if="scope.row.platform ==='Local'">
                  {{ scope.row.platformStatus ? tapdIssueStatusMap[scope.row.platformStatus] : '--' }}
                </span>
                <span v-else-if="platformStatusMap && platformStatusMap.get(scope.row.platformStatus)">
                  {{ platformStatusMap.get(scope.row.platformStatus) }}
                </span>
                <span v-else>
                  {{ scope.row.platformStatus ? scope.row.platformStatus : '--' }}
                </span>
              </span>

              <ms-review-table-item-lazy
                v-else-if="item.id === 'description'"
                :data="scope.row"
                prop="description"/>

              <span v-else-if="item.id === 'resourceName'">
                 <el-link v-if="scope.row.resourceName"
                          @click="$router.push('/track/plan/view/' + scope.row.resourceId)">
                  {{ scope.row.resourceName }}
                </el-link>
                <span v-else>
                  --
                 </span>
              </span>

              <span v-else-if="item.id === 'createTime'">
                 {{ scope.row.createTime | datetimeFormat }}
              </span>

              <span v-else-if="item.id === 'updateTime'">
                 {{ scope.row.updateTime | datetimeFormat }}
              </span>

              <span v-else-if="item.id === 'caseCount'">
                 <el-link type="primary" class="member-size" @click="handleEdit(scope.row)">
                  {{ scope.row.caseCount }}
                </el-link>
              </span>

              <!-- 自定义字段 -->
              <span v-else-if="item.isCustom">
                <span v-if="item.type === 'richText' && scope.row.displayValueMap[item.id]">
                     <ms-review-table-item-lazy
                       :data="scope.row.displayValueMap" :prop="item.id"/>
                </span>
                <!-- 状态字段：显示可流转的下拉菜单 -->
                <span v-else-if="item.id === '状态' && !isThirdPart && hasPermission('PROJECT_TRACK_ISSUE:READ+EDIT')">
                  <el-dropdown
                    class="test-case-status"
                    @command="statusChange"
                    @visible-change="(visible) => onStatusDropdownVisibleChange(visible, scope.row)"
                    placement="bottom"
                    trigger="click"
                  >
                    <span class="el-dropdown-link" :class="{'cursor-pointer': true}">
                      {{
                        scope.row.displayValueMap[item.id]
                          ? scope.row.displayValueMap[item.id]
                          : issueStatusMap[scope.row.status]
                      }}
                      <i class="el-icon-arrow-down el-icon--right"></i>
                    </span>
                    <el-dropdown-menu slot="dropdown">
                      <template v-if="availableTransitionsLoading[scope.row.id]">
                        <el-dropdown-item disabled>
                          加载中...
                        </el-dropdown-item>
                      </template>
                      <template v-else>
                        <el-dropdown-item
                          v-for="(statusOption, index) in getAvailableTransitionsForIssue(scope.row)"
                          :key="index"
                          :command="{ id: scope.row.id, status: statusOption.value }"
                        >
                          {{ statusOption.system ? $t(statusOption.text) : statusOption.text }}
                        </el-dropdown-item>
                        <el-dropdown-item
                          v-if="availableTransitionsCache[scope.row.id] && !getAvailableTransitionsForIssue(scope.row).length"
                          disabled
                        >
                          {{ $t('test_track.issue.no_available_transitions') }}
                        </el-dropdown-item>
                      </template>
                    </el-dropdown-menu>
                  </el-dropdown>
                </span>
                <span v-else>
                  {{ scope.row.displayValueMap[item.id] }}
                </span>
              </span>

              <span v-else>
                {{ scope.row[item.id] }}
              </span>

            </template>
          </ms-table-column>

        </ms-table>

        <ms-table-pagination
          :change-current="handlePageChange"
          :change-size="handlePageSizeChange"
          :current-page.sync="page.currentPage"
          :page-size.sync="page.pageSize"
          :total="page.total"
        />
        <issue-edit @refresh="getIssues" ref="issueEdit"/>
        <issue-sync-select @syncConfirm="syncConfirm" ref="issueSyncSelect"/>
        <issue-import @refresh="getIssues" ref="issueImport"/>
        <issue-export @export="exportIssue" ref="issueExport"/>
      </el-card>
    </ms-main-container>
  </ms-container>
</template>

<script>
import Vue from "vue";
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import MsTableOperators from "metersphere-frontend/src/components/MsTableOperators";
import MsTableButton from "metersphere-frontend/src/components/MsTableButton";
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import {
  ISSUE_PLATFORM_OPTION,
  ISSUE_STATUS_MAP,
  SYSTEM_FIELD_NAME_MAP,
  TAPD_ISSUE_STATUS_MAP
} from "metersphere-frontend/src/utils/table-constants";
import MsTableHeader from "metersphere-frontend/src/components/MsTableHeader";
import IssueDescriptionTableItem from "@/business/issue/IssueDescriptionTableItem";
import IssueEdit from "@/business/issue/IssueEdit";
import IssueSyncSelect from "@/business/issue/IssueSyncSelect";
import IssueImport from "@/business/issue/components/import/IssueImport";
import IssueExport from "@/business/issue/components/export/IssueExport";
import {
  batchDeleteIssue,
  checkSyncIssues,
  deleteIssue,
  getIssuePartTemplateWithProject,
  getIssues,
  getIssuesById,
  getPlatformOption,
  getPlatformStatus,
  issueStatusChange,
  syncAllIssues,
  syncIssues
} from "@/api/issue";
import { getAvailableTransitions } from "@/api/issue";
import {
  getCustomFieldFilter,
  getCustomFieldValue,
  getCustomTableWidth,
  getLastTableSortField,
  getPageInfo,
  getTableHeaderWithCustomFields,
  parseCustomFilesForItem,
  parseCustomFilesForList
} from "metersphere-frontend/src/utils/tableUtils";
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsMainContainer from "metersphere-frontend/src/components/MsMainContainer";
import {getCurrentProjectID, getCurrentUserId, getCurrentWorkspaceId} from "metersphere-frontend/src/utils/token";
import {hasLicense, hasPermission} from "metersphere-frontend/src/utils/permission";
import {getProjectMember, getProjectMemberUserFilter} from "@/api/user";
import {getUserGroupProject} from "@/api/user-group";
import {LOCAL} from "metersphere-frontend/src/utils/constants";
import {TEST_TRACK_ISSUE_LIST, WORKSPACE, PROJECT} from "metersphere-frontend/src/components/search/search-components";
import {generateColumnKey, getAdvSearchCustomField} from "metersphere-frontend/src/components/search/custom-component";
import MsMarkDownText from "metersphere-frontend/src/components/MsMarkDownText";
import MsReviewTableItem from "@/business/issue/MsReviewTableItem";
import MsReviewTableItemLazy from "@/business/issue/MsReviewTableItemLazy";
import {setIssuePlatformComponent} from "@/business/issue/issue";

export default {
  name: "IssueList",
  components: {
    MsReviewTableItem,
    MsReviewTableItemLazy,
    MsMarkDownText,
    MsMainContainer,
    MsContainer,
    IssueEdit,
    IssueDescriptionTableItem,
    IssueSyncSelect,
    IssueImport,
    IssueExport,
    MsTableHeader,
    MsTablePagination, MsTableButton, MsTableOperators, MsTableColumn, MsTable
  },
  data() {
    return {
      page: getPageInfo({
        components: [...TEST_TRACK_ISSUE_LIST, WORKSPACE, PROJECT],  // 添加工作空间和项目筛选
        custom: false,
      }),
      fields: [],
      customFields: [], // 通过表头过滤后的自定义字段列表
      tableHeaderKey: "ISSUE_LIST",
      fieldsWidth: getCustomTableWidth('ISSUE_LIST'),
      screenHeight: 'calc(100vh - 160px)',
      operators: [
        {
          tip: this.$t('commons.edit'), icon: "el-icon-edit",
          exec: this.handleEdit,
          permissions: ['PROJECT_TRACK_ISSUE:READ+EDIT']
        }, {
          tip: this.$t('commons.copy'), icon: "el-icon-copy-document", type: "success",
          exec: this.handleCopy,
          isDisable: this.btnDisable,
          permissions: ['PROJECT_TRACK_ISSUE:READ+DELETE']
        }, {
          tip: this.$t('commons.delete'), icon: "el-icon-delete", type: "danger",
          exec: this.handleDelete,
          permissions: ['PROJECT_TRACK_ISSUE:READ+DELETE']
        }
      ],
      batchButtons: [
        {
          name: this.$t('test_track.issue.batch_delete_issue'),
          handleClick: this.handleBatchDelete,
          permissions: ['PROJECT_TRACK_ISSUE:READ+DELETE']
        }
      ],
      issueTemplate: {},
      members: [],
      userFilter: [],
      isThirdPart: false,
      creatorFilters: [],
      loading: false,
      // MsTable 的 pageRefresh：
      // - true：表示这是“翻页触发的数据刷新”，让 MsTable 不要在 data 变化时清空跨页勾选
      // - false：表示“搜索/筛选/排序”等刷新，此时应清空勾选（沿用 MsTable 默认 clear 行为）
      pageRefresh: false,
      availableTransitionsCache: {}, // 缓存每个缺陷的可流转状态
      availableTransitionsLoading: {}, // 缓存每个缺陷的可流转状态请求中标记，防止重复请求
      dataSelectRange: "",
      platformOptions: [],
      platformStatus: [],
      platformStatusMap: new Map(),
      associatedSystemMap: new Map(),
      hasLicense: false,
      syncDisable: false,
      // 用户组权限过滤相关
      currentUserGroupId: null, // 当前用户在项目中的用户组ID
      userGroupFilterKeys: [], // 记录施加的过滤条件key，用于搜索时清除
      columns: {
        num: {
          sortable: true,
          minWidth: 100
        },
        title: {
          sortable: true,
          minWidth: 120,
        },
        platform: {
          minWidth: 80,
          filters: this.platformFilters
        },
        platformStatus: {
          sortable: true,
          minWidth: 110,
          type: 'select',
          filters: this.getPlatformStatusFiltes(),
        },
        creatorName: {
          columnKey: 'creator',
          minWidth: 100,
          filters: this.creatorFilters
        },
        resourceName: {},
        createTime: {
          sortable: true,
          minWidth: 180
        },
        updateTime: {
          sortable: true,
          minWidth: 180
        },
        caseCount: {}
      }
    };
  },
  watch: {
    '$route'(to, from) {
      window.removeEventListener("resize", this.tableDoLayout);
    }
  },
  activated() {
    if (this.$route.params.dataSelectRange) {
      this.dataSelectRange = this.$route.params.dataSelectRange;
    } else {
      this.dataSelectRange = "";
    }
    this.loading = true;

    // 我在做：重置用户组过滤相关状态
    // 目的是：每次进入页面都重新获取用户组并施加权限过滤
    // 如果不这样做：从其他页面返回时不会重新施加权限过滤
    this.currentUserGroupId = null;
    this.userGroupFilterKeys = [];

    this.$nextTick(() => {
      // 解决错位问题
      window.addEventListener('resize', this.tableDoLayout);

      // 加载所属系统数据
      this.$get('/associatedSystem/list/all').then((response) => {
        if (response.data) {
          response.data.forEach((item) => {
            this.associatedSystemMap.set(item.id, item.name);
          });
        }
      }).catch(() => {
        // 忽略错误，系统可能没有所属系统数据
      });

      // 我在做：按顺序执行4个步骤来初始化页面数据
      // 目的是：确保数据加载的正确顺序，避免依赖关系错误
      // 如果不这样做：可能会出现数据未加载完成就使用的情况
      
      // 步骤1：获取用户组（1次HTTP请求，~50-200ms）
      // 目的：判断当前用户属于哪个用户组（developer/tester），用于后续的权限过滤
      getUserGroupProject(getCurrentProjectID(), getCurrentUserId())
        .then((response) => {
          // 后端返回的是字符串，直接就是用户组ID（如 'developer', 'tester'）
          this.currentUserGroupId = response.data;
        })
        .catch(() => {
          // 获取失败时不施加权限过滤
          this.currentUserGroupId = null;
        })
        .finally(() => {
          // 步骤2：加载成员列表（1次HTTP请求，~50-200ms）
          // 目的：获取项目成员信息，用于成员字段的显示和过滤
          getProjectMember()
            .then((response) => {
              this.members = response.data;
              this.userFilter = response.data.map(u => {
                return {text: u.name, value: u.id};
              });
              
              // 步骤3：加载缺陷模板（1次HTTP请求，~50-200ms）
              // 目的：获取当前项目的缺陷模板，包含所有自定义字段定义
              getIssuePartTemplateWithProject((template) => {
                this.initFields(template);
                
                // 步骤4：施加用户组过滤（纯内存操作，< 1ms）
                // 目的：根据用户组在 page.condition.filters 中设置过滤条件
                // developer: 设置"处理人"过滤条件
                // tester: 设置"创建人"过滤条件
                this.applyUserGroupFilter();
                
                // 步骤5：加载缺陷数据（1次HTTP请求，~100-500ms）
                // 目的：根据过滤条件从后端获取缺陷列表数据
                // 此时 page.condition.filters 中已经包含了用户组权限过滤条件
                this.getIssues();
              }, () => {
                this.loading = false;
              });
            });
        });
    });

    getPlatformOption()
      .then((r) => {
        this.platformOptions = r.data;
        setIssuePlatformComponent(this.platformOptions, this.page.condition.components);
      });

    this.hasLicense = hasLicense();

    getPlatformStatus({
      projectId: getCurrentProjectID(),
      workspaceId: getCurrentWorkspaceId()
    }).then((r) => {
      this.platformStatus = r.data;
      this.platformStatusMap = new Map();
      if (this.platformStatus) {
        this.platformStatus.forEach(item => {
          this.platformStatusMap.set(item.value, item.label);
        });
        this.page.condition.components.forEach(item => {
          if (item.key === 'platformStatus') {
            item.options =[];
            this.platformStatus.forEach(option => {
              item.options.push({label: option.label, value: option.value});
            });
          }
        });
      }
    });
  },
  computed: {
    platformFilters() {
      let options = [...ISSUE_PLATFORM_OPTION];
      options.push(...this.platformOptions);
      return options;
    },
    issueStatusMap() {
      return ISSUE_STATUS_MAP;
    },
    tapdIssueStatusMap() {
      return TAPD_ISSUE_STATUS_MAP;
    },
    systemNameMap() {
      return SYSTEM_FIELD_NAME_MAP;
    },
    projectId() {
      return getCurrentProjectID();
    },
    workspaceId() {
      return getCurrentWorkspaceId();
    }
  },
  created() {
    this.getMaintainerOptions();
    //跳转
    this.editParam();
  },
  methods: {
    generateColumnKey,
    hasPermission,
    tableDoLayout() {
      if (this.$refs.table) this.$refs.table.doLayout();
    },
    getPlatformStatusFiltes() {
      let options = [];
      getPlatformStatus({
        projectId: getCurrentProjectID(),
        workspaceId: getCurrentWorkspaceId()
      }).then((r) => {
        this.platformStatus = r.data;
        if (this.platformStatus) {
          this.platformStatus.forEach(item => {
            options.push({"text":item.label,"value":item.value,"system": false});
          });
          return options;
        }
        return options;
      });
      return options;
    },
    getCustomFieldValue(row, field, defaultVal) {
      let value = getCustomFieldValue(row, field, this.members);

      // 处理缺陷所属系统字段，将ID转换为名称
      if (field.name === '缺陷所属系统') {
        if (value) {
          if (Array.isArray(value)) {
            // 多选所属系统
            return value.map(id => this.associatedSystemMap.get(id) || id).join(', ');
          } else {
            // 单选所属系统
            return this.associatedSystemMap.get(value) || value;
          }
        }
      }

      return value ? value : defaultVal;
    },
    getCustomFieldFilter(field) {
      return getCustomFieldFilter(field, this.userFilter);
    },
    initFields(template) {
      if (template.platform === LOCAL) {
        this.isThirdPart = false;
      } else {
        this.isThirdPart = true;
      }
      let fields = getTableHeaderWithCustomFields('ISSUE_LIST', template.customFields, this.members);
      if (!this.isThirdPart) {
        for (let i = 0; i < fields.length; i++) {
          if (fields[i].id === 'platformStatus') {
            fields.splice(i, 1);
            break;
          }
        }
        // 如果不是三方平台则移除备选字段中的平台状态
        let removeField = {id: 'platformStatus', name: 'platformStatus', remove: true};
        template.customFields.push(removeField);
        for (let i = 0; i < this.page.condition.components.length; i++) {
          if (this.page.condition.components[i].key === 'platformStatus') {
            this.page.condition.components.splice(i, 1);
            break;
          }
        }

      }
      this.issueTemplate = template;
      fields.forEach(item => {
        if (this.columns[item.id]) {
          Object.assign(item, this.columns[item.id]);
          if (this.columns[item.id].filters) {
            item.filters = this.columns[item.id].filters;
          }
        }
      });

      this.fields = fields;

      // 过滤自定义字段
      this.page.condition.components = this.page.condition.components.filter(item => item.custom !== true);
      let comp = getAdvSearchCustomField(this.page.condition, template.customFields);
      this.page.condition.components.push(...comp);

      this.initCustomFieldValue();

      if (this.$refs.table) this.$refs.table.reloadTable();
    },
    search() {
      // 我在做：清除用户组权限过滤条件
      // 目的是：用户进行搜索/筛选/重置时，应该能看到所有符合条件的缺陷
      // 如果不这样做：搜索/筛选仍会施加权限过滤，用户无法搜索到其他人的缺陷
      this.clearUserGroupFilter();

      // 添加搜索条件时，当前页设置成第一页
      this.page.currentPage = 1;
      this.pageRefresh = false;
      this.getIssues();
    },
    /**
     * 我在做：处理分页翻页事件，并标记 pageRefresh。
     * 目的是：让 MsTable 在翻页加载时不清空跨页勾选。
     * 如果不这样做，就无法实现：翻页后仍保留之前页的选中状态。
     */
    handlePageChange() {
      this.pageRefresh = true;
      this.getIssues();
    },
    /**
     * 我在做：处理分页大小变更。
     * 目的是：页大小变化属于“重新刷新列表”，按统一体验应清空跨页勾选，并回到第一页。
     * 如果不这样做，就无法实现：页大小变化后勾选状态不混乱且分页正确。
     */
    handlePageSizeChange() {
      this.page.currentPage = 1;
      this.pageRefresh = false;
      this.getIssues();
    },
    handleHeadChange() {
      this.initFields(this.issueTemplate);
    },
    /**
     * 我在做：加载缺陷列表数据
     * 目的是：使用 page.condition.filters 中的过滤条件（包括用户组权限过滤）
     * 如果不这样做：无法实现用户组权限过滤
     */
    getIssues() {
      this.loading = true;

      if (this.dataSelectRange === 'thisWeekUnClosedIssue') {
        this.page.condition.thisWeekUnClosedTestPlanIssue = true;
      } else if (this.dataSelectRange === 'unClosedRelatedTestPlan') {
        this.page.condition.unClosedTestPlanIssue = true;
      } else if (this.dataSelectRange === 'AllRelatedTestPlan') {
        this.page.condition.allTestPlanIssue = true;
      } else {
        delete this.page.condition['thisWeekUnClosedTestPlanIssue'];
        delete this.page.condition['unClosedTestPlanIssue'];
        delete this.page.condition['allTestPlanIssue'];
      }
      this.page.condition.projectId = this.projectId;
      this.page.condition.workspaceId = this.workspaceId;
      this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);
      getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
        .then((response) => {
          this.page.total = response.data.itemCount;
          this.page.data = response.data.listObject;
          parseCustomFilesForList(this.page.data);
          this.initCustomFieldValue();
          if (this.pageRefresh) {
            this.$nextTick(() => {
              this.pageRefresh = false;
            });
          }
          this.loading = false;
        });
    },
    initCustomFieldValue() {
      if (this.fields.length <= 0) {
        return;
      }
      this.page.data.forEach(item => {
        let displayValueMap = {};
        let fieldIdSet = new Set(this.fields.map(i => i.id));
        this.issueTemplate.customFields.forEach(field => {
          let displayValue;
          if (!fieldIdSet.has(field.name)) {
            return;
          }
          if (field.name === '状态') {
            displayValue = this.getCustomFieldValue(item, field, this.issueStatusMap[item.status]);
          } else {
            displayValue = this.getCustomFieldValue(item, field);
          }
          displayValueMap[field.name] = displayValue;
        });
        item.displayValueMap = displayValueMap;
      });
      this.loading = false;
    },
    getMaintainerOptions() {
      getProjectMemberUserFilter((data) => {
        this.creatorFilters = data;
      });
    },
    handleEdit(data) {
      this.$refs.issueEdit.open(data, 'edit');
    },
    handleCreate() {
      this.$refs.issueEdit.open(null, 'add');
    },
    handleCopy(data) {
      let copyData = {};
      Object.assign(copyData, data)
      copyData.copyIssueId = copyData.id
      copyData.id = null;
      copyData.name = data.name + '_copy';
      this.$refs.issueEdit.open(copyData, 'copy');
    },
    handleDelete(data) {
      let tip = this.$t('test_track.issue.delete_tip') + ' ' + data.title + " ？";
      if (this.isThirdPart) {
        tip = this.$t('test_track.issue.delete_third_part_tip') + ", " + tip;
      }
      this.$alert(tip, '', {
        confirmButtonText: this.$t('commons.confirm'),
        callback: (action) => {
          if (action === 'confirm') {
            this._handleDelete(data);
          }
        }
      });
    },
    _handleDelete(data) {
      deleteIssue(data.id).then(() => {
        this.$success(this.$t('commons.delete_success'));
        this.getIssues();
      })
    },
    handleBatchDelete() {
      let tip = this.$t('test_track.issue.batch_delete_tip') + " ？";
      if (this.isThirdPart) {
        tip = this.$t('test_track.issue.delete_third_part_tip') + ", " + tip;
      }
      this.$alert(tip, '', {
        confirmButtonText: this.$t('commons.confirm'),
        callback: (action) => {
          if (action === 'confirm') {
            this._handleBatchDelete();
          }
        }
      });
    },
    _handleBatchDelete() {
      let selectIds = this.$refs.table.selectIds;
      if (selectIds.length == 0) {
        this.$warning(this.$t("test_track.issue.check_select"));
        return;
      }
      // 我在做：批量删除时把“全选范围”一并传给后端（而不是只传 selectAll=true）。
      // 目的是：MsTable 的 selectAll=true 代表“选择当前筛选结果的全集（跨页）”，后端必须用同样的筛选条件计算全集 Q(...)，再扣除 unSelectIds。
      // 如果不这样做，就会出现：筛选后全选批量删除误删项目/工作空间全量数据（后端无法得知 Q(...) 的范围，只能按全量处理）。
      batchDeleteIssue({
        "batchDeleteIds": selectIds,
        "batchDeleteAll": this.page.condition.selectAll,
        "unSelectIds": this.page.condition.unSelectIds,
        "moduleIds": this.page.condition.moduleIds,
        "nodeIds": this.page.condition.nodeIds,
        "filters": this.page.condition.filters,
        "combine": this.page.condition.combine,
        "name": this.page.condition.name,
      })
        .then(() => {
          this.$success(this.$t('commons.delete_success'));
          this.getIssues();
        })
    },
    btnDisable(row) {
      if (this.issueTemplate.platform !== row.platform) {
        return true;
      }
      return false;
    },
    syncAllIssues() {
      this.$refs.issueSyncSelect.open();
    },
    handleImport() {
      this.$refs.issueImport.open();
    },
    handleExport() {
      let exportIds = this.$refs.table.selectIds;
      if (exportIds.length == 0) {
        this.$warning(this.$t("test_track.issue.check_select"));
        return;
      }
      this.$refs.issueExport.open();
    },
    exportIssue(data) {
      let param = {
        "projectId": getCurrentProjectID(),
        "workspaceId": getCurrentWorkspaceId(),
        "userId": getCurrentUserId(),
        "isSelectAll": this.page.condition.selectAll,
        "exportIds": this.$refs.table.selectIds,
        "exportFields": data,
        "orders": getLastTableSortField(this.tableHeaderKey),
        "combine": this.page.condition.combine,
        "name": this.page.condition.name
      }
      this.$fileDownloadPost("/issues/export", param);
    },
    syncConfirm(data) {
      this.loading = true;
      this.syncDisable = true;
      let param = {
        "projectId": getCurrentProjectID(),
        "createTime": data.createTime.getTime(),
        "pre": data.preValue
      }
      syncAllIssues(param)
        .then(() => {
          this.repeatCheckSyncRes();
        }).catch(() => {
        this.resetSyncParam();
      });
    },
    syncIssues() {
      this.loading = true;
      this.syncDisable = true;
      syncIssues()
        .then(() => {
          this.repeatCheckSyncRes();
        }).catch(() => {
        this.resetSyncParam();
      });
    },
    repeatCheckSyncRes() {
      checkSyncIssues(this.loading, false, (errorData) => {
        this.loading = false;
        this.syncDisable = false;
        if (errorData.syncResult && errorData.syncResult !== '') {
          this.$error(errorData.syncResult, false);
        } else {
          this.$success(this.$t('test_track.issue.sync_complete'), false);
          this.getIssues();
        }
      });
    },
    resetSyncParam() {
      this.loading = false;
      this.syncDisable = false;
    },
    /**
     * 状态变更
     */
    statusChange(param) {
      issueStatusChange(param).then(() => {
        this.getIssues();
        this.$success(this.$t("commons.modify_success"), false);
        // 清除缓存
        if (this.availableTransitionsCache[param.id]) {
          delete this.availableTransitionsCache[param.id];
        }
      }).catch(() => {
        // 清除缓存以便重新获取
        if (this.availableTransitionsCache[param.id]) {
          delete this.availableTransitionsCache[param.id];
        }
      });
    },
    /**
     * 获取缺陷可流转的状态列表
     */
    getAvailableTransitionsForIssue(issue) {
      if (!issue || !issue.id) {
        return [];
      }

      // 懒加载：这里仅返回缓存，不在渲染期触发接口请求
      return this.availableTransitionsCache[issue.id] || [];
    },
    /**
     * 我在做：在用户真正展开“状态下拉”时，再去请求可流转状态。
     * 目的是：把缺陷列表页的状态流转列表改为懒加载，避免列表渲染时触发大量接口请求。
     * 如果不这样做，就无法实现：列表首屏不展开下拉也会请求每行缺陷的可流转状态。
     */
    onStatusDropdownVisibleChange(visible, issue) {
      if (!visible) {
        return;
      }
      this.loadAvailableTransitionsForIssue(issue);
    },
    /**
     * 我在做：发起获取“可流转状态”的请求，并把结果写入 cache。
     * 目的是：配合 onStatusDropdownVisibleChange，实现状态下拉的真正懒加载。
     * 如果不这样做，就无法实现：下拉展开时无法动态拉取后端返回的可流转状态列表。
     */
    loadAvailableTransitionsForIssue(issue) {
      if (!issue || !issue.id) {
        return;
      }

      // 已加载过（即便是空数组）则不重复请求
      if (this.availableTransitionsCache[issue.id]) {
        return;
      }

      // 请求中不重复发起
      if (this.availableTransitionsLoading[issue.id]) {
        return;
      }

      // 从模板中获取状态字段的所有选项
      let statusField = null;
      if (this.issueTemplate && this.issueTemplate.customFields) {
        statusField = this.issueTemplate.customFields.find(field => field.name === '状态');
      }

      if (!statusField || !statusField.options) {
        // 没有状态字段时，直接缓存空数组，避免反复触发
        this.$set(this.availableTransitionsCache, issue.id, []);
        return;
      }

      this.$set(this.availableTransitionsLoading, issue.id, true);
      getAvailableTransitions(issue.id).then((response) => {
        const availableStatusValues = response.data || [];
        const availableOptions = statusField.options.filter(option =>
          availableStatusValues.includes(option.value)
        );
        this.$set(this.availableTransitionsCache, issue.id, availableOptions);
        Vue.delete(this.availableTransitionsLoading, issue.id);
      }).catch(() => {
        // 获取失败时沿用原逻辑：降级为模板全部选项
        this.$set(this.availableTransitionsCache, issue.id, statusField.options || []);
        Vue.delete(this.availableTransitionsLoading, issue.id);
      });
    },
    editParam() {
      let id = this.$route.query.id;
      if (id) {
        getIssuesById(id).then((response) => {
          response.data.fields.forEach(field => {
            parseCustomFilesForItem(field);
          });
          this.handleEdit(response.data)
        });
      } else {
        let type = this.$route.query.type;
        if (type === 'create') {
          this.$nextTick(() => {
            this.handleCreate()
          });
        }
      }
    },
    /**
     * 我在做：根据用户组在 page.condition.filters 中设置过滤条件
     * 目的是：
     *   1. 开发人员组（developer）：设置处理人过滤条件（自定义字段）
     *   2. 测试人员组（tester）：设置创建人过滤条件
     *   3. 利用现有的高级搜索过滤机制，不需要修改后端SQL
     * 如果不这样做：无法实现用户组权限过滤
     */
    applyUserGroupFilter() {
      // 如果用户组不是 developer 或 tester，不施加过滤
      if (!this.currentUserGroupId || (this.currentUserGroupId !== 'developer' && this.currentUserGroupId !== 'tester')) {
        return;
      }

      // 确保 filters 对象存在
      if (!this.page.condition.filters) {
        this.page.condition.filters = {};
      }

      const currentUserId = getCurrentUserId();

      if (this.currentUserGroupId === 'developer') {
        // 我在做：为开发人员组设置"处理人"过滤条件
        // 目的是：开发人员只能看到处理人是自己的缺陷
        // 如果不这样做：开发人员会看到所有缺陷

        // 从模板中查找"处理人"字段
        const handlerField = this.issueTemplate.customFields.find(f => f.name === '处理人');
        if (handlerField) {
          // 我在做：使用 generateColumnKey 函数生成过滤条件的key
          // 目的是：确保key格式与后端SQL中的格式一致（custom_single-{fieldId}）
          // 如果不这样做：手动拼接可能出错，且不符合系统的统一规范
          const filterKey = generateColumnKey(handlerField);
          this.page.condition.filters[filterKey] = [currentUserId];
          this.userGroupFilterKeys.push(filterKey);
        }
      } else if (this.currentUserGroupId === 'tester') {
        // 我在做：为测试人员组设置"创建人"过滤条件
        // 目的是：测试人员只能看到创建人是自己的缺陷
        // 如果不这样做：测试人员会看到所有缺陷

        const filterKey = 'creator';
        this.page.condition.filters[filterKey] = [currentUserId];
        this.userGroupFilterKeys.push(filterKey);
      }
    },
    /**
     * 我在做：清除用户组权限过滤条件
     * 目的是：用户进行搜索/筛选/重置时，应该能看到所有符合条件的缺陷
     * 如果不这样做：搜索/筛选仍会施加权限过滤，用户无法搜索到其他人的缺陷
     */
    clearUserGroupFilter() {
      if (!this.page.condition.filters) {
        return;
      }

      // 清除之前记录的过滤条件
      this.userGroupFilterKeys.forEach(key => {
        delete this.page.condition.filters[key];
      });
      this.userGroupFilterKeys = [];
    }
  }
};
</script>

<style scoped>
.table-page {
  padding-top: 20px;
  margin-right: -9px;
  float: right;
}

.el-table {
  cursor: pointer;
}

:deep(.el-table) {
  overflow: auto;
}

span.operate-button button {
  margin-left: 10px;
}
</style>
