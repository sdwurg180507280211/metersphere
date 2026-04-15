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
      <el-form-item label="当前环节" prop="actName">
        <el-input
          v-model="form.actName"
          maxlength="100"
          show-word-limit
          placeholder="请输入当前环节"
        />
      </el-form-item>
      <el-form-item label="主流程编码" prop="parentWfinstCode">
        <el-input
          v-model="form.parentWfinstCode"
          maxlength="100"
          show-word-limit
          placeholder="请输入主流程编码"
        />
      </el-form-item>
      <el-form-item label="当前处理人" prop="assigneeName">
        <el-input
          v-model="form.assigneeName"
          maxlength="64"
          show-word-limit
          placeholder="请输入当前处理人"
        />
      </el-form-item>
      <el-form-item label="需求申请部门" prop="createdDept">
        <el-input
          v-model="form.createdDept"
          maxlength="255"
          show-word-limit
          placeholder="请输入需求申请部门"
        />
      </el-form-item>
      <el-form-item label="需求申请人" prop="createUser1">
        <el-input
          v-model="form.createUser1"
          maxlength="64"
          show-word-limit
          placeholder="请输入需求申请人"
        />
      </el-form-item>
      <el-form-item label="负责人处室" prop="deptName">
        <el-input
          v-model="form.deptName"
          maxlength="255"
          show-word-limit
          placeholder="请输入需求负责人处室"
        />
      </el-form-item>
      <el-form-item label="创建人" prop="startUserName">
        <el-input
          v-model="form.startUserName"
          maxlength="64"
          show-word-limit
          placeholder="请输入创建人"
        />
      </el-form-item>
      <el-form-item label="预计上线时间" prop="upTime">
        <el-date-picker
          v-model="form.upTime"
          type="datetime"
          placeholder="选择预计上线时间"
          value-format="timestamp"
          style="width: 100%"
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
  systemName: "未指定",
  reqManagerName: "待分配",
  reqFatherClass: "功能需求",
  reqSonClass: "新增功能",
  actName: "测试待处理",
  parentWfinstCode: "-",
  assigneeName: "待分配",
  createdDept: "-",
  createUser1: "-",
  deptName: "-",
  startUserName: "-",
  upTime: null,
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
