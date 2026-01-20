<template>
  <div v-loading="loading">
    <el-timeline v-if="transitionHistory && transitionHistory.length > 0">
      <el-timeline-item
        v-for="(item, index) in transitionHistory"
        :key="index"
        :timestamp="formatTime(item.createTime)"
        placement="top"
      >
        <el-card>
          <div class="transition-item">
            <div class="transition-header">
              <div class="header-left">
                <span class="operator">
                  <i class="el-icon-user"></i>
                  {{ item.operatorName || item.operator }}
                </span>
              </div>
            </div>
            <div v-if="getChangeList(item).length > 0" class="change-list">
              <div v-for="(chg, cIndex) in getChangeList(item)" :key="cIndex" class="change-line">
                <span class="field-name">{{ chg.fieldName }}</span>
                <span class="arrow">:</span>
                <span class="old-value">{{ chg.oldValue }}</span>
                <span class="arrow"> -> </span>
                <span class="new-value">{{ chg.newValue }}</span>
              </div>
            </div>
            <div v-else class="legacy-status">
              <span class="from-status">{{ getStatusText(item.fromStatus) }}</span>
              <i class="el-icon-right"></i>
              <span class="to-status">{{ getStatusText(item.toStatus) }}</span>
              <div v-if="item.comment" class="transition-comment">
                <i class="el-icon-chat-line-square"></i>
                {{ item.comment }}
              </div>
            </div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    <div v-else class="empty-state">
      <i class="el-icon-info"></i>
      <span>{{ $t('test_track.issue.transition_history_empty') }}</span>
    </div>
  </div>
</template>

<script>
import { getTransitionHistory } from "@/api/issue";
import { datetimeFormat } from "fit2cloud-ui/src/filters/time";

export default {
  name: "IssueTransitionHistory",
  props: {
    issueId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      loading: false,
      transitionHistory: [],
      associatedSystemMap: new Map()
    };
  },
  mounted() {
    this.loadAssociatedSystems();
    this.loadHistory();
  },
  watch: {
    issueId() {
      this.loadHistory();
    }
  },
  methods: {
    // 提供给父组件调用的刷新入口：避免 Tab 切换后仍展示旧的历史数据
    refresh() {
      this.loadHistory();
    },
    loadAssociatedSystems() {
      this.associatedSystemMap = new Map();
      this.$get('/associatedSystem/list/all')
        .then((response) => {
          const nextMap = new Map();
          if (response && Array.isArray(response.data)) {
            response.data.forEach((item) => {
              if (item && item.id) {
                nextMap.set(item.id, item.name || item.id);
              }
            });
          }
          // Map 内部 set 在 Vue2 中不具备响应式，整体替换引用触发更新
          this.associatedSystemMap = nextMap;
          this.$forceUpdate();
        })
        .catch(() => {
          this.associatedSystemMap = new Map();
          this.$forceUpdate();
        });
    },
    loadHistory() {
      if (!this.issueId) {
        return;
      }
      this.loading = true;
      getTransitionHistory(this.issueId)
        .then(response => {
          this.transitionHistory = response.data || [];
        })
        .catch(() => {
          this.transitionHistory = [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    formatTime(timestamp) {
      if (!timestamp) {
        return '';
      }
      return datetimeFormat(timestamp);
    },
    getStatusText(status) {
      if (!status) {
        return this.$t('test_track.issue.status_new');
      }
      const normalized = String(status).trim();
      // 兼容：后端可能把 i18n key 用 [] 包起来
      const keyCandidate = normalized
        .replace(/^\[\s*/, '')
        .replace(/\s*\]$/, '')
        .replace(/"/g, '')
        .trim();

      // 1) 如果已经是完整 i18n key（例如 test_track.issue.status_closed），直接翻译
      if (keyCandidate.indexOf('test_track.issue.status_') === 0) {
        const translated = this.$t(keyCandidate);
        return translated !== keyCandidate ? translated : keyCandidate;
      }

      // 2) 否则认为是状态值（closed/resolved/new...），拼接 key 再翻译
      const cleanStatus = keyCandidate;
      const statusKey = `test_track.issue.status_${cleanStatus}`;
      const translated = this.$t(statusKey);
      return translated !== statusKey ? translated : cleanStatus;
    },
    formatAssociatedSystemValue(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      const raw = String(value).replace(/"/g, '').trim();
      // 兼容多选：逗号分隔或 JSON 数组字符串
      try {
        const maybeArr = JSON.parse(raw);
        if (Array.isArray(maybeArr)) {
          return maybeArr
            .map((id) => this.associatedSystemMap.get(id) || id)
            .join(', ');
        }
      } catch (e) {
        // ignore
      }
      if (raw.indexOf(',') > -1) {
        return raw
          .split(',')
          .map((s) => s.trim())
          .filter((s) => s)
          .map((id) => this.associatedSystemMap.get(id) || id)
          .join(', ');
      }
      return this.associatedSystemMap.get(raw) || raw;
    },
    formatChangeValue(fieldName, value) {
      const val = value === null || value === undefined || value === '' ? '-' : value;
      const name = (fieldName || '').trim();
      if (name === '状态') {
        return this.getStatusText(val);
      }
      if (name === '缺陷所属系统') {
        return this.formatAssociatedSystemValue(val);
      }
      return val;
    },
    getChangeList(item) {
      if (item && Array.isArray(item.details) && item.details.length > 0) {
        return item.details
          .map(c => {
            return {
              fieldName: c.fieldName || c.fieldKey || c.fieldId || '',
              oldValue: this.formatChangeValue(c.fieldName || c.fieldKey, c.oldValue),
              newValue: this.formatChangeValue(c.fieldName || c.fieldKey, c.newValue)
            };
          })
          .filter(c => c.fieldName);
      }
      const comment = item && item.comment;
      if (!comment) {
        return [];
      }
      try {
        const payload = JSON.parse(comment);
        if (!payload || payload.type !== 'issue_change' || !Array.isArray(payload.changes)) {
          return [];
        }
        return payload.changes
          .map(c => {
            return {
              fieldName: c.fieldName || c.fieldKey || c.fieldId || '',
              oldValue: this.formatChangeValue(c.fieldName || c.fieldKey, c.oldValue),
              newValue: this.formatChangeValue(c.fieldName || c.fieldKey, c.newValue)
            };
          })
          .filter(c => c.fieldName);
      } catch (e) {
        return [];
      }
    },
    getActionText() {
      const key = 'test_track.issue.transition_history_action_modify';
      const translated = this.$t(key);
      return translated !== key ? translated : '做了修改';
    }
  }
};
</script>

<style scoped>
.el-card {
  border-radius: 8px;
  border-color: #ebeef5;
}

.el-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.transition-item {
  padding: 6px 0;
}

.transition-header {
  display: flex;
  justify-content: flex-start;
  align-items: baseline;
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.action-text {
  color: #303133;
  font-size: 13px;
  white-space: nowrap;
}

.operator {
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

.operator i {
  margin-right: 5px;
}

.status-change {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legacy-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.change-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.change-line {
  color: #606266;
  font-size: 14px;
  word-break: break-word;
  line-height: 24px;
}

.field-name {
  color: #303133;
  font-weight: 600;
}

.old-value {
  color: #909399;
}

.new-value {
  color: #409eff;
  font-weight: 600;
}

.from-status,
.to-status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
}

.from-status {
  background-color: #f4f4f5;
  color: #909399;
}

.to-status {
  background-color: #ecf5ff;
  color: #409eff;
}

.transition-comment {
  margin-top: 12px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  color: #606266;
  font-size: 13px;
  line-height: 18px;
}

.transition-comment i {
  margin-right: 5px;
  color: #909399;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.empty-state i {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
}
</style>


