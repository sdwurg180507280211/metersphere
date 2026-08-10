<template>
  <div>
    <el-form :model="form" :rules="rules" ref="editPasswordForm" label-width="120px" class="demo-ruleForm" size="small">
      <el-form-item :label="$t('member.old_password')" prop="password" style="margin-bottom: 29px">
        <el-input v-model="form.password" autocomplete="off" show-password/>
      </el-form-item>
      <el-form-item :label="$t('member.new_password')" prop="newpassword">
        <el-input v-model="form.newpassword" autocomplete="off" show-password/>
      </el-form-item>
      <el-form-item :label="$t('member.repeat_password')" prop="repeatPassword">
        <el-input v-model="form.repeatPassword" autocomplete="off" show-password/>
      </el-form-item>
      <el-form-item>
        <el-button v-if="!forceChange" @click="cancel">{{ $t('commons.cancel') }}</el-button>
        <el-button type="primary" @click="updatePassword('editPasswordForm')" @keydown.enter.native.prevent>{{ $t('commons.confirm') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script>

import {useUserStore} from "@/store";
import {updatePassword} from "../../api/user";

export default {
  name: 'PasswordInfo',
  data() {
    return {
      form: {
        password: this.ruleForm.password || '',
        id: this.ruleForm.id || '',
        newpassword: '',
        repeatPassword: ''
      },
      result: {},
      updatePasswordPath: '/user/update/password',
      rules: {
        password: [
          {required: true, message: this.$t('user.input_password'), trigger: 'blur'},
        ],
        newpassword: [
          {required: true, message: this.$t('user.input_password'), trigger: 'blur'},
          {
            required: true,
            pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[^a-zA-Z0-9\s]).{8,65}$/,
            message: this.$t('member.password_format_is_incorrect'),
            trigger: 'blur'
          },
        ],
        repeatPassword: [
          {required: true, message: this.$t('user.input_password'), trigger: 'blur'},
          {
            required: true,
            pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[^a-zA-Z0-9\s]).{8,65}$/,
            message: this.$t('member.password_format_is_incorrect'),
            trigger: 'blur'
          },
        ]
      }
    }
  },
  props: {
    ruleForm: {
      type: Object,
      default: () => ({})
    },
    forceChange: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    cancel() {
      this.$emit("cancel");
    },
    confirm() {
      this.$emit("confirm");
    },
    updatePassword(editPasswordForm) {
      this.$refs[editPasswordForm].validate(valid => {
        if (valid) {
          if (this.form.newpassword !== this.form.repeatPassword) {
            this.$warning(this.$t('member.inconsistent_passwords'));
            return;
          }
          this.result = updatePassword(this.form)
            .then(response => {
              if (!response.data) {
                this.$error(this.$t('commons.personal_password_info'));
              } else {
                this.$success(this.$t('commons.modify_success'));
                if (this.forceChange) {
                  this.$emit("forceChangeSuccess");
                } else {
                  useUserStore().userLogout();
                }
              }
            });
        } else {
          return false;
        }
      });
    },
  }
}
</script>
<style scoped>

</style>
