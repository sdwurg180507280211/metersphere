<template>
  <div class="condition-input">
    <!-- 文本类型 -->
    <el-input
      v-if="field.type === 'text'"
      v-model="inputValue"
      :placeholder="$t('advanced_search.enter_value')"
      clearable
      @change="handleChange"
    />
    
    <!-- 单选下拉 -->
    <el-select
      v-else-if="field.type === 'select' && !field.multiple"
      v-model="inputValue"
      :placeholder="$t('advanced_search.select_value')"
      clearable
      @change="handleChange"
    >
      <el-option
        v-for="option in field.options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
    
    <!-- 多选下拉 -->
    <el-select
      v-else-if="field.type === 'select' && field.multiple"
      v-model="inputValue"
      multiple
      collapse-tags
      :placeholder="$t('advanced_search.select_value')"
      clearable
      @change="handleChange"
    >
      <el-option
        v-for="option in field.options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
    
    <!-- 用户选择器 -->
    <user-selector
      v-else-if="field.type === 'user'"
      v-model="inputValue"
      :workspace-ids="workspaceIds"
      :max-selection="field.maxSelection || 10"
      :show-current-user="true"
      @change="handleChange"
    />
    
    <!-- 日期选择器 -->
    <el-date-picker
      v-else-if="field.type === 'date' && operator === 'between'"
      v-model="inputValue"
      type="daterange"
      :range-separator="$t('advanced_search.to')"
      :start-placeholder="$t('advanced_search.start_date')"
      :end-placeholder="$t('advanced_search.end_date')"
      value-format="timestamp"
      @change="handleChange"
    />
    
    <el-date-picker
      v-else-if="field.type === 'date'"
      v-model="inputValue"
      type="date"
      :placeholder="$t('advanced_search.select_date')"
      value-format="timestamp"
      @change="handleChange"
    />
    
    <!-- 树形选择器（用于模块选择） -->
    <el-tree-select
      v-else-if="field.type === 'treeSelect'"
      v-model="inputValue"
      :data="field.options"
      :placeholder="$t('advanced_search.select_module')"
      clearable
      @change="handleChange"
    />
    
    <!-- 数字输入 -->
    <el-input-number
      v-else-if="field.type === 'number'"
      v-model="inputValue"
      :placeholder="$t('advanced_search.enter_value')"
      @change="handleChange"
    />
    
    <!-- 默认文本输入 -->
    <el-input
      v-else
      v-model="inputValue"
      :placeholder="$t('advanced_search.enter_value')"
      clearable
      @change="handleChange"
    />
  </div>
</template>

<script>
import UserSelector from './UserSelector.vue';

export default {
  name: 'ConditionInput',
  
  components: {
    UserSelector
  },
  
  props: {
    // 字段元数据
    field: {
      type: Object,
      required: true
    },
    
    // 操作符
    operator: {
      type: String,
      required: true
    },
    
    // v-model 绑定的值
    modelValue: {
      type: [String, Number, Array],
      default: ''
    },
    
    // 工作空间ID列表（用于用户选择器）
    workspaceIds: {
      type: Array,
      default: () => []
    }
  },
  
  emits: ['update:modelValue', 'change'],
  
  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },
  
  methods: {
    handleChange(value) {
      this.$emit('change', value);
    }
  }
};
</script>

<style scoped>
.condition-input {
  width: 100%;
}
</style>
