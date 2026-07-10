<template>
  <div :class="['sql-query-optimized', {
    'show-advanced-options': advancedOptionsVisible,
    'is-result-collapsed': resultCollapsed
  }]">
    <optimized-sql-query
      @toggle-advanced-options="toggleAdvancedOptions"
      @result-collapsed-change="handleResultCollapsedChange" />
  </div>
</template>

<script>
import Vue from 'vue';
import SqlQuery from './SqlQuery';
import { saveSqlQueryPool } from '@/api/sql-query';

const RESTORE_DELETED_TITLE_ERROR = 'SQL_QUERY_POOL_TITLE_DELETED';
const MIN_EDITOR_HEIGHT = 140;
const FALLBACK_TOOLBAR_HEIGHT = 64;
const FALLBACK_RESIZER_HEIGHT = 8;
const RESULT_COLLAPSE_THRESHOLD = 24;
const originalOpenPoolForm = SqlQuery.methods.openPoolForm;
const originalExecute = SqlQuery.methods.execute;
const originalInsertSqlAsDraft = SqlQuery.methods.insertSqlAsDraft;
const originalStartNewHistory = SqlQuery.methods.startNewHistory;

const OptimizedSqlQuery = Vue.extend({
  extends: SqlQuery,
  data() {
    return {
      resultCollapsed: false
    };
  },
  mounted() {
    window.addEventListener('resize', this.handleResultPanelResize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResultPanelResize);
  },
  methods: {
    /**
     * 顶部原“上传公共池”入口改为查询设置开关。
     * 公共池弹窗内的“上传当前 SQL”仍复用原逻辑。
     */
    openPoolForm(item = null) {
      if (!this.poolDialogVisible && !item) {
        this.$emit('toggle-advanced-options');
        return;
      }
      return originalOpenPoolForm.call(this, item);
    },

    /**
     * 执行查询时自动展开结果区，避免收起后看不到新结果。
     */
    async execute() {
      this.setResultCollapsed(false);
      return originalExecute.call(this);
    },

    /**
     * 清空仅作用于当前编辑区，不再自动创建右侧历史窗口。
     */
    clearSql() {
      this.error = '';
      this.result = null;
      this.editorScrollTop = 0;
      this.setResultCollapsed(false);

      if (this.isSelectedLocalDraft()) {
        this.sql = '';
      } else {
        this.suppressDraftSync = true;
        this.sql = '';
        this.selectedHistory = null;
        this.creatingHistory = true;
      }

      this.$nextTick(() => {
        this.suppressDraftSync = false;
        if (this.$refs.sqlEditor) {
          this.$refs.sqlEditor.scrollTop = 0;
          this.$refs.sqlEditor.focus();
        }
      });
    },

    /**
     * 关闭结果时收起整个下侧结果区，分隔条仍保留在底部。
     */
    closeResult() {
      this.result = null;
      this.error = '';
      this.editorHeight = this.resolveEditorFullHeight();
      this.setResultCollapsed(true);
    },

    /**
     * 从公共池或新增窗口切换 SQL 时，恢复结果区可见。
     */
    insertSqlAsDraft(sql) {
      this.setResultCollapsed(false);
      return originalInsertSqlAsDraft.call(this, sql);
    },

    startNewHistory() {
      this.setResultCollapsed(false);
      return originalStartNewHistory.call(this);
    },

    /**
     * 允许分隔条一路向下拖到底；接近底部时进入收起状态。
     */
    handleEditorResize(event) {
      if (!this.resizingEditor) {
        return;
      }
      const nextHeight = this.resizeStartHeight + event.clientY - this.resizeStartY;
      this.editorHeight = this.normalizeEditorHeight(nextHeight);
    },

    normalizeEditorHeight(height) {
      const maxHeight = this.resolveEditorFullHeight();
      const nextHeight = Math.min(Math.max(MIN_EDITOR_HEIGHT, height), maxHeight);
      const shouldCollapse = nextHeight >= maxHeight - RESULT_COLLAPSE_THRESHOLD;
      this.setResultCollapsed(shouldCollapse);
      return shouldCollapse ? maxHeight : nextHeight;
    },

    resolveEditorFullHeight() {
      const main = this.$refs.queryMain;
      if (!main) {
        return Math.max(MIN_EDITOR_HEIGHT, this.editorHeight || MIN_EDITOR_HEIGHT);
      }
      const toolbar = main.querySelector('.sql-query-toolbar');
      const resizer = main.querySelector('.editor-result-resizer');
      const toolbarHeight = toolbar ? toolbar.offsetHeight : FALLBACK_TOOLBAR_HEIGHT;
      const resizerHeight = resizer ? resizer.offsetHeight : FALLBACK_RESIZER_HEIGHT;
      return Math.max(MIN_EDITOR_HEIGHT, main.clientHeight - toolbarHeight - resizerHeight);
    },

    setResultCollapsed(collapsed) {
      this.resultCollapsed = !!collapsed;
      this.$emit('result-collapsed-change', this.resultCollapsed);
    },

    handleResultPanelResize() {
      if (this.resultCollapsed) {
        this.editorHeight = this.resolveEditorFullHeight();
      }
    },

    /**
     * 公共池简介改为选填；遇到已软删除的同名标题时，确认后恢复原记录。
     */
    async savePoolForm() {
      const title = this.normalizeHistoryTitle(this.poolForm.title);
      const summary = (this.poolForm.summary || '').trim();
      const sql = (this.poolForm.sql || '').trim();
      if (!title) {
        this.$message.error(this.$t('sql_query.title_required'));
        return;
      }
      if (!sql) {
        this.$message.error(this.$t('sql_query.pool_sql_required'));
        return;
      }

      const payload = {
        id: this.poolForm.id,
        sql,
        title,
        summary,
        description: this.poolForm.description
      };

      this.poolSaving = true;
      try {
        let response;
        try {
          response = await saveSqlQueryPool(payload);
        } catch (error) {
          const message = error && (error.message || error.data || String(error));
          if (!message || !message.includes(RESTORE_DELETED_TITLE_ERROR)) {
            throw error;
          }

          await this.$confirm(
            this.$t('sql_query.pool_restore_confirm', { title }),
            this.$t('sql_query.pool_restore_title'),
            { type: 'warning' }
          );
          response = await saveSqlQueryPool({ ...payload, restoreDeleted: true });
        }

        const savedItem = this.normalizePoolItem(response.data);
        this.poolDialogVisible = true;
        this.poolFormVisible = false;
        await this.loadPoolList(savedItem.id);
        this.$message.success(this.$t(
          this.poolFormMode === 'edit' ? 'sql_query.pool_save_success' : 'sql_query.pool_upload_success'
        ));
      } catch (error) {
        if (error === 'cancel' || error === 'close') {
          return;
        }
        this.$message.error(
          (error && (error.message || error.data)) || this.$t('sql_query.pool_save_failed')
        );
      } finally {
        this.poolSaving = false;
      }
    }
  }
});

export default {
  name: 'SqlQueryOptimized',
  components: {
    OptimizedSqlQuery
  },
  data() {
    return {
      advancedOptionsVisible: false,
      resultCollapsed: false
    };
  },
  methods: {
    toggleAdvancedOptions() {
      this.advancedOptionsVisible = !this.advancedOptionsVisible;
    },
    handleResultCollapsedChange(collapsed) {
      this.resultCollapsed = !!collapsed;
    }
  }
};
</script>

<style scoped>
.sql-query-optimized {
  height: 100%;
}

.sql-query-optimized.is-result-collapsed /deep/ .sql-result {
  display: none;
}

.sql-query-optimized.is-result-collapsed /deep/ .editor-result-resizer {
  border-bottom-color: transparent;
}

/* 顶部第三个按钮由“上传公共池”改作查询设置开关，隐藏原上传图标。 */
.sql-query-optimized /deep/ .toolbar-actions > .el-button:nth-of-type(3) i {
  display: none;
}

/* 默认隐藏刷新连接、格式化、行数与超时设置。 */
.sql-query-optimized /deep/ .toolbar-actions > .el-button:nth-of-type(4),
.sql-query-optimized /deep/ .toolbar-actions > .el-button:nth-of-type(5),
.sql-query-optimized /deep/ .toolbar-actions > .toolbar-number {
  display: none;
}

/* 点击“查询设置”后再显示高级设置。 */
.sql-query-optimized.show-advanced-options /deep/ .toolbar-actions > .el-button:nth-of-type(4),
.sql-query-optimized.show-advanced-options /deep/ .toolbar-actions > .el-button:nth-of-type(5) {
  display: inline-block;
}

.sql-query-optimized.show-advanced-options /deep/ .toolbar-actions > .toolbar-number {
  display: inline-flex;
}

/* 公共池打开时即自动加载，去掉重复的刷新按钮。 */
.sql-query-optimized /deep/ .pool-toolbar > .el-button:first-of-type {
  display: none;
}
</style>