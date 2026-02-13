<template>
  <!-- 需求上线内容展示面板，使用 el-card 包裹，样式对齐上方卡片 -->
  <div class="dashboard-card">
    <el-card shadow="never" class="box-card" style="height: 100%">
      <div slot="header" class="clearfix">
        <span class="dashboard-title">需求上线内容</span>
      </div>
      <div v-loading="loading" element-loading-background="#FFFFFF">
        <div v-if="loadError"
             style="width: 100%; height: 300px; display: flex; flex-direction: column; justify-content: center; align-items: center">
          <img style="height: 100px; width: 100px" src="/assets/module/figma/icon_load_error.svg" />
          <span class="addition-info-title" style="color: #646A73">{{ $t("home.dashboard.public.load_error") }}</span>
        </div>
        <div v-if="!loadError">
          <div v-if="releaseNotes.length > 0" class="release-notes-list">
            <div v-for="item in releaseNotes" :key="item.id" class="release-note-item" @click="showDetail(item)">
              <div class="release-note-title">{{ formatDateChinese(item.createTime) }}上线公告</div>
              <div class="release-note-meta">创建时间: {{ formatDate(item.createTime) }}&nbsp;&nbsp;创建者: {{ item.creator }}</div>
            </div>
          </div>
          <div v-else style="width: 100%; height: 238px; display: flex; flex-direction: column; justify-content: center; align-items: center">
            <img style="height: 100px; width: 100px; margin-bottom: 8px" src="/assets/module/figma/icon_none.svg" />
            <span class="addition-info-title">暂无上线记录</span>
          </div>
        </div>
      </div>
      <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" :close-on-click-modal="true" append-to-body>
        <div class="release-note-content" v-html="formattedContent"></div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { getRecentReleaseNotes } from "@/api/release-note";

export default {
  name: "ReleaseNotesBoard",
  data() {
    return {
      releaseNotes: [],
      loading: false,
      loadError: false,
      dialogVisible: false,
      dialogTitle: "",
      currentContent: ""
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
      this.dialogTitle = this.formatDateChinese(item.createTime) + "上线公告";
      this.currentContent = item.content || "";
      this.dialogVisible = true;
    },
    formatDateChinese(timestamp) {
      const d = new Date(timestamp);
      return d.getFullYear() + "年" + (d.getMonth() + 1).toString().padStart(2, "0") + "月" + d.getDate().toString().padStart(2, "0") + "日";
    },
    formatDate(timestamp) {
      const d = new Date(timestamp);
      return d.getFullYear() + "-" + (d.getMonth() + 1).toString().padStart(2, "0") + "-" + d.getDate().toString().padStart(2, "0");
    }
  }
};
</script>

<style scoped>
.release-notes-list { min-height: 228px; }
.release-note-item { padding: 12px 0; border-bottom: 1px solid rgba(31, 35, 41, 0.15); cursor: pointer; transition: background-color 0.2s; }
.release-note-item:hover { background-color: #f5f6f7; }
.release-note-item:last-child { border-bottom: none; }
.release-note-title { font-size: 14px; font-weight: 500; color: #1f2329; line-height: 22px; }
.release-note-meta { font-size: 12px; color: #646a73; line-height: 20px; margin-top: 4px; }
.release-note-content { font-size: 14px; color: #1f2329; line-height: 24px; word-break: break-word; white-space: pre-wrap; }

/* 对齐上方卡片的 header 样式 */
.el-card :deep(.el-card__header) {
  border-bottom: 0px solid #EBEEF5;
}
</style>
