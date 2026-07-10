<template>
  <div :class="['sql-query-optimized', { 'show-advanced-options': advancedOptionsVisible }]">
    <optimized-sql-query @toggle-advanced-options="toggleAdvancedOptions" />
  </div>
</template>

<script>
import Vue from 'vue';
import SqlQuery from './SqlQuery';
import { saveSqlQueryPool } from '@/api/sql-query';

const RESTORE_DELETED_TITLE_ERROR = 'SQL_QUERY_POOL_TITLE_DELETED';
const originalOpenPoolForm = SqlQuery.methods.openPoolForm;

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
     * 清空仅作用于当前编辑区，不再自动创建右侧历史窗口。
     */
    clearSql() {
      this.error = '';
      this.result = null;
      this.editorScrollTop = 0;

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
      advancedOptionsVisible: false
    };
  },
  methods: {
    toggleAdvancedOptions() {
      this.advancedOptionsVisible = !this.advancedOptionsVisible;
    }
  }
};
</script>

<style scoped>
.sql-query-optimized {
  height: 100%;
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
