<template>
  <!--
    公告设置组件（改版）
    主体：上线内容管理表格
    横幅公告：通过右上角按钮打开弹窗配置
  -->
  <div>
    <!-- 主体内容：上线记录管理（横幅公告按钮已移入 ReleaseNoteManager 同一行） -->
    <release-note-manager @openBanner="openBannerDialog" />

    <!-- 横幅公告设置弹窗（美化版） -->
    <el-dialog
      :title="$t('announcement.banner_setting')"
      :visible.sync="bannerDialogVisible"
      width="960px"
      :close-on-click-modal="false"
      custom-class="banner-dialog"
      append-to-body>

      <div v-loading="bannerLoading" class="banner-dialog-body">
        <!-- 第一行：公告开关 / 样式设置 / 字体大小 / 滚动效果 四卡片并列 -->
        <div class="banner-row">
          <!-- 公告开关 -->
          <div class="banner-section banner-section-sm">
            <div class="banner-section-header">
              <i class="el-icon-open banner-section-icon" style="color: #67C23A"></i>
              <span>{{ $t('announcement.enabled') }}</span>
            </div>
            <div class="banner-section-content" style="display: flex; align-items: center; justify-content: center; min-height: 60px;">
              <el-switch v-model="form.enabled" :active-text="$t('announcement.enabled')" :inactive-text="$t('announcement.disabled_hint')" />
            </div>
          </div>

          <!-- 样式设置 -->
          <div class="banner-section banner-section-md">
            <div class="banner-section-header">
              <i class="el-icon-magic-stick banner-section-icon" style="color: #E6A23C"></i>
              <span>{{ $t('announcement.style') }}</span>
            </div>
            <div class="banner-section-content">
              <div class="style-selector">
                <div
                  v-for="item in styleOptions"
                  :key="item.value"
                  class="style-option"
                  :class="{ 'style-option-active': form.styleType === item.value }"
                  :style="{ backgroundColor: item.bg, color: item.text }"
                  @click="selectStyle(item.value)">
                  <i :class="item.icon"></i>
                  <span>{{ item.label }}</span>
                </div>
              </div>
              <div v-if="form.styleType === 'custom'" class="custom-color-row">
                <span class="color-label">{{ $t('announcement.background_color') }}</span>
                <el-color-picker v-model="form.backgroundColor" size="small" />
                <span class="color-label" style="margin-left: 12px;">{{ $t('announcement.text_color') }}</span>
                <el-color-picker v-model="form.textColor" size="small" />
              </div>
            </div>
          </div>

          <!-- 字体大小 -->
          <div class="banner-section banner-section-sm">
            <div class="banner-section-header">
              <i class="el-icon-font-size banner-section-icon" style="color: #F56C6C"></i>
              <span>{{ $t('announcement.font_size') }}</span>
            </div>
            <div class="banner-section-content" style="min-height: 60px;">
              <div class="font-size-selector">
                <div
                  v-for="opt in fontSizeOptions"
                  :key="opt.value"
                  class="font-size-option"
                  :class="{ 'font-size-option-active': form.fontSize === opt.value }"
                  @click="form.fontSize = opt.value">
                  <span :style="{ fontSize: opt.value + 'px' }">A</span>
                  <span class="font-size-label">{{ opt.label }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 滚动效果 -->
          <div class="banner-section banner-section-sm">
            <div class="banner-section-header">
              <i class="el-icon-d-arrow-right banner-section-icon" style="color: #783887"></i>
              <span>{{ $t('announcement.scroll') }}</span>
            </div>
            <div class="banner-section-content" style="min-height: 60px;">
              <el-switch v-model="form.scroll" :active-text="$t('announcement.scroll')" inactive-text="" />
              <div v-if="form.scroll" style="margin-top: 8px;">
                <el-slider
                  v-model="form.scrollSpeed"
                  :min="40"
                  :max="80"
                  :step="1"
                  :format-tooltip="val => val + $t('announcement.speed_unit')" />
              </div>
            </div>
          </div>
        </div>

        <!-- 第二行：公告内容（全宽） -->
        <div class="banner-section" style="margin-top: 12px;">
          <div class="banner-section-header">
            <i class="el-icon-edit-outline banner-section-icon" style="color: #409EFF"></i>
            <span>{{ $t('announcement.content') }}</span>
          </div>
          <div class="banner-section-content">
            <el-input
              type="textarea"
              v-model="form.content"
              :rows="3"
              :placeholder="$t('announcement.content_placeholder')"
              maxlength="2000"
              show-word-limit />
          </div>
        </div>

        <!-- 第三行：实时预览（全宽） -->
        <div class="banner-section" style="margin-top: 12px;">
          <div class="banner-section-header">
            <i class="el-icon-view banner-section-icon" style="color: #909399"></i>
            <span>{{ $t('announcement.preview') }}</span>
          </div>
          <div class="banner-section-content">
            <div
              v-if="form.content && form.enabled"
              class="banner-preview"
              :class="{ 'banner-preview-scroll': form.scroll }"
              :style="previewStyle">
              <span class="banner-preview-text" :style="scrollAnimStyle">{{ form.content }}</span>
            </div>
            <div v-else-if="!form.enabled" class="banner-preview-empty">
              {{ $t('announcement.disabled_hint') }}
            </div>
            <div v-else class="banner-preview-empty">
              {{ $t('announcement.no_content') }}
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button size="small" @click="bannerDialogVisible = false">{{ $t('commons.cancel') }}</el-button>
        <el-button size="small" type="primary" @click="saveBanner">{{ $t('commons.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAnnouncementContent,
  saveAnnouncementContent,
  getAnnouncementEnabled,
  saveAnnouncementEnabled,
  getAnnouncementStyle,
  saveAnnouncementStyle,
  getAnnouncementScroll,
  saveAnnouncementScroll,
  getAnnouncementScrollSpeed,
  saveAnnouncementScrollSpeed,
  getAnnouncementFontSize,
  saveAnnouncementFontSize
} from "../../../api/system";
import ReleaseNoteManager from './ReleaseNoteManager';

/** 预设样式配置 */
const PRESET_STYLES = {
  info:    { backgroundColor: '#409EFF', textColor: '#FFFFFF' },
  warning: { backgroundColor: '#E6A23C', textColor: '#FFFFFF' },
  danger:  { backgroundColor: '#F56C6C', textColor: '#FFFFFF' },
  success: { backgroundColor: '#67C23A', textColor: '#FFFFFF' },
  custom:  { backgroundColor: '#E6A23C', textColor: '#FFFFFF' }
};

export default {
  name: "AnnouncementSetting",
  components: { ReleaseNoteManager },
  data() {
    return {
      bannerDialogVisible: false,
      bannerLoading: false,
      form: {
        content: '',
        enabled: true,
        styleType: 'warning',
        backgroundColor: '#E6A23C',
        textColor: '#FFFFFF',
        scroll: false,
        scrollSpeed: 15,
        fontSize: 14
      },
      /** 字体大小选项 */
      fontSizeOptions: [
        { value: 12, label: this.$t('announcement.font_size_small') || '小' },
        { value: 14, label: this.$t('announcement.font_size_medium') || '中' },
        { value: 16, label: this.$t('announcement.font_size_large') || '大' },
        { value: 20, label: this.$t('announcement.font_size_xlarge') || '特大' }
      ],
      /** 样式选择器选项 */
      styleOptions: [
        { value: 'info',    label: this.$t('announcement.style_info') || '通知', bg: '#409EFF', text: '#fff', icon: 'el-icon-info' },
        { value: 'warning', label: this.$t('announcement.style_warning') || '警告', bg: '#E6A23C', text: '#fff', icon: 'el-icon-warning' },
        { value: 'danger',  label: this.$t('announcement.style_danger') || '紧急', bg: '#F56C6C', text: '#fff', icon: 'el-icon-error' },
        { value: 'success', label: this.$t('announcement.style_success') || '成功', bg: '#67C23A', text: '#fff', icon: 'el-icon-success' },
        { value: 'custom',  label: this.$t('announcement.style_custom') || '自定义', bg: '#f0f0f0', text: '#333', icon: 'el-icon-brush' }
      ]
    };
  },
  computed: {
    /** 预览区域的动态样式（含字体大小） */
    previewStyle() {
      const base = { fontSize: this.form.fontSize + 'px', lineHeight: (this.form.fontSize + 16) + 'px' };
      if (this.form.styleType === 'custom') {
        return { ...base, backgroundColor: this.form.backgroundColor, color: this.form.textColor };
      }
      const s = PRESET_STYLES[this.form.styleType] || PRESET_STYLES.warning;
      return { ...base, backgroundColor: s.backgroundColor, color: s.textColor };
    },
    /** 滚动动画样式 */
    scrollAnimStyle() {
      return this.form.scroll ? { animationDuration: `${this.form.scrollSpeed}s` } : {};
    }
  },
  methods: {
    /** 打开横幅公告弹窗，同时加载最新配置 */
    openBannerDialog() {
      this.bannerDialogVisible = true;
      this.loadBanner();
    },
    /** 选择预设样式 */
    selectStyle(type) {
      this.form.styleType = type;
      if (type !== 'custom') {
        const s = PRESET_STYLES[type];
        this.form.backgroundColor = s.backgroundColor;
        this.form.textColor = s.textColor;
      }
    },
    /** 从后端加载横幅公告配置 */
    loadBanner() {
      this.bannerLoading = true;
      Promise.all([
        getAnnouncementContent().catch(() => ({ data: null })),
        getAnnouncementEnabled().catch(() => ({ data: null })),
        getAnnouncementStyle().catch(() => ({ data: null })),
        getAnnouncementScroll().catch(() => ({ data: null })),
        getAnnouncementScrollSpeed().catch(() => ({ data: null })),
        getAnnouncementFontSize().catch(() => ({ data: null }))
      ]).then(([cRes, eRes, sRes, scrRes, spdRes, fsRes]) => {
        this.form.content = (cRes.data && cRes.data.paramValue) || '';
        this.form.enabled = eRes.data && eRes.data.paramValue ? eRes.data.paramValue === 'true' : true;
        if (sRes.data && sRes.data.paramValue) {
          try {
            const cfg = JSON.parse(sRes.data.paramValue);
            this.form.styleType = cfg.styleType || 'warning';
            this.form.backgroundColor = cfg.backgroundColor || '#E6A23C';
            this.form.textColor = cfg.textColor || '#FFFFFF';
          } catch (e) { /* 解析失败用默认值 */ }
        }
        this.form.scroll = scrRes.data && scrRes.data.paramValue ? scrRes.data.paramValue === 'true' : false;
        this.form.scrollSpeed = spdRes.data && spdRes.data.paramValue ? parseInt(spdRes.data.paramValue) || 15 : 15;
        this.form.fontSize = fsRes.data && fsRes.data.paramValue ? parseInt(fsRes.data.paramValue) || 14 : 14;
      }).finally(() => { this.bannerLoading = false; });
    },
    /** 保存横幅公告配置到后端 */
    saveBanner() {
      this.bannerLoading = true;
      const styleConfig = {
        styleType: this.form.styleType,
        backgroundColor: this.form.styleType === 'custom' ? this.form.backgroundColor : PRESET_STYLES[this.form.styleType].backgroundColor,
        textColor: this.form.styleType === 'custom' ? this.form.textColor : PRESET_STYLES[this.form.styleType].textColor
      };
      Promise.all([
        saveAnnouncementContent(this.form.content),
        saveAnnouncementEnabled(this.form.enabled),
        saveAnnouncementStyle(styleConfig),
        saveAnnouncementScroll(this.form.scroll),
        saveAnnouncementScrollSpeed(this.form.scrollSpeed),
        saveAnnouncementFontSize(this.form.fontSize)
      ]).then(([c, e, s, sc, sp, fs]) => {
        if (c.success && e.success && s.success && sc.success && sp.success && fs.success) {
          this.$success(this.$t('commons.save_success'));
          this.bannerDialogVisible = false;
          this.$EventBus.$emit('announcement-updated');
        } else {
          this.$error(this.$t('commons.save_failed'));
        }
      }).catch(err => {
        this.$error(err.message || this.$t('commons.save_failed'));
      }).finally(() => { this.bannerLoading = false; });
    }
  }
};
</script>

<style scoped>
/* ========== 弹窗整体 ========== */
:deep(.banner-dialog) {
  border-radius: 8px;
}
:deep(.banner-dialog .el-dialog__header) {
  background: linear-gradient(135deg, #783887 0%, #409EFF 100%);
  border-radius: 8px 8px 0 0;
  padding: 16px 20px;
}
:deep(.banner-dialog .el-dialog__title) {
  color: #fff;
  font-weight: 600;
}
:deep(.banner-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
}
:deep(.banner-dialog .el-dialog__body) {
  padding: 16px 20px;
}
:deep(.banner-dialog .el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
}

/* ========== 分区卡片 ========== */
.banner-dialog-body {
  max-height: 520px;
  overflow-y: auto;
}

/* 横向并列行容器 */
.banner-row {
  display: flex;
  gap: 12px;
}

/* 小卡片（公告开关、滚动效果）：固定宽度 */
.banner-section-sm {
  flex: 0 0 180px;
}

/* 中卡片（样式设置）：自适应填充剩余空间 */
.banner-section-md {
  flex: 1;
}

.banner-section {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}
.banner-section-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.banner-section-icon {
  margin-right: 8px;
  font-size: 16px;
}
.banner-section-content {
  padding: 14px 16px;
}

/* ========== 样式选择器 ========== */
.style-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.style-option {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  border: 2px solid transparent;
  user-select: none;
}
.style-option:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.style-option-active {
  border-color: #303133;
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
  transform: translateY(-1px);
}
.custom-color-row {
  display: flex;
  align-items: center;
  margin-top: 12px;
}
.color-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}

/* ========== 字体大小选择器 ========== */
.font-size-selector {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.font-size-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.font-size-option:hover {
  border-color: #409EFF;
  background: #ecf5ff;
}
.font-size-option-active {
  border-color: #409EFF;
  background: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}
.font-size-option span:first-child {
  font-weight: 600;
  color: #303133;
  line-height: 1;
}
.font-size-label {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* ========== 预览区域 ========== */
.banner-preview {
  min-height: 36px;
  line-height: 36px;
  padding: 0 20px;
  border-radius: 4px;
  word-wrap: break-word;
  word-break: break-all;
  font-size: 14px;
}
.banner-preview-empty {
  min-height: 36px;
  line-height: 36px;
  text-align: center;
  color: #909399;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  font-size: 13px;
}
.banner-preview-scroll {
  overflow: hidden;
  white-space: nowrap;
}
.banner-preview-scroll .banner-preview-text {
  display: inline-block;
  padding-left: 100%;
  animation-name: banner-scroll;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}
@keyframes banner-scroll {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}
</style>
