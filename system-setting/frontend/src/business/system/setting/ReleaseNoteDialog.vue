<template>
  <!-- 上线记录新增/编辑弹窗组件 -->
  <el-dialog
    :close-on-click-modal="false"
    :title="dialogTitle"
    :visible.sync="dialogVisible"
    width="680px"
    :before-close="close"
  >
    <el-form :rules="rules" ref="form" :model="form" label-width="80px" size="small">
      <!-- 标题输入框，最大100字符 -->
      <el-form-item :label="$t('announcement.release_note_title')" prop="title">
        <el-input v-model="form.title" :maxlength="100" show-word-limit :placeholder="$t('announcement.release_note_title')" />
      </el-form-item>
      <!-- 内容输入框，多行文本，最大2000字符 -->
      <el-form-item :label="$t('announcement.release_note_content')" prop="content">
        <el-input type="textarea" v-model="form.content" :rows="8" :maxlength="2000" show-word-limit :placeholder="$t('announcement.release_note_content')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button size="small" @click="close">{{ $t('commons.cancel') }}</el-button>
      <el-button size="small" type="primary" @click="save">{{ $t('commons.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: "ReleaseNoteDialog",
  data() {
    return {
      dialogVisible: false,
      isEdit: false,
      form: { id: '', title: '', content: '' },
      rules: {
        title: [{ required: true, message: () => this.$t('announcement.release_note_title'), trigger: ['change', 'blur'] }],
        content: [{ required: true, message: () => this.$t('announcement.release_note_content'), trigger: ['change', 'blur'] }]
      }
    };
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? this.$t('announcement.release_note_edit') : this.$t('announcement.release_note_add');
    }
  },
  methods: {
    open(data) {
      this.dialogVisible = true;
      if (data) {
        this.isEdit = true;
        this.$nextTick(() => {
          this.form = { id: data.id, title: data.title, content: data.content };
        });
      } else {
        this.isEdit = false;
        this.$nextTick(() => {
          this.form = { id: '', title: '', content: '' };
          if (this.$refs.form) this.$refs.form.resetFields();
        });
      }
    },
    save() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.$emit('save', Object.assign({}, this.form));
          this.close();
        }
      });
    },
    close() {
      this.dialogVisible = false;
      this.form = { id: '', title: '', content: '' };
      if (this.$refs.form) this.$refs.form.resetFields();
    }
  }
};
</script>
