<template>
  <div class="ms-drawer-wrapper">
    <!-- 遮罩层 -->
    <div
      v-if="visible && mask"
      class="ms-drawer-mask"
      @click="handleMaskClick"
    ></div>
    
    <div
      v-if="visible"
      id="ms-drawer"
      class="ms-drawer"
      :class="[directionStyle, { 'ms-drawer-no-mask': !mask, 'with-footer': showFooter }]"
      :style="{ width: w + 'px', height: h + 'px' }"
      ref="msDrawer"
    >
    <ms-bottom2-top-drag-bar v-if="direction == 'bottom'" />

    <ms-right2-left-drag-bar v-if="direction == 'right'" />

    <ms-right2-left-drag-bar v-if="direction == 'default'" />

    <div class="ms-drawer-header">
      <div class="ms-drawer-header-content">
        <div class="ms-drawer-header-left">
          <slot name="titleLeft"></slot>
          <slot name="header">
            <span v-if="title" class="ms-drawer-title">{{ title }}</span>
          </slot>
        </div>
        <div class="ms-drawer-header-right">
          <slot name="titleRight"></slot>
          <ms-full-screen-button
            v-if="showFullScreen"
            :is-full-screen.sync="isFullScreen"
          />
          <i v-if="isShowClose" class="el-icon-close" @click="close" />
        </div>
      </div>
    </div>
    <div class="ms-drawer-body">
      <slot></slot>
    </div>

    <!-- 页脚区域 -->
    <div v-if="showFooter" class="ms-drawer-footer">
      <div class="ms-drawer-footer-content">
        <div class="ms-drawer-footer-left">
          <slot name="footerLeft"></slot>
        </div>
        <div class="ms-drawer-footer-right">
          <slot name="footerRight">
            <el-button
              v-if="showCancel"
              :disabled="okLoading"
              @click="handleCancel"
            >
              {{ cancelText || $t('commons.cancel') }}
            </el-button>
            <el-button
              v-if="showContinue"
              type="primary"
              :loading="okLoading"
              :disabled="okDisabled"
              @click="handleContinue"
            >
              {{ saveContinueText || $t('commons.save_continue') }}
            </el-button>
            <el-button
              v-if="showOk"
              type="primary"
              :loading="okLoading"
              :disabled="okDisabled"
              @click="handleOk"
            >
              {{ okText || $t('commons.confirm') }}
            </el-button>
          </slot>
        </div>
      </div>
    </div>

    <ms-left2-right-drag-bar v-if="direction == 'left'" />
    </div>
  </div>
</template>

<script>
import MsRight2LeftDragBar from "./dragbar/MsRight2LeftDragBar";
import MsLeft2RightDragBar from "./dragbar/MsLeft2RightDragBar";
import MsBottom2TopDragBar from "./dragbar/MsBottom2TopDragBar";
import MsFullScreenButton from "./MsFullScreenButton";

export default {
  name: "MsDrawer",
  components: {
    MsFullScreenButton,
    MsBottom2TopDragBar,
    MsLeft2RightDragBar,
    MsRight2LeftDragBar,
  },
  data() {
    return {
      x: 0,
      y: 0,
      w: 100,
      h: 100,
      directionStyle: "left-style",
      dragBarDirection: "vertical",
      isFullScreen: false,
      originalW: 0,
      originalH: 0,
    };
  },
  props: {
    appendToBody: {
      type: Boolean,
      default: false,
    },
    direction: {
      type: String,
      default() {
        return "left";
      },
    },
    visible: {
      type: Boolean,
      default() {
        return true;
      },
    },
    size: {
      type: Number,
      default() {
        return 40;
      },
    },
    showFullScreen: {
      type: Boolean,
      default() {
        return true;
      },
    },
    isShowClose: {
      type: Boolean,
      default() {
        return true;
      },
    },
    // 是否显示遮罩
    mask: {
      type: Boolean,
      default: true,
    },
    // 点击遮罩是否关闭
    maskClosable: {
      type: Boolean,
      default: true,
    },
    // 标题
    title: {
      type: String,
      default: "",
    },
    // 是否显示页脚
    showFooter: {
      type: Boolean,
      default: false,
    },
    // 是否显示取消按钮
    showCancel: {
      type: Boolean,
      default: true,
    },
    // 是否显示确认按钮
    showOk: {
      type: Boolean,
      default: true,
    },
    // 是否显示保存并继续按钮
    showContinue: {
      type: Boolean,
      default: false,
    },
    // 确认按钮文字
    okText: {
      type: String,
      default: "",
    },
    // 取消按钮文字
    cancelText: {
      type: String,
      default: "",
    },
    // 保存并继续按钮文字
    saveContinueText: {
      type: String,
      default: "",
    },
    // 确认按钮loading状态
    okLoading: {
      type: Boolean,
      default: false,
    },
    // 确认按钮禁用状态
    okDisabled: {
      type: Boolean,
      default: false,
    },
  },
  mounted() {
    this.init();
  },
  watch: {
    isFullScreen() {
      if (this.isFullScreen) {
        this.fullScreen();
      } else {
        this.unFullScreen();
      }
    },
    visible(val) {
      if (val && this.appendToBody) {
        document.body.appendChild(this.$el);
      }
    },
  },
  methods: {
    setfullScreen() {
      if (!this.isFullScreen) {
        this.$nextTick(() => {
          this.isFullScreen = true;
        });
      }
    },
    init() {
      window.addEventListener("resize", this.listenScreenChange, false);
      //  todo 其他方向待优化
      switch (this.direction) {
        case "left":
          this.w = this.getWidthPercentage(this.size);
          this.h = this.getHeightPercentage(100);
          this.x = 0;
          this.y = 0;
          this.directionStyle = "left-style";
          this.dragBarDirection = "horizontal";
          break;
        case "right":
          this.w = this.getWidthPercentage(this.size);
          this.h = this.getHeightPercentage(100);
          this.x = document.body.clientWidth - this.w;
          this.y = 0;
          this.directionStyle = "right-style";
          this.dragBarDirection = "horizontal";
          break;
        case "top":
          this.w = this.getWidthPercentage(100);
          this.h = this.getHeightPercentage(this.size);
          this.x = 0;
          this.y = 0;
          this.directionStyle = "top-style";
          this.dragBarDirection = "vertical";
          break;
        case "bottom":
          this.w = this.getWidthPercentage(100);
          this.h = this.getHeightPercentage(this.size);
          this.x = 0;
          this.y = document.body.clientHeight - this.h;
          this.directionStyle = "bottom-style";
          this.dragBarDirection = "vertical";
          break;
        default:
          this.w = this.getWidthPercentage(this.size);
          this.h = this.getHeightPercentage(100);
          this.x = document.body.clientWidth - this.w;
          this.y = 0;
          this.directionStyle = "right-style";
          this.dragBarDirection = "horizontal";
          break;
      }
    },
    getWidthPercentage(per) {
      return (document.body.clientWidth * per) / 100.0;
    },
    getHeightPercentage(per) {
      return (document.body.clientHeight * per) / 100.0;
    },
    fullScreen() {
      if (this.originalW === 0) {
        this.originalW = this.w;
      }
      if (this.originalH === 0) {
        this.originalH = this.h;
      }
      this.w = document.body.clientWidth;
      this.h = document.body.clientHeight;
      this.$emit("changeScreen", this.h);
    },
    unFullScreen() {
      this.w = this.originalW;
      this.h = this.originalH;
      this.$emit("changeScreen", this.h);
    },
    close() {
      this.$emit("close");
      this.$emit("update:visible", false);
      window.removeEventListener("resize", this.listenScreenChange);
    },
    handleMaskClick() {
      if (this.maskClosable) {
        this.close();
      }
    },
    handleOk() {
      this.$emit("confirm");
    },
    handleCancel() {
      this.$emit("cancel");
      this.close();
    },
    handleContinue() {
      this.$emit("continue");
    },
    listenScreenChange() {
      if (this.isFullScreen) {
        this.w = document.body.clientWidth;
        this.h = document.body.clientHeight;
      } else {
        switch (this.direction) {
          case "left":
            this.h = document.documentElement.clientHeight;
            break;
          case "right":
            this.h = document.documentElement.clientHeight;
            break;
          case "top":
            this.w = document.documentElement.clientWidth;
            break;
          case "bottom":
            this.w = document.documentElement.clientWidth;
            break;
          default:
            this.h = document.documentElement.clientHeight;
            this.w = document.documentElement.clientWidth;
            break;
        }
      }
    },
  },
  beforeDestroy() {
    if (this.$el?.style && this.appendToBody) {
      document.body.removeChild(this.$el);
    }
  },
};
</script>

<style scoped>
/* 根容器 - 不占用布局空间 */
.ms-drawer-wrapper {
  position: relative;
  display: contents;
}

/* 遮罩层 */
.ms-drawer-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 998;
}

.ms-drawer {
  background-color: white;
  border: 1px #dcdfe6 solid;
  -webkit-box-shadow: 0 8px 10px -5px rgba(0, 0, 0, 0.2),
    0 16px 24px 2px rgba(0, 0, 0, 0.14), 0 6px 30px 5px rgba(0, 0, 0, 0.12);
  box-shadow: 0 8px 10px -5px rgba(0, 0, 0, 0.2),
    0 16px 24px 2px rgba(0, 0, 0, 0.14), 0 6px 30px 5px rgba(0, 0, 0, 0.12);
  z-index: 999 !important;
  position: fixed;
  overflow: auto;
}

/* 无遮罩模式 */
.ms-drawer-no-mask {
  box-shadow: 0 4px 10px -1px rgba(100, 100, 102, 0.15);
}

.left-style {
  top: 0;
  left: 0;
}

.right-style {
  top: 0;
  right: 0;
}

.top-style {
  top: 0;
  left: 0;
}

.bottom-style {
  bottom: 0;
  left: 0;
  border-top: 5px;
}

.ms-drawer-body {
  margin-top: 10px;
  padding: 0 15px;
  height: calc(100vh - 40px);
  overflow: scroll;
}

/* 有页脚时调整body高度 */
.ms-drawer.with-footer .ms-drawer-body {
  height: calc(100vh - 100px);
}

/* 页脚样式 */
.ms-drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 15px;
  border-top: 1px solid #e4e7ed;
  background-color: white;
  z-index: 999;
}

.ms-drawer-footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ms-drawer-footer-left {
  display: flex;
  align-items: center;
}

.ms-drawer-footer-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ms-drawer-header {
  z-index: 999;
  width: 100%;
  margin: 0;
  padding: 10px 15px;
  border-bottom: 1px solid #e4e7ed;
}

.ms-drawer-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.ms-drawer-header-left {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.ms-drawer-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ms-drawer-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bottom-style .ms-drawer-header {
  position: fixed;
}

.el-icon-close {
  font-size: 20px;
  color: gray;
  cursor: pointer;
  flex-shrink: 0;
}

.el-icon-close:hover {
  color: red;
}

.el-icon-close:hover {
  color: red;
}

:deep(alt-ico) {
  position: absolute;
  right: 40px;
  top: 15px;
}
</style>
