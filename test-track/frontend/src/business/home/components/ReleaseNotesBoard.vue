<template>
  <!-- 需求上线内容展示面板，使用 el-card 包裹，样式对齐上方卡片 -->
  <div class="dashboard-card">
    <el-card shadow="never" class="box-card" style="height: 100%">
      <div slot="header" class="clearfix">
        <span class="dashboard-title">需求上线内容</span>
        <span class="release-note-more" @click="goToSetting">
          更多 <i class="el-icon-arrow-right"></i>
        </span>
      </div>
      <div v-loading="loading" element-loading-background="#FFFFFF">
        <div v-if="loadError"
             style="width: 100%; height: 300px; display: flex; flex-direction: column; justify-content: center; align-items: center">
          <img style="height: 100px; width: 100px" src="/assets/module/figma/icon_load_error.svg" />
          <span class="addition-info-title" style="color: #646A73">{{ $t("home.dashboard.public.load_error") }}</span>
        </div>
        <div v-if="!loadError">
          <div v-if="releaseNotes.length > 0" class="release-notes-list">
            <div v-for="item in releaseNotes" :key="item.id" class="release-note-item" :class="{ 'is-unread': !isRead(item.id) }" @click="showDetail(item)">
              <div class="release-note-row">
                <div class="release-note-title">
                  {{ formatDateChinese(item.createTime) }}上线公告
                </div>
                <span :class="isRead(item.id) ? 'read-tag' : 'unread-tag'">{{ isRead(item.id) ? '已读' : '未读' }}</span>
              </div>
              <div class="release-note-meta">创建时间: {{ formatDate(item.createTime) }}&nbsp;&nbsp;创建者: {{ item.creator }}</div>
            </div>
          </div>
          <div v-else style="width: 100%; height: 238px; display: flex; flex-direction: column; justify-content: center; align-items: center">
            <img style="height: 100px; width: 100px; margin-bottom: 8px" src="/assets/module/figma/icon_none.svg" />
            <span class="addition-info-title">暂无上线记录</span>
          </div>
        </div>
      </div>
      <el-dialog :visible.sync="dialogVisible" width="60%" :close-on-click-modal="true" append-to-body class="release-note-dialog" :show-close="true">
        <div slot="title" class="release-note-dialog-header">
          <span class="release-note-dialog-title">{{ dialogTitle }}</span>
        </div>
        <div class="release-note-dialog-body">
          <div class="release-note-info-bar">
            <div class="release-note-info-left">
              <span class="info-item">创建时间：{{ formatDate(currentItem.createTime) }}</span>
              <span class="info-item">创建者：{{ currentItem.creator }}</span>
            </div>
            <div class="release-note-info-right">
              <span class="info-type">【类型：公告】</span>
            </div>
          </div>
          <div class="release-note-divider"></div>
          <div class="release-note-content" v-html="formattedContent"></div>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { getRecentReleaseNotes } from "@/api/release-note";
import { getCurrentUserId } from "metersphere-frontend/src/utils/token";
import { hasPermission } from "metersphere-frontend/src/utils/permission";

export default {
  name: "ReleaseNotesBoard",
  data() {
    return {
      releaseNotes: [],
      loading: false,
      loadError: false,
      dialogVisible: false,
      dialogTitle: "",
      currentContent: "",
      currentItem: {}
    };
  },
  computed: {
    formattedContent() {
      if (!this.currentContent) return "";
      const escaped = this.currentContent.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
      return escaped.replace(/\n/g, "<br>");
    }
  },
  activated() { this.loadData(); },
  mounted() { this.loadData(); },
  methods: {
    loadData() {
      this.loading = true;
      this.loadError = false;
      getRecentReleaseNotes(5)
        .then((res) => { this.releaseNotes = res.data || []; this.loading = false; })
        .catch(() => { this.loading = false; this.loadError = true; });
    },
    showDetail(item) {
      this.markAsRead(item.id);
      this.dialogTitle = this.formatDateChinese(item.createTime) + "上线公告";
      this.currentContent = item.content || "";
      this.currentItem = item;
      this.dialogVisible = true;
    },
    isRead(noteId) {
      const readIds = this.getReadSet();
      return readIds.has(noteId);
    },
    markAsRead(noteId) {
      const readIds = this.getReadSet();
      readIds.add(noteId);
      const key = 'release_note_read_' + getCurrentUserId();
      localStorage.setItem(key, JSON.stringify([...readIds]));
    },
    getReadSet() {
      try {
        const key = 'release_note_read_' + getCurrentUserId();
        const stored = localStorage.getItem(key);
        return new Set(stored ? JSON.parse(stored) : []);
      } catch (e) {
        return new Set();
      }
    },
    formatDateChinese(timestamp) {
      const d = new Date(timestamp);
      return d.getFullYear() + "年" + (d.getMonth() + 1).toString().padStart(2, "0") + "月" + d.getDate().toString().padStart(2, "0") + "日";
    },
    formatDate(timestamp) {
      const d = new Date(timestamp);
      return d.getFullYear() + "-" + (d.getMonth() + 1).toString().padStart(2, "0") + "-" + d.getDate().toString().padStart(2, "0");
    },
    /**
     * 跳转到系统设置-系统参数设置-公告设置 tab 页
     * 无权限时提示用户
     */
    goToSetting() {
      if (!hasPermission('SYSTEM_SETTING:READ')) {
        this.$warning(this.$t('announcement.no_setting_permission') || '暂无系统设置权限，请联系管理员');
        return;
      }
      this.$router.push({ path: '/setting/systemparametersetting', query: { tab: 'announcement' } });
    }
  }
};
</script>

<style scoped>
.release-notes-list { min-height: 228px; }
.release-note-item { padding: 12px 0; border-bottom: 1px solid rgba(31, 35, 41, 0.15); cursor: pointer; transition: background-color 0.2s; }
.release-note-item:hover { background-color: #f5f6f7; }
.release-note-item:last-child { border-bottom: none; }
.release-note-row { display: flex; justify-content: space-between; align-items: center; }
.release-note-title { font-size: 14px; font-weight: 500; color: #1f2329; line-height: 22px; }
.release-note-item.is-unread .release-note-title { font-weight: 600; }
.read-tag { font-size: 12px; color: #909399; flex-shrink: 0; }
.unread-tag { font-size: 12px; color: #e6a23c; font-weight: 500; flex-shrink: 0; }
.release-note-meta { font-size: 12px; color: #646a73; line-height: 20px; margin-top: 4px; }
.release-note-more { float: right; font-size: 14px; color: #409EFF; cursor: pointer; font-weight: 400; }
.release-note-more:hover { color: #66b1ff; }
.release-note-more i { font-size: 12px; }
.release-note-content { font-size: 14px; color: #1f2329; line-height: 24px; word-break: break-word; white-space: pre-wrap; }

/* 对话框样式 */
.release-note-dialog :deep(.el-dialog__header) {
  padding: 20px 20px 10px;
  text-align: center;
}
.release-note-dialog-header {
  text-align: center;
}
.release-note-dialog-title {
  font-size: 24px;
  font-weight: bold;
  color: #1890ff;
}
.release-note-dialog-body {
  padding: 0 20px 20px;
}
.release-note-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}
.release-note-info-left {
  font-size: 14px;
  color: #646a73;
}
.release-note-info-right {
  font-size: 14px;
  color: #1f2329;
  font-weight: 500;
}
.info-item {
  margin-right: 20px;
}
.info-type {
  font-weight: 500;
}
.release-note-divider {
  height: 1px;
  background-color: #dcdfe6;
  margin: 10px 0 20px;
}
.release-note-content {
  font-size: 14px;
  color: #1f2329;
  line-height: 32px;
  word-break: break-word;
}
.release-note-content >>> p {
  margin: 0 0 16px;
}
.release-note-content >>> ol,
.release-note-content >>> ul {
  padding-left: 24px;
  margin: 0 0 16px;
}
.release-note-content >>> li {
  margin: 8px 0;
}

/* 对齐上方卡片的 header 样式 */
.el-card :deep(.el-card__header) {
  border-bottom: 0px solid #EBEEF5;
}
</style>
