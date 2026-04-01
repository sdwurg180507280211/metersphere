<template>
  <div class="advanced-search-page">
    <!-- 顶部筛选栏 -->
    <div class="top-filter-bar">
      <!-- 业务模块选择 -->
      <el-select
        v-model="store.currentModule"
        :placeholder="$t('advanced_search.select_module')"
        @change="onModuleChange"
      >
        <el-option value="TEST_CASE" :label="$t('advanced_search.module_test_case')" />
        <el-option value="ISSUE" :label="$t('advanced_search.module_issue')" />
        <el-option value="TEST_PLAN" :label="$t('advanced_search.module_test_plan')" />
        <el-option value="TEST_CASE_REVIEW" :label="$t('advanced_search.module_review')" />
      </el-select>

      <div style="width: 1px; height: 16px; background: #ddd;"></div>

      <!-- 工作空间多选 -->
      <el-select
        v-model="store.selectedWorkspaces"
        multiple
        collapse-tags
        :placeholder="$t('advanced_search.select_workspace')"
        @change="onWorkspaceChange"
      >
        <el-option v-for="ws in store.workspaces" :key="ws.id" :label="ws.name" :value="ws.id" />
      </el-select>

      <!-- 项目多选 -->
      <el-select
        v-model="store.selectedProjects"
        multiple
        collapse-tags
        :placeholder="$t('advanced_search.select_project')"
        @change="onProjectChange"
      >
        <el-option v-for="proj in store.projects" :key="proj.id" :value="proj.id" :label="proj.name" />
      </el-select>

      <!-- 筛选条件按钮 -->
      <el-popover placement="bottom-start" width="450" trigger="click" v-model="popoverVisible">
        <div style="padding: 10px;">
          <div style="font-weight: bold; margin-bottom: 12px; font-size: 13px;">{{ $t('advanced_search.select_filter_field') }}</div>

          <!-- 基础信息 -->
          <div class="field-group">
            <div class="field-group-title">📋 {{ $t('advanced_search.basic_info') }}</div>
            <div class="field-group-items">
              <el-tag
                v-for="field in fieldsByGroup.basic"
                :key="field.field || field.value"
                size="small"
                style="cursor: pointer;"
                :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                @click="addFilter(field)"
              >
                <i :class="field.icon || 'el-icon-document'"></i> {{ field.label || field.name }}
              </el-tag>
            </div>
          </div>

          <!-- 人员相关 -->
          <div class="field-group">
            <div class="field-group-title">👤 {{ $t('advanced_search.user_related') }}</div>
            <div class="field-group-items">
              <el-tag
                v-for="field in fieldsByGroup.user"
                :key="field.field || field.value"
                size="small"
                style="cursor: pointer;"
                :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                @click="addFilter(field)"
              >
                <i :class="field.icon || 'el-icon-user'"></i> {{ field.label || field.name }}
              </el-tag>
            </div>
          </div>

          <!-- 模块专属 -->
          <div class="field-group">
            <div class="field-group-title">🔧 {{ currentModuleLabel }}专属</div>
            <div class="field-group-items">
              <el-tag
                v-for="field in fieldsByGroup.module"
                :key="field.field || field.value"
                size="small"
                style="cursor: pointer;"
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
                style="cursor: pointer;"
                :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                @click="addFilter(field)"
              >
                {{ field.label || field.name }}
              </el-tag>
            </div>
          </div>

          <!-- 时间相关 -->
          <div class="field-group">
            <div class="field-group-title">📅 {{ $t('advanced_search.date_related') }}</div>
            <div class="field-group-items">
              <el-tag
                v-for="field in fieldsByGroup.date"
                :key="field.field || field.value"
                size="small"
                style="cursor: pointer;"
                :effect="isFieldSelected(field.field || field.value) ? 'dark' : 'plain'"
                @click="addFilter(field)"
              >
                <i :class="field.icon || 'el-icon-date'"></i> {{ field.label || field.name }}
              </el-tag>
            </div>
          </div>
        </div>
        <div slot="reference" class="inline-add-filter-btn">
          <i class="el-icon-plus" style="margin-right: 4px;"></i> {{ $t('advanced_search.add_filter') }}
        </div>
      </el-popover>

      <el-button type="primary" icon="el-icon-search" size="small" style="margin-left: auto;" @click="handleSearch" :loading="store.loading">
        {{ $t('advanced_search.search') }}
      </el-button>

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
      <div v-for="(cond, idx) in store.conditions" :key="idx" class="condition-tag">
        <span class="condition-field-label">{{ getFieldLabel(cond.field) }}:</span>

        <!-- 操作符选择 -->
        <el-select v-model="cond.op" size="mini" style="width: 75px; margin-right: 4px;">
          <el-option v-for="o in getOperators(cond.field)" :key="o.v" :label="o.l" :value="o.v" />
        </el-select>

        <!-- 根据字段类型渲染不同输入控件 -->
        <div style="width: 180px;">
          <!-- 日期类型 -->
          <el-date-picker
            v-if="getFieldType(cond.field) === 'date'"
            v-model="cond.value"
            type="daterange"
            size="mini"
            style="width: 100%"
            range-separator="-"
            :start-placeholder="$t('advanced_search.start_date')"
            :end-placeholder="$t('advanced_search.end_date')"
            value-format="yyyy-MM-dd"
          />

          <!-- 下拉选择类型 -->
          <el-select
            v-else-if="getFieldType(cond.field) === 'select'"
            v-model="cond.value"
            size="mini"
            style="width: 100%"
            :placeholder="$t('advanced_search.select_value')"
            multiple
            collapse-tags
          >
            <el-option v-for="opt in getFieldOptions(cond.field)" :key="opt" :label="opt" :value="opt" />
          </el-select>

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

        <i class="el-icon-close" style="margin-left: 6px; cursor: pointer; color: #999;" @click="removeCondition(idx)"></i>
      </div>
      <el-button type="text" size="mini" style="margin-left: auto; color: #909399" @click="clearConditions">
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
            <div style="font-size: 12px; color: #909399; margin-bottom: 8px;">
              {{ store.selectedRow.workspaceName || '-' }} / {{ store.selectedRow.projectName || '-' }} / {{ store.selectedRow.num || store.selectedRow.id }}
            </div>
            <div class="detail-title">
              <el-tag size="medium" :type="getModuleTagType()" effect="dark" style="vertical-align: 2px; margin-right: 8px;">
                {{ currentModuleShortLabel }}
              </el-tag>
              {{ store.selectedRow.name || store.selectedRow.title }}
            </div>
            <div class="detail-meta-row">
              <span><i class="el-icon-user"></i> {{ $t('commons.create_user') }}: {{ store.selectedRow.createUserName || store.selectedRow.creator }}</span>
              <span><i class="el-icon-date"></i> {{ $t('commons.create_time') }}: {{ formatTime(store.selectedRow.createTime || store.selectedRow.create_time) || '-' }}</span>
              <span><i class="el-icon-time"></i> {{ $t('advanced_search.updated_at') }}: {{ formatTime(store.selectedRow.updateTime || store.selectedRow.update_time) }}</span>
            </div>
          </div>

          <div class="detail-section-title">{{ $t('advanced_search.basic_attributes') }}</div>
          <div class="detail-field-grid">
            <div class="detail-field-item" v-for="f in currentModuleFields" :key="f.field || f.value">
              <span class="detail-label">{{ f.label || f.name }}</span>
              <span class="detail-value">
                <el-tag v-if="(f.field || f.value) === 'status'" size="small" :type="getStatusType(store.selectedRow[f.field || f.value])">
                  {{ store.selectedRow[f.field || f.value] }}
                </el-tag>
                <el-tag v-else-if="(f.field || f.value) === 'priority'" size="small" :type="getPriorityType(store.selectedRow[f.field || f.value])">
                  {{ store.selectedRow[f.field || f.value] }}
                </el-tag>
                <el-tag v-else-if="(f.field || f.value) === 'severity'" size="small" :type="getSeverityType(store.selectedRow[f.field || f.value])">
                  {{ store.selectedRow[f.field || f.value] }}
                </el-tag>
                <span v-else>{{ store.selectedRow[f.field || f.value] || '-' }}</span>
              </span>
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
    currentModuleLabel() {
      const labels = {
        'TEST_CASE': this.$t('advanced_search.module_test_case'),
        'ISSUE': this.$t('advanced_search.module_issue'),
        'TEST_PLAN': this.$t('advanced_search.module_test_plan'),
        'TEST_CASE_REVIEW': this.$t('advanced_search.module_review')
      };
      return labels[this.store.currentModule] || '';
    },
    
    currentModuleShortLabel() {
      const labels = {
        'TEST_CASE': 'Case',
        'ISSUE': 'Bug',
        'TEST_PLAN': 'Plan',
        'TEST_CASE_REVIEW': 'Review'
      };
      return labels[this.store.currentModule] || '';
    },
    
    fieldsByGroup() {
      const fields = Array.isArray(this.store.availableFields) ? this.store.availableFields : [];
      return {
        basic: fields.filter(f => f && f.group === 'basic'),
        user: fields.filter(f => f && f.group === 'user'),
        module: fields.filter(f => f && f.group === 'module'),
        custom: fields.filter(f => f && f.group === 'custom'),
        date: fields.filter(f => f && f.group === 'date')
      };
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
      return {
        basic: this.allColDefs.filter(c => c.group === 'basic'),
        system: this.allColDefs.filter(c => c.group === 'system'),
        module: this.allColDefs.filter(c => c.group === 'module')
      };
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
      await this.store.loadProjects();
      this.store.selectedProjects = [];
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
      this.store.addCondition(field);
      this.popoverVisible = false;
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
      return f ? (f.options || []) : [];
    },
    
    getOperators(val) {
      const type = this.getFieldType(val);
      if (type === 'select' || type === 'user') {
        return [{l: this.$t('advanced_search.op_in'), v: 'in'}, {l: this.$t('advanced_search.op_not_in'), v: 'not_in'}];
      }
      if (type === 'date') {
        return [{l: this.$t('advanced_search.op_between'), v: 'between'}, {l: this.$t('advanced_search.op_before'), v: 'lt'}, {l: this.$t('advanced_search.op_after'), v: 'gt'}];
      }
      return [{l: this.$t('advanced_search.op_contains'), v: 'like'}, {l: this.$t('advanced_search.op_equals'), v: '='}, {l: this.$t('advanced_search.op_not_equals'), v: '!='}];
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
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
  align-items: center;
  
  .el-select {
    width: 200px;
  }
}

.inline-add-filter-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  
  &:hover {
    color: #783887;
    border-color: #783887;
    background: rgba(120, 56, 135, 0.05);
  }
}

.cross-project-warning {
  width: 100%;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 4px;
  color: #e6a23c;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-group {
  margin-bottom: 12px;
}

.field-group-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 600;
}

.field-group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.query-condition-area {
  margin-bottom: 20px;
  padding: 16px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.active-tags-bar {
  padding: 8px 20px;
  background: #fdfdfd;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  min-height: 40px;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.condition-tag {
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 2px 4px 2px 8px;
  display: flex;
  align-items: center;
  font-size: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.condition-field-label {
  color: #909399;
  margin-right: 6px;
  font-weight: 600;
}

.user-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 13px;
  color: #303133;
}

.user-email {
  font-size: 11px;
  color: #909399;
}

.result-toolbar {
  padding: 8px 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fcfcfc;
  height: 40px;
}

.col-config-group {
  margin-bottom: 12px;
}

.col-config-title {
  font-size: 12px;
  font-weight: bold;
  color: #909399;
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px dashed #eee;
}

.col-config-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.view-container {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.list-view-wrap {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.pagination-wrap {
  padding: 16px;
  text-align: right;
  background: #fff;
}

.split-layout {
  display: flex;
  height: 100%;
  width: 100%;
}

.split-left {
  width: 320px;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow-y: auto;
}

.split-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
  
  &:hover {
    background: #fafafa;
  }
  
  &.active {
    background: #f0f7ff;
    border-left: 3px solid #783887;
  }
}

.split-item-title {
  font-weight: 500;
  margin-bottom: 6px;
  color: #303133;
  line-height: 1.4;
}

.split-item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.split-right {
  flex: 1;
  background: #fff;
  overflow-y: auto;
  padding: 24px 40px;
}

.detail-header {
  border-bottom: 1px solid #eee;
  padding-bottom: 16px;
  margin-bottom: 20px;
}

.detail-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #1f2f3d;
}

.detail-meta-row {
  display: flex;
  gap: 24px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
}

.detail-section-title {
  font-size: 14px;
  font-weight: bold;
  border-left: 4px solid #783887;
  padding-left: 10px;
  margin: 24px 0 12px;
  color: #303133;
}

.detail-field-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 32px;
}

.detail-field-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 12px;
  color: #909399;
}

.detail-value {
  font-size: 14px;
  color: #303133;
}
</style>
