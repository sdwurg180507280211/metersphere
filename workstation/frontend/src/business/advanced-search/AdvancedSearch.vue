<template>
  <div class="advanced-search-page">
    <div class="main-card">
    <!-- 顶部筛选栏 -->
    <div class="top-filter-bar">
      <!-- 查询模式切换 -->
      <el-radio-group v-model="store.queryMode" size="small" style="margin-right: 8px;" @change="onQueryModeChange">
        <el-radio-button label="visual">
          <i class="el-icon-view"></i> {{ $t('advanced_search.mode_visual') }}
        </el-radio-button>
        <el-radio-button label="jql">
          <i class="el-icon-edit-outline"></i> {{ $t('advanced_search.mode_jql') }}
        </el-radio-button>
      </el-radio-group>

      <div class="vertical-divider"></div>

      <!-- 业务模块选择 -->
      <el-select
        v-model="store.currentModule"
        :placeholder="$t('advanced_search.select_module')"
        style="width: 140px;"
        @change="onModuleChange"
      >
        <el-option value="TEST_CASE" :label="$t('advanced_search.module_test_case')" />
        <el-option value="ISSUE" :label="$t('advanced_search.module_issue')" />
        <el-option value="TEST_PLAN" :label="$t('advanced_search.module_test_plan')" />
        <el-option value="TEST_CASE_REVIEW" :label="$t('advanced_search.module_review')" />
      </el-select>

      <!-- 工作空间和项目多选 -->
      <div class="scope-selectors">
        <el-select
          v-model="store.selectedWorkspaces"
          multiple
          collapse-tags
          :placeholder="$t('advanced_search.select_workspace')"
          style="width: 180px;"
          @change="onWorkspaceChange"
        >
          <el-option v-for="ws in store.workspaces" :key="ws.id" :label="ws.name" :value="ws.id" />
        </el-select>

        <el-select
          v-model="store.selectedProjects"
          multiple
          collapse-tags
          :disabled="store.selectedWorkspaces.length === 0"
          :placeholder="store.selectedWorkspaces.length === 0 ? $t('advanced_search.please_select_workspace_first') : $t('advanced_search.select_project')"
          style="width: 180px;"
          @change="onProjectChange"
        >
          <el-option v-for="proj in store.projects" :key="proj.id" :value="proj.id" :label="proj.name" />
        </el-select>
      </div>

      <!-- 筛选条件按钮 (仅可视化模式) -->
      <el-popover
        v-if="store.queryMode === 'visual'"
        placement="bottom-start"
        width="450"
        trigger="click"
        v-model="popoverVisible"
      >
        <div class="filter-popover-content">
          <div class="filter-groups-container">
            <!-- 基础信息 -->
            <div class="field-group" v-if="fieldsByGroup.basic.length > 0">
              <div class="field-group-title">📋 {{ $t('advanced_search.basic_info') }}</div>
              <div class="field-group-items">
                <el-tag
                  v-for="field in fieldsByGroup.basic"
                  :key="field.field || field.value"
                  size="small"
                  class="field-tag"
                  :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                  @click="addFilter(field)"
                >
                  <i :class="field.icon || 'el-icon-document'"></i> {{ field.label || field.name }}
                </el-tag>
              </div>
            </div>

            <!-- 审计追踪 (人员与时间) -->
            <div class="field-group" v-if="fieldsByGroup.audit.length > 0">
              <div class="field-group-title">👤 {{ $t('advanced_search.audit_trail') }}</div>
              <div class="field-group-items">
                <el-tag
                  v-for="field in fieldsByGroup.audit"
                  :key="field.field || field.value"
                  size="small"
                  class="field-tag"
                  :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                  @click="addFilter(field)"
                >
                  <i :class="field.icon || 'el-icon-time'"></i> {{ field.label || field.name }}
                </el-tag>
              </div>
            </div>

            <!-- 模块专属 -->
            <div class="field-group" v-if="fieldsByGroup.module.length > 0">
              <div class="field-group-title">🔧 {{ currentModuleLabel }} {{ $t('advanced_search.basic_attributes') }}</div>
              <div class="field-group-items">
                <el-tag
                  v-for="field in fieldsByGroup.module"
                  :key="field.field || field.value"
                  size="small"
                  class="field-tag"
                  :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                  @click="addFilter(field)"
                  :disabled="field.projectSpecific && !store.isSingleProjectMode"
                >
                  <i :class="field.icon || 'el-icon-setting'"></i> {{ field.label || field.name }}
                  <span v-if="field.projectSpecific" style="color:#e6a23c; margin-left:2px;">*</span>
                </el-tag>
              </div>
            </div>

            <!-- 自定义字段 -->
            <div class="field-group" v-if="store.isSingleProjectMode && fieldsByGroup.custom.length > 0">
              <div class="field-group-title">📝 {{ $t('advanced_search.custom_fields') }}</div>
              <div class="field-group-items">
                <el-tag
                  v-for="field in fieldsByGroup.custom"
                  :key="field.field || field.value"
                  size="small"
                  class="field-tag"
                  :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                  @click="addFilter(field)"
                >
                  {{ field.label || field.name }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        <div slot="reference" class="inline-add-filter-btn">
          <i class="el-icon-plus"></i> {{ $t('advanced_search.add_filter') }}
        </div>
      </el-popover>

      <div class="toolbar-actions">
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleSearch" :loading="store.loading">
          {{ $t('advanced_search.search') }}
        </el-button>
        <el-button v-if="store.queryMode === 'visual' && store.conditions.length > 0" size="small" icon="el-icon-star-off" @click="saveVisualQuery">
          {{ $t('advanced_search.jql_save') }}
        </el-button>
        <el-button v-if="store.queryMode === 'visual'" size="small" icon="el-icon-refresh" @click="clearConditions">
          {{ $t('advanced_search.clear_all') }}
        </el-button>
      </div>
    </div>

      <!-- 跨项目模式提示 -->
      <div class="cross-project-warning" v-if="!store.isSingleProjectMode && store.selectedProjects.length > 0">
        <i class="el-icon-warning"></i>
        <span>{{ $t('advanced_search.cross_project_tip') }}</span>
      </div>
    </div>

    <!-- JQL 模式 -->
    <div v-if="store.queryMode === 'jql'" class="query-condition-area">
      <jql-editor v-model="store.jqlQuery" @execute="handleSearch" />
    </div>

    <!-- 活跃条件标签栏（仅可视化模式） -->
    <div v-else-if="store.conditions.length > 0" class="active-tags-bar">
      <div class="active-tags-header">
        <i class="el-icon-finished"></i>
        <span>{{ $t('advanced_search.add_filter') }} ({{ store.conditions.length }})</span>
      </div>
      <div class="tags-container">
        <div v-for="(cond, idx) in store.conditions" :key="idx" class="condition-tag">
          <span class="condition-field-label">{{ getFieldLabel(cond.field) }}</span>

          <!-- 操作符选择 -->
          <el-select v-model="cond.op" size="mini" class="op-selector">
            <el-option v-for="o in getOperators(cond.field)" :key="o.v" :label="o.l" :value="o.v" />
          </el-select>

          <!-- 根据字段类型渲染不同输入控件 -->
          <div class="cond-value-input">
            <!-- 数字类型 -->
            <el-input-number
              v-if="getFieldType(cond.field) === 'number'"
              v-model="cond.value"
              size="mini"
              controls-position="right"
              style="width: 100%"
            />

            <!-- 日期类型 -->
            <el-date-picker
              v-else-if="getFieldType(cond.field) === 'date' || getFieldType(cond.field) === 'datetime'"
              v-model="cond.value"
              :type="cond.op === 'between' ? 'daterange' : 'date'"
              size="mini"
              style="width: 100%"
              range-separator="-"
              :start-placeholder="$t('advanced_search.start_date')"
              :end-placeholder="$t('advanced_search.end_date')"
              :value-format="getFieldType(cond.field) === 'date' ? 'yyyy-MM-dd' : 'yyyy-MM-dd HH:mm:ss'"
            />

            <!-- 下拉选择类型 -->
            <el-select
              v-else-if="getFieldType(cond.field) === 'select'"
              v-model="cond.value"
              size="mini"
              style="width: 100%"
              :placeholder="$t('advanced_search.select_value')"
              :multiple="['in', 'not_in'].includes(cond.op)"
              collapse-tags
            >
              <el-option 
                v-for="opt in getFieldOptions(cond.field)" 
                :key="opt.value || opt" 
                :label="opt.label || opt" 
                :value="opt.value || opt" 
              />
            </el-select>

            <!-- 树形选择 (如模块) -->
            <ms-node-tree-select
              v-else-if="getFieldType(cond.field) === 'treeSelect'"
              v-model="cond.value"
              size="mini"
              style="width: 100%"
              :module="store.currentModule"
              :project-ids="store.selectedProjects"
            />

            <!-- 用户类型 -->
            <el-select
              v-else-if="getFieldType(cond.field) === 'user'"
              v-model="cond.value"
              size="mini"
              style="width: 100%"
              :placeholder="$t('advanced_search.select_users')"
              multiple
              collapse-tags
              filterable
              remote
              :remote-method="searchUsers"
              :loading="userLoading"
            >
              <el-option key="CURRENT_USER" :label="$t('advanced_search.current_user')" value="CURRENT_USER">
                <div class="user-option">
                  <div class="user-avatar" style="background: #409eff;">⭐</div>
                  <div class="user-info">
                    <span class="user-name">{{ $t('advanced_search.current_user') }}</span>
                  </div>
                </div>
              </el-option>
              <el-option v-for="user in userList" :key="user.id" :label="user.name" :value="user.id">
                <div class="user-option">
                  <div class="user-avatar">{{ (user.name || '-').charAt(0) }}</div>
                  <div class="user-info">
                    <span class="user-name">{{ user.name }}</span>
                    <span class="user-email">{{ user.email }}</span>
                  </div>
                </div>
              </el-option>
            </el-select>

            <!-- 文本类型 -->
            <el-input v-else v-model="cond.value" size="mini" :placeholder="$t('advanced_search.enter_value')" />
          </div>

          <div class="remove-cond-btn" @click="removeCondition(idx)">
            <i class="el-icon-close"></i>
          </div>
        </div>
      </div>
      <el-button type="text" size="mini" class="clear-all-btn" @click="clearConditions">
        {{ $t('advanced_search.clear_all') }}
      </el-button>
    </div>

    <!-- 结果工具栏 -->
    <div class="result-toolbar">
      <div style="display: flex; align-items: center; gap: 8px;">
        <el-radio-group v-model="store.viewMode" size="small" @change="onViewModeChange">
          <el-radio-button label="list">
            <i class="el-icon-tickets"></i> {{ $t('advanced_search.view_list') }}
          </el-radio-button>
          <el-radio-button label="split">
            <i class="el-icon-s-cooperation"></i> {{ $t('advanced_search.view_split') }}
          </el-radio-button>
        </el-radio-group>
        <span style="color: #909399; font-size: 12px; margin-left: 10px;">
          {{ $t('advanced_search.result_count', { count: store.pagination.total }) }}
        </span>
      </div>

      <div style="display: flex; align-items: center; gap: 10px;">
        <!-- 列配置 -->
        <el-popover placement="bottom-end" width="450" trigger="click" @show="syncVisibleColumns">
          <div style="max-height: 400px; overflow-y: auto;">
            <div class="col-config-group">
              <div class="col-config-title">{{ $t('advanced_search.basic_info') }}</div>
              <el-checkbox-group v-model="store.userSelectedColumns" class="col-config-checkboxes">
                <el-checkbox v-for="c in groupedColumns.basic" :label="c.prop" :key="c.prop">{{ c.label }}</el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="col-config-group">
              <div class="col-config-title">{{ currentModuleLabel }}专属</div>
              <el-checkbox-group v-model="store.userSelectedColumns" class="col-config-checkboxes">
                <el-checkbox v-for="c in groupedColumns.module" :label="c.prop" :key="c.prop">{{ c.label }}</el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="col-config-group" style="margin-bottom:0">
              <div class="col-config-title">{{ $t('advanced_search.audit_trail') }}</div>
              <el-checkbox-group v-model="store.userSelectedColumns" class="col-config-checkboxes">
                <el-checkbox v-for="c in groupedColumns.system" :label="c.prop" :key="c.prop">{{ c.label }}</el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
          <el-button slot="reference" icon="el-icon-setting" size="small" circle :title="$t('advanced_search.column_config')"></el-button>
        </el-popover>
        <el-button icon="el-icon-download" size="small" @click="handleExport">{{ $t('advanced_search.export') }}</el-button>
      </div>
    </div>

    <!-- 视图容器 -->
    <div class="view-container">
      <!-- 列表视图 -->
      <div v-if="store.viewMode === 'list'" class="list-view-wrap">
        <el-table
          :data="store.results"
          style="width: 100%"
          height="100%"
          stripe
          border
          size="medium"
          highlight-current-row
          v-loading="store.loading"
          element-loading-text="正在查询数据..."
          element-loading-spinner="el-icon-loading"
          element-loading-background="rgba(255, 255, 255, 0.8)"
          @row-click="handleRowClick"
        >
          <el-table-column type="index" width="50" align="center" fixed="left"></el-table-column>
          <el-table-column
            v-for="c in activeTableColumns"
            :key="c.prop"
            :prop="c.prop"
            :label="c.label"
            :min-width="c.width || 120"
            show-overflow-tooltip
            :sortable="c.sortable ? 'custom' : false"
          >
            <template slot-scope="scope">
              <span
                v-if="c.prop === 'name' || c.prop === 'title'"
                style="font-weight: 500; color: #303133; cursor: pointer;"
                @click.stop="showDetail(scope.row)"
              >
                {{ scope.row[c.prop] }}
              </span>
              <el-tag
                v-else-if="c.prop === 'status'"
                size="mini"
                :type="getStatusType(scope.row[c.prop])"
                effect="dark"
                style="border-radius: 2px;"
              >
                {{ scope.row[c.prop] }}
              </el-tag>
              <el-tag
                v-else-if="c.prop === 'priority'"
                size="mini"
                :type="getPriorityType(scope.row[c.prop])"
                effect="plain"
              >
                {{ scope.row[c.prop] }}
              </el-tag>
              <el-tag
                v-else-if="c.prop === 'severity'"
                size="mini"
                :type="getSeverityType(scope.row[c.prop])"
                effect="dark"
              >
                {{ scope.row[c.prop] }}
              </el-tag>
              <div
                v-else-if="['creator', 'maintainer', 'assignee', 'principal', 'reviewer', 'createUserName'].includes(c.prop)"
                style="display: flex; align-items: center; gap: 6px;"
              >
                <div class="user-avatar" style="width: 20px; height: 20px; font-size: 10px;">
                  {{ (scope.row[c.prop] || '-').charAt(0) }}
                </div>
                <span>{{ scope.row[c.prop] || '-' }}</span>
              </div>
              <span v-else>{{ scope.row[c.prop] || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrap">
          <el-pagination
            :current-page="store.pagination.current"
            :page-size="store.pagination.pageSize"
            :total="store.pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <!-- 分屏详情视图 -->
      <div v-else class="split-layout">
        <div class="split-left">
          <div
            v-for="item in store.results"
            :key="item.id"
            class="split-item"
            :class="{active: store.selectedRow && store.selectedRow.id === item.id}"
            @click="store.selectedRow = item"
          >
            <div class="split-item-title">{{ item.name || item.title }}</div>
            <div class="split-item-meta">
              <span>{{ item.num || item.id }}</span>
              <el-tag size="mini" :type="getStatusType(item.status)" effect="plain" style="border:none; padding:0;">
                {{ item.status }}
              </el-tag>
            </div>
            <div style="font-size: 11px; color: #aaa; margin-top: 4px;">
              <i class="el-icon-user"></i> {{ item.createUserName || item.creator }} · {{ formatTime(item.updateTime || item.update_time) }}
            </div>
          </div>
        </div>

        <!-- 右侧详情面板 -->
        <div class="split-right" v-if="store.selectedRow">
          <div class="detail-header">
            <div class="detail-top-nav">
              <span class="detail-breadcrumb">
                {{ store.selectedRow.workspaceName || '-' }} / {{ store.selectedRow.projectName || '-' }} / {{ store.selectedRow.num || store.selectedRow.id }}
              </span>
              <div class="detail-actions">
                <el-button size="mini" icon="el-icon-document-copy" @click="copyId(store.selectedRow.num || store.selectedRow.id)">
                  {{ $t('advanced_search.detail_copy_id') }}
                </el-button>
                <el-button type="primary" size="mini" icon="el-icon-full-screen" @click="openFullDetail(store.selectedRow)">
                  {{ $t('advanced_search.detail_open_in_new_tab') }}
                </el-button>
              </div>
            </div>
            <div class="detail-title">
              <el-tag size="medium" :type="getModuleTagType()" effect="dark" class="module-tag">
                {{ currentModuleShortLabel }}
              </el-tag>
              {{ store.selectedRow.name || store.selectedRow.title }}
            </div>
            <div class="detail-meta-row">
              <div class="meta-item">
                <i class="el-icon-user"></i> 
                <span class="meta-label">{{ $t('commons.create_user') }}:</span>
                <span class="meta-value">{{ store.selectedRow.createUserName || store.selectedRow.creator }}</span>
              </div>
              <div class="meta-item">
                <i class="el-icon-date"></i>
                <span class="meta-label">{{ $t('commons.create_time') }}:</span>
                <span class="meta-value">{{ formatTime(store.selectedRow.createTime || store.selectedRow.create_time) || '-' }}</span>
              </div>
              <div class="meta-item">
                <i class="el-icon-time"></i>
                <span class="meta-label">{{ $t('advanced_search.updated_at') }}:</span>
                <span class="meta-value">{{ formatTime(store.selectedRow.updateTime || store.selectedRow.update_time) }}</span>
              </div>
            </div>
          </div>

          <div class="detail-content">
            <div class="detail-section">
              <div class="detail-section-title">
                <i class="el-icon-info"></i> {{ $t('advanced_search.basic_attributes') }}
              </div>
              <div class="detail-field-grid">
                <div class="detail-field-item" v-for="f in currentModuleFields" :key="f.field || f.value">
                  <span class="detail-label">{{ f.label || f.name }}</span>
                  <div class="detail-value">
                    <el-tag v-if="(f.field || f.value) === 'status'" size="small" :type="getStatusType(store.selectedRow[f.field || f.value])" effect="plain">
                      {{ store.selectedRow[f.field || f.value] }}
                    </el-tag>
                    <el-tag v-else-if="(f.field || f.value) === 'priority'" size="small" :type="getPriorityType(store.selectedRow[f.field || f.value])" effect="dark">
                      {{ store.selectedRow[f.field || f.value] }}
                    </el-tag>
                    <el-tag v-else-if="(f.field || f.value) === 'severity'" size="small" :type="getSeverityType(store.selectedRow[f.field || f.value])" effect="dark">
                      {{ store.selectedRow[f.field || f.value] }}
                    </el-tag>
                    <span v-else>{{ store.selectedRow[f.field || f.value] || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store/advancedSearch';
import JQLEditor from './JQLEditor.vue';
import { formatTime } from '@/utils/format';

export default {
  name: 'AdvancedSearch',
  
  components: {
    JQLEditor
  },
  
  data() {
    return {
      store: useAdvancedSearchStore(),
      popoverVisible: false,
      userLoading: false,
      userList: []
    };
  },
  
  computed: {
    fieldsByGroup() {
      const fields = Array.isArray(this.store.availableFields) ? this.store.availableFields : [];
      const grouped = {
        basic: [],
        module: [],
        audit: [],
        custom: []
      };
      fields.forEach(f => {
        const group = f.group || 'basic';
        if (grouped[group]) {
          grouped[group].push(f);
        } else {
          // 兜底处理：未知的组放进基础信息
          grouped.basic.push(f);
        }
      });
      return grouped;
    },
    
    allColDefs() {
      const base = [
        { prop: 'num', label: this.$t('commons.id'), group: 'basic', sortable: true },
        { prop: 'name', label: this.$t('commons.name'), group: 'basic', width: 200 },
        { prop: 'title', label: this.$t('commons.title'), group: 'basic', width: 200 },
        { prop: 'status', label: this.$t('commons.status'), group: 'basic', width: 80 },
        { prop: 'projectName', label: this.$t('commons.project'), group: 'basic' },
        { prop: 'createUserName', label: this.$t('commons.create_user'), group: 'system' },
        { prop: 'createTime', label: this.$t('commons.create_time'), group: 'system', sortable: true },
        { prop: 'updateTime', label: this.$t('commons.update_time'), group: 'system', sortable: true }
      ];
      const moduleCols = this.currentModuleColumns.map(c => ({...c, group: 'module'}));
      return [...base, ...moduleCols];
    },
    
    currentModuleColumns() {
      const moduleCols = {
        'TEST_CASE': [
          { prop: 'priority', label: this.$t('advanced_search.priority'), width: 80 },
          { prop: 'maintainer', label: this.$t('advanced_search.maintainer'), width: 100 },
          { prop: 'type', label: this.$t('advanced_search.case_type'), width: 80 },
          { prop: 'reviewStatus', label: this.$t('advanced_search.review_status'), width: 90 }
        ],
        'ISSUE': [
          { prop: 'severity', label: this.$t('advanced_search.severity'), width: 80 },
          { prop: 'assignee', label: this.$t('advanced_search.assignee'), width: 100 },
          { prop: 'platform', label: this.$t('advanced_search.platform'), width: 80 }
        ],
        'TEST_PLAN': [
          { prop: 'principal', label: this.$t('advanced_search.principal'), width: 100 },
          { prop: 'stage', label: this.$t('advanced_search.stage'), width: 80 }
        ],
        'TEST_CASE_REVIEW': [
          { prop: 'reviewer', label: this.$t('advanced_search.reviewer'), width: 100 },
          { prop: 'reviewStatus', label: this.$t('advanced_search.review_status'), width: 90 }
        ]
      };
      return moduleCols[this.store.currentModule] || [];
    },
    
    currentModuleFields() {
      const fields = Array.isArray(this.store.availableFields) ? this.store.availableFields : [];
      return fields.filter(f => f && (f.group === 'module' || f.group === 'custom'));
    },
    
    groupedColumns() {
      const grouped = {
        basic: [],
        system: [],
        module: []
      };
      this.allColDefs.forEach(c => {
        if (c.group && Object.hasOwn(grouped, c.group)) {
          grouped[c.group].push(c);
        }
      });
      return grouped;
    },
    
    defaultColumns() {
      const defaults = {
        'TEST_CASE': ['num', 'name', 'status', 'priority', 'maintainer', 'reviewStatus'],
        'ISSUE': ['num', 'title', 'status', 'severity', 'assignee', 'platform'],
        'TEST_PLAN': ['num', 'name', 'status', 'principal', 'stage'],
        'TEST_CASE_REVIEW': ['num', 'name', 'status', 'reviewer', 'reviewStatus']
      };
      return defaults[this.store.currentModule] || ['num', 'name', 'status'];
    },
    
    activeTableColumns() {
      if (this.store.userSelectedColumns && this.store.userSelectedColumns.length > 0) {
        return this.allColDefs.filter(c => this.store.userSelectedColumns.includes(c.prop));
      }
      return this.allColDefs.filter(c => this.defaultColumns.includes(c.prop));
    }
  },

  async mounted() {
    await this.store.loadWorkspaces();
    await this.store.loadFieldMetadata();
    this.loadUsers();
  },
  
  methods: {
    async onWorkspaceChange() {
      try {
        await this.store.loadProjects();
        if (this.store.selectedWorkspaces.length > 0) {
          this.store.selectedProjects = [];
          this.$message.success(this.$t('advanced_search.project_list_updated'));
        }
      } catch (error) {
        this.$message.error(this.$t('advanced_search.load_projects_failed'));
      }
    },
    
    async onProjectChange() {
      await this.store.onProjectChange();
      if (!this.store.isSingleProjectMode) {
        const removedCount = this.store.conditions.filter(c => {
          const field = this.getSafeFields().find(f => (f.field || f.value) === c.field);
          return field && field.projectSpecific;
        }).length;
        if (removedCount > 0) {
          this.$message.warning(this.$t('advanced_search.removed_project_specific', { count: removedCount }));
          this.store.conditions = this.store.conditions.filter(c => {
            const field = this.store.availableFields.find(f => (f.field || f.value) === c.field);
            return !field || !field.projectSpecific;
          });
        }
      }
    },
    
    async onModuleChange() {
      await this.store.switchModule(this.store.currentModule);
      this.addDefaultConditions();
    },

    addDefaultConditions() {
      // 根据不同模块添加默认的筛选条件，提升开箱即用的体验
      const module = this.store.currentModule;
      
      // 先清空现有条件（switchModule 已经清空了 store.conditions，这里是保险）
      this.store.clearConditions();
      
      const defaultFields = {
        'TEST_CASE': ['status', 'priority', 'maintainer'],
        'ISSUE': ['status', 'severity', 'assignee'],
        'TEST_PLAN': ['status', 'principal', 'stage'],
        'TEST_CASE_REVIEW': ['status', 'reviewer', 'reviewStatus']
      };
      
      const fieldsToHead = defaultFields[module] || ['status'];
      
      // 延迟一下确保 availableFields 已经加载完成
      this.$nextTick(() => {
        fieldsToHead.forEach(fieldName => {
          const field = this.store.availableFields.find(f => (f.field || f.value) === fieldName);
          if (field) {
            this.store.addCondition(field);
          }
        });
      });
    },
    
    onQueryModeChange() {
      this.store.switchQueryMode(this.store.queryMode);
    },
    
    onViewModeChange(mode) {
      this.store.switchViewMode(mode);
      if (mode === 'split' && this.store.results.length > 0 && !this.store.selectedRow) {
        this.store.selectedRow = this.store.results[0];
      }
    },
    
    async handleSearch() {
      try {
        await this.store.executeQuery();
        this.$message.success(this.$t('advanced_search.search_success'));
        if (this.store.viewMode === 'split' && this.store.results.length > 0) {
          this.store.selectedRow = this.store.results[0];
        }
      } catch (error) {
        this.$message.error(this.$t('advanced_search.search_failed'));
      }
    },
    
    addFilter(field) {
      const fieldVal = field.field || field.value;
      const index = this.store.conditions.findIndex(c => c.field === fieldVal);
      
      if (index > -1) {
        // 如果已存在，则反选（移除）
        this.store.removeCondition(index);
      } else {
        // 如果不存在，则添加
        this.store.addCondition(field);
      }
      // 不再关闭弹窗，允许持续操作
    },
    
    removeCondition(idx) {
      this.store.removeCondition(idx);
    },
    
    clearConditions() {
      this.store.clearConditions();
    },
    
    isFieldSelected(val) {
      return this.store.conditions.some(c => c.field === val);
    },
    
    getSafeFields() {
      return Array.isArray(this.store.availableFields) ? this.store.availableFields : [];
    },
    
    getFieldLabel(val) {
      const f = this.getSafeFields().find(f => (f.field || f.value) === val);
      return f ? (f.label || f.name) : val;
    },
    
    getFieldType(val) {
      const f = this.getSafeFields().find(f => (f.field || f.value) === val);
      return f ? f.type : 'text';
    },
    
    getFieldOptions(val) {
      const f = this.getSafeFields().find(f => (f.field || f.value) === val);
      return f && f.options ? f.options : [];
    },
    
    getOperators(val) {
      const f = this.getSafeFields().find(f => (f.field || f.value) === val);
      
      // 定义符号到国际化 Key 的映射
      const opMap = {
        '=': 'op_equals',
        '!=': 'op_not_equals',
        '~': 'op_like',
        'like': 'op_like',
        'in': 'op_in',
        'IN': 'op_in',
        'not_in': 'op_not_in',
        'NOT IN': 'op_not_in',
        'between': 'op_between',
        'BETWEEN': 'op_between',
        'lt': 'op_before',
        '<': 'op_less',
        '<=': 'op_less_equal',
        'gt': 'op_after',
        '>': 'op_greater',
        '>=': 'op_greater_equal',
        'contains': 'op_contains'
      };

      if (f && f.operators && f.operators.length > 0) {
        return f.operators.map(op => {
          const key = opMap[op] || `op_${op}`;
          return {
            l: this.$t(`advanced_search.${key}`) || op,
            v: op
          };
        });
      }

      const type = this.getFieldType(val);
      if (type === 'select' || type === 'user' || type === 'treeSelect') {
        return [
          {l: this.$t('advanced_search.op_in'), v: 'in'},
          {l: this.$t('advanced_search.op_not_in'), v: 'not_in'}
        ];
      }
      if (type === 'date' || type === 'datetime') {
        return [
          {l: this.$t('advanced_search.op_between'), v: 'between'},
          {l: this.$t('advanced_search.op_before'), v: 'lt'},
          {l: this.$t('advanced_search.op_after'), v: 'gt'}
        ];
      }
      if (type === 'number') {
        return [
          {l: this.$t('advanced_search.op_equals'), v: '='},
          {l: this.$t('advanced_search.op_not_equals'), v: '!='},
          {l: this.$t('advanced_search.op_greater'), v: 'gt'},
          {l: this.$t('advanced_search.op_less'), v: 'lt'}
        ];
      }
      return [
        {l: this.$t('advanced_search.op_like'), v: 'like'},
        {l: this.$t('advanced_search.op_equals'), v: '='},
        {l: this.$t('advanced_search.op_not_equals'), v: '!='}
      ];
    },
    
    saveVisualQuery() {
      if (this.store.conditions.length === 0) return;
      
      const parts = this.store.conditions.filter(c => c.value && (Array.isArray(c.value) ? c.value.length > 0 : c.value !== '')).map(c => {
        let val = c.value;
        let op = c.op;
        
        if (op === 'in' || op === 'not_in') {
          const list = Array.isArray(val) ? val : [val];
          val = `(${list.map(v => `"${v}"`).join(', ')})`;
          op = op === 'in' ? 'IN' : 'NOT IN';
        } else if (op === 'between') {
          if (Array.isArray(val) && val.length === 2) {
            return `${c.field} >= "${val[0]}" AND ${c.field} <= "${val[1]}"`;
          }
          return null;
        } else {
          val = `"${val}"`;
          if (op === 'like') op = '~';
          else if (op === 'lt') op = '<';
          else if (op === 'gt') op = '>';
        }
        return `${c.field} ${op} ${val}`;
      }).filter(p => p !== null);
      
      if (parts.length === 0) return;
      
      this.store.jqlQuery = parts.join(' AND ');
      this.store.queryMode = 'jql';
      this.$message.info(this.$t('advanced_search.switch_to_jql') + '，' + this.$t('advanced_search.jql_save'));
    },

    openFullDetail(row) {
      const module = this.store.currentModule.toLowerCase().replace('_', '-');
      const url = `/${module}/detail/${row.id}`;
      window.open(url, '_blank');
    },

    copyId(id) {
      if (!id) return;
      navigator.clipboard.writeText(id).then(() => {
        this.$message.success(this.$t('commons.copy_success'));
      });
    },

    loadUsers() {
      this.userList = [
        { id: 'user-001', name: '张三', email: 'zhangsan@example.com' },
        { id: 'user-002', name: '李四', email: 'lisi@example.com' },
        { id: 'user-003', name: '王五', email: 'wangwu@example.com' },
        { id: 'user-004', name: '赵六', email: 'zhaoliu@example.com' }
      ];
    },
    
    searchUsers(query) {
      if (query !== '') {
        this.userLoading = true;
        setTimeout(() => {
          this.userLoading = false;
          this.userList = [
            { id: 'user-001', name: '张三', email: 'zhangsan@example.com' },
            { id: 'user-002', name: '李四', email: 'lisi@example.com' },
            { id: 'user-003', name: '王五', email: 'wangwu@example.com' }
          ].filter(u => u.name.includes(query) || u.email.includes(query));
        }, 200);
      } else {
        this.loadUsers();
      }
    },
    
    syncVisibleColumns() {
      if (!this.store.userSelectedColumns || this.store.userSelectedColumns.length === 0) {
        this.store.userSelectedColumns = this.activeTableColumns.map(c => c.prop);
      }
    },
    
    handleRowClick(row) {
      if (this.store.viewMode === 'split') {
        this.store.selectedRow = row;
      }
    },
    
    showDetail(row) {
      this.store.selectedRow = row;
      this.store.viewMode = 'split';
    },
    
    async handleExport() {
      try {
        await this.store.exportExcel();
        this.$message.success(this.$t('commons.export_success'));
      } catch (error) {
        this.$message.error(this.$t('commons.export_failed'));
      }
    },
    
    handleSizeChange(size) {
      this.store.pagination.pageSize = size;
      this.handleSearch();
    },
    
    handleCurrentChange(page) {
      this.store.pagination.current = page;
      this.handleSearch();
    },
    
    formatTime(timestamp) {
      return formatTime(timestamp);
    },
    
    getStatusType(s) {
      if (['已解决', '已完成', '已通过'].includes(s)) return 'success';
      if (['新建', '未开始', '未评审'].includes(s)) return 'info';
      if (['进行中', '处理中', '评审中'].includes(s)) return 'primary';
      if (['已关闭', '未通过'].includes(s)) return 'danger';
      return 'info';
    },
    
    getPriorityType(p) {
      if (p === 'P0') return 'danger';
      if (p === 'P1') return 'warning';
      if (p === 'P2') return 'primary';
      return 'info';
    },
    
    getSeverityType(s) {
      if (s === '致命') return 'danger';
      if (s === '严重') return 'warning';
      if (s === '一般') return 'primary';
      return 'info';
    },
    
    getModuleTagType() {
      const types = {
        'TEST_CASE': 'success',
        'ISSUE': 'danger',
        'TEST_PLAN': 'primary',
        'TEST_CASE_REVIEW': 'warning'
      };
      return types[this.store.currentModule] || 'info';
    }
  }
};
</script>

<style scoped lang="scss">
.advanced-search-page {
  padding: 0;
  background-color: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}

.top-filter-bar {
  display: flex;
  gap: 12px;
  padding: 8px 20px;
  background: #fff;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);

  .vertical-divider {
    width: 1px;
    height: 20px;
    background: #e6e6e6;
    margin: 0 4px;
  }

  .scope-selectors {
    display: flex;
    gap: 8px;
  }

  .toolbar-actions {
    margin-left: auto;
    display: flex;
    gap: 8px;
  }
}

.filter-popover-content {
  padding: 4px;
}

.filter-groups-container {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;

  &::-webkit-scrollbar {
    width: 5px;
  }

  &::-webkit-scrollbar-thumb {
    background: #e8e8e8;
    border-radius: 3px;
  }
}

.inline-add-filter-btn {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;

  i {
    margin-right: 6px;
    font-size: 14px;
    color: #783887;
  }

  &:hover {
    border-color: #783887;
    color: #783887;
    background: #fdf6ff;
  }
}

.field-group {
  margin-bottom: 24px;
  padding: 0 4px;

  &:last-child {
    margin-bottom: 8px;
  }
}

.field-group-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  text-transform: uppercase;
  letter-spacing: 1px;

  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: linear-gradient(to right, #f0f0f0, transparent);
  }
}

.field-group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;

  .field-tag {
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    border-radius: 6px;
    padding: 0 10px;
    height: 28px;
    line-height: 26px;
    font-size: 13px;
    border: 1px solid #e4e7ed;
    background: #fff;
    color: #606266;

    i {
      margin-right: 4px;
      font-size: 14px;
      color: #909399;
    }

    &:hover {
      color: #783887;
      border-color: #783887;
      background: #fdf6ff;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(120, 56, 135, 0.12);

      i {
        color: #783887;
      }
    }

    &:active {
      transform: translateY(0) scale(0.95);
    }

    &.el-tag--dark {
      background: linear-gradient(135deg, #783887 0%, #9b59b6 100%);
      border-color: #783887;
      color: #fff;
      box-shadow: 0 2px 8px rgba(120, 56, 135, 0.25);

      i {
        color: #fff;
      }

      &:hover {
        opacity: 0.9;
        box-shadow: 0 4px 15px rgba(120, 56, 135, 0.35);
      }
    }
  }
}

.active-tags-bar {
  padding: 6px 20px;
  background: #f8f9fb;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #ebeef5;
  min-height: 40px;

  .active-tags-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #909399;
    font-weight: 600;
    white-space: nowrap;

    i {
      color: #67c23a;
    }
  }

  .tags-container {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .clear-all-btn {
    color: #909399;
    font-size: 12px;
    &:hover {
      color: #f56c6c;
    }
  }
}

.condition-tag {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 2px 2px 2px 10px;
  display: flex;
  align-items: center;
  font-size: 12px;
  transition: all 0.2s;

  &:hover {
    border-color: #783887;
    box-shadow: 0 2px 6px rgba(120, 56, 135, 0.1);
  }

  .condition-field-label {
    color: #606266;
    font-weight: 600;
    margin-right: 8px;
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .op-selector {
    width: 80px;
    margin-right: 8px;
    ::v-deep .el-input__inner {
      border: none;
      background: #f4f4f5;
      height: 24px;
      line-height: 24px;
      padding: 0 20px 0 8px;
    }
  }

  .cond-value-input {
    width: 160px;
    ::v-deep .el-input__inner {
      border: none;
      border-bottom: 1px dashed #dcdfe6;
      border-radius: 0;
      height: 24px;
      line-height: 24px;
      padding: 0 4px;
      &:focus {
        border-bottom-color: #783887;
      }
    }
  }

  .remove-cond-btn {
    margin-left: 6px;
    padding: 4px;
    cursor: pointer;
    color: #c0c4cc;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;

    &:hover {
      background: #fef0f0;
      color: #f56c6c;
    }
  }
}

.user-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  transition: all 0.2s ease;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.1);
  }
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.user-email {
  font-size: 11px;
  color: #909399;
}

.result-toolbar {
  padding: 8px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #fafbfc 0%, #ffffff 100%);
  border-bottom: 1px solid #ebeef5;
  min-height: 44px;
}

.col-config-group {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.col-config-title {
  font-size: 13px;
  font-weight: 700;
  color: #606266;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid #f0f0f0;
  letter-spacing: 0.3px;
}

.col-config-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;

  .el-checkbox {
    margin-right: 0 !important;
    transition: all 0.2s ease;

    &:hover {
      transform: translateX(2px);
    }
  }
}

.view-container {
  flex: 1;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

.list-view-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.list-view-wrap .el-table {
  flex: 1;
  overflow-y: auto;
}

.pagination-wrap {
  padding: 16px 20px;
  text-align: right;
  background: #fafbfc;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
}

.split-layout {
  display: flex;
  flex: 1;
  width: 100%;
  background: #fff;
  overflow: hidden;
}

.split-left {
  width: 340px;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #d9d9d9;
    border-radius: 3px;

    &:hover {
      background: #bfbfbf;
    }
  }
}

.split-item {
  padding: 14px 18px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #fff;
  margin: 4px 8px;
  border-radius: 6px;

  &:hover {
    background: #f5f7fa;
    transform: translateX(4px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  &.active {
    background: linear-gradient(135deg, #f0f7ff 0%, #e6f4ff 100%);
    border-left: 4px solid #783887;
    box-shadow: 0 2px 12px rgba(120, 56, 135, 0.15);
    transform: translateX(0);
  }
}

.split-item-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
  line-height: 1.5;
  font-size: 14px;
}

.split-item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.split-right {
  flex: 1;
  background: #fff;
  overflow-y: auto;
  padding: 0;
}

.detail-header {
  padding: 24px 32px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;

  .detail-top-nav {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .detail-breadcrumb {
    font-size: 12px;
    color: #909399;
    background: #f4f4f5;
    padding: 2px 8px;
    border-radius: 4px;
  }

  .detail-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 12px;

    .module-tag {
      border-radius: 4px;
      font-size: 11px;
      padding: 0 8px;
      height: 22px;
      line-height: 22px;
    }
  }

  .detail-meta-row {
    display: flex;
    gap: 24px;
    flex-wrap: wrap;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;

      i {
        color: #909399;
      }

      .meta-label {
        color: #909399;
      }

      .meta-value {
        color: #606266;
        font-weight: 500;
      }
    }
  }
}

.detail-content {
  padding: 24px 32px;
}

.detail-section {
  margin-bottom: 32px;

  .detail-section-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 8px;

    i {
      color: #783887;
    }
  }
}

.detail-field-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.detail-field-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  background: #f8f9fb;
  border-radius: 6px;
  border: 1px solid transparent;
  transition: all 0.2s;

  &:hover {
    background: #fff;
    border-color: #e4e7ed;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  }

  .detail-label {
    font-size: 12px;
    color: #909399;
  }

  .detail-value {
    font-size: 14px;
    color: #303133;
    font-weight: 500;
  }
}

// 响应式设计
@media (max-width: 1200px) {
  .split-left {
    width: 280px;
  }

  .detail-field-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .advanced-search-page {
    padding: 12px;
  }

  .top-filter-bar {
    padding: 16px;
  }

  .split-layout {
    flex-direction: column;
  }

  .split-left {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e8eaed;
    max-height: 300px;
  }

  .split-right {
    padding: 20px;
  }
}

// 全局优化
::v-deep {
  .el-table {
    border-radius: 8px;
    overflow: hidden;

    th {
      background: #fafbfc !important;
      font-weight: 600;
      color: #606266;
    }

    tr:hover {
      background: #f5f7fa !important;
    }
  }

  .el-button--primary {
    background: linear-gradient(135deg, #783887 0%, #9b59b6 100%);
    border: none;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(120, 56, 135, 0.3);
    }
  }

  .el-tag {
    border-radius: 4px;
    font-weight: 500;
  }

  .el-select-dropdown__item {
    transition: all 0.2s ease;

    &:hover {
      background: #f5f7fa;
    }
  }
}

</style>
