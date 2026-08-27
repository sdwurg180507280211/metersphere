<script>
import SqlQuery from './SqlQuery';

const DEFAULT_EXPANDED_EDITOR_HEIGHT = 240;
const originalExecute = SqlQuery.methods.execute;
const originalInsertSqlAsDraft = SqlQuery.methods.insertSqlAsDraft;
const originalStartNewHistory = SqlQuery.methods.startNewHistory;
const originalUseHistorySql = SqlQuery.methods.useHistorySql;

export default {
  name: 'SqlQueryResultExpand',
  extends: SqlQuery,
  methods: {
    expandResultArea() {
      if (this.resultCollapsed) {
        this.editorHeight = DEFAULT_EXPANDED_EDITOR_HEIGHT;
      }
      this.setResultCollapsed(false);
    },

    async execute() {
      if (!this.sql.trim()) {
        return;
      }
      this.expandResultArea();
      return originalExecute.call(this);
    },

    insertSqlAsDraft(sql) {
      this.expandResultArea();
      return originalInsertSqlAsDraft.call(this, sql);
    },

    startNewHistory() {
      this.expandResultArea();
      return originalStartNewHistory.call(this);
    },

    useHistorySql(item, focusEditor = true) {
      this.expandResultArea();
      return originalUseHistorySql.call(this, item, focusEditor);
    }
  }
};
</script>
