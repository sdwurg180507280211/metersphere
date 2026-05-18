<template>
  <div class="data-dictionary">
    <el-card>
      <!-- 卡片头部 -->
      <div slot="header" class="header">
        <span>{{ $t('analytics.data_dictionary') }}</span>
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
          {{ $t('analytics.add_dictionary') }}
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="dictionaryList"
        border
        style="width: 100%"
      >
        <el-table-column prop="dictType" :label="$t('analytics.dict_type')" width="150" />
        <el-table-column prop="dictCode" :label="$t('analytics.dict_code')" width="150" />
        <el-table-column prop="dictLabel" :label="$t('analytics.dict_label')" width="150" />
        <el-table-column prop="dictValue" :label="$t('analytics.dict_value')" />
        <el-table-column prop="sortOrder" :label="$t('analytics.sort_order')" width="80" />
        <el-table-column :label="$t('analytics.status')" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? $t('analytics.enabled') : $t('analytics.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('commons.operating')" width="150" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              {{ $t('analytics.edit') }}
            </el-button>
            <el-button type="text" size="small" style="color: #F56C6C;" @click="handleDelete(row)">
              {{ $t('analytics.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="dictionaryList.length === 0" :description="$t('analytics.no_data')" />
    </el-card>
  </div>
</template>

<script>
/**
 * 数据字典页面
 * 
 * 功能：
 * 1. 展示数据字典列表
 * 2. 支持新增、编辑、删除操作
 */
export default {
  name: 'DataDictionary',
  data() {
    return {
      // 字典列表数据
      dictionaryList: []
    };
  },
  methods: {
    /**
     * 新增字典
     */
    handleAdd() {
      this.$message.info(this.$t('analytics.feature_in_development'));
    },

    /**
     * 编辑字典
     * @param {Object} row - 当前行数据
     */
    handleEdit(row) {
      console.log('编辑:', row);
      this.$message.info(this.$t('analytics.feature_in_development'));
    },

    /**
     * 删除字典
     * @param {Object} row - 当前行数据
     */
    handleDelete(row) {
      console.log('删除:', row);
      this.$message.info(this.$t('analytics.feature_in_development'));
    }
  }
};
</script>

<style scoped>
.data-dictionary {
  width: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
