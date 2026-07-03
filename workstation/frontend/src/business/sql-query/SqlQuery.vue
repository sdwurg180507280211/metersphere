<template>
  <div class="sql-query-page">
    <section ref="queryMain" class="sql-query-main">
      <div class="sql-query-toolbar">
        <div class="toolbar-title">
          <h2>{{ $t('sql_query.title') }}</h2>
          <span :class="['connection-badge', status.connected ? 'is-online' : 'is-offline']">
            <i></i>
            {{ connectionText }}
          </span>
        </div>
        <div class="toolbar-actions">
          <el-button
            size="small"
            icon="el-icon-document-checked"
            :disabled="!sql.trim()"
            @click="openDetailDialog">
            {{ $t('sql_query.detail') }}
          </el-button>
          <el-button size="small" icon="el-icon-collection" @click="openPoolDialog">
            {{ $t('sql_query.pool') }}
          </el-button>
          <el-button
            size="small"
            icon="el-icon-upload2"
            :disabled="!sql.trim()"
            @click="openPoolForm()">
            {{ $t('sql_query.upload_pool') }}
          </el-button>
          <el-button size="small" icon="el-icon-refresh" @click="loadStatus">
            {{ $t('sql_query.refresh') }}
          </el-button>
          <el-button size="small" icon="el-icon-magic-stick" @click="formatSql">
            {{ $t('sql_query.format') }}
          </el-button>
          <el-button size="small" icon="el-icon-delete" @click="clearSql">
            {{ $t('sql_query.clear') }}
          </el-button>
          <div class="toolbar-number">
            <span>{{ $t('sql_query.limit') }}</span>
            <el-input-number
              v-model="limit"
              class="limit-input"
              size="small"
              :min="1"
              :max="5000"
              :step="100"
              controls-position="right"/>
          </div>
          <div class="toolbar-number">
            <span>{{ $t('sql_query.timeout_seconds') }}</span>
            <el-input-number
              v-model="timeoutSeconds"
              class="timeout-input"
              size="small"
              :min="1"
              :max="300"
              :step="5"
              controls-position="right"/>
          </div>
          <el-button
            type="primary"
            size="small"
            icon="el-icon-video-play"
            :loading="loading"
            :disabled="!sql.trim()"
            @click="execute">
            {{ $t('sql_query.run') }}
          </el-button>
        </div>
      </div>

      <div class="sql-editor" :style="editorStyle">
        <div class="editor-gutter">
          <div class="editor-gutter-lines" :style="gutterStyle">
            <span v-for="line in lineNumbers" :key="line">{{ line }}</span>
          </div>
        </div>
        <textarea
          ref="sqlEditor"
          v-model="sql"
          class="sql-textarea"
          spellcheck="false"
          :placeholder="$t('sql_query.placeholder')"
          @scroll="handleEditorScroll"
          @keydown="handleKeydown"/>
      </div>

      <div
        :class="['editor-result-resizer', resizingEditor ? 'is-dragging' : '']"
        @mousedown="startEditorResize"></div>

      <div class="sql-result">
        <div v-if="!result && !error && !loading" class="result-empty">
          <i class="el-icon-data-analysis"></i>
          <p>{{ $t('sql_query.empty') }}</p>
          <div class="quick-hints">
            <span>Ctrl / Cmd + Enter</span>
            <span>{{ $t('sql_query.select_only') }}</span>
            <span>{{ $t('sql_query.max_rows', { count: 5000 }) }}</span>
          </div>
        </div>

        <div v-if="loading && !result" class="result-loading">
          <i class="el-icon-loading"></i>
          <p>{{ $t('sql_query.loading') }}</p>
        </div>

        <div v-if="error" class="result-error">
          <div class="error-title">
            <i class="el-icon-warning-outline"></i>
            {{ $t('sql_query.failed') }}
          </div>
          <pre>{{ error }}</pre>
        </div>

        <div v-if="result" class="result-panel">
          <div class="result-meta">
            <div class="meta-left">
              <el-tag size="mini" type="success">{{ $t('sql_query.success') }}</el-tag>
              <span>{{ $t('sql_query.rows', { count: result.rowCount || 0 }) }}</span>
              <span>{{ $t('sql_query.cost', { time: result.executionTime || 0 }) }}</span>
              <el-tag v-if="result.truncated" size="mini" type="warning">
                {{ $t('sql_query.truncated') }}
              </el-tag>
            </div>
            <div class="meta-right">
              <el-popover
                placement="bottom-end"
                width="260"
                trigger="hover">
                <div class="export-popover">
                  <div class="export-popover-title">{{ $t('sql_query.excel_name') }}</div>
                  <el-input
                    v-model="excelName"
                    size="mini"
                    clearable
                    :placeholder="$t('sql_query.excel_name_placeholder')"
                    @keyup.enter.native="exportXlsx"/>
                </div>
                <el-button slot="reference" type="text" size="mini" :disabled="!hasRows" @click="exportXlsx">
                  {{ $t('sql_query.export_xlsx') }}
                </el-button>
              </el-popover>
              <el-button type="text" size="mini" @click="closeResult">
                {{ $t('sql_query.close') }}
              </el-button>
            </div>
          </div>

          <div class="table-wrap">
            <table class="result-table">
              <thead>
              <tr>
                <th class="row-index">#</th>
                <th v-for="(column, columnIndex) in result.columns" :key="columnKey(column, columnIndex)">
                  {{ columnLabel(column) }}
                </th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(row, rowIndex) in pagedRows" :key="rowIndex">
                <td class="row-index">{{ (currentPage - 1) * pageSize + rowIndex + 1 }}</td>
                <td
                  v-for="(column, columnIndex) in result.columns"
                  :key="columnKey(column, columnIndex)"
                  :title="stringifyValue(row[columnKey(column, columnIndex)])">
                  <span :class="cellClass(row[columnKey(column, columnIndex)])">
                    {{ stringifyValue(row[columnKey(column, columnIndex)]) }}
                  </span>
                </td>
              </tr>
              </tbody>
            </table>
          </div>

          <div v-if="totalPages > 1" class="result-pagination">
            <span>
              {{ $t('sql_query.page_info', {
                start: (currentPage - 1) * pageSize + 1,
                end: Math.min(currentPage * pageSize, result.rows.length),
                total: result.rows.length
              }) }}
            </span>
            <el-pagination
              small
              layout="prev, pager, next, sizes"
              :current-page="currentPage"
              :page-size="pageSize"
              :page-sizes="[50, 100, 200, 500, 1000]"
              :total="result.rows.length"
              @current-change="handleCurrentChange"
              @size-change="handlePageSizeChange"/>
          </div>
        </div>
      </div>
    </section>

    <div
      :class="['history-width-resizer', resizingHistory ? 'is-dragging' : '']"
      @mousedown="startHistoryResize"></div>

    <aside class="sql-history" :style="historyStyle">
      <div class="history-header">
        <h3>{{ $t('sql_query.history') }}</h3>
        <el-button v-if="localHistory.length" type="text" size="mini" @click="clearHistory">
          {{ $t('sql_query.clear_history') }}
        </el-button>
      </div>
      <div class="history-list">
        <button
          class="history-card history-create-card"
          type="button"
          @click="startNewHistory">
          {{ $t('sql_query.new_history_title') }}
        </button>
        <button
          v-for="(item, index) in history"
          :key="item.id || item.localId || item.timestamp || index"
          :class="['history-card', isHistorySelected(item) ? 'is-selected' : '']"
          type="button"
          @click="useHistorySql(item)">
          <span v-if="item.saved" class="history-saved-tag">{{ $t('sql_query.saved') }}</span>
          <span class="history-title">{{ resolveHistoryTitle(item) }}</span>
          <span class="history-time">{{ formatTime(item.timestamp) }}</span>
        </button>
        <div v-if="!history.length" class="history-empty">
          {{ $t('sql_query.no_history') }}
        </div>
      </div>
    </aside>

    <el-dialog
      :title="$t('sql_query.history_detail')"
      :visible.sync="historyDialogVisible"
      width="720px"
      append-to-body>
      <div class="history-detail-form">
        <label>{{ $t('sql_query.history_record_title') }}</label>
        <el-input
          v-model="historyForm.title"
          maxlength="120"
          show-word-limit
          :placeholder="$t('sql_query.history_record_title_placeholder')"/>
        <label>{{ $t('sql_query.sql_content') }}</label>
        <el-input
          v-model="historyForm.sql"
          class="history-detail-sql"
          type="textarea"
          :rows="13"/>
        <label>{{ $t('sql_query.description') }}</label>
        <el-input
          v-model="historyForm.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          :placeholder="$t('sql_query.description_placeholder')"/>
      </div>
      <div slot="footer" class="history-dialog-footer">
        <el-button
          v-if="historyForm.id"
          type="danger"
          size="small"
          plain
          :loading="deletingHistory"
          @click="deleteHistoryFromDatabase">
          {{ $t('sql_query.delete') }}
        </el-button>
        <div class="history-dialog-footer-right">
          <el-button size="small" @click="historyDialogVisible = false">
            {{ $t('sql_query.cancel') }}
          </el-button>
          <el-button type="primary" size="small" :loading="savingHistory" @click="saveHistoryToDatabase">
            {{ $t('sql_query.save') }}
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      class="sql-pool-dialog"
      :title="$t('sql_query.pool')"
      :visible.sync="poolDialogVisible"
      width="86vw"
      top="7vh"
      append-to-body>
      <div class="pool-toolbar">
        <el-input
          v-model="poolKeyword"
          class="pool-search"
          size="small"
          clearable
          prefix-icon="el-icon-search"
          :placeholder="$t('sql_query.pool_search_placeholder')"
          @keyup.enter.native="loadPoolList"
          @clear="loadPoolList"/>
        <el-checkbox v-model="poolOnlyMine" @change="loadPoolList">
          {{ $t('sql_query.pool_only_mine') }}
        </el-checkbox>
        <el-button size="small" icon="el-icon-refresh" :loading="poolLoading" @click="loadPoolList">
          {{ $t('sql_query.refresh') }}
        </el-button>
        <el-button
          type="primary"
          size="small"
          icon="el-icon-upload2"
          :disabled="!sql.trim()"
          @click="openPoolForm()">
          {{ $t('sql_query.pool_upload_current') }}
        </el-button>
      </div>

      <div v-loading="poolLoading" class="pool-body">
        <div class="pool-list">
          <button
            v-for="item in poolList"
            :key="item.id"
            :class="['pool-card', isPoolSelected(item) ? 'is-selected' : '']"
            type="button"
            @click="selectPoolItem(item)">
            <span class="pool-card-title">{{ item.title }}</span>
            <span class="pool-card-summary">{{ item.summary }}</span>
            <span class="pool-card-meta">{{ formatPoolMeta(item) }}</span>
          </button>
          <div v-if="!poolList.length && !poolLoading" class="pool-empty">
            {{ $t('sql_query.pool_empty') }}
          </div>
        </div>

        <div v-if="selectedPoolItem" class="pool-detail">
          <div class="pool-detail-header">
            <div class="pool-detail-title">
              <h3>{{ selectedPoolItem.title }}</h3>
              <p>{{ selectedPoolItem.summary }}</p>
            </div>
            <el-tag size="mini" type="info">
              {{ $t('sql_query.pool_use_count', { count: selectedPoolItem.useCount || 0 }) }}
            </el-tag>
          </div>
          <div class="pool-detail-meta">
            <span>{{ selectedPoolItem.createUserName || selectedPoolItem.createUser }}</span>
            <span>{{ formatTime(selectedPoolItem.updateTime || selectedPoolItem.createTime) }}</span>
          </div>
          <label>{{ $t('sql_query.sql_content') }}</label>
          <pre class="pool-sql-preview">{{ selectedPoolItem.sql }}</pre>
          <label>{{ $t('sql_query.description') }}</label>
          <div class="pool-description">
            {{ selectedPoolItem.description || $t('sql_query.pool_no_description') }}
          </div>
          <div class="pool-detail-actions">
            <el-button
              type="primary"
              size="small"
              :loading="insertingPool"
              @click="insertPoolToConsole">
              {{ $t('sql_query.pool_insert_console') }}
            </el-button>
            <el-button
              size="small"
              :loading="copyingPool"
              @click="copyPoolToMyHistory">
              {{ $t('sql_query.pool_copy_to_my') }}
            </el-button>
            <el-button size="small" @click="openPoolForm(selectedPoolItem)">
              {{ $t('sql_query.pool_edit') }}
            </el-button>
            <el-button
              type="danger"
              size="small"
              plain
              :loading="offliningPool"
              @click="offlinePoolItem">
              {{ $t('sql_query.pool_offline') }}
            </el-button>
          </div>
        </div>
        <div v-else class="pool-detail pool-detail-empty">
          {{ $t('sql_query.pool_detail_empty') }}
        </div>
      </div>
    </el-dialog>

    <el-dialog
      :title="poolFormMode === 'edit' ? $t('sql_query.pool_edit_title') : $t('sql_query.pool_upload_title')"
      :visible.sync="poolFormVisible"
      width="720px"
      append-to-body>
      <div class="pool-form">
        <label>{{ $t('sql_query.history_record_title') }}</label>
        <el-input
          v-model="poolForm.title"
          maxlength="120"
          show-word-limit
          :placeholder="$t('sql_query.history_record_title_placeholder')"/>
        <label>{{ $t('sql_query.pool_summary') }}</label>
        <el-input
          v-model="poolForm.summary"
          maxlength="200"
          show-word-limit
          :placeholder="$t('sql_query.pool_summary_placeholder')"/>
        <label>{{ $t('sql_query.sql_content') }}</label>
        <el-input
          v-model="poolForm.sql"
          class="pool-form-sql"
          type="textarea"
          :rows="12"/>
        <label>{{ $t('sql_query.description') }}</label>
        <el-input
          v-model="poolForm.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          :placeholder="$t('sql_query.description_placeholder')"/>
      </div>
      <div slot="footer" class="history-dialog-footer">
        <el-button size="small" @click="poolFormVisible = false">
          {{ $t('sql_query.cancel') }}
        </el-button>
        <el-button type="primary" size="small" :loading="poolSaving" @click="savePoolForm">
          {{ $t('sql_query.save') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  copySqlQueryPoolToHistory,
  getSqlQueryPool,
  executeSqlQuery,
  getSqlQueryHistory,
  getSqlQueryStatus,
  offlineSqlQueryPool,
  recordSqlQueryPoolUse,
  saveSqlQueryPool,
  saveSqlQueryHistory,
  deleteSqlQueryHistory
} from '@/api/sql-query';

const HISTORY_KEY = 'workstation-sql-query-history';
const DEFAULT_EDITOR_HEIGHT = 240;
const MIN_EDITOR_HEIGHT = 140;
const MIN_RESULT_HEIGHT = 220;
const DEFAULT_HISTORY_WIDTH = 300;
const MIN_HISTORY_WIDTH = 240;
const MAX_HISTORY_WIDTH = 520;
const INDENT_TEXT = '  ';
const SQL_LINE_COMMENT = '-- ';
const XLSX_MIME_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
const CRC_TABLE = createCrcTable();

function createCrcTable() {
  const table = [];
  for (let i = 0; i < 256; i++) {
    let value = i;
    for (let j = 0; j < 8; j++) {
      value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
    }
    table[i] = value >>> 0;
  }
  return table;
}

function crc32(bytes) {
  let crc = 0xffffffff;
  for (let i = 0; i < bytes.length; i++) {
    crc = CRC_TABLE[(crc ^ bytes[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function writeUint16(target, value) {
  target.push(value & 0xff, (value >>> 8) & 0xff);
}

function writeUint32(target, value) {
  target.push(value & 0xff, (value >>> 8) & 0xff, (value >>> 16) & 0xff, (value >>> 24) & 0xff);
}

function concatUint8Arrays(parts) {
  const totalLength = parts.reduce((total, part) => total + part.length, 0);
  const result = new Uint8Array(totalLength);
  let offset = 0;
  parts.forEach(part => {
    result.set(part, offset);
    offset += part.length;
  });
  return result;
}

function encodeText(value) {
  return new TextEncoder().encode(value);
}

function createZip(files) {
  const localParts = [];
  const centralParts = [];
  let offset = 0;

  files.forEach(file => {
    const nameBytes = encodeText(file.name);
    const dataBytes = encodeText(file.content);
    const checksum = crc32(dataBytes);
    const localHeader = [];

    writeUint32(localHeader, 0x04034b50);
    writeUint16(localHeader, 20);
    writeUint16(localHeader, 0);
    writeUint16(localHeader, 0);
    writeUint16(localHeader, 0);
    writeUint16(localHeader, 0);
    writeUint32(localHeader, checksum);
    writeUint32(localHeader, dataBytes.length);
    writeUint32(localHeader, dataBytes.length);
    writeUint16(localHeader, nameBytes.length);
    writeUint16(localHeader, 0);

    const localPart = concatUint8Arrays([new Uint8Array(localHeader), nameBytes, dataBytes]);
    localParts.push(localPart);

    const centralHeader = [];
    writeUint32(centralHeader, 0x02014b50);
    writeUint16(centralHeader, 20);
    writeUint16(centralHeader, 20);
    writeUint16(centralHeader, 0);
    writeUint16(centralHeader, 0);
    writeUint16(centralHeader, 0);
    writeUint16(centralHeader, 0);
    writeUint32(centralHeader, checksum);
    writeUint32(centralHeader, dataBytes.length);
    writeUint32(centralHeader, dataBytes.length);
    writeUint16(centralHeader, nameBytes.length);
    writeUint16(centralHeader, 0);
    writeUint16(centralHeader, 0);
    writeUint16(centralHeader, 0);
    writeUint16(centralHeader, 0);
    writeUint32(centralHeader, 0);
    writeUint32(centralHeader, offset);
    centralParts.push(concatUint8Arrays([new Uint8Array(centralHeader), nameBytes]));

    offset += localPart.length;
  });

  const centralDirectory = concatUint8Arrays(centralParts);
  const endRecord = [];
  writeUint32(endRecord, 0x06054b50);
  writeUint16(endRecord, 0);
  writeUint16(endRecord, 0);
  writeUint16(endRecord, files.length);
  writeUint16(endRecord, files.length);
  writeUint32(endRecord, centralDirectory.length);
  writeUint32(endRecord, offset);
  writeUint16(endRecord, 0);

  return concatUint8Arrays([...localParts, centralDirectory, new Uint8Array(endRecord)]);
}

export default {
  name: 'SqlQuery',
  data() {
    return {
      sql: '',
      limit: 1000,
      timeoutSeconds: 30,
      loading: false,
      status: {
        connected: false
      },
      result: null,
      error: '',
      history: [],
      localHistory: [],
      savedHistory: [],
      currentPage: 1,
      pageSize: 100,
      excelName: '',
      editorHeight: DEFAULT_EDITOR_HEIGHT,
      editorScrollTop: 0,
      resizingEditor: false,
      resizeStartY: 0,
      resizeStartHeight: DEFAULT_EDITOR_HEIGHT,
      historyWidth: DEFAULT_HISTORY_WIDTH,
      resizingHistory: false,
      historyResizeStartX: 0,
      historyResizeStartWidth: DEFAULT_HISTORY_WIDTH,
      previousBodyCursor: '',
      previousBodyUserSelect: '',
      historyDialogVisible: false,
      savingHistory: false,
      deletingHistory: false,
      historyForm: {
        id: '',
        sql: '',
        title: '',
        description: '',
        saved: false,
        timestamp: null
      },
      selectedHistory: null,
      creatingHistory: false,
      suppressDraftSync: false,
      poolDialogVisible: false,
      poolLoading: false,
      poolKeyword: '',
      poolOnlyMine: false,
      poolList: [],
      selectedPoolItem: null,
      poolFormVisible: false,
      poolFormMode: 'create',
      poolSaving: false,
      copyingPool: false,
      insertingPool: false,
      offliningPool: false,
      poolForm: {
        id: '',
        sql: '',
        title: '',
        summary: '',
        description: ''
      }
    };
  },
  computed: {
    editorStyle() {
      return {
        height: `${this.editorHeight}px`
      };
    },
    gutterStyle() {
      return {
        transform: `translateY(-${this.editorScrollTop}px)`
      };
    },
    historyStyle() {
      return {
        width: `${this.historyWidth}px`
      };
    },
    lineNumbers() {
      return Math.max(this.sql.split('\n').length, 1);
    },
    connectionText() {
      if (this.status.connected) {
        return this.status.database || this.$t('sql_query.connected');
      }
      return this.status.message || this.$t('sql_query.disconnected');
    },
    hasRows() {
      return this.result && this.result.rows && this.result.rows.length > 0;
    },
    totalPages() {
      if (!this.hasRows) {
        return 1;
      }
      return Math.max(1, Math.ceil(this.result.rows.length / this.pageSize));
    },
    pagedRows() {
      if (!this.hasRows) {
        return [];
      }
      const start = (this.currentPage - 1) * this.pageSize;
      return this.result.rows.slice(start, start + this.pageSize);
    }
  },
  watch: {
    sql(value) {
      this.persistLocalDraft(value);
    }
  },
  mounted() {
    this.loadStatus();
    this.loadHistory();
  },
  beforeDestroy() {
    this.stopEditorResize();
    this.stopHistoryResize();
  },
  methods: {
    async loadStatus() {
      try {
        const response = await getSqlQueryStatus();
        this.status = response.data || { connected: false };
      } catch (e) {
        this.status = {
          connected: false,
          message: e.message || this.$t('sql_query.status_failed')
        };
      }
    },
    async execute() {
      if (!this.sql.trim()) {
        return;
      }
      this.loading = true;
      this.error = '';
      this.result = null;
      this.currentPage = 1;
      try {
        const response = await executeSqlQuery(this.sql, this.limit, this.timeoutSeconds);
        this.result = response.data;
        this.saveHistory(this.sql);
      } catch (e) {
        this.error = e.message || e.data || this.$t('sql_query.failed');
      } finally {
        this.loading = false;
      }
    },
    handleKeydown(event) {
      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
        event.preventDefault();
        this.execute();
        return;
      }
      if (event.key === 'Tab') {
        event.preventDefault();
        if (event.shiftKey) {
          this.handleShiftTab(event.target);
        } else {
          this.indentSelection(event.target);
        }
        return;
      }
      if (event.key === 'Enter' && !event.shiftKey && !event.altKey && !event.ctrlKey && !event.metaKey) {
        event.preventDefault();
        this.insertIndentedNewLine(event.target);
        return;
      }
      if ((event.ctrlKey || event.metaKey) && event.key === '/') {
        event.preventDefault();
        this.toggleLineComment(event.target);
        return;
      }
      if ((event.ctrlKey || event.metaKey) && event.key === ']') {
        event.preventDefault();
        this.indentSelectedLines(event.target);
        return;
      }
      if ((event.ctrlKey || event.metaKey) && event.key === '[') {
        event.preventDefault();
        this.outdentSelectedLines(event.target, false);
        return;
      }
      if (event.key === 'Home' && !event.ctrlKey && !event.metaKey && !event.altKey) {
        event.preventDefault();
        this.moveToSmartLineStart(event.target, event.shiftKey);
      }
    },
    handleShiftTab(editor) {
      if (editor.selectionStart !== editor.selectionEnd) {
        this.outdentSelectedLines(editor, false);
        return;
      }
      this.moveToSmartLineStart(editor, false);
    },
    indentSelection(editor) {
      if (editor.selectionStart !== editor.selectionEnd) {
        this.indentSelectedLines(editor);
        return;
      }
      this.replaceEditorRange(editor, editor.selectionStart, editor.selectionEnd, INDENT_TEXT);
    },
    indentSelectedLines(editor) {
      this.transformSelectedLines(editor, line => ({
        text: INDENT_TEXT + line,
        editStart: 0,
        oldLength: 0,
        newLength: INDENT_TEXT.length
      }));
    },
    outdentSelectedLines(editor, moveToLineStartWhenUnchanged = true) {
      const selectionStart = editor.selectionStart;
      const selectionEnd = editor.selectionEnd;
      const transformed = this.transformSelectedLines(editor, line => {
        const removeLength = this.getIndentRemoveLength(line);
        if (!removeLength) {
          return { text: line };
        }
        return {
          text: line.slice(removeLength),
          editStart: 0,
          oldLength: removeLength,
          newLength: 0
        };
      });
      if (moveToLineStartWhenUnchanged && selectionStart === selectionEnd && !transformed) {
        this.moveToSmartLineStart(editor, false);
      }
    },
    getIndentRemoveLength(line) {
      if (line.startsWith(INDENT_TEXT)) {
        return INDENT_TEXT.length;
      }
      if (line.startsWith('\t')) {
        return 1;
      }
      const spaces = line.match(/^ +/);
      return spaces ? Math.min(spaces[0].length, INDENT_TEXT.length) : 0;
    },
    insertIndentedNewLine(editor) {
      const value = this.sql;
      const lineStart = this.getLineStart(value, editor.selectionStart);
      const lineBeforeCursor = value.slice(lineStart, editor.selectionStart);
      const indent = (lineBeforeCursor.match(/^\s*/) || [''])[0];
      this.replaceEditorRange(editor, editor.selectionStart, editor.selectionEnd, `\n${indent}`);
    },
    toggleLineComment(editor) {
      const value = this.sql;
      const bounds = this.getSelectedLineBounds(value, editor.selectionStart, editor.selectionEnd);
      const lines = value.slice(bounds.start, bounds.end).split('\n');
      const shouldUncomment = lines
        .filter(line => line.trim())
        .every(line => line.slice(this.getLeadingWhitespaceLength(line)).startsWith('--'));

      this.transformSelectedLines(editor, line => {
        if (!line.trim()) {
          return { text: line };
        }
        const indentLength = this.getLeadingWhitespaceLength(line);
        if (shouldUncomment) {
          const commentLength = line.slice(indentLength).startsWith(SQL_LINE_COMMENT)
            ? SQL_LINE_COMMENT.length
            : 2;
          return {
            text: line.slice(0, indentLength) + line.slice(indentLength + commentLength),
            editStart: indentLength,
            oldLength: commentLength,
            newLength: 0
          };
        }
        return {
          text: line.slice(0, indentLength) + SQL_LINE_COMMENT + line.slice(indentLength),
          editStart: indentLength,
          oldLength: 0,
          newLength: SQL_LINE_COMMENT.length
        };
      });
    },
    moveToSmartLineStart(editor, extendSelection) {
      const value = this.sql;
      const lineStart = this.getLineStart(value, editor.selectionStart);
      const lineEnd = this.getLineEnd(value, editor.selectionStart);
      const line = value.slice(lineStart, lineEnd);
      const firstContentIndex = line.search(/\S/);
      const smartStart = firstContentIndex >= 0 ? lineStart + firstContentIndex : lineStart;
      const target = editor.selectionStart === smartStart ? lineStart : smartStart;

      if (extendSelection) {
        editor.setSelectionRange(target, editor.selectionEnd);
      } else {
        editor.setSelectionRange(target, target);
      }
    },
    transformSelectedLines(editor, transformer) {
      const value = this.sql;
      const selectionStart = editor.selectionStart;
      const selectionEnd = editor.selectionEnd;
      const bounds = this.getSelectedLineBounds(value, selectionStart, selectionEnd);
      const block = value.slice(bounds.start, bounds.end);
      const lines = block.split('\n');
      const edits = [];
      let offset = bounds.start;
      const updatedBlock = lines.map(line => {
        const result = transformer(line) || { text: line };
        if ((result.oldLength || result.newLength) && result.editStart !== undefined) {
          edits.push({
            index: offset + result.editStart,
            oldLength: result.oldLength || 0,
            newLength: result.newLength || 0
          });
        }
        offset += line.length + 1;
        return result.text;
      }).join('\n');

      if (updatedBlock === block) {
        return false;
      }

      const newValue = value.slice(0, bounds.start) + updatedBlock + value.slice(bounds.end);
      const newSelectionStart = this.adjustSelectionIndex(selectionStart, edits, true);
      const newSelectionEnd = this.adjustSelectionIndex(selectionEnd, edits, true);
      this.updateEditorValue(editor, newValue, newSelectionStart, newSelectionEnd);
      return true;
    },
    adjustSelectionIndex(index, edits, includeInsertionAtBoundary) {
      let adjusted = index;
      edits.forEach(edit => {
        if (edit.oldLength > 0) {
          if (index > edit.index + edit.oldLength) {
            adjusted += edit.newLength - edit.oldLength;
          } else if (index > edit.index) {
            adjusted += edit.newLength - (index - edit.index);
          }
          return;
        }
        if (edit.newLength > 0 && (index > edit.index || (includeInsertionAtBoundary && index === edit.index))) {
          adjusted += edit.newLength;
        }
      });
      return adjusted;
    },
    replaceEditorRange(editor, start, end, text) {
      const value = this.sql;
      const newValue = value.slice(0, start) + text + value.slice(end);
      const cursor = start + text.length;
      this.updateEditorValue(editor, newValue, cursor, cursor);
    },
    updateEditorValue(editor, value, selectionStart, selectionEnd) {
      this.sql = value;
      this.$nextTick(() => {
        editor.focus();
        editor.setSelectionRange(selectionStart, selectionEnd);
      });
    },
    getSelectedLineBounds(value, selectionStart, selectionEnd) {
      const start = this.getLineStart(value, selectionStart);
      const endIndex = selectionEnd > selectionStart && value[selectionEnd - 1] === '\n'
        ? selectionEnd - 1
        : selectionEnd;
      return {
        start,
        end: this.getLineEnd(value, endIndex)
      };
    },
    getLineStart(value, index) {
      return value.lastIndexOf('\n', Math.max(0, index - 1)) + 1;
    },
    getLineEnd(value, index) {
      const lineEnd = value.indexOf('\n', index);
      return lineEnd === -1 ? value.length : lineEnd;
    },
    getLeadingWhitespaceLength(line) {
      const whitespace = line.match(/^\s*/);
      return whitespace ? whitespace[0].length : 0;
    },
    handleEditorScroll(event) {
      this.editorScrollTop = event.target.scrollTop;
    },
    startEditorResize(event) {
      event.preventDefault();
      this.resizingEditor = true;
      this.resizeStartY = event.clientY;
      this.resizeStartHeight = this.editorHeight;
      document.addEventListener('mousemove', this.handleEditorResize);
      document.addEventListener('mouseup', this.stopEditorResize);
      this.previousBodyCursor = document.body.style.cursor;
      this.previousBodyUserSelect = document.body.style.userSelect;
      document.body.style.cursor = 'ns-resize';
      document.body.style.userSelect = 'none';
    },
    handleEditorResize(event) {
      if (!this.resizingEditor) {
        return;
      }
      const nextHeight = this.resizeStartHeight + event.clientY - this.resizeStartY;
      this.editorHeight = this.normalizeEditorHeight(nextHeight);
    },
    stopEditorResize() {
      if (!this.resizingEditor) {
        return;
      }
      this.resizingEditor = false;
      document.removeEventListener('mousemove', this.handleEditorResize);
      document.removeEventListener('mouseup', this.stopEditorResize);
      document.body.style.cursor = this.previousBodyCursor;
      document.body.style.userSelect = this.previousBodyUserSelect;
    },
    normalizeEditorHeight(height) {
      const main = this.$refs.queryMain;
      if (!main) {
        return Math.max(MIN_EDITOR_HEIGHT, height);
      }
      const toolbarHeight = 64;
      const resizerHeight = 8;
      const maxHeight = Math.max(MIN_EDITOR_HEIGHT, main.clientHeight - toolbarHeight - resizerHeight - MIN_RESULT_HEIGHT);
      return Math.min(Math.max(MIN_EDITOR_HEIGHT, height), maxHeight);
    },
    startHistoryResize(event) {
      event.preventDefault();
      this.resizingHistory = true;
      this.historyResizeStartX = event.clientX;
      this.historyResizeStartWidth = this.historyWidth;
      document.addEventListener('mousemove', this.handleHistoryResize);
      document.addEventListener('mouseup', this.stopHistoryResize);
      this.previousBodyCursor = document.body.style.cursor;
      this.previousBodyUserSelect = document.body.style.userSelect;
      document.body.style.cursor = 'ew-resize';
      document.body.style.userSelect = 'none';
    },
    handleHistoryResize(event) {
      if (!this.resizingHistory) {
        return;
      }
      const nextWidth = this.historyResizeStartWidth - (event.clientX - this.historyResizeStartX);
      this.historyWidth = Math.min(Math.max(MIN_HISTORY_WIDTH, nextWidth), MAX_HISTORY_WIDTH);
    },
    stopHistoryResize() {
      if (!this.resizingHistory) {
        return;
      }
      this.resizingHistory = false;
      document.removeEventListener('mousemove', this.handleHistoryResize);
      document.removeEventListener('mouseup', this.stopHistoryResize);
      document.body.style.cursor = this.previousBodyCursor;
      document.body.style.userSelect = this.previousBodyUserSelect;
    },
    formatSql() {
      const keywords = ['select', 'from', 'where', 'and', 'or', 'group by', 'order by', 'limit', 'left join', 'right join', 'inner join', 'on', 'as', 'having', 'in', 'is', 'not', 'null', 'like', 'between'];
      const pattern = new RegExp(`\\b(${keywords.join('|')})\\b`, 'gi');
      this.sql = this.replaceOutsideLiterals(this.sql, text => text.replace(pattern, keyword => keyword.toUpperCase()));
    },
    replaceOutsideLiterals(value, replacer) {
      let result = '';
      let segment = '';
      let quote = '';
      let escaped = false;

      const flushSegment = () => {
        if (segment) {
          result += replacer(segment);
          segment = '';
        }
      };

      for (let i = 0; i < value.length; i++) {
        const current = value[i];
        if (!quote) {
          if (current === '\'' || current === '"' || current === '`') {
            flushSegment();
            quote = current;
            result += current;
          } else {
            segment += current;
          }
          continue;
        }

        result += current;
        if (escaped) {
          escaped = false;
          continue;
        }
        if (current === '\\') {
          escaped = true;
        } else if (current === quote) {
          quote = '';
        }
      }

      flushSegment();
      return result;
    },
    clearSql() {
      this.error = '';
      this.result = null;
      this.editorScrollTop = 0;
      if (!this.isSelectedLocalDraft()) {
        this.startNewHistory();
        return;
      }
      this.sql = '';
      this.$nextTick(() => {
        if (this.$refs.sqlEditor) {
          this.$refs.sqlEditor.scrollTop = 0;
          this.$refs.sqlEditor.focus();
        }
      });
    },
    closeResult() {
      this.result = null;
      this.error = '';
    },
    async loadHistory() {
      this.loadLocalHistory();
      await this.loadSavedHistory();
      this.mergeHistory();
      this.ensureSelectedHistory();
    },
    loadLocalHistory() {
      try {
        const history = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
        this.localHistory = history.map(item => {
          if (item && item.localDraft && !item.localId) {
            return {
              ...item,
              localId: this.createLocalDraftId()
            };
          }
          return item;
        });
      } catch (e) {
        this.localHistory = [];
      }
    },
    async loadSavedHistory() {
      try {
        const response = await getSqlQueryHistory();
        this.savedHistory = (response.data || []).map(item => this.normalizeSavedHistoryItem(item));
      } catch (e) {
        this.savedHistory = [];
      }
    },
    saveHistory(sql) {
      if (this.isSelectedLocalDraft()) {
        this.persistLocalDraft(sql);
        return;
      }
      if (this.savedHistory.some(history => history.sql === sql)) {
        return;
      }
      const item = {
        sql,
        timestamp: Date.now()
      };
      this.localHistory = [item, ...this.localHistory.filter(history => history.sql !== sql)].slice(0, 50);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.localHistory));
      this.mergeHistory();
      this.ensureSelectedHistory();
    },
    persistLocalDraft(sql) {
      if (this.suppressDraftSync || !this.isSelectedLocalDraft()) {
        return;
      }
      const value = sql || '';
      const draftItem = {
        ...this.selectedHistory,
        sql: value,
        timestamp: Date.now(),
        localDraft: true
      };
      this.localHistory = [
        draftItem,
        ...this.localHistory.filter(item => item && item.localId !== draftItem.localId && item.sql !== value)
      ].slice(0, 50);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.localHistory));
      this.mergeHistory();
      this.selectedHistory = this.findHistoryItem(draftItem) || draftItem;
    },
    removeLocalDraft(target = this.selectedHistory) {
      const nextHistory = this.localHistory.filter(item => {
        if (!item || !item.localDraft) {
          return true;
        }
        if (target && target.localId) {
          return item.localId !== target.localId;
        }
        return false;
      });
      if (nextHistory.length === this.localHistory.length) {
        return;
      }
      this.localHistory = nextHistory;
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.localHistory));
      this.mergeHistory();
      if (this.isSameHistoryItem(this.selectedHistory, target)) {
        this.selectedHistory = null;
      }
      this.ensureSelectedHistory();
    },
    clearHistory() {
      this.localHistory = [];
      localStorage.removeItem(HISTORY_KEY);
      if (this.isSelectedLocalDraft()) {
        this.selectedHistory = null;
      }
      this.mergeHistory();
      this.ensureSelectedHistory();
    },
    handleCurrentChange(page) {
      this.currentPage = page;
    },
    handlePageSizeChange(size) {
      this.pageSize = size;
      this.currentPage = 1;
    },
    useHistorySql(item, focusEditor = true) {
      this.suppressDraftSync = true;
      this.sql = item.sql || '';
      this.selectedHistory = item || null;
      this.creatingHistory = false;
      this.editorScrollTop = 0;
      this.$nextTick(() => {
        this.suppressDraftSync = false;
        if (focusEditor && this.$refs.sqlEditor) {
          this.$refs.sqlEditor.scrollTop = 0;
          this.$refs.sqlEditor.focus();
        }
      });
    },
    startNewHistory() {
      const draftItem = this.createLocalDraft('');
      this.localHistory = [draftItem, ...this.localHistory].slice(0, 50);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.localHistory));
      this.mergeHistory();
      this.error = '';
      this.result = null;
      this.useHistorySql(this.findHistoryItem(draftItem) || draftItem);
      this.historyDialogVisible = false;
    },
    openDetailDialog() {
      const currentSql = this.sql.trim();
      if (!currentSql) {
        return;
      }
      const existing = this.creatingHistory ? null : this.resolveCurrentHistory(currentSql);
      this.historyForm = {
        id: existing ? existing.id : '',
        sql: currentSql,
        title: existing ? existing.title : '',
        description: existing ? existing.description : '',
        saved: existing ? !!existing.saved : false,
        timestamp: existing ? existing.timestamp : null
      };
      this.historyDialogVisible = true;
    },
    resolveCurrentHistory(currentSql) {
      if (this.selectedHistory && (this.selectedHistory.saved || this.selectedHistory.localDraft)) {
        return this.selectedHistory;
      }
      return this.savedHistory.find(item => item.sql === currentSql);
    },
    isHistorySelected(item) {
      if (!this.selectedHistory) {
        return false;
      }
      return this.isSameHistoryItem(this.selectedHistory, item);
    },
    isSelectedLocalDraft() {
      return !!(this.selectedHistory && this.selectedHistory.localDraft);
    },
    async saveHistoryToDatabase() {
      if (!this.historyForm.sql || !this.historyForm.sql.trim()) {
        return;
      }
      const title = this.normalizeHistoryTitle(this.historyForm.title);
      if (!title) {
        this.$message.error(this.$t('sql_query.title_required'));
        return;
      }
      const titleKey = this.normalizeHistoryTitleKey(title);
      const matched = this.savedHistory.find(item => this.normalizeHistoryTitleKey(item.title) === titleKey);
      if (matched && (!this.historyForm.id || matched.id !== this.historyForm.id)) {
        this.$message.error(this.$t('sql_query.title_duplicate'));
        return;
      }
      this.savingHistory = true;
      try {
        const response = await saveSqlQueryHistory({
          id: this.historyForm.id,
          sql: this.historyForm.sql,
          title,
          description: this.historyForm.description
        });
        const draftToRemove = this.isSelectedLocalDraft() ? this.selectedHistory : null;
        const savedItem = this.normalizeSavedHistoryItem(response.data);
        this.applySavedHistoryItem(savedItem);
        if (draftToRemove) {
          this.removeLocalDraft(draftToRemove);
        }
        this.selectedHistory = savedItem;
        this.creatingHistory = false;
        this.historyDialogVisible = false;
        this.$message.success(this.$t('sql_query.save_success'));
      } catch (e) {
        this.$message.error(e.message || e.data || this.$t('sql_query.save_failed'));
      } finally {
        this.savingHistory = false;
      }
    },
    async deleteHistoryFromDatabase() {
      if (!this.historyForm.id) {
        return;
      }
      const deletedId = this.historyForm.id;
      this.deletingHistory = true;
      try {
        await deleteSqlQueryHistory(deletedId);
        this.savedHistory = this.savedHistory.filter(item => item.id !== deletedId);
        this.mergeHistory();
        this.selectedHistory = null;
        this.ensureSelectedHistory();
        this.historyDialogVisible = false;
        this.$message.success(this.$t('sql_query.delete_success'));
      } catch (e) {
        this.$message.error(e.message || e.data || this.$t('sql_query.delete_failed'));
      } finally {
        this.deletingHistory = false;
      }
    },
    applySavedHistoryItem(savedItem) {
      this.savedHistory = [
        savedItem,
        ...this.savedHistory.filter(item => item.id !== savedItem.id)
      ];
      this.localHistory = this.localHistory.filter(item => item.sql !== savedItem.sql);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.localHistory));
      this.mergeHistory();
    },
    createLocalDraftId() {
      return `draft-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    },
    createLocalDraft(sql = '') {
      return {
        id: '',
        localId: this.createLocalDraftId(),
        sql,
        title: '',
        description: '',
        saved: false,
        localDraft: true,
        timestamp: Date.now()
      };
    },
    isSameHistoryItem(left, right) {
      if (!left || !right) {
        return false;
      }
      if (left.id && right.id) {
        return left.id === right.id;
      }
      if (left.localId && right.localId) {
        return left.localId === right.localId;
      }
      return left.sql === right.sql && left.timestamp === right.timestamp;
    },
    findHistoryItem(item) {
      return this.history.find(historyItem => this.isSameHistoryItem(historyItem, item));
    },
    ensureSelectedHistory() {
      if (!this.history.length) {
        this.startNewHistory();
        return;
      }
      const matched = this.findHistoryItem(this.selectedHistory);
      if (matched) {
        this.selectedHistory = matched;
        this.creatingHistory = false;
        return;
      }
      this.useHistorySql(this.history[0], false);
    },
    normalizeHistoryTitle(title) {
      return (title || '').trim();
    },
    normalizeHistoryTitleKey(title) {
      return this.normalizeHistoryTitle(title).toLowerCase();
    },
    normalizeSavedHistoryItem(item) {
      const timestamp = item.updateTime || item.createTime || Date.now();
      return {
        id: item.id,
        sql: item.sql,
        title: item.title || '',
        description: item.description || '',
        saved: !!item.saved,
        timestamp,
        createTime: item.createTime,
        updateTime: item.updateTime
      };
    },
    mergeHistory() {
      const savedSqlSet = new Set(this.savedHistory.map(item => item.sql));
      const localItems = this.localHistory
        .filter(item => item && (item.localDraft || item.sql) && (item.localDraft || !savedSqlSet.has(item.sql)))
        .map(item => ({
          id: '',
          localId: item.localId || (item.localDraft ? this.createLocalDraftId() : ''),
          sql: item.sql,
          title: item.title || '',
          description: '',
          saved: false,
          localDraft: !!item.localDraft,
          timestamp: item.timestamp || Date.now()
        }));
      this.history = [...this.savedHistory, ...localItems]
        .sort((left, right) => (right.timestamp || 0) - (left.timestamp || 0))
        .slice(0, 100);
    },
    resolveHistoryTitle(item) {
      return item.title || this.$t('sql_query.no_history_record_title');
    },
    openPoolDialog() {
      this.poolDialogVisible = true;
      this.loadPoolList();
    },
    async loadPoolList(selectedId) {
      this.poolLoading = true;
      const currentSelectedId = typeof selectedId === 'string'
        ? selectedId
        : (this.selectedPoolItem && this.selectedPoolItem.id);
      try {
        const response = await getSqlQueryPool({
          keyword: this.poolKeyword,
          onlyMine: this.poolOnlyMine
        });
        this.poolList = (response.data || []).map(item => this.normalizePoolItem(item));
        this.selectedPoolItem = this.poolList.find(item => item.id === currentSelectedId) || this.poolList[0] || null;
      } catch (e) {
        this.poolList = [];
        this.selectedPoolItem = null;
        this.$message.error(e.message || e.data || this.$t('sql_query.pool_load_failed'));
      } finally {
        this.poolLoading = false;
      }
    },
    normalizePoolItem(item) {
      return {
        id: item.id,
        sql: item.sql || '',
        title: item.title || '',
        summary: item.summary || '',
        description: item.description || '',
        createUser: item.createUser || '',
        createUserName: item.createUserName || '',
        updateUser: item.updateUser || '',
        updateUserName: item.updateUserName || '',
        enabled: item.enabled !== false,
        useCount: item.useCount || 0,
        createTime: item.createTime,
        updateTime: item.updateTime
      };
    },
    selectPoolItem(item) {
      this.selectedPoolItem = item;
    },
    isPoolSelected(item) {
      return !!(this.selectedPoolItem && item && this.selectedPoolItem.id === item.id);
    },
    openPoolForm(item = null) {
      if (item) {
        this.poolFormMode = 'edit';
        this.poolForm = {
          id: item.id,
          sql: item.sql || '',
          title: item.title || '',
          summary: item.summary || '',
          description: item.description || ''
        };
      } else {
        this.poolFormMode = 'create';
        this.poolForm = {
          id: '',
          sql: this.sql.trim(),
          title: this.selectedHistory && this.selectedHistory.title ? this.selectedHistory.title : '',
          summary: '',
          description: this.selectedHistory && this.selectedHistory.description ? this.selectedHistory.description : ''
        };
      }
      this.poolFormVisible = true;
    },
    async savePoolForm() {
      const title = this.normalizeHistoryTitle(this.poolForm.title);
      const summary = (this.poolForm.summary || '').trim();
      const sql = (this.poolForm.sql || '').trim();
      if (!title) {
        this.$message.error(this.$t('sql_query.title_required'));
        return;
      }
      if (!summary) {
        this.$message.error(this.$t('sql_query.pool_summary_required'));
        return;
      }
      if (!sql) {
        this.$message.error(this.$t('sql_query.pool_sql_required'));
        return;
      }
      this.poolSaving = true;
      try {
        const response = await saveSqlQueryPool({
          id: this.poolForm.id,
          sql,
          title,
          summary,
          description: this.poolForm.description
        });
        const savedItem = this.normalizePoolItem(response.data);
        this.poolDialogVisible = true;
        this.poolFormVisible = false;
        await this.loadPoolList(savedItem.id);
        this.$message.success(this.$t(this.poolFormMode === 'edit' ? 'sql_query.pool_save_success' : 'sql_query.pool_upload_success'));
      } catch (e) {
        this.$message.error(e.message || e.data || this.$t('sql_query.pool_save_failed'));
      } finally {
        this.poolSaving = false;
      }
    },
    async insertPoolToConsole() {
      if (!this.selectedPoolItem) {
        return;
      }
      const poolSql = this.selectedPoolItem.sql || '';
      if (this.sql.trim() && this.sql.trim() !== poolSql.trim()) {
        try {
          await this.$confirm(this.$t('sql_query.pool_overwrite_confirm'), this.$t('sql_query.pool_insert_console'), {
            type: 'warning'
          });
        } catch (e) {
          return;
        }
      }
      this.insertingPool = true;
      try {
        await recordSqlQueryPoolUse(this.selectedPoolItem.id);
        this.selectedPoolItem.useCount = (this.selectedPoolItem.useCount || 0) + 1;
        this.insertSqlAsDraft(poolSql);
        this.poolDialogVisible = false;
        this.$message.success(this.$t('sql_query.pool_insert_success'));
      } catch (e) {
        this.$message.error(e.message || e.data || this.$t('sql_query.pool_insert_failed'));
      } finally {
        this.insertingPool = false;
      }
    },
    insertSqlAsDraft(sql) {
      const draftItem = this.createLocalDraft(sql || '');
      this.localHistory = [draftItem, ...this.localHistory].slice(0, 50);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(this.localHistory));
      this.mergeHistory();
      this.error = '';
      this.result = null;
      this.useHistorySql(this.findHistoryItem(draftItem) || draftItem);
    },
    async copyPoolToMyHistory() {
      if (!this.selectedPoolItem) {
        return;
      }
      this.copyingPool = true;
      try {
        const response = await copySqlQueryPoolToHistory(this.selectedPoolItem.id);
        const savedItem = this.normalizeSavedHistoryItem(response.data);
        this.selectedPoolItem.useCount = (this.selectedPoolItem.useCount || 0) + 1;
        this.applySavedHistoryItem(savedItem);
        this.useHistorySql(savedItem);
        this.poolDialogVisible = false;
        this.$message.success(this.$t('sql_query.pool_copy_success'));
      } catch (e) {
        this.$message.error(e.message || e.data || this.$t('sql_query.pool_copy_failed'));
      } finally {
        this.copyingPool = false;
      }
    },
    async offlinePoolItem() {
      if (!this.selectedPoolItem) {
        return;
      }
      try {
        await this.$confirm(this.$t('sql_query.pool_offline_confirm'), this.$t('sql_query.pool_offline'), {
          type: 'warning'
        });
      } catch (e) {
        return;
      }
      this.offliningPool = true;
      try {
        const offlineId = this.selectedPoolItem.id;
        await offlineSqlQueryPool(offlineId);
        await this.loadPoolList();
        if (this.selectedPoolItem && this.selectedPoolItem.id === offlineId) {
          this.selectedPoolItem = this.poolList[0] || null;
        }
        this.$message.success(this.$t('sql_query.pool_offline_success'));
      } catch (e) {
        this.$message.error(e.message || e.data || this.$t('sql_query.pool_offline_failed'));
      } finally {
        this.offliningPool = false;
      }
    },
    formatPoolMeta(item) {
      const userName = item.createUserName || item.createUser || '';
      const timeText = this.formatTime(item.updateTime || item.createTime);
      if (userName && timeText) {
        return `${userName} · ${timeText}`;
      }
      return userName || timeText;
    },
    columnKey(column, index) {
      if (column && typeof column === 'object') {
        return column.key || `col_${index + 1}`;
      }
      return column;
    },
    columnLabel(column) {
      if (column && typeof column === 'object') {
        return column.label;
      }
      return column;
    },
    exportXlsx() {
      if (!this.hasRows) {
        return;
      }
      const headers = this.result.columns.map(column => this.columnLabel(column));
      const rows = this.result.rows.map(row => this.result.columns.map((column, index) => row[this.columnKey(column, index)]));
      const blob = this.createXlsxBlob(headers, rows);
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = this.resolveExcelFileName();
      link.click();
      URL.revokeObjectURL(link.href);
    },
    createXlsxBlob(headers, rows) {
      const now = new Date().toISOString();
      const worksheet = this.createWorksheetXml(headers, rows);
      const files = [
        {
          name: '[Content_Types].xml',
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
            + '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
            + '<Default Extension="xml" ContentType="application/xml"/>'
            + '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
            + '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
            + '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
            + '<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>'
            + '<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>'
            + '</Types>'
        },
        {
          name: '_rels/.rels',
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>'
            + '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>'
            + '<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>'
            + '</Relationships>'
        },
        {
          name: 'docProps/core.xml',
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            + '<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" '
            + 'xmlns:dc="http://purl.org/dc/elements/1.1/" '
            + 'xmlns:dcterms="http://purl.org/dc/terms/" '
            + 'xmlns:dcmitype="http://purl.org/dc/dcmitype/" '
            + 'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">'
            + '<dc:creator>MeterSphere</dc:creator>'
            + `<cp:lastModifiedBy>MeterSphere</cp:lastModifiedBy><dcterms:created xsi:type="dcterms:W3CDTF">${now}</dcterms:created>`
            + `<dcterms:modified xsi:type="dcterms:W3CDTF">${now}</dcterms:modified>`
            + '</cp:coreProperties>'
        },
        {
          name: 'docProps/app.xml',
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            + '<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" '
            + 'xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">'
            + '<Application>MeterSphere</Application></Properties>'
        },
        {
          name: 'xl/workbook.xml',
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            + '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
            + 'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
            + '<sheets><sheet name="查询结果" sheetId="1" r:id="rId1"/></sheets></workbook>'
        },
        {
          name: 'xl/_rels/workbook.xml.rels',
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>'
            + '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>'
            + '</Relationships>'
        },
        {
          name: 'xl/styles.xml',
          content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            + '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
            + '<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>'
            + '<fills count="1"><fill><patternFill patternType="none"/></fill></fills>'
            + '<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>'
            + '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>'
            + '<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>'
            + '</styleSheet>'
        },
        {
          name: 'xl/worksheets/sheet1.xml',
          content: worksheet
        }
      ];
      return new Blob([createZip(files)], { type: XLSX_MIME_TYPE });
    },
    createWorksheetXml(headers, rows) {
      const allRows = [headers, ...rows];
      const sheetRows = allRows.map((row, rowIndex) => {
        const rowNumber = rowIndex + 1;
        const cells = row.map((value, columnIndex) => this.createCellXml(value, rowNumber, columnIndex + 1)).join('');
        return `<row r="${rowNumber}">${cells}</row>`;
      }).join('');
      const columnCount = Math.max(headers.length, 1);
      const rowCount = Math.max(allRows.length, 1);
      const dimension = `A1:${this.columnName(columnCount)}${rowCount}`;
      return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        + 'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        + `<dimension ref="${dimension}"/><sheetViews><sheetView workbookViewId="0"/></sheetViews>`
        + `<sheetFormatPr defaultRowHeight="15"/><sheetData>${sheetRows}</sheetData>`
        + '</worksheet>';
    },
    createCellXml(value, rowNumber, columnNumber) {
      if (value === null || value === undefined) {
        return '';
      }
      const cellRef = `${this.columnName(columnNumber)}${rowNumber}`;
      if (typeof value === 'number' && Number.isFinite(value)) {
        return `<c r="${cellRef}"><v>${value}</v></c>`;
      }
      if (typeof value === 'boolean') {
        return `<c r="${cellRef}" t="b"><v>${value ? 1 : 0}</v></c>`;
      }
      return `<c r="${cellRef}" t="inlineStr"><is><t xml:space="preserve">${this.escapeXml(this.stringifyValue(value))}</t></is></c>`;
    },
    columnName(index) {
      let name = '';
      let value = index;
      while (value > 0) {
        const remainder = (value - 1) % 26;
        name = String.fromCharCode(65 + remainder) + name;
        value = Math.floor((value - 1) / 26);
      }
      return name;
    },
    escapeXml(value) {
      return this.removeInvalidXmlChars(String(value))
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&apos;');
    },
    removeInvalidXmlChars(value) {
      let result = '';
      for (let i = 0; i < value.length; i++) {
        const code = value.charCodeAt(i);
        if (code === 9 || code === 10 || code === 13 || code >= 32) {
          result += value[i];
        }
      }
      return result;
    },
    resolveExcelFileName() {
      const customName = this.excelName.trim()
        .replace(/[\\/:*?"<>|]/g, '_')
        .replace(/\s+/g, ' ');
      const baseName = customName || `sql_result_${this.formatDateForFileName(new Date())}`;
      return baseName.toLowerCase().endsWith('.xlsx') ? baseName : `${baseName}.xlsx`;
    },
    formatDateForFileName(date) {
      const pad = value => String(value).padStart(2, '0');
      return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`;
    },
    stringifyValue(value) {
      if (value === null || value === undefined) {
        return 'NULL';
      }
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }
      return String(value);
    },
    cellClass(value) {
      if (value === null || value === undefined) {
        return 'cell-null';
      }
      if (typeof value === 'number') {
        return 'cell-number';
      }
      if (typeof value === 'boolean') {
        return 'cell-boolean';
      }
      return '';
    },
    formatTime(timestamp) {
      if (!timestamp) {
        return '';
      }
      return new Date(timestamp).toLocaleString();
    }
  }
};
</script>

<style scoped>
.sql-query-page {
  display: flex;
  height: calc(100vh - 57px);
  min-height: 640px;
  background: #f5f7fa;
  color: #1f2933;
}

.sql-query-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #ffffff;
  border-right: 1px solid #e6e6e6;
}

.sql-query-toolbar {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px;
  border-bottom: 1px solid #e6e6e6;
  background: #ffffff;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.toolbar-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2933;
  white-space: nowrap;
}

.connection-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: 360px;
  padding: 4px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.connection-badge i {
  width: 7px;
  height: 7px;
  flex: none;
  border-radius: 50%;
  background: #f56c6c;
}

.connection-badge.is-online i {
  background: #67c23a;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-number {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 12px;
  white-space: nowrap;
}

.limit-input {
  width: 116px;
}

.timeout-input {
  width: 104px;
}

.sql-editor {
  flex: none;
  display: flex;
  background: #1f2937;
}

.editor-gutter {
  width: 48px;
  padding-top: 16px;
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #111827;
  color: #6b7280;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  user-select: none;
  overflow: hidden;
}

.editor-gutter-lines {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  will-change: transform;
}

.editor-gutter span {
  flex: none;
  height: 21px;
  line-height: 21px;
}

.sql-textarea {
  flex: 1;
  min-width: 0;
  padding: 16px;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: #f9fafb;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
  line-height: 1.5;
}

.sql-textarea::placeholder {
  color: #9ca3af;
}

.editor-result-resizer {
  height: 8px;
  flex: none;
  position: relative;
  background: #f5f7fa;
  border-top: 1px solid #dcdfe6;
  border-bottom: 1px solid #dcdfe6;
  cursor: ns-resize;
}

.editor-result-resizer::before {
  content: "";
  position: absolute;
  left: 50%;
  top: 3px;
  width: 48px;
  height: 2px;
  margin-left: -24px;
  border-radius: 2px;
  background: #c0c4cc;
}

.editor-result-resizer:hover::before,
.editor-result-resizer.is-dragging::before {
  background: #409eff;
}

.sql-result {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
}

.result-empty,
.result-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.result-empty i,
.result-loading i {
  font-size: 42px;
  margin-bottom: 14px;
  color: #c0c4cc;
}

.quick-hints {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  flex-wrap: wrap;
  justify-content: center;
}

.quick-hints span {
  padding: 4px 10px;
  border-radius: 4px;
  background: #f2f6fc;
  color: #606266;
  font-size: 12px;
}

.result-error {
  margin: 24px;
  padding: 18px;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
}

.error-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
}

.result-error pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.result-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.result-meta {
  min-height: 44px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  font-size: 13px;
  color: #606266;
}

.meta-left,
.meta-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-right {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.export-popover-title {
  margin-bottom: 8px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.result-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.result-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 10px 12px;
  text-align: left;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.result-table td {
  max-width: 320px;
  padding: 9px 12px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-table tr:hover td {
  background: #f5f7fa;
}

.row-index {
  width: 52px;
  text-align: center !important;
  color: #909399 !important;
  background: #fafafa;
}

.cell-null {
  color: #909399;
  font-style: italic;
}

.cell-number {
  color: #e6a23c;
}

.cell-boolean {
  color: #409eff;
}

.result-pagination {
  min-height: 44px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px solid #ebeef5;
  background: #ffffff;
  color: #606266;
  font-size: 12px;
}

.history-width-resizer {
  width: 8px;
  flex: none;
  position: relative;
  background: #f5f7fa;
  border-left: 1px solid #e6e6e6;
  border-right: 1px solid #e6e6e6;
  cursor: ew-resize;
}

.history-width-resizer::before {
  content: "";
  position: absolute;
  left: 3px;
  top: 50%;
  width: 2px;
  height: 48px;
  margin-top: -24px;
  border-radius: 2px;
  background: #c0c4cc;
}

.history-width-resizer:hover::before,
.history-width-resizer.is-dragging::before {
  background: #409eff;
}

.sql-history {
  flex: none;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.history-header {
  min-height: 64px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
}

.history-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.history-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
}

.history-card {
  width: 100%;
  display: block;
  position: relative;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s, box-shadow 0.2s;
}

.history-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.history-card.is-selected {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: inset 3px 0 0 #409eff, 0 2px 8px rgba(64, 158, 255, 0.16);
}

.history-card.is-selected:hover {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: inset 3px 0 0 #409eff, 0 3px 10px rgba(64, 158, 255, 0.18);
}

.history-create-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 64px;
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.history-saved-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 1px 6px;
  border-radius: 3px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 11px;
  line-height: 18px;
  opacity: 0.78;
}

.history-title {
  display: -webkit-box;
  padding-right: 58px;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.45;
}

.history-time {
  display: block;
  margin-top: 8px;
  color: #909399;
  font-size: 11px;
  text-align: right;
}

.history-empty {
  padding-top: 40px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.history-detail-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-detail-form label {
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.history-detail-sql ::v-deep textarea {
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
}

.history-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-dialog-footer-right {
  display: inline-flex;
  gap: 8px;
}

.sql-pool-dialog ::v-deep .el-dialog__body {
  padding-top: 12px;
}

.pool-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.pool-search {
  width: 360px;
  max-width: 44vw;
}

.pool-body {
  height: 58vh;
  min-height: 420px;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  padding-top: 16px;
}

.pool-list {
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.pool-card {
  width: 100%;
  display: block;
  padding: 13px 14px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s, box-shadow 0.2s;
}

.pool-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.pool-card.is-selected {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: inset 3px 0 0 #409eff, 0 2px 8px rgba(64, 158, 255, 0.16);
}

.pool-card-title,
.pool-card-summary,
.pool-card-meta {
  display: block;
}

.pool-card-title {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
}

.pool-card-summary {
  margin-top: 7px;
  color: #606266;
  font-size: 12px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pool-card-meta {
  margin-top: 9px;
  color: #909399;
  font-size: 11px;
  text-align: right;
}

.pool-empty {
  padding-top: 80px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.pool-detail {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #ffffff;
}

.pool-detail-empty {
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
}

.pool-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.pool-detail-title {
  min-width: 0;
}

.pool-detail-title h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.pool-detail-title p {
  margin: 8px 0 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.pool-detail-meta {
  display: flex;
  gap: 14px;
  margin: 12px 0 16px;
  color: #909399;
  font-size: 12px;
}

.pool-detail label,
.pool-form label {
  margin-bottom: 8px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;
}

.pool-sql-preview {
  flex: 1;
  min-height: 0;
  margin: 0 0 16px;
  padding: 14px;
  overflow: auto;
  border-radius: 4px;
  background: #1f2937;
  color: #f9fafb;
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre;
}

.pool-description {
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.pool-detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.pool-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pool-form-sql ::v-deep textarea {
  font-family: Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .sql-query-page {
    height: auto;
    min-height: calc(100vh - 57px);
    flex-direction: column;
  }

  .history-width-resizer {
    display: none;
  }

  .sql-history {
    width: auto !important;
    max-height: 260px;
    border-top: 1px solid #e6e6e6;
  }

  .pool-body {
    height: auto;
    grid-template-columns: 1fr;
  }

  .pool-search {
    width: 100%;
    max-width: none;
  }
}
</style>
