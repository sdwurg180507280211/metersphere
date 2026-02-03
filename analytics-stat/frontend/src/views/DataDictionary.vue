<template>
  <div class="data-dictionary">
    <el-card>
      <!-- 卡片头部 -->
      <div slot="header" class="header">
        <span>数据字典</span>
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
          新增字典
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="dictionaryList"
        border
        style="width: 100%"
      >
        <el-table-column prop="dictType" label="字典类型" width="150" />
        <el-table-column prop="dictCode" label="字典编码" width="150" />
        <el-table-column prop="dictLabel" label="字典标签" width="150" />
        <el-table-column prop="dictValue" label="字典值" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="text" size="small" style="color: #F56C6C;" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="dictionaryList.length === 0" description="暂无数据" />
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
      this.$message.info('新增功能开发中...');
    },

    /**
     * 编辑字典
     * @param {Object} row - 当前行数据
     */
    handleEdit(row) {
      console.log('编辑:', row);
      this.$message.info('编辑功能开发中...');
    },

    /**
     * 删除字典
     * @param {Object} row - 当前行数据
     */
    handleDelete(row) {
      console.log('删除:', row);
      this.$message.info('删除功能开发中...');
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
