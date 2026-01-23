<template>
  <div class="active-tags-bar">
    <div class="tags-container">
      <el-tag
        v-for="(condition, field) in conditions"
        :key="field"
        closable
        @close="handleRemove(field)"
      >
        {{ getFieldLabel(field) }}: {{ formatValue(condition) }}
      </el-tag>
    </div>
    
    <el-button 
      v-if="Object.keys(conditions).length > 0"
      type="text" 
      size="small"
      @click="handleClearAll"
    >
      {{ $t('advanced_search.clear_all') }}
    </el-button>
  </div>
</template>

<script>
import { useAdvancedSearchStore } from '@/store';

export default {
  name: 'ActiveTagsBar',
  emits: ['remove-condition', 'clear-all'],
  setup() {
    const store = useAdvancedSearchStore();
    return { store };
  },
  computed: {
    conditions() {
      return this.store.combine;
    },
    fieldMetadata() {
      return this.store.fieldMetadata;
    }
  },
  methods: {
    getFieldLabel(field) {
      const allFields = [
        ...this.fieldMetadata.systemFields,
        ...this.fieldMetadata.customFields
      ];
      const fieldMeta = allFields.find(f => f.field === field);
      return fieldMeta ? fieldMeta.label : field;
    },
    
    formatValue(condition) {
      const { operator, value } = condition;
      
      if (Array.isArray(value)) {
        if (operator === 'between') {
          return `${value[0]} ~ ${value[1]}`;
        }
        return value.join(', ');
      }
      
      return value;
    },
    
    handleRemove(field) {
      this.$emit('remove-condition', field);
    },
    
    handleClearAll() {
      this.$emit('clear-all');
    }
  }
};
</script>

<style scoped>
.active-tags-bar {
  background-color: #fff;
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.el-tag {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
