<template>
  <div class="apitable-multitable">
    <div v-if="!embedUrl" class="apitable-state">
      <div class="apitable-state__title">
        {{ configLoading ? $t("test_track.multitable.loading") : $t("test_track.multitable.apitable_not_configured") }}
      </div>
      <div class="apitable-state__desc">
        {{ configLoading ? "" : $t("test_track.multitable.apitable_config_hint") }}
      </div>
    </div>

    <div v-else class="apitable-frame-shell" v-loading="loading" :element-loading-text="$t('test_track.multitable.loading')">
      <iframe
        class="apitable-frame"
        :src="embedUrl"
        title="APITable"
        @load="handleLoad"
      />

      <div v-if="showLoadWarning" class="apitable-load-warning">
        <div>
          <div class="apitable-load-warning__title">
            {{ $t("test_track.multitable.load_timeout") }}
          </div>
          <div class="apitable-load-warning__desc">
            {{ $t("test_track.multitable.load_timeout_hint") }}
          </div>
        </div>
        <el-button
          v-if="openInNewWindow"
          size="small"
          type="primary"
          icon="el-icon-top-right"
          @click="openExternal"
        >
          {{ $t("test_track.multitable.open_in_new_window") }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getApitableConfig } from "@/api/multitable";
import { getApitableEmbedConfig, mergeApitableEmbedConfig } from "./apitable-config";

export default {
  name: "ApitableMultitable",
  data() {
    const config = getApitableEmbedConfig();
    return {
      embedUrl: config.embedUrl,
      openInNewWindow: config.openInNewWindow,
      loadTimeout: config.loadTimeout,
      configLoading: !config.embedUrl,
      loading: !!config.embedUrl,
      showLoadWarning: false,
      warningTimer: null,
    };
  },
  mounted() {
    this.loadServerConfig();
    if (this.embedUrl) {
      this.startLoadTimer();
    }
  },
  activated() {
    this.startLoadTimer();
  },
  deactivated() {
    this.clearLoadTimer();
  },
  beforeDestroy() {
    this.clearLoadTimer();
  },
  methods: {
    loadServerConfig() {
      if (this.embedUrl) {
        this.configLoading = false;
        return;
      }
      getApitableConfig()
        .then((response) => {
          const config = mergeApitableEmbedConfig(response.data || {});
          this.embedUrl = config.embedUrl;
          this.openInNewWindow = config.openInNewWindow;
          this.loadTimeout = config.loadTimeout;
          this.loading = !!config.embedUrl;
          this.$nextTick(() => this.startLoadTimer());
        })
        .catch(() => {
          const config = mergeApitableEmbedConfig();
          this.embedUrl = config.embedUrl;
          this.openInNewWindow = config.openInNewWindow;
          this.loadTimeout = config.loadTimeout;
          this.loading = !!config.embedUrl;
          this.$nextTick(() => this.startLoadTimer());
        })
        .finally(() => {
          this.configLoading = false;
        });
    },
    startLoadTimer() {
      this.clearLoadTimer();
      if (!this.embedUrl || !this.loading) {
        return;
      }
      this.warningTimer = window.setTimeout(() => {
        if (this.loading) {
          this.showLoadWarning = true;
        }
      }, this.loadTimeout);
    },
    clearLoadTimer() {
      if (this.warningTimer) {
        window.clearTimeout(this.warningTimer);
        this.warningTimer = null;
      }
    },
    handleLoad() {
      this.loading = false;
      this.showLoadWarning = false;
      this.clearLoadTimer();
    },
    openExternal() {
      window.open(this.embedUrl, "_blank", "noopener,noreferrer");
    },
  },
};
</script>

<style scoped>
.apitable-multitable {
  height: calc(100vh - 125px);
  min-height: 520px;
  background: #ffffff;
  overflow: hidden;
}

.apitable-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
  color: #646a73;
  text-align: center;
}

.apitable-state__title {
  font-size: 16px;
  line-height: 24px;
  font-weight: 500;
  color: #1f2329;
}

.apitable-state__desc {
  max-width: 520px;
  font-size: 14px;
  line-height: 22px;
}

.apitable-frame-shell {
  position: relative;
  height: 100%;
}

.apitable-frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
  background: #ffffff;
}

.apitable-load-warning {
  position: absolute;
  left: 24px;
  right: 24px;
  bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(31, 35, 41, 0.08);
}

.apitable-load-warning__title {
  font-size: 14px;
  line-height: 22px;
  font-weight: 500;
  color: #1f2329;
}

.apitable-load-warning__desc {
  margin-top: 2px;
  font-size: 13px;
  line-height: 20px;
  color: #646a73;
}
</style>
