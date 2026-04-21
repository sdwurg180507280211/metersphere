<template>
  <!--
    公告设置组件
    功能：管理员可以在此配置页面顶部公告栏的内容和样式
    包含：公告开关、公告内容输入框、样式选择器、实时预览区域、编辑/保存/取消按钮
    权限：需要 SYSTEM_SETTING:READ 权限查看，SYSTEM_SETTING:READ+EDIT 权限编辑
  -->
  <div v-loading="loading">
    <!-- 公告内容表单 -->
    <el-form :model="form" ref="form" :disabled="!isEditing" size="small" label-width="100px">
      <!-- 公告开关 -->
      <el-form-item :label="$t('announcement.enabled')">
        <el-switch v-model="form.enabled" />
      </el-form-item>

      <!-- 公告内容输入框 -->
      <el-form-item :label="$t('announcement.content')" prop="content">
        <el-input
          type="textarea"
          v-model="form.content"
          :rows="4"
          :placeholder="$t('announcement.content_placeholder')"
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>

      <!-- 样式选择 -->
      <el-form-item :label="$t('announcement.style')">
        <el-radio-group v-model="form.styleType" @change="onStyleTypeChange">
          <el-radio-button label="info">{{ $t('announcement.style_info') }}</el-radio-button>
          <el-radio-button label="warning">{{ $t('announcement.style_warning') }}</el-radio-button>
          <el-radio-button label="danger">{{ $t('announcement.style_danger') }}</el-radio-button>
          <el-radio-button label="success">{{ $t('announcement.style_success') }}</el-radio-button>
          <el-radio-button label="custom">{{ $t('announcement.style_custom') }}</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 自定义颜色选择器（仅自定义模式显示） -->
      <el-form-item v-if="form.styleType === 'custom'" :label="$t('announcement.custom_colors')">
        <div class="color-picker-group">
          <span class="color-label">{{ $t('announcement.background_color') }}:</span>
          <el-color-picker v-model="form.backgroundColor" size="small" />
          <span class="color-label" style="margin-left: 20px;">{{ $t('announcement.text_color') }}:</span>
          <el-color-picker v-model="form.textColor" size="small" />
        </div>
      </el-form-item>

      <!-- 滚动效果开关 -->
      <el-form-item :label="$t('announcement.scroll')">
        <el-switch v-model="form.scroll" />
        <span class="scroll-hint">{{ $t('announcement.scroll_hint') }}</span>
      </el-form-item>

      <!-- 滚动速度调节（仅滚动开启时显示） -->
      <el-form-item v-if="form.scroll" :label="$t('announcement.scroll_speed')">
        <el-slider
          v-model="form.scrollSpeed"
          :min="40"
          :max="80"
          :step="1"
          :marks="scrollSpeedMarks"
          show-input
          :format-tooltip="formatSpeedTooltip"
          style="width: 400px;"
        />
        <span class="scroll-hint">{{ $t('announcement.scroll_speed_hint') }}</span>
      </el-form-item>

      <!-- 实时预览区域 -->
      <el-form-item :label="$t('announcement.preview')">
        <!-- 启用且有内容时显示预览 -->
        <div
          class="announcement-preview"
          :class="{ 'announcement-scroll': form.scroll }"
          v-if="form.content && form.enabled"
          :style="previewStyle">
          <span class="announcement-text" :style="scrollAnimationStyle">{{ form.content }}</span>
        </div>
        <!-- 禁用时显示提示 -->
        <div class="announcement-preview-empty" v-else-if="!form.enabled">
          {{ $t('announcement.disabled_hint') }}
        </div>
        <!-- 无内容时显示空提示 -->
        <div class="announcement-preview-empty" v-else>
          {{ $t('announcement.no_content') }}
        </div>
      </el-form-item>
    </el-form>

    <!-- 操作按钮组 -->
    <div class="button-group">
      <!-- 编辑按钮：非编辑模式时显示，需要编辑权限 -->
      <el-button
        v-if="showEdit"
        @click="edit"
        size="small"
        v-permission="['SYSTEM_SETTING:READ+EDIT']">
        {{ $t('commons.edit') }}
      </el-button>
      <!-- 保存按钮：编辑模式时显示 -->
      <el-button
        v-if="showSave"
        type="success"
        @click="save"
        size="small">
        {{ $t('commons.save') }}
      </el-button>
      <!-- 取消按钮：编辑模式时显示 -->
      <el-button
        v-if="showCancel"
        type="info"
        @click="cancel"
        size="small">
        {{ $t('commons.cancel') }}
      </el-button>
    </div>
  </div>
</template>

<script>
/**
 * AnnouncementSetting 组件
 *
 * 用于管理系统公告栏内容和样式的配置组件
 *
 * 功能特性：
 * 1. 加载并显示当前公告内容、启用状态、样式配置
 * 2. 实时预览公告效果（支持动态样式）
 * 3. 预设样式选择（通知/警告/紧急/成功/自定义）
 * 4. 自定义颜色选择器（背景色、文字颜色）
 * 5. 公告开关控制
 * 6. 编辑/保存/取消操作
 * 7. 保存成功后通过 EventBus 通知页面顶部更新
 * 8. 权限控制：仅具有编辑权限的用户可修改
 */

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
  saveAnnouncementScrollSpeed
} from "../../../api/system";

// 预设样式配置
const PRESET_STYLES = {
  info: {
    name: '通知',
    backgroundColor: '#409EFF',  // Element UI primary blue
    textColor: '#FFFFFF'
  },
  warning: {
    name: '警告',
    backgroundColor: '#E6A23C',  // Element UI warning orange
    textColor: '#FFFFFF'
  },
  danger: {
    name: '紧急',
    backgroundColor: '#F56C6C',  // Element UI danger red
    textColor: '#FFFFFF'
  },
  success: {
    name: '成功',
    backgroundColor: '#67C23A',  // Element UI success green
    textColor: '#FFFFFF'
  },
  custom: {
    name: '自定义',
    backgroundColor: '#E6A23C',  // 默认同警告
    textColor: '#FFFFFF'
  }
};

export default {
  name: "AnnouncementSetting",
  data() {
    return {
      // 表单数据
      form: {
        content: '',           // 公告内容
        enabled: true,         // 是否启用
        styleType: 'warning',  // 样式类型: info/warning/danger/success/custom
        backgroundColor: '#E6A23C',  // 背景色（自定义模式）
        textColor: '#FFFFFF',  // 文字颜色（自定义模式）
        scroll: false,         // 是否启用滚动效果
        scrollSpeed: 15        // 滚动速度（秒），范围 5-30，默认 15
      },
      // 加载状态
      loading: false,
      // 是否处于编辑模式
      isEditing: false,
      // 原始表单数据（用于取消时恢复）
      originalForm: null,
      // 滚动速度刻度标记
      scrollSpeedMarks: {
        40: this.$t('announcement.speed_fast'),
        60: this.$t('announcement.speed_normal'),
        80: this.$t('announcement.speed_slow')
      }
    };
  },
  computed: {
    /**
     * 是否显示编辑按钮
     * 非编辑模式时显示
     */
    showEdit() {
      return !this.isEditing;
    },
    /**
     * 是否显示保存按钮
     * 编辑模式时显示
     */
    showSave() {
      return this.isEditing;
    },
    /**
     * 是否显示取消按钮
     * 编辑模式时显示
     */
    showCancel() {
      return this.isEditing;
    },
    /**
     * 预览样式
     * 根据样式类型返回对应的背景色和文字颜色
     */
    previewStyle() {
      if (this.form.styleType === 'custom') {
        return {
          backgroundColor: this.form.backgroundColor,
          color: this.form.textColor
        };
      }
      const style = PRESET_STYLES[this.form.styleType] || PRESET_STYLES.warning;
      return {
        backgroundColor: style.backgroundColor,
        color: style.textColor
      };
    },
    /**
     * 滚动动画样式
     * 根据滚动速度动态设置动画时长
     */
    scrollAnimationStyle() {
      if (this.form.scroll) {
        return {
          animationDuration: `${this.form.scrollSpeed}s`
        };
      }
      return {};
    }
  },
  created() {
    // 组件创建时加载公告配置
    this.loadAnnouncement();
    // 初始化滚动速度刻度标记（需要在 created 中初始化以使用 $t）
    this.scrollSpeedMarks = {
      40: this.$t('announcement.speed_fast') || '快',
      60: this.$t('announcement.speed_normal') || '正常',
      80: this.$t('announcement.speed_slow') || '慢'
    };
  },
  methods: {
    /**
     * 格式化速度提示文本
     * @param {number} value - 速度值（秒）
     * @returns {string} 格式化后的提示文本
     */
    formatSpeedTooltip(value) {
      return `${value}${this.$t('announcement.speed_unit') || '秒'}`;
    },
    /**
     * 从后端加载公告配置（内容、启用状态、样式、滚动速度）
     */
    loadAnnouncement() {
      this.loading = true;

      // 并行加载五个配置
      Promise.all([
        getAnnouncementContent().catch(() => ({ data: null })),
        getAnnouncementEnabled().catch(() => ({ data: null })),
        getAnnouncementStyle().catch(() => ({ data: null })),
        getAnnouncementScroll().catch(() => ({ data: null })),
        getAnnouncementScrollSpeed().catch(() => ({ data: null }))
      ]).then(([contentRes, enabledRes, styleRes, scrollRes, scrollSpeedRes]) => {
        // 加载内容
        if (contentRes.data && contentRes.data.paramValue) {
          this.form.content = contentRes.data.paramValue;
        } else {
          this.form.content = '';
        }

        // 加载启用状态
        if (enabledRes.data && enabledRes.data.paramValue) {
          this.form.enabled = enabledRes.data.paramValue === 'true';
        } else {
          this.form.enabled = true;  // 默认启用
        }

        // 加载样式配置
        if (styleRes.data && styleRes.data.paramValue) {
          try {
            const styleConfig = JSON.parse(styleRes.data.paramValue);
            this.form.styleType = styleConfig.styleType || 'warning';
            this.form.backgroundColor = styleConfig.backgroundColor || '#E6A23C';
            this.form.textColor = styleConfig.textColor || '#FFFFFF';
          } catch (e) {
            // JSON 解析失败，使用默认值
            this.form.styleType = 'warning';
            this.form.backgroundColor = '#E6A23C';
            this.form.textColor = '#FFFFFF';
          }
        }

        // 加载滚动配置
        if (scrollRes.data && scrollRes.data.paramValue) {
          this.form.scroll = scrollRes.data.paramValue === 'true';
        } else {
          this.form.scroll = false;  // 默认不滚动
        }

        // 加载滚动速度配置
        if (scrollSpeedRes.data && scrollSpeedRes.data.paramValue) {
          this.form.scrollSpeed = parseInt(scrollSpeedRes.data.paramValue) || 15;
        } else {
          this.form.scrollSpeed = 15;  // 默认速度 15 秒
        }

        // 保存原始表单数据
        this.originalForm = JSON.parse(JSON.stringify(this.form));
      }).finally(() => {
        this.loading = false;
      });
    },

    /**
     * 样式类型切换时更新颜色
     * 切换到预设样式时，自动应用对应的颜色
     */
    onStyleTypeChange(type) {
      if (type !== 'custom') {
        const style = PRESET_STYLES[type];
        if (style) {
          this.form.backgroundColor = style.backgroundColor;
          this.form.textColor = style.textColor;
        }
      }
    },

    /**
     * 进入编辑模式
     * 保存当前表单数据作为原始数据，以便取消时恢复
     */
    edit() {
      this.originalForm = JSON.parse(JSON.stringify(this.form));
      this.isEditing = true;
    },

    /**
     * 保存公告配置到后端
     * 保存成功后通知页面顶部公告栏更新
     */
    save() {
      this.loading = true;

      // 构建样式配置对象
      const styleConfig = {
        styleType: this.form.styleType,
        backgroundColor: this.form.styleType === 'custom'
          ? this.form.backgroundColor
          : PRESET_STYLES[this.form.styleType].backgroundColor,
        textColor: this.form.styleType === 'custom'
          ? this.form.textColor
          : PRESET_STYLES[this.form.styleType].textColor
      };

      // 并行保存五个配置
      Promise.all([
        saveAnnouncementContent(this.form.content),
        saveAnnouncementEnabled(this.form.enabled),
        saveAnnouncementStyle(styleConfig),
        saveAnnouncementScroll(this.form.scroll),
        saveAnnouncementScrollSpeed(this.form.scrollSpeed)
      ]).then(([contentRes, enabledRes, styleRes, scrollRes, scrollSpeedRes]) => {
        // 检查所有保存是否成功
        if (contentRes.success && enabledRes.success && styleRes.success && scrollRes.success && scrollSpeedRes.success) {
          // 保存成功提示
          this.$success(this.$t('commons.save_success'));
          // 更新原始表单数据
          this.originalForm = JSON.parse(JSON.stringify(this.form));
          // 退出编辑模式
          this.isEditing = false;
          // 通知页面顶部公告栏更新
          this.notifyAnnouncementUpdate();
        } else {
          // 保存失败提示
          this.$error(this.$t('commons.save_failed'));
        }
      }).catch(error => {
        // 网络或服务器错误
        this.$error(error.message || this.$t('commons.save_failed'));
      }).finally(() => {
        this.loading = false;
      });
    },

    /**
     * 取消编辑
     * 恢复原始表单数据，退出编辑模式
     */
    cancel() {
      if (this.originalForm) {
        this.form = JSON.parse(JSON.stringify(this.originalForm));
      }
      this.isEditing = false;
    },

    /**
     * 通过 EventBus 通知页面顶部公告栏更新
     * 发送 'announcement-updated' 事件
     */
    notifyAnnouncementUpdate() {
      // 使用 EventBus 发送事件，通知 AppLayout 更新公告栏
      this.$EventBus.$emit('announcement-updated');
    }
  }
};
</script>

<style scoped>
/**
 * 公告预览区域样式
 * 支持动态背景色和文字颜色
 */
.announcement-preview {
  min-height: 30px;
  text-align: left;
  line-height: 30px;
  padding: 0 20px;
  word-wrap: break-word;
  word-break: break-all;
  border-radius: 4px;
}

/**
 * 空预览区域样式
 * 灰色边框和文字，提示用户当前无公告内容或已禁用
 */
.announcement-preview-empty {
  min-height: 30px;
  background: #f5f7fa;
  text-align: center;
  line-height: 30px;
  color: #909399;
  padding: 0 20px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

/**
 * 颜色选择器组样式
 */
.color-picker-group {
  display: flex;
  align-items: center;
}

.color-label {
  color: #606266;
  font-size: 14px;
}

/**
 * 滚动提示文字样式
 */
.scroll-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

/**
 * 公告滚动动画样式
 */
.announcement-scroll {
  overflow: hidden;
  white-space: nowrap;
}

.announcement-scroll .announcement-text {
  display: inline-block;
  padding-left: 100%;
  animation-name: scroll-left;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
  /* 动画时长通过内联样式动态设置 */
}

@keyframes scroll-left {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

/**
 * 按钮组样式
 * 与其他设置组件保持一致的间距
 */
.button-group {
  margin-top: 20px;
  margin-left: 100px;
}
</style>
