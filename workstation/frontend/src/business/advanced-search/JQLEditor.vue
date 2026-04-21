<template>
  <div class="jql-editor-container">
    <!-- 工具栏 -->
    <div class="jql-toolbar">
      <div class="toolbar-left">
        <el-tooltip :content="$t('advanced_search.jql_history')" placement="top">
          <el-button size="small" icon="el-icon-time" @click="showHistoryPanel = !showHistoryPanel">
            {{ $t('advanced_search.jql_history') }}
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('advanced_search.jql_saved')" placement="top">
          <el-button size="small" icon="el-icon-document" @click="showSavedPanel = !showSavedPanel">
            {{ $t('advanced_search.jql_saved') }}
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('advanced_search.jql_examples_title')" placement="top">
          <el-button size="small" icon="el-icon-lightbulb" @click="showExamplesPanel = !showExamplesPanel">
            {{ $t('advanced_search.jql_examples_title') }}
          </el-button>
        </el-tooltip>
      </div>
      <div class="toolbar-right">
        <el-tooltip :content="$t('advanced_search.jql_format')" placement="top">
          <el-button size="small" icon="el-icon-s-operation" @click="formatJQL">
            {{ $t('advanced_search.jql_format') }}
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('advanced_search.jql_copy')" placement="top">
          <el-button size="small" icon="el-icon-document-copy" @click="copyJQL">
            {{ $t('advanced_search.jql_copy') }}
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('advanced_search.jql_clear')" placement="top">
          <el-button size="small" icon="el-icon-delete" @click="clearJQL">
            {{ $t('advanced_search.jql_clear') }}
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('advanced_search.jql_save')" placement="top">
          <el-button type="primary" size="small" icon="el-icon-save" @click="openSaveDialog">
            {{ $t('advanced_search.jql_save') }}
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 侧边面板区域 -->
    <div class="jql-main-area" :class="{ 'with-side-panel': showHistoryPanel || showSavedPanel || showExamplesPanel }">
      <!-- 左侧面板 -->
      <div v-if="showHistoryPanel || showSavedPanel || showExamplesPanel" class="side-panel">
        <!-- 查询历史 -->
        <div v-if="showHistoryPanel" class="panel-content">
          <div class="panel-header">
            <span class="panel-title">{{ $t('advanced_search.query_history_title') }}</span>
            <el-button type="text" size="small" @click="clearHistory" :disabled="queryHistory.length === 0">
              {{ $t('advanced_search.query_history_clear') }}
            </el-button>
          </div>
          <div v-if="queryHistory.length === 0" class="panel-empty">
            <i class="el-icon-info"></i>
            <p>{{ $t('advanced_search.query_history_empty') }}</p>
          </div>
          <div v-else class="panel-list">
            <div
              v-for="(item, index) in queryHistory"
              :key="index"
              class="history-item"
              @click="useHistoryItem(item)"
            >
              <div class="history-text">{{ item.jql }}</div>
              <div class="history-time">{{ formatTime(item.timestamp) }}</div>
              <div class="history-actions">
                <el-tooltip :content="$t('advanced_search.query_history_use')" placement="top">
                  <i class="el-icon-right" @click.stop="useHistoryItem(item)"></i>
                </el-tooltip>
                <el-tooltip :content="$t('advanced_search.query_history_delete')" placement="top">
                  <i class="el-icon-delete" @click.stop="deleteHistoryItem(index)"></i>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>

        <!-- 已保存查询 -->
        <div v-if="showSavedPanel" class="panel-content">
          <div class="panel-header">
            <span class="panel-title">{{ $t('advanced_search.jql_saved') }}</span>
          </div>
          <div v-if="savedQueries.length === 0" class="panel-empty">
            <i class="el-icon-folder-opened"></i>
            <p>{{ $t('advanced_search.visual_mode_placeholder') }}</p>
          </div>
          <div v-else class="panel-list">
            <div
              v-for="(item, index) in savedQueries"
              :key="index"
              class="saved-item"
              @click="useSavedItem(item)"
            >
              <div class="saved-name">
                <i class="el-icon-star-on"></i>
                {{ item.name }}
              </div>
              <div class="saved-text">{{ item.jql }}</div>
              <div class="saved-actions">
                <el-tooltip :content="$t('advanced_search.query_history_use')" placement="top">
                  <i class="el-icon-right" @click.stop="useSavedItem(item)"></i>
                </el-tooltip>
                <el-tooltip :content="$t('advanced_search.edit')" placement="top">
                  <i class="el-icon-edit" @click.stop="editSavedItem(index)"></i>
                </el-tooltip>
                <el-tooltip :content="$t('advanced_search.delete')" placement="top">
                  <i class="el-icon-delete" @click.stop="deleteSavedItem(index)"></i>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>

        <!-- 示例查询 -->
        <div v-if="showExamplesPanel" class="panel-content">
          <div class="panel-header">
            <span class="panel-title">{{ $t('advanced_search.jql_examples_title') }}</span>
          </div>
          <div class="panel-list">
            <div class="example-item" @click="useExample($t('advanced_search.jql_example_1'))">
              <div class="example-desc">{{ $t('advanced_search.example_status_priority') }}</div>
              <div class="example-code"><code>{{ $t('advanced_search.jql_example_1') }}</code></div>
              <div class="example-use">
                <el-button type="text" size="small" icon="el-icon-right">
                  {{ $t('advanced_search.query_history_use') }}
                </el-button>
              </div>
            </div>
            <div class="example-item" @click="useExample($t('advanced_search.jql_example_2'))">
              <div class="example-desc">{{ $t('advanced_search.example_in_list') }}</div>
              <div class="example-code"><code>{{ $t('advanced_search.jql_example_2') }}</code></div>
              <div class="example-use">
                <el-button type="text" size="small" icon="el-icon-right">
                  {{ $t('advanced_search.query_history_use') }}
                </el-button>
              </div>
            </div>
            <div class="example-item" @click="useExample($t('advanced_search.jql_example_3'))">
              <div class="example-desc">{{ $t('advanced_search.example_date_range') }}</div>
              <div class="example-code"><code>{{ $t('advanced_search.jql_example_3') }}</code></div>
              <div class="example-use">
                <el-button type="text" size="small" icon="el-icon-right">
                  {{ $t('advanced_search.query_history_use') }}
                </el-button>
              </div>
            </div>
            <div class="example-item" @click="useExample($t('advanced_search.jql_example_4'))">
              <div class="example-desc">{{ $t('advanced_search.example_complex') }}</div>
              <div class="example-code"><code>{{ $t('advanced_search.jql_example_4') }}</code></div>
              <div class="example-use">
                <el-button type="text" size="small" icon="el-icon-right">
                  {{ $t('advanced_search.query_history_use') }}
                </el-button>
              </div>
            </div>
            <div class="example-item" @click="useExample($t('advanced_search.jql_example_6'))">
              <div class="example-desc">{{ $t('advanced_search.example_contains') }}</div>
              <div class="example-code"><code>{{ $t('advanced_search.jql_example_6') }}</code></div>
              <div class="example-use">
                <el-button type="text" size="small" icon="el-icon-right">
                  {{ $t('advanced_search.query_history_use') }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- JQL 输入区域 -->
      <div class="jql-input-area">
        <div class="jql-input-wrapper">
          <el-input
            ref="jqlInput"
            v-model="localJql"
            type="textarea"
            :rows="5"
            :placeholder="$t('advanced_search.jql_placeholder')"
            @input="onInput"
            @keydown="onKeyDown"
            @blur="onBlur"
            class="jql-textarea"
          />

          <!-- 智能提示下拉框 -->
          <div
            v-if="showSuggestions && suggestions.length > 0"
            class="suggestions-dropdown"
            :style="dropdownStyle"
          >
            <div
              v-for="(suggestion, index) in suggestions"
              :key="index"
              class="suggestion-item"
              :class="{ active: index === selectedIndex }"
              @click="selectSuggestion(suggestion)"
              @mouseenter="selectedIndex = index"
            >
              <span class="suggestion-type" :class="'type-' + suggestion.type">
                {{ getSuggestionTypeLabel(suggestion.type) }}
              </span>
              <span class="suggestion-value">{{ suggestion.value }}</span>
              <span v-if="suggestion.description" class="suggestion-desc">{{ suggestion.description }}</span>
            </div>
          </div>
        </div>

        <!-- 验证结果提示 -->
        <div v-if="validationResult" class="validation-result" :class="validationResult.valid ? 'success' : 'error'">
          <i :class="validationResult.valid ? 'el-icon-circle-check' : 'el-icon-warning'"></i>
          <span v-if="validationResult.valid">{{ $t('advanced_search.validation_success') }}</span>
          <span v-else>
            {{ validationResult.message }}
            <span v-if="validationResult.line" class="error-position">
              ({{ $t('advanced_search.error_position', { line: validationResult.line, column: validationResult.column }) }})
            </span>
          </span>
        </div>

        <!-- 快捷键提示 -->
        <div class="shortcut-hints">
          <span class="hint-item">
            <kbd>Ctrl</kbd>+<kbd>Enter</kbd> {{ $t('advanced_search.shortcut_execute') }}
          </span>
          <span class="hint-item">
            <kbd>Ctrl</kbd>+<kbd>/</kbd> {{ $t('advanced_search.shortcut_toggle_help') }}
          </span>
        </div>

        <!-- 语法帮助（可折叠） -->
        <div class="jql-help">
          <el-collapse v-model="helpCollapse">
            <el-collapse-item :title="$t('advanced_search.jql_help_title')" name="help">
              <div class="help-content">
                <div class="help-section">
                  <h4><i class="el-icon-s-operation"></i> {{ $t('advanced_search.jql_operators') }}</h4>
                  <div class="operator-grid">
                    <div class="operator-item">
                      <code>=</code>
                      <span>{{ $t('advanced_search.op_equals') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>!=</code>
                      <span>{{ $t('advanced_search.op_not_equals') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>~</code>
                      <span>{{ $t('advanced_search.op_like') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>CONTAINS</code>
                      <span>{{ $t('advanced_search.op_contains') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>&gt;</code>
                      <span>{{ $t('advanced_search.op_greater') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>&gt;=</code>
                      <span>{{ $t('advanced_search.op_greater_equal') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>&lt;</code>
                      <span>{{ $t('advanced_search.op_less') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>&lt;=</code>
                      <span>{{ $t('advanced_search.op_less_equal') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>IN</code>
                      <span>{{ $t('advanced_search.op_in') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>NOT IN</code>
                      <span>{{ $t('advanced_search.op_not_in') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>AND</code>
                      <span>{{ $t('advanced_search.op_and') }}</span>
                    </div>
                    <div class="operator-item">
                      <code>OR</code>
                      <span>{{ $t('advanced_search.op_or') }}</span>
                    </div>
                  </div>
                </div>

                <div class="help-section">
                  <h4><i class="el-icon-document"></i> {{ $t('advanced_search.jql_examples') }}</h4>
                  <ul class="example-list">
                    <li @click="useExample($t('advanced_search.jql_example_1'))">
                      <code>{{ $t('advanced_search.jql_example_1') }}</code>
                    </li>
                    <li @click="useExample($t('advanced_search.jql_example_2'))">
                      <code>{{ $t('advanced_search.jql_example_2') }}</code>
                    </li>
                    <li @click="useExample($t('advanced_search.jql_example_3'))">
                      <code>{{ $t('advanced_search.jql_example_3') }}</code>
                    </li>
                    <li @click="useExample($t('advanced_search.jql_example_4'))">
                      <code>{{ $t('advanced_search.jql_example_4') }}</code>
                    </li>
                  </ul>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </div>

    <!-- 保存查询对话框 -->
    <el-dialog
      :title="$t('advanced_search.save_query_title')"
      :visible.sync="saveDialogVisible"
      width="400px"
      @close="resetSaveForm"
    >
      <el-form :model="saveForm" label-width="80px">
        <el-form-item :label="$t('advanced_search.save_query_name')" required>
          <el-input
            v-model="saveForm.name"
            :placeholder="$t('advanced_search.save_query_name_placeholder')"
            @keyup.enter="confirmSave"
          />
        </el-form-item>
        <el-form-item v-if="false" :label="$t('advanced_search.save_query_share')">
          <el-switch v-model="saveForm.shared" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">{{ $t('advanced_search.save_query_cancel') }}</el-button>
        <el-button type="primary" @click="confirmSave">{{ $t('advanced_search.save_query_save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store/advancedSearch';
import { debounce } from 'lodash';

export default {
  name: 'JQLEditor',

  props: {
    value: {
      type: String,
      default: ''
    }
  },

  data() {
    return {
      localJql: this.value,
      suggestions: [],
      showSuggestions: false,
      selectedIndex: 0,
      validationResult: null,
      dropdownStyle: {},
      helpCollapse: ['help'],

      // 面板显示状态
      showHistoryPanel: false,
      showSavedPanel: false,
      showExamplesPanel: false,

      // 查询历史
      queryHistory: [],

      // 已保存查询
      savedQueries: [],

      // 保存对话框
      saveDialogVisible: false,
      saveForm: {
        name: '',
        shared: false
      }
    };
  },

  computed: {
    store() {
      return useAdvancedSearchStore();
    }
  },

  watch: {
    value(newVal) {
      this.localJql = newVal;
    }
  },

  created() {
    this.loadFromLocalStorage();
  },

  mounted() {
    // 注册全局快捷键
    document.addEventListener('keydown', this.handleGlobalKeydown);
  },

  beforeDestroy() {
    document.removeEventListener('keydown', this.handleGlobalKeydown);
  },

  methods: {
    /**
     * 输入事件处理（防抖）
     */
    onInput: debounce(async function() {
      this.$emit('input', this.localJql);

      // 获取光标位置
      const textarea = this.$refs.jqlInput?.$el?.querySelector('textarea');
      const cursorPosition = textarea ? textarea.selectionStart : this.localJql.length;

      // 获取智能提示
      await this.loadSuggestions(cursorPosition);

      // 验证 JQL 语法
      await this.validateJQL();
    }, 300),

    /**
     * 键盘事件处理
     */
    onKeyDown(event) {
      // Ctrl+Enter 执行查询
      if (event.ctrlKey && event.key === 'Enter') {
        event.preventDefault();
        this.executeQuery();
        return;
      }

      if (!this.showSuggestions || this.suggestions.length === 0) {
        return;
      }

      // 上箭头
      if (event.key === 'ArrowUp') {
        event.preventDefault();
        this.selectedIndex = Math.max(0, this.selectedIndex - 1);
      }
      // 下箭头
      else if (event.key === 'ArrowDown') {
        event.preventDefault();
        this.selectedIndex = Math.min(
          this.suggestions.length - 1,
          this.selectedIndex + 1
        );
      }
      // 回车或Tab
      else if (event.key === 'Enter' || event.key === 'Tab') {
        event.preventDefault();
        this.selectSuggestion(this.suggestions[this.selectedIndex]);
      }
      // Esc
      else if (event.key === 'Escape') {
        this.showSuggestions = false;
      }
    },

    /**
     * 全局快捷键处理
     */
    handleGlobalKeydown(event) {
      // Ctrl+/ 切换帮助面板
      if (event.ctrlKey && event.key === '/') {
        event.preventDefault();
        this.helpCollapse = this.helpCollapse.length === 0 ? ['help'] : [];
      }
    },

    /**
     * 失焦事件处理
     */
    onBlur() {
      // 延迟隐藏，以便点击提示项
      setTimeout(() => {
        this.showSuggestions = false;
      }, 200);
    },

    /**
     * 加载智能提示
     */
    async loadSuggestions(cursorPosition) {
      try {
        this.suggestions = await this.store.getJQLSuggestions(
          this.localJql,
          cursorPosition
        );

        this.showSuggestions = this.suggestions.length > 0;
        this.selectedIndex = 0;

        // 计算下拉框位置
        this.calculateDropdownPosition();
      } catch (error) {
        console.error('加载智能提示失败:', error);
      }
    },

    /**
     * 选择提示项
     */
    selectSuggestion(suggestion) {
      const textarea = this.$refs.jqlInput?.$el?.querySelector('textarea');
      const cursorPosition = textarea ? textarea.selectionStart : this.localJql.length;

      // 获取光标前后的文本
      const before = this.localJql.substring(0, cursorPosition);
      const after = this.localJql.substring(cursorPosition);

      // 找到最后一个词的开始位置
      const lastWordStart = before.lastIndexOf(' ') + 1;
      const beforeWord = before.substring(0, lastWordStart);

      // 插入提示文本（使用 insertText 或 value）
      const insertText = suggestion.insertText || suggestion.value;
      this.localJql = beforeWord + insertText + after;
      this.$emit('input', this.localJql);

      // 隐藏提示
      this.showSuggestions = false;

      // 设置光标位置
      this.$nextTick(() => {
        if (textarea) {
          const newPosition = beforeWord.length + insertText.length;
          textarea.setSelectionRange(newPosition, newPosition);
          textarea.focus();
        }
      });
    },

    /**
     * 验证 JQL 语法
     */
    async validateJQL() {
      if (!this.localJql.trim()) {
        this.validationResult = null;
        return;
      }

      try {
        this.validationResult = await this.store.validateJQL(this.localJql);
      } catch (error) {
        console.error('JQL 验证失败:', error);
      }
    },

    /**
     * 计算下拉框位置
     */
    calculateDropdownPosition() {
      this.$nextTick(() => {
        const textarea = this.$refs.jqlInput?.$el?.querySelector('textarea');
        if (!textarea) return;

        const rect = textarea.getBoundingClientRect();
        this.dropdownStyle = {
          top: `${rect.height}px`,
          left: '0px',
          width: `${rect.width}px`
        };
      });
    },

    /**
     * 获取提示类型标签
     */
    getSuggestionTypeLabel(type) {
      const labels = {
        field: this.$t('advanced_search.suggestion_field'),
        operator: this.$t('advanced_search.suggestion_operator'),
        value: this.$t('advanced_search.suggestion_value'),
        keyword: this.$t('advanced_search.suggestion_keyword'),
        recent: this.$t('advanced_search.suggestion_recent')
      };
      return labels[type] || type;
    },

    /**
     * 执行查询
     */
    executeQuery() {
      // 保存到历史
      if (this.localJql.trim()) {
        this.addToHistory(this.localJql);
      }
      this.$emit('execute');
    },

    /**
     * 格式化 JQL
     */
    formatJQL() {
      if (!this.localJql.trim()) return;

      // 简单的格式化：关键字大写，适当换行
      let formatted = this.localJql
        .replace(/\s+/g, ' ')
        .replace(/\b(AND|OR|IN|NOT|CONTAINS)\b/gi, match => match.toUpperCase())
        .replace(/\s+AND\s+/gi, '\n  AND ')
        .replace(/\s+OR\s+/gi, '\n  OR ');

      this.localJql = formatted;
      this.$emit('input', this.localJql);
    },

    /**
     * 复制 JQL
     */
    async copyJQL() {
      if (!this.localJql.trim()) return;

      try {
        await navigator.clipboard.writeText(this.localJql);
        this.$message.success(this.$t('advanced_search.copy_success'));
      } catch (error) {
        this.$message.error(this.$t('advanced_search.copy_failed'));
      }
    },

    /**
     * 清空 JQL
     */
    clearJQL() {
      this.$confirm(
        this.$t('advanced_search.confirm_clear_query'),
        this.$t('advanced_search.confirm_title'),
        {
          confirmButtonText: this.$t('advanced_search.confirm_yes'),
          cancelButtonText: this.$t('advanced_search.confirm_no'),
          type: 'warning'
        }
      ).then(() => {
        this.localJql = '';
        this.$emit('input', '');
        this.validationResult = null;
      }).catch(() => {});
    },

    /**
     * 打开保存对话框
     */
    openSaveDialog() {
      if (!this.localJql.trim()) {
        this.$message.warning(this.$t('advanced_search.jql_placeholder'));
        return;
      }
      this.saveForm.name = '';
      this.saveForm.shared = false;
      this.saveDialogVisible = true;
    },

    /**
     * 确认保存
     */
    confirmSave() {
      if (!this.saveForm.name.trim()) {
        this.$message.warning(this.$t('advanced_search.save_query_name_required'));
        return;
      }

      this.savedQueries.unshift({
        name: this.saveForm.name,
        jql: this.localJql,
        shared: this.saveForm.shared,
        timestamp: Date.now()
      });

      this.saveToLocalStorage();
      this.saveDialogVisible = false;
      this.$message.success(this.$t('advanced_search.save_query_success'));
    },

    /**
     * 重置保存表单
     */
    resetSaveForm() {
      this.saveForm.name = '';
      this.saveForm.shared = false;
    },

    /**
     * 使用示例
     */
    useExample(jql) {
      this.localJql = jql;
      this.$emit('input', jql);
      this.showExamplesPanel = false;
      this.$nextTick(() => {
        this.validateJQL();
      });
    },

    /**
     * 添加到历史
     */
    addToHistory(jql) {
      // 去重：如果已经存在则移到最前面
      const existingIndex = this.queryHistory.findIndex(item => item.jql === jql);
      if (existingIndex !== -1) {
        this.queryHistory.splice(existingIndex, 1);
      }

      this.queryHistory.unshift({
        jql,
        timestamp: Date.now()
      });

      // 限制历史记录数量
      if (this.queryHistory.length > 50) {
        this.queryHistory = this.queryHistory.slice(0, 50);
      }

      this.saveToLocalStorage();
    },

    /**
     * 使用历史记录
     */
    useHistoryItem(item) {
      this.localJql = item.jql;
      this.$emit('input', item.jql);
      this.showHistoryPanel = false;
      this.$nextTick(() => {
        this.validateJQL();
      });
    },

    /**
     * 删除历史记录
     */
    deleteHistoryItem(index) {
      this.queryHistory.splice(index, 1);
      this.saveToLocalStorage();
    },

    /**
     * 清空历史
     */
    clearHistory() {
      this.$confirm(
        this.$t('advanced_search.confirm_clear_history'),
        this.$t('advanced_search.confirm_title'),
        {
          confirmButtonText: this.$t('advanced_search.confirm_yes'),
          cancelButtonText: this.$t('advanced_search.confirm_no'),
          type: 'warning'
        }
      ).then(() => {
        this.queryHistory = [];
        this.saveToLocalStorage();
      }).catch(() => {});
    },

    /**
     * 使用保存的查询
     */
    useSavedItem(item) {
      this.localJql = item.jql;
      this.$emit('input', item.jql);
      this.showSavedPanel = false;
      this.$nextTick(() => {
        this.validateJQL();
      });
    },

    /**
     * 编辑保存的查询
     */
    editSavedItem(index) {
      const item = this.savedQueries[index];
      this.saveForm.name = item.name;
      this.saveForm.shared = item.shared;
      this.saveDialogVisible = true;
      this.saveForm.editingIndex = index;
    },

    /**
     * 删除保存的查询
     */
    deleteSavedItem(index) {
      this.$confirm(
        this.$t('advanced_search.confirm_delete_saved'),
        this.$t('advanced_search.confirm_title'),
        {
          confirmButtonText: this.$t('advanced_search.confirm_yes'),
          cancelButtonText: this.$t('advanced_search.confirm_no'),
          type: 'warning'
        }
      ).then(() => {
        this.savedQueries.splice(index, 1);
        this.saveToLocalStorage();
      }).catch(() => {});
    },

    /**
     * 从本地存储加载
     */
    loadFromLocalStorage() {
      try {
        const history = localStorage.getItem('jql_query_history');
        const saved = localStorage.getItem('jql_saved_queries');
        if (history) {
          this.queryHistory = JSON.parse(history);
        }
        if (saved) {
          this.savedQueries = JSON.parse(saved);
        }
      } catch (error) {
        console.error('加载本地存储失败:', error);
      }
    },

    /**
     * 保存到本地存储
     */
    saveToLocalStorage() {
      try {
        localStorage.setItem('jql_query_history', JSON.stringify(this.queryHistory));
        localStorage.setItem('jql_saved_queries', JSON.stringify(this.savedQueries));
      } catch (error) {
        console.error('保存本地存储失败:', error);
      }
    },

    /**
     * 格式化时间
     */
    formatTime(timestamp) {
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;

      // 1分钟内
      if (diff < 60000) {
        return this.$t('commons.just_now') || '刚刚';
      }
      // 1小时内
      if (diff < 3600000) {
        return Math.floor(diff / 60000) + ' ' + (this.$t('commons.minutes_ago') || '分钟前');
      }
      // 1天内
      if (diff < 86400000) {
        return Math.floor(diff / 3600000) + ' ' + (this.$t('commons.hours_ago') || '小时前');
      }
      // 超过1天显示日期
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
  }
};
</script>

<style scoped lang="scss">
.jql-editor-container {
  position: relative;
}

.jql-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    gap: 8px;
  }
}

.jql-main-area {
  display: flex;
  gap: 16px;

  &.with-side-panel {
    .jql-input-area {
      flex: 1;
    }
  }
}

.side-panel {
  width: 320px;
  flex-shrink: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;

  .panel-content {
    display: flex;
    flex-direction: column;
    height: 100%;
    max-height: 500px;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;

    .panel-title {
      font-weight: 500;
      color: #303133;
    }
  }

  .panel-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    color: #909399;

    i {
      font-size: 48px;
      margin-bottom: 12px;
    }

    p {
      margin: 0;
      font-size: 14px;
    }
  }

  .panel-list {
    flex: 1;
    overflow-y: auto;

    .history-item,
    .saved-item,
    .example-item {
      padding: 12px 16px;
      border-bottom: 1px solid #f5f7fa;
      cursor: pointer;
      transition: background-color 0.2s;

      &:hover {
        background-color: #f5f7fa;
      }

      &:last-child {
        border-bottom: none;
      }
    }

    .history-text,
    .saved-text,
    .example-code {
      font-family: 'Courier New', monospace;
      font-size: 12px;
      color: #606266;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 4px;

      code {
        background: none;
        padding: 0;
        color: inherit;
      }
    }

    .history-time {
      font-size: 12px;
      color: #c0c4cc;
    }

    .saved-name {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 4px;

      i {
        color: #e6a23c;
        font-size: 14px;
      }
    }

    .example-desc {
      font-size: 13px;
      color: #303133;
      margin-bottom: 6px;
    }

    .example-use {
      margin-top: 4px;

      .el-button {
        padding: 0;
        font-size: 12px;
      }
    }

    .history-actions,
    .saved-actions {
      display: flex;
      gap: 8px;
      margin-top: 8px;

      i {
        font-size: 14px;
        color: #909399;
        cursor: pointer;
        padding: 4px;
        border-radius: 2px;

        &:hover {
          color: #409eff;
          background-color: #ecf5ff;
        }
      }
    }
  }
}

.jql-input-area {
  flex: 1;
}

.jql-input-wrapper {
  position: relative;
}

.jql-textarea {
  ::v-deep textarea {
    font-family: 'Courier New', 'Monaco', monospace;
    font-size: 14px;
    line-height: 1.6;
    padding: 12px;
  }
}

.suggestions-dropdown {
  position: absolute;
  z-index: 1000;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.15);
  max-height: 320px;
  overflow-y: auto;

  .suggestion-item {
    padding: 10px 14px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 10px;

    &:hover,
    &.active {
      background-color: #f5f7fa;
    }

    .suggestion-type {
      padding: 2px 8px;
      border-radius: 3px;
      font-size: 11px;
      font-weight: 500;
      flex-shrink: 0;
      text-transform: uppercase;

      &.type-field {
        background-color: #ecf5ff;
        color: #409eff;
      }
      &.type-operator {
        background-color: #f0f9eb;
        color: #67c23a;
      }
      &.type-value {
        background-color: #fdf6ec;
        color: #e6a23c;
      }
      &.type-keyword {
        background-color: #fef0f0;
        color: #f56c6c;
      }
      &.type-recent {
        background-color: #f4f4f5;
        color: #909399;
      }
    }

    .suggestion-value {
      font-weight: 500;
      color: #303133;
      flex-shrink: 0;
      font-family: 'Courier New', monospace;
    }

    .suggestion-desc {
      color: #909399;
      font-size: 12px;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.validation-result {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 4px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;

  &.success {
    background-color: #f0f9eb;
    border: 1px solid #c2e7b0;
    color: #67c23a;
  }

  &.error {
    background-color: #fef0f0;
    border: 1px solid #fde2e2;
    color: #f56c6c;
  }

  i {
    font-size: 16px;
  }

  .error-position {
    color: #909399;
    margin-left: 6px;
  }
}

.shortcut-hints {
  display: flex;
  gap: 20px;
  margin-top: 12px;
  padding: 8px 0;

  .hint-item {
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 4px;

    kbd {
      display: inline-block;
      padding: 2px 6px;
      font-size: 11px;
      font-family: inherit;
      background-color: #f5f7fa;
      border: 1px solid #dcdfe6;
      border-radius: 3px;
      box-shadow: 0 1px 0 #dcdfe6;
    }
  }
}

.jql-help {
  margin-top: 16px;

  .help-content {
    .help-section {
      margin-bottom: 20px;

      &:last-child {
        margin-bottom: 0;
      }

      h4 {
        margin: 0 0 12px;
        font-size: 14px;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 6px;

        i {
          color: #409eff;
        }
      }
    }

    .operator-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 10px;

      .operator-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 6px 10px;
        background-color: #fafafa;
        border-radius: 4px;
        font-size: 13px;

        code {
          padding: 2px 8px;
          background-color: #fff;
          border: 1px solid #e4e7ed;
          border-radius: 3px;
          font-family: 'Courier New', monospace;
          color: #409eff;
          font-weight: 500;
        }

        span {
          color: #606266;
        }
      }
    }

    .example-list {
      margin: 0;
      padding-left: 0;
      list-style: none;

      li {
        margin: 6px 0;
        padding: 8px 12px;
        background-color: #fafafa;
        border-radius: 4px;
        cursor: pointer;
        transition: background-color 0.2s;

        &:hover {
          background-color: #f0f9eb;
        }

        code {
          font-family: 'Courier New', monospace;
          font-size: 13px;
          color: #606266;
          background: none;
          padding: 0;
        }
      }
    }
  }
}
</style>
