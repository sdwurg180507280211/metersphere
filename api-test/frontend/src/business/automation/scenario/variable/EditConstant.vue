<template>
  <div>
    <el-form
      :model="editData"
      label-position="right"
      label-width="80px"
      size="small"
      ref="form"
      :rules="rules"
      :key="isActive">
      <el-form-item :label="$t('api_test.variable_name')" prop="name">
        <el-input v-model="editData.name" :placeholder="$t('api_test.variable_name')" ref="nameInput" />
      </el-form-item>

      <el-form-item :label="$t('commons.description')" prop="description">
        <el-input
          class="ms-http-textarea"
          v-model="editData.description"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 10 }"
          :rows="2"
          size="small"
          :disabled="disabled" />
      </el-form-item>

      <el-form-item :label="$t('api_test.value')" prop="value">
        <el-col class="item">
          <div class="variable-value-field" @click.stop>
            <el-input
              class="variable-value-input"
              :disabled="disabled"
              size="small"
              :placeholder="$t('api_test.value')"
              v-model="editData.value">
              <span slot="suffix" class="value-input-actions">
                <i
                  class="el-input__icon el-icon-arrow-down pointer value-func-trigger"
                  :class="{ 'is-disabled': disabled }"
                  @click.stop="toggleFuncDropdown"></i>
                <i class="el-input__icon el-icon-edit pointer" @click.stop="advanced(editData.value)"></i>
              </span>
            </el-input>
            <div v-show="funcDropdownVisible" class="api-variable-func-panel">
              <div
                v-for="func in functionOptions"
                :key="func.name"
                class="api-variable-func-option"
                @click.stop="selectFunc(func.name)">
                <span class="func-name">{{ func.name }}</span>
                <span class="func-description" :title="func.des || func.description">
                  {{ func.des || func.description }}
                </span>
              </div>
            </div>
          </div>
        </el-col>
      </el-form-item>
    </el-form>
    <ms-api-variable-advance ref="variableAdvance" :current-item.sync="editData" @advancedRefresh="reload" />
  </div>
</template>

<script>
import { JMETER_FUNC, MOCKJS_FUNC } from 'metersphere-frontend/src/utils/constants';
import MsApiVariableAdvance from 'metersphere-frontend/src/components/environment/commons/ApiVariableAdvance';

export default {
  name: 'MsEditConstant',
  components: { MsApiVariableAdvance },
  props: {
    editData: {},
  },
  data() {
    return {
      currentItem: null,
      rules: {
        name: [
          {
            required: true,
            message: this.$t('test_track.case.input_name'),
            trigger: 'blur',
          },
        ],
      },
      isActive: true,
      funcDropdownVisible: false,
    };
  },
  computed: {
    disabled() {
      return !(this.editData.name && this.editData.name !== '');
    },
    functionOptions() {
      return MOCKJS_FUNC.concat(JMETER_FUNC);
    },
  },
  methods: {
    advanced(item) {
      this.closeFuncDropdown();
      this.editData.value = item;
      this.$refs.variableAdvance.open();
    },
    toggleFuncDropdown() {
      if (this.disabled) {
        return;
      }
      this.funcDropdownVisible = !this.funcDropdownVisible;
    },
    closeFuncDropdown() {
      this.funcDropdownVisible = false;
    },
    selectFunc(value) {
      this.editData.value = value;
      this.closeFuncDropdown();
    },
    reload() {
      this.isActive = false;
      this.$nextTick(() => {
        this.isActive = true;
      });
    },
  },
  created() {
    this.$nextTick(() => {
      this.$refs.nameInput.focus();
    });
  },
  mounted() {
    document.addEventListener('click', this.closeFuncDropdown);
  },
  beforeDestroy() {
    document.removeEventListener('click', this.closeFuncDropdown);
  },
};
</script>

<style>
.variable-value-field {
  position: relative;
  width: 100%;
}

.variable-value-field .variable-value-input {
  width: 100%;
}

.variable-value-field .value-input-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 100%;
}

.variable-value-field .value-input-actions .el-input__icon {
  width: 18px;
}

.variable-value-field .value-input-actions .el-input__icon:hover {
  color: #783887;
}

.variable-value-field .value-func-trigger.is-disabled {
  cursor: not-allowed;
  color: #c0c4cc;
}

.variable-value-field .value-func-trigger.is-disabled:hover {
  color: #c0c4cc;
}

.variable-value-field .api-variable-func-panel {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  z-index: 3000;
  width: 100%;
  max-height: 240px;
  overflow-y: auto;
  padding: 4px 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}

.variable-value-field .api-variable-func-option {
  display: flex;
  align-items: center;
  height: 34px;
  padding: 0 12px;
  line-height: 34px;
  cursor: pointer;
  box-sizing: border-box;
}

.variable-value-field .api-variable-func-option:hover {
  background-color: #f5f7fa;
}

.variable-value-field .func-name {
  flex: 0 0 230px;
  display: inline-block;
  font-family: Consolas, Monaco, monospace;
  color: #303133;
}

.variable-value-field .func-description {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #909399;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
