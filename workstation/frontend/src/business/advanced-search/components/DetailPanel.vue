<template>
  <div class="detail-panel">
    <div class="detail-header">
      <h3>{{ item.name || item.title }}</h3>
      <div class="header-actions">
        <el-button size="small" icon="el-icon-edit">
          {{ $t('commons.edit') }}
        </el-button>
        <el-button size="small" icon="el-icon-share">
          {{ $t('commons.share') }}
        </el-button>
      </div>
    </div>
    
    <div class="detail-content">
      <!-- 基本信息 -->
      <div class="info-section">
        <div class="section-title">{{ $t('advanced_search.basic_info') }}</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">{{ $t('advanced_search.id') }}:</span>
            <span class="value">{{ item.num }}</span>
          </div>
          <div class="info-item">
            <span class="label">{{ $t('advanced_search.status') }}:</span>
            <el-tag :type="getStatusType(item.status)">{{ item.status }}</el-tag>
          </div>
          <div class="info-item">
            <span class="label">{{ $t('advanced_search.creator') }}:</span>
            <span class="value">{{ item.createUser }}</span>
          </div>
          <div class="info-item">
            <span class="label">{{ $t('advanced_search.create_time') }}:</span>
            <span class="value">{{ formatDate(item.createTime) }}</span>
          </div>
        </div>
      </div>
      
      <!-- 详细描述 -->
      <div class="info-section">
        <div class="section-title">{{ $t('advanced_search.description') }}</div>
        <div class="description-content" v-html="item.description || $t('commons.none')"></div>
      </div>
      
      <!-- 自定义字段 -->
      <div v-if="item.customFields && item.customFields.length > 0" class="info-section">
        <div class="section-title">{{ $t('advanced_search.custom_fields') }}</div>
        <div class="info-grid">
          <div 
            v-for="field in item.customFields"
            :key="field.name"
            class="info-item"
          >
            <span class="label">{{ field.name }}:</span>
            <span class="value">{{ field.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DetailPanel',
  props: {
    item: {
      type: Object,
      required: true
    }
  },
  methods: {
    getStatusType(status) {
      const statusMap = {
        'Pass': 'success',
        'Failure': 'danger',
        'Prepare': 'info',
        'Underway': 'warning'
      };
      return statusMap[status] || 'info';
    },
    
    formatDate(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      return date.toLocaleString();
    }
  }
};
</script>

<style scoped>
.detail-panel {
  padding: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 24px;
}

.detail-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-section {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item .label {
  color: #909399;
  margin-right: 8px;
  min-width: 80px;
}

.info-item .value {
  color: #606266;
}

.description-content {
  color: #606266;
  line-height: 1.6;
}
</style>
