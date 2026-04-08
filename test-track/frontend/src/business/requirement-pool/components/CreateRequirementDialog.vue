<template>
  <el-dialog
    :visible.sync="dialogVisible"
    title="创建需求"
    width="600px"
    :close-on-click-modal="false"
    append-to-body
    @close="close"
  >
    <el-form
      ref="createRequirementForm"
      :model="form"
      :rules="rules"
      label-width="100px"
      size="small"
    >
      <el-form-item label="需求编号" prop="dmpNum">
        <el-input
          v-model="form.dmpNum"
          maxlength="64"
          show-word-limit
          placeholder="请输入需求编号"
        />
      </el-form-item>
      <el-form-item label="需求名称" prop="requirementName">
        <el-input
          v-model="form.requirementName"
          maxlength="255"
          show-word-limit
          placeholder="请输入需求名称"
        />
      </el-form-item>
      <el-form-item label="所属系统" prop="systemName">
        <el-input
          v-model="form.systemName"
          maxlength="255"
          show-word-limit
          placeholder="请输入所属系统"
        />
      </el-form-item>
      <el-form-item label="需求负责人" prop="reqManagerName">
        <el-input
          v-model="form.reqManagerName"
          maxlength="64"
          show-word-limit
          placeholder="请输入需求负责人"
        />
      </el-form-item>
      <el-form-item label="需求父类" prop="reqFatherClass">
        <el-input
          v-model="form.reqFatherClass"
          maxlength="255"
          show-word-limit
          placeholder="请输入需求父类"
        />
      </el-form-item>
      <el-form-item label="需求子类" prop="reqSonClass">
        <el-input
          v-model="form.reqSonClass"
          maxlength="255"
          show-word-limit
          placeholder="请输入需求子类"
        />
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button size="small" @click="close">取 消</el-button>
      <el-button type="primary" size="small" :loading="saving" @click="save">保 存</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { addRequirement } from "@/api/requirement-pool";

const defaultForm = () => ({
  dmpNum: "",
  requirementName: "",
  systemName: "",
  reqManagerName: "",
  reqFatherClass: "",
  reqSonClass: "",
});

export default {
  name: "CreateRequirementDialog",
  data() {
    return {
      dialogVisible: false,
      saving: false,
      form: defaultForm(),
      rules: {
        dmpNum: [
          { required: true, message: "请输入需求编号", trigger: "blur" },
        ],
        requirementName: [
          { required: true, message: "请输入需求名称", trigger: "blur" },
        ],
      },
    };
  },
  methods: {
    open() {
      this.resetForm();
      this.dialogVisible = true;
      this.$nextTick(() => {
        if (this.$refs.createRequirementForm) {
          this.$refs.createRequirementForm.clearValidate();
        }
      });
    },
    close() {
      this.dialogVisible = false;
      this.resetForm();
    },
    resetForm() {
      this.form = defaultForm();
    },
    save() {
      this.$refs.createRequirementForm.validate((valid) => {
        if (!valid) {
          return false;
        }
        this.saving = true;
        addRequirement(this.form)
          .then(() => {
            this.saving = false;
            this.$success(this.$t("commons.save_success"));
            this.dialogVisible = false;
            this.$emit("refresh");
          })
          .catch(() => {
            this.saving = false;
          });
      });
    },
  },
};
</script>
