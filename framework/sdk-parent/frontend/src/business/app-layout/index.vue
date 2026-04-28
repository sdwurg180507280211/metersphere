<template>
  <el-container>
    <el-header :height="headerHeight" class="ms-header-w">
      <el-row>
        <el-col>
          <mx-license-message/>
        </el-col>
      </el-row>
      <el-row v-if="announcementContent && announcementEnabled">
        <el-col>
          <div class="announcement-tip" :class="{ 'announcement-scroll': announcementScroll }" :style="announcementStyle">
            <span class="announcement-text" :style="scrollTextStyle">{{ announcementContent }}</span>
          </div>
        </el-col>
      </el-row>
    </el-header>

    <el-container>
      <el-aside
        :class="[isCollapse ? 'ms-aside': 'ms-aside-collapse-open', isFullScreen ? 'is-fullscreen' : '']"
        class="ms-left-aside shepherd-menu"
        :style="isFixed ? 'opacity:100%; position: relative;z-index: 666;': 'opacity: 95%;position: fixed'"
        @mouseenter.native="collapseOpen"
        @mouseleave.native="collapseClose">
        <ms-aside-header :sideTheme="sideTheme" :isCollapse="isCollapse" :title="sysTitle"/>
        <ms-aside-menus :sideTheme="sideTheme" :color="color" :isCollapse="isCollapse"/>
        <div class="ms-header-fixed" v-show="!isCollapse">
          <svg-icon iconClass="pushpin" class-name="ms-menu-pin" v-if="isFixed" @click.native="fixedChange(false)"/>
          <svg-icon iconClass="unpin" class-name="ms-menu-pin" v-else @click.native="fixedChange(true)"/>
        </div>
      </el-aside>
      <el-main class="container">
        <div :class="isFixed ? 'ms-left-fixed': 'ms-aside-left'"/>
        <div :class="isFixed ? 'ms-right-fixed': 'ms-main-view ms-aside-right'">
          <micro-app
            v-if="currentApp"
            :key="currentApp.name"
            :name="currentApp.name"
            :url="currentApp.entry"
            :data="appData"
            :destroy="false"
            :fiber="true"
            :iframe="currentApp.isViteApp || false"
            :inline="true"
            :disable-memory-router="true"
            :disable-scopecss="true"
            @datachange="handleDataChange"
            @error="handleError"
          />
          <router-view v-else-if="isShow"/>
        </div>
      </el-main>
      <mx-theme/>
    </el-container>
  </el-container>
</template>

<script>

import MsAsideFooter from "../../components/layout/AsideFooter";
import MsAsideHeader from "../../components/layout/AsideHeader";
import MsAsideMenus from "../../components/layout/AsideMenus";
import MsView from "../../components/layout/View";
import MxLicenseMessage from "../../components/MxLicenseMessage";
import MxTheme from "../../components/MxTheme";
import {hasLicense} from "../../utils/permission";
import {setAsideColor, setColor, setCustomizeColor, setDefaultTheme, setLightColor} from "../../utils";
import {ORIGIN_COLOR} from "../../utils/constants";
import {getDisplayInfo, getSystemTheme, isLogin, getSystemParameter} from "../../api/user";
import {useUserStore} from "@/store";
import {getModuleList} from "../../api/module";
import {MIGRATED_MODULES} from "../../micro-app-config";


export default {
  name: "AppLayout",
  components: {MsView, MsAsideFooter, MsAsideHeader, MsAsideMenus, MxLicenseMessage, MxTheme},
  data() {
    return {
      licenseHeader: null,
      header: {},
      logoId: '_blank',
      color: '',
      sessionTimer: null,
      isShow: true,
      isMenuShow: true,
      isCollapse: true,
      headerHeight: "0px",
      isFixed: false,
      sideTheme: "",
      sysTitle: undefined,
      isFullScreen: false,
      announcementContent: '',
      announcementEnabled: true,
      announcementStyleConfig: {
        styleType: 'warning',
        backgroundColor: '#E6A23C',
        textColor: '#FFFFFF'
      },
      announcementScroll: false,
      announcementScrollSpeed: 15,  // 滚动速度（秒），默认15秒
    };
  },
  created() {
    this.loadAnnouncement();
    this.initSessionTimer();
    getModuleList()
      .then(response => {
        let modules = {};
        response.data.forEach(m => {
          modules[m.key] = m.status;
        });
        localStorage.setItem('modules', JSON.stringify(modules));
      });
    if (!hasLicense()) {
      setDefaultTheme();
      setCustomizeColor();
      this.color = ORIGIN_COLOR;
    } else {
      getSystemTheme()
        .then(res => {
          this.color = res.data ? res.data : ORIGIN_COLOR;
          setColor(this.color, this.color, this.color, this.color, this.color);
          // this.$store.commit('setTheme', res.data);
        });
      this.getDisplayInfo();
    }

    this.isFixed = localStorage.getItem('app-fixed') === 'true' || false;
    this.isCollapse = this.isFixed === true ? false : true;

    this.$EventBus.$on("toggleFullScreen", (param) => {
      this.isFullScreen = param
    });

    // 监听公告更新事件，收到事件后重新加载公告内容
    this.$EventBus.$on('announcement-updated', this.loadAnnouncement);
  },
  destroyed() {
    this.$EventBus.$off("toggleFullScreen");
    // 移除公告更新事件监听，防止内存泄漏
    this.$EventBus.$off('announcement-updated', this.loadAnnouncement);
  },
  // 提供可注入子组件属性
  provide() {
    return {
      reload: this.reload,
      reloadTopMenus: this.reloadTopMenus,
    };
  },
  computed: {
    /**
     * 从当前 hash 路由中提取模块名称
     * MeterSphere 使用 hash 路由，格式为 #/{moduleName}/...
     * 例如 #/api/definition → 模块名为 'api'
     */
    currentModuleName() {
      const path = this.$route.path || '';
      const match = path.match(/^\/([^/]+)/);
      return match ? match[1] : '';
    },
    /**
     * 当前激活的子应用配置
     * 根据模块名从配置表中获取迁移状态和技术栈信息
     * 返回 null 表示当前路由不对应任何子应用（如主应用自身页面）
     */
    currentApp() {
      const name = this.currentModuleName;
      if (!name || !MIGRATED_MODULES[name]) {
        return null;
      }
      const config = MIGRATED_MODULES[name];
      return {
        name: name,
        entry: this.getEntryUrl(name),
        migrated: config.migrated,
        isViteApp: config.isViteApp,
      };
    },
    /**
     * 传递给子应用的数据
     * 通过 <micro-app :data="appData"> 传入，子应用在 window.mount(data) 中接收
     */
    appData() {
      return {
        defaultPath: this.$route.path,
      };
    },
    /**
     * 公告栏动态样式
     * 根据样式配置返回背景色和文字颜色
     */
    announcementStyle() {
      return {
        backgroundColor: this.announcementStyleConfig.backgroundColor,
        color: this.announcementStyleConfig.textColor
      };
    },
    /**
     * 滚动文本动态样式
     * 根据滚动速度设置动画时长
     */
    scrollTextStyle() {
      if (this.announcementScroll) {
        return {
          animationDuration: `${this.announcementScrollSpeed}s`
        };
      }
      return {};
    }
  },
  methods: {
    /**
     * 计算子应用入口 URL
     * 开发环境：//127.0.0.1:{port-4000}
     * 生产环境：{origin}/{serviceId}
     */
    getEntryUrl(name) {
      const microPorts = JSON.parse(sessionStorage.getItem('micro_ports') || '{}');
      if (process.env.NODE_ENV === 'development') {
        return '//127.0.0.1:' + (microPorts[name] - 4000);
      }
      return window.location.origin + '/' + name;
    },
    /** 处理子应用通过 dispatch 发送的数据 */
    handleDataChange(e) {
      // console.log('[Layout] 收到子应用数据:', e.detail.data);
    },
    /** 处理子应用加载错误 */
    handleError(e) {
      console.error('[Layout] 子应用加载出错:', e.detail.name, e.detail.error);
    },
    loadAnnouncement() {
      // 获取公告内容
      getSystemParameter('announcement.content').then(response => {
        if (response.data && response.data.paramValue) {
          this.announcementContent = response.data.paramValue;
        } else {
          this.announcementContent = '';
        }
        this.updateHeaderHeight();
      }).catch(() => {
        this.announcementContent = '';
      });

      // 获取公告启用状态
      getSystemParameter('announcement.enabled').then(response => {
        if (response.data && response.data.paramValue) {
          this.announcementEnabled = response.data.paramValue === 'true';
        } else {
          this.announcementEnabled = true;  // 默认启用
        }
        this.updateHeaderHeight();
      }).catch(() => {
        this.announcementEnabled = true;
      });

      // 获取公告样式配置
      getSystemParameter('announcement.style').then(response => {
        if (response.data && response.data.paramValue) {
          try {
            const styleConfig = JSON.parse(response.data.paramValue);
            this.announcementStyleConfig = {
              styleType: styleConfig.styleType || 'warning',
              backgroundColor: styleConfig.backgroundColor || '#E6A23C',
              textColor: styleConfig.textColor || '#FFFFFF'
            };
          } catch (e) {
            // JSON 解析失败，使用默认样式
          }
        }
      }).catch(() => {
        // 忽略错误，使用默认样式
      });

      // 获取公告滚动配置
      getSystemParameter('announcement.scroll').then(response => {
        if (response.data && response.data.paramValue) {
          this.announcementScroll = response.data.paramValue === 'true';
        } else {
          this.announcementScroll = false;
        }
      }).catch(() => {
        this.announcementScroll = false;
      });

      // 获取公告滚动速度配置
      getSystemParameter('announcement.scroll.speed').then(response => {
        if (response.data && response.data.paramValue) {
          this.announcementScrollSpeed = parseInt(response.data.paramValue) || 15;
        } else {
          this.announcementScrollSpeed = 15;
        }
      }).catch(() => {
        this.announcementScrollSpeed = 15;
      });
    },
    updateHeaderHeight() {
      if (this.licenseHeader != null || (this.announcementContent && this.announcementEnabled)) {
        // 公告栏支持自动换行，高度自适应
        this.headerHeight = "auto";
      } else {
        this.headerHeight = "0px";
      }
    },
    getDisplayInfo() {
      this.result = getDisplayInfo()
        .then(response => {
          let theme = "";
          if (response.data && response.data[5] && response.data[5].paramValue) {
            theme = response.data[5].paramValue;
          }
          if (response.data && response.data[7] && response.data[7].paramValue) {
            this.sideTheme = response.data[7].paramValue;
          }

          if (response.data && response.data[6] && response.data[6].paramValue) {
            this.sysTitle = response.data[6].paramValue || "MeterSphere";
            localStorage.setItem("default-sys-title", this.sysTitle);
          }

          let title = response.data[4].paramValue;
          if (title) {
            localStorage.setItem("default-document-title", title);
            if (this.$route.fullPath.indexOf("/track/case/edit") === -1) {
              // 如果不是用例编辑, 新增, 复制页面则需修改title为系统title
              document.title = title;
            }
          } else {
            localStorage.setItem("default-document-title", "MeterSphere");
          }

          this.setAsideTheme(theme);
        });
    },

    setAsideTheme(theme) {
      switch (this.sideTheme) {
        case "theme-light":
          setLightColor();
          break;
        case "theme-default":
          setAsideColor();
          break;
        default:
          setCustomizeColor(theme);
          break;
      }
    },
    fixedChange(isFixed) {
      this.isFixed = isFixed;
      if (this.isFixed) {
        this.isCollapse = false;
      }
      localStorage.removeItem('app-fixed');
      localStorage.setItem('app-fixed', this.isFixed);
      this.$EventBus.$emit('appFixedChange', this.isFixed);
    },
    collapseOpen() {
      this.isCollapse = false;
    },
    collapseClose() {
      if (!this.isFixed) {
        this.isCollapse = true;
      }
    },
    initSessionTimer() {
      let timeout = 1800;
      this.initTimer(timeout);
    },
    initTimer(timeout) {
      const userStore = useUserStore()
      setInterval(() => {
        isLogin()
          .then(() => {
          })
          .catch(() => {
            userStore.userLogout();
          });
      }, timeout * 1000);
    },
    reload() {
      // 先隐藏
      this.isShow = false;
      this.$nextTick(() => {
        this.isShow = true;
      });
    },
    reloadTopMenus() {
      const userStore = useUserStore()
      return userStore.getIsLogin()
        .then(response => {
          this.$setLang(response.data.language);
          // 先隐藏
          this.isMenuShow = false;
          this.isShow = false;
          this.$nextTick(() => {
            this.isShow = true;
            this.isMenuShow = true;
          });
        })
    }
  },
}
</script>


<style scoped>
.ms-aside {
  z-index: 666;
  width: var(--asideWidth) !important;
  background-color: var(--aside_color);
  color: var(--font_color);
  opacity: 100%;
  height: calc(100vh);
}

.ms-aside-collapse-open {
  width: var(--asideOpenWidth) !important;
  background-color: var(--aside_color);
  color: var(--font_color);
  opacity: 95%;
  z-index: 9999;
  border-right: 1px #DCDFE6 solid;
  border-radius: 2px;
}

.announcement-tip {
  min-height: 30px;
  text-align: left;
  line-height: 30px;
  padding: 0 20px;
  word-wrap: break-word;
  word-break: break-all;
}

/* 公告滚动动画样式 */
.announcement-scroll {
  overflow: hidden;
  white-space: nowrap;
  word-wrap: normal;
  word-break: normal;
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

.ms-left-aside {
  position: fixed;
  left: 0;
  height: calc(100vh);
  background-color: var(--aside_color);
  padding-left: 0px;
  overflow: hidden;
}

.ms-main-view {
  margin-left: var(--asideWidth);
}

.container {
  padding: 0!important;
  height: calc(100vh);
  background-color: #F5F6F7;
}

.ms-aside-left {
  float: left;
  width: var(--asideWidth);
  height: calc(100vh);
  background-color: var(--aside_color);
}

.ms-left-fixed {
  width: 0px;
  border-right: 0px;
}

.ms-aside-right {
  flex: 1;
  height: calc(100vh);
  background-color: #F5F6F7;
}

.ms-right-fixed {
  flex: 0;
  margin-left: 0px;
  height: calc(100vh);
  background-color: #F5F6F7;
}

.ms-header-w {
  width: 100%;
  padding: 0px;
  height: auto !important;
}

.ms-header-fixed {
  margin-left: var(--asideOpenMargin);
  position: absolute;
  bottom: 20px;
}

.checkBox-input :deep(.el-checkbox__inner) {
  border-color: #fff;
}

.checkBox-input :deep(.el-checkbox__inner::after) {
  top: 4px;
  left: 4px;
  width: 3px;
  height: 3px;
  border-radius: 100%;
  background-color: #fff !important;
  content: "";
  position: absolute;
  border-color: #fff !important;
}

.ms-menu-pin {
  color: var(--font_color);
  fill: currentColor;
  font-size: 20px;
}

.ms-menu-pin:hover {
  cursor: pointer;
}

.is-fullscreen {
  display: none;
}
</style>
