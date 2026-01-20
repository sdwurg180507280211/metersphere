<template>
  <ms-edit-dialog
    width="600px"
    :visible.sync="visible"
    @confirm="save"
    :title="title"
    append-to-body
    v-loading="loading"
    ref="msEditDialog">

    <el-form :model="form" :rules="rules" label-position="right" size="small" ref="form">
      <el-form-item :label="$t('custom_field.field_name')" prop="name" :label-width="labelWidth">
        <el-input
          v-if="isSystem"
          :disabled="isSystem"
          :value="$t(systemNameMap[form.name])"
          autocomplete="off">
        </el-input>
        <el-input
          v-else
          v-model="form.name"
          autocomplete="off">
        </el-input>
      </el-form-item>

      <el-form-item :label="$t('custom_field.field_remark')" prop="remark" :label-width="labelWidth">
        <el-input :autosize="{ minRows: 6}" type="textarea"
                  maxlength="255" show-word-limit v-model="form.remark" />
      </el-form-item>

      <el-form-item :label="$t('custom_field.scene')" prop="type" :label-width="labelWidth">
        <el-select
          :disabled="isSystem || isTemplateEdit"
          filterable
          v-model="form.scene"
          :placeholder="$t('custom_field.scene')">
          <el-option
            v-for="item in sceneOptions"
            :key="item.value"
            :label="$t(item.text)"
            :value="item.value">
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('custom_field.field_type')" prop="type" :label-width="labelWidth">
        <el-select :disabled="isSystem" filterable v-model="form.type" :placeholder="$t('custom_field.field_type')">
          <el-option
            v-for="item in fieldTypeOptions"
            :key="item.value"
            :label="$t(item.text)"
            :value="item.value">
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item
        v-if="showOptions"
        :label="$t('custom_field.field_option')"
        prop="options" :label-width="labelWidth">
        <ms-single-handle-drag
          v-if="form.type !== 'cascadingSelect'"
          :is-kv="form.scene === 'ISSUE'"
          :data="form.options"/>
        <ms-cascader-options-editor
          v-else
          :data="form.options"/>
      </el-form-item>

    </el-form>

  </ms-edit-dialog>
</template>

<script>
import MsEditDialog from "../common/MsEditDialog";
import MsSingleHandleDrag from "../common/MsSingleHandleDrag";
import MsCascaderOptionsEditor from "../common/MsCascaderOptionsEditor";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";
import {
  CUSTOM_FIELD_SCENE_OPTION,
  CUSTOM_FIELD_TYPE_OPTION,
  SYSTEM_FIELD_NAME_MAP
} from "metersphere-frontend/src/utils/table-constants";
import i18n from "@/i18n";
import {modifyFieldTemplateByUrl} from "../../../api/template";

export default {
  name: "CustomFieldEdit",
  components: {MsSingleHandleDrag, MsCascaderOptionsEditor, MsEditDialog},
  props: {
    scene: String,
    labelWidth: {
      Object: String,
      default() {
        return '100px';
      }
    }
  },
  data() {
    return {
      form: {
        name: "",
        type: 'input',
        scene: 'TEST_CASE',
        remark: '',
        system: false,
        options: []
      },
      rules: {
        name: [
          {required: true, message: this.$t('test_track.case.input_name'), trigger: 'blur'},
          {max: 64, message: this.$t('test_track.length_less_than') + '64', trigger: 'blur'}
        ],
        scene: [{required: true, trigger: 'change'}],
        type: [{required: true, trigger: 'change'}],
      },
      visible: false,
      url: '',
      title: this.$t('custom_field.create'),
      loading: false,
    };
  },
  watch: {
    'form.type'(newType, oldType) {
      // 当字段类型改变时，如果切换到级联下拉框，确保 options 是数组格式
      if (newType === 'cascadingSelect' && (!this.form.options || !Array.isArray(this.form.options))) {
        this.form.options = [];
      }
      // 如果从级联下拉框切换到其他类型，且 options 是树形结构，需要转换
      if (oldType === 'cascadingSelect' && newType !== 'cascadingSelect') {
        // 将树形结构转换为扁平结构（如果需要）
        if (Array.isArray(this.form.options) && this.form.options.length > 0) {
          // 检查是否是树形结构
          if (this.form.options[0].children !== undefined) {
            // 保持原样，让用户重新配置
            this.form.options = [];
          }
        }
      }
    }
  },
  computed: {
    fieldTypeOptions() {
      return CUSTOM_FIELD_TYPE_OPTION;
    },
    sceneOptions() {
      return CUSTOM_FIELD_SCENE_OPTION;
    },
    showOptions() {
      return ['select', 'multipleSelect', 'radio', 'checkbox', 'cascadingSelect'].indexOf(this.form.type) > -1;
    },
    isSystem() {
      return this.form.system;
    },
    systemNameMap() {
      return SYSTEM_FIELD_NAME_MAP;
    },
    isTemplateEdit() {
      return !!this.scene;
    }
  },
  methods: {
    open(data, title) {
      this.visible = true;
      this.title = title;
      if (data) {
        Object.assign(this.form, data);
        if (!(data.options instanceof Array)) {
          this.form.options = data.options ? JSON.parse(data.options) : [];
        }
        this.url = data.id ? 'custom/field/update' : 'custom/field/add';
      } else {
        this.form = {
          name: "",
          type: 'input',
          scene: 'TEST_CASE',
          remark: '',
          system: false,
          options: []
        };
        if (this.isTemplateEdit) {
          this.form.scene = this.scene;
        }
        this.url = 'custom/field/add';
      }
    },
    save() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          let param = {};
          if (this.form.name.indexOf('.') > -1) {
            this.$error("名称不能包含'.'号");
            return;
          }
          if (this.form.name === i18n.t('custom_field.case_priority')) {
            for (let i = 0; i < this.form.options.length; i++) {
              if (this.form.options[i].text !== "P" + i) {
                this.$warning(this.$t('custom_field.case_priority_option_check_error') + i);
                return;
              }
            }
          }
          Object.assign(param, this.form);
          param.projectId = getCurrentProjectID();
          if (['select', 'multipleSelect', 'radio', 'checkbox'].indexOf(param.type) > -1) {
            if (param.options.length < 1) {
              this.$warning(this.$t('custom_field.option_check'));
              return;
            }
            for (const item of param.options) {
              if (!item.text || !item.value) {
                this.$warning(this.$t('custom_field.option_value_check'));
                return;
              }
            }
          }
          if (param.type === 'cascadingSelect') {
            if (!param.options || param.options.length < 1) {
              this.$warning(this.$t('custom_field.option_check'));
              return;
            }
            // 验证级联选项的树形结构
            for (const parent of param.options) {
              if (!parent.text || !parent.value) {
                this.$warning(this.$t('custom_field.option_value_check'));
                return;
              }
              // 验证子选项（children 是可选的）
              if (parent.children && Array.isArray(parent.children) && parent.children.length > 0) {
                for (const child of parent.children) {
                  if (!child.text || !child.value) {
                    this.$warning(this.$t('custom_field.option_value_check'));
                    return;
                  }
                }
              }
              // 清理空的 children 数组，保持数据结构简洁
              if (parent.children && Array.isArray(parent.children) && parent.children.length === 0) {
                delete parent.children;
              }
            }
          }
          param.options = JSON.stringify(this.form.options);
          this.loading = modifyFieldTemplateByUrl(this.url, param).then((response) => {
            this.visible = false;
            param.id = response.data;
            this.$success(this.$t('commons.save_success'));
            this.$emit('refresh');
            this.$emit('save', param);
          });
        }
      });
    }
  }
};
</script>

<style scoped>

</style>
