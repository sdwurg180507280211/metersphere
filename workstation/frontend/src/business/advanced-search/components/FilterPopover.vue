<template>
  <div class="filter-popover">
    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        :placeholder="$t('advanced_search.search_field')"
        prefix-icon="el-icon-search"
        size="small"
        clearable
      />
    </div>
    
    <div class="field-groups">
      <div 
        v-for="group in filteredGroups" 
        :key="group.key"
        class="field-group"
      >
        <div class="group-title">{{ $t(group.label) }}</div>
        <div class="field-list">
          <div
            v-for="field in group.fields"
            :key="field.field"
            class="field-item"
            @click="handleFieldClick(field)"
          >
            <span>{{ field.label }}</span>
            <i class="el-icon-plus"></i>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FilterPopover',
  props: {
    fields: {
      type: Array,
      default: () => []
    }
  },
  emits: ['add-condition'],
  data() {
    return {
      searchKeyword: '',
      fieldGroups: [
        { key: 'basic', label: 'advanced_search.basic_info' },
        { key: 'module', label: 'advanced_search.module_specific' },
        { key: 'audit', label: 'advanced_search.audit_trail' },
        { key: 'custom', label: 'advanced_search.custom_fields' }
      ]
    };
  },
  computed: {
    filteredGroups() {
      const groups = this.fieldGroups.map(group => {
        const fields = this.fields.filter(f => {
          const matchGroup = f.group === group.key;
          const matchKeyword = !this.searchKeyword || 
            f.label.toLowerCase().includes(this.searchKeyword.toLowerCase());
          return matchGroup && matchKeyword;
        });
        
        return {
          ...group,
          fields
        };
      }).filter(group => group.fields.length > 0);
      
      return groups;
    }
  },
  methods: {
    handleFieldClick(field) {
      // 根据字段类型设置默认操作符和值
      let operator = '=';
      let value = '';
      
      switch (field.type) {
        case 'text':
          operator = 'like';
          break;
        case 'select':
        case 'user':
          operator = 'in';
          value = [];
          break;
        case 'date':
          operator = 'between';
          value = [];
          break;
      }
      
      this.$emit('add-condition', field.field, operator, value);
    }
  }
};
</script>

<style scoped>
.filter-popover {
  max-height: 400px;
  overflow-y: auto;
}

.search-box {
  padding: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.field-groups {
  padding: 8px 0;
}

.field-group {
  margin-bottom: 16px;
}

.group-title {
  padding: 8px 12px;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.field-list {
  padding: 0 12px;
}

.field-item {
  padding: 8px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 4px;
  transition: all 0.3s;
}

.field-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.field-item i {
  opacity: 0;
  transition: opacity 0.3s;
}

.field-item:hover i {
  opacity: 1;
}
</style>
