<template>
  <el-radio-group v-model="mode" size="small" @change="handleChange">
    <el-radio-button label="visual">
      <i class="el-icon-s-operation"></i>
      {{ $t('advanced_search.visual_mode') }}
    </el-radio-button>
    <el-radio-button label="jql">
      <i class="el-icon-edit"></i>
      {{ $t('advanced_search.jql_mode') }}
    </el-radio-button>
  </el-radio-group>
</template>

<script>
export default {
  name: 'QueryModeSwitch',
  props: {
    modelValue: {
      type: String,
      default: 'visual'
    }
  },
  emits: ['update:modelValue', 'change'],
  data() {
    return {
      mode: this.modelValue
    };
  },
  watch: {
    modelValue(newVal) {
      this.mode = newVal;
    }
  },
  methods: {
    handleChange(value) {
      this.$emit('update:modelValue', value);
      this.$emit('change', value);
    }
  }
};
</script>

<style scoped>
.el-radio-button >>> .el-radio-button__inner {
  padding: 9px 15px;
}
</style>
