<template>
  <div class="analytics-stat-home">
    <!-- 统计卡片行 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <query-count-card :count="queryCount" :trend="queryTrend" />
      </el-col>
      <el-col :span="12">
        <data-volume-card :volume="dataVolume" :unit="dataUnit" />
      </el-col>
    </el-row>

    <!-- 快捷入口行 -->
    <el-row style="margin-top: 16px">
      <el-col>
        <quick-access-card :items="quickAccessItems" />
      </el-col>
    </el-row>

    <!-- 最近查询列表行 -->
    <el-row style="margin-top: 16px">
      <el-col>
        <recent-query-list :queries="recentQueries" />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import QueryCountCard from "./components/QueryCountCard";
import DataVolumeCard from "./components/DataVolumeCard";
import QuickAccessCard from "./components/QuickAccessCard";
import RecentQueryList from "./components/RecentQueryList";

/**
 * 分析统计模块工作台首页
 * 
 * 功能：
 * 1. 展示查询次数统计
 * 2. 展示数据量统计
 * 3. 提供快捷入口
 * 4. 展示最近查询列表
 * 
 * 布局说明：
 * - 采用左右布局，左侧菜单由父组件 AnalyticsStat.vue 提供
 * - 本组件只负责右侧内容区域的展示
 * - 移除了 ms-container 嵌套，由父组件统一管理
 */
export default {
  name: "AnalyticsStatHome",

  components: {
    QueryCountCard,
    DataVolumeCard,
    QuickAccessCard,
    RecentQueryList
  },

  data() {
    return {
      // 查询次数统计
      queryCount: 0,
      queryTrend: 'stable', // 'up' | 'down' | 'stable'
      
      // 数据量统计
      dataVolume: 0,
      dataUnit: 'MB', // 'MB' | 'GB' | 'TB'
      
      // 最近查询列表
      recentQueries: [],
      
      // 加载状态
      loading: false,
      
      // 快捷入口配置（使用 i18n key，支持多语言切换）
      quickAccessItems: [
        {
          i18nKey: 'analytics.menu.sql_console',
          icon: 'el-icon-document',
          path: '/analytics/sql-console',
          descKey: 'analytics.sql_console_desc'
        },
        {
          i18nKey: 'analytics.menu.data_dictionary',
          icon: 'el-icon-collection',
          path: '/analytics/data-dictionary',
          descKey: 'analytics.data_dictionary_desc'
        },
        {
          i18nKey: 'analytics.comprehensive_query',
          icon: 'el-icon-search',
          path: '/analytics/query',
          descKey: 'analytics.comprehensive_query_desc'
        }
      ]
    };
  },

  mounted() {
    this.initData();
  },

  activated() {
    // 子应用激活时刷新数据
    this.refreshStats();
  },

  methods: {
    /**
     * 初始化数据
     */
    initData() {
      this.loading = true;
      Promise.all([
        this.loadQueryCount(),
        this.loadDataVolume(),
        this.loadRecentQueries()
      ]).finally(() => {
        this.loading = false;
      });
    },

    /**
     * 刷新统计数据
     */
    refreshStats() {
      this.loadQueryCount();
      this.loadDataVolume();
      this.loadRecentQueries();
    },

    /**
     * 加载查询次数统计
     */
    async loadQueryCount() {
      try {
        // TODO: 调用后端 API
        // const res = await getQueryCount();
        // this.queryCount = res.data.total;
        // this.queryTrend = res.data.trend;
        
        // Mock 数据
        this.queryCount = 1234;
        this.queryTrend = 'up';
      } catch (error) {
        this.$message.error(this.$t('analytics.load_query_count_failed'));
        console.error(error);
      }
    },

    /**
     * 加载数据量统计
     */
    async loadDataVolume() {
      try {
        // TODO: 调用后端 API
        // const res = await getDataVolume();
        // this.dataVolume = res.data.volume;
        // this.dataUnit = res.data.unit;
        
        // Mock 数据
        this.dataVolume = 1024;
        this.dataUnit = 'MB';
      } catch (error) {
        this.$message.error(this.$t('analytics.load_data_volume_failed'));
        console.error(error);
      }
    },

    /**
     * 加载最近查询列表
     */
    async loadRecentQueries() {
      try {
        // TODO: 调用后端 API
        // const res = await getRecentQueries({ limit: 10 });
        // this.recentQueries = res.data;
        
        // Mock 数据
        this.recentQueries = [
          {
            id: '1',
            name: this.$t('analytics.mock_query_user_stat'),
            type: 'sql',
            createTime: new Date(),
            status: 'success'
          },
          {
            id: '2',
            name: this.$t('analytics.mock_query_project_data'),
            type: 'query',
            createTime: new Date(),
            status: 'success'
          }
        ];
      } catch (error) {
        this.$message.error(this.$t('analytics.load_recent_queries_failed'));
        console.error(error);
      }
    }
  }
};
</script>

<style scoped>
.analytics-stat-home {
  padding: 20px;
  background-color: #f5f6f7;
  min-height: calc(100vh - 50px);
}

/* 卡片样式 */
:deep(.el-card__header) {
  border: 0px;
  padding: 24px;
}

:deep(.el-card__body) {
  border: 0px;
  padding: 0px;
  margin: 0px 24px 24px 24px;
}

:deep(.el-card) {
  border: 0;
}

/* 统计卡片样式 */
:deep(.dashboard-card) {
  height: 208px;
}

:deep(.main-info-card) {
  height: 208px;
  width: 100%;
  color: #646a73;
  background-color: #ffffff;
  box-sizing: border-box;
  border: 1px solid #dee0e3;
  border-radius: 4px;
}

:deep(.dashboard-title) {
  font-size: 18px;
  font-weight: 500;
  color: #1f2329;
}

/* 表格样式 */
:deep(.home-table-cell) {
  height: 40px;
  background-color: #f5f6f7;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid rgba(31, 35, 41, 0.15);
  border-right-width: 0;
  border-left-width: 0;
  color: #646a73;
  line-height: 22px;
}

:deep(.table-title) {
  color: #1f2329;
  font-weight: 500;
  font-size: 18px;
  line-height: 26px;
}

:deep(.el-table__row) {
  height: 40px;
  font-size: 14px;
  font-weight: 400;
  line-height: 22px;
  color: #1f2329;
}

:deep(.el-table__body tr:hover) {
  cursor: pointer;
}

:deep(.el-table .cell) {
  padding-left: 12px;
  padding-right: 12px;
}
</style>
