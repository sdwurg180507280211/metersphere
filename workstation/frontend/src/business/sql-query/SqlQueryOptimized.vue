<template>
  <div
    :class="[
      'sql-query-optimized',
      {
        'show-advanced-options': advancedOptionsVisible,
        'is-result-collapsed': resultCollapsed
      }
    ]">
    <el-button
      v-if="showFloatingResultClose"
      class="result-floating-close"
      :style="resultCloseStyle"
      type="text"
      size="mini"
      icon="el-icon-close"
      @click="collapseResult">
      {{ $t('sql_query.close') }}
    </el-button>
    <optimized-sql-query
      ref="query"
      @toggle-advanced-options="toggleAdvancedOptions"
      @result-collapsed-change="handleResultCollapsedChange"
      @result-state-change="handleResultStateChange" />
  </div>
</template>

<script>
import Vue from 'vue';
import SqlQuery from './SqlQuery';
import { saveSqlQueryPool } from '@/api/sql-query';

const RESTORE_DELETED_TITLE_ERROR = 'SQL_QUERY_POOL_TITLE_DELETED';
const originalOpenPoolForm = SqlQuery.methods.openPoolForm;
const originalExecute = SqlQuery.methods.execute;
const originalInsertSqlAsDraft = SqlQuery.methods.insertSqlAsDraft;
const originalStartNewHistory = SqlQuery.methods.startNewHistory;

const OptimizedSqlQuery = Vue.extend({
  extends: SqlQuery,
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
     * 执行查询时自动展开结果区，避免上次收起后看不到新结果。
     */
    async execute() {
      this.$emit('result-collapsed-change', false);
      this.$emit('result-state-change', false);
      try {
        return await originalExecute.call(this);
      } finally {
        this.$emit('result-state-change', !!this.result);
      }
    },

    /**
     * 清空仅作用于当前编辑区，不再自动创建右侧历史窗口。
     */
    clearSql() {
      this.error = '';
      this.result = null;
      this.editorScrollTop = 0;
      this.$emit('result-state-change', false);

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
     * 关闭按钮语义调整为收起整个结果区，而不是只清空结果数据。
     */
    closeResult() {
      this.result = null;
      this.error = '';
      this.$emit('result-state-change', false);
      this.$emit('result-collapsed-change', true);
    },

    insertSqlAsDraft(sql) {
      const result = originalInsertSqlAsDraft.call(this, sql);
      this.$emit('result-collapsed-change', false);
      this.$emit('result-state-change', false);
      return result;
    },

    startNewHistory() {
      const result = originalStartNewHistory.call(this);
      this.$emit('result-collapsed-change', false);
      this.$emit('result-state-change', false);
      return result;
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
      resultCollapsed: false,
      resultHasData: false,
      resultCloseStyle: {
        top: '0px',
        right: '16px'
      }
    };
  },
  computed: {
    showFloatingResultClose() {
      return !this.resultCollapsed && !this.resultHasData;
    }
  },
  mounted() {
    this.syncResultClosePosition();
    window.addEventListener('resize', this.syncResultClosePosition);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncResultClosePosition);
  },
  methods: {
    toggleAdvancedOptions() {
      this.advancedOptionsVisible = !this.advancedOptionsVisible;
      this.syncResultClosePosition();
    },
    handleResultCollapsedChange(collapsed) {
      this.resultCollapsed = !!collapsed;
      this.syncResultClosePosition();
    },
    handleResultStateChange(hasResult) {
      this.resultHasData = !!hasResult;
      this.syncResultClosePosition();
    },
    collapseResult() {
      const query = this.$refs.query;
      if (query && typeof query.closeResult === 'function') {
        query.closeResult();
        return;
      }
      this.resultCollapsed = true;
    },
    syncResultClosePosition() {
      this.$nextTick(() => {
        if (this.resultCollapsed) {
          return;
        }
        const wrapper = this.$el;
        const query = this.$refs.query && this.$refs.query.$el;
        if (!wrapper || !query) {
          return;
        }
        const result = query.querySelector('.sql-result');
        if (!result) {
          return;
        }
        const wrapperRect = wrapper.getBoundingClientRect();
        const resultRect = result.getBoundingClientRect();
        if (!resultRect.width && !resultRect.height) {
          return;
        }
        this.resultCloseStyle = {
          top: `${Math.max(0, resultRect.top - wrapperRect.top + 8)}px`,
          right: `${Math.max(16, wrapperRect.right - resultRect.right + 16)}px`
        };
      });
    }
  }
};
</script>

<style scoped>
.sql-query-optimized {
  height: 100%;
  position: relative;
}

.result-floating-close {
  position: absolute;
  z-index: 20;
  padding: 2px 4px;
}

.sql-query-optimized.is-result-collapsed /deep/ .editor-result-resizer,
.sql-query-optimized.is-result-collapsed /deep/ .sql-result {
  display: none;
}

.sql-query-optimized.is-result-collapsed /deep/ .sql-editor {
  flex: 1 1 auto !important;
  height: auto !important;
  min-height: 0;
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