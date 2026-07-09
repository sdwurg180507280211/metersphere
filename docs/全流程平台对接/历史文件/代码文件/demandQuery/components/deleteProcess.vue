<template>
  <el-dialog
    v-model="dialogVisible"
    title="删除流程"
    width="40%"
    :destroy-on-close="true"
    :close-on-click-modal="false"
  >
    <el-form ref="ruleFormRef" :model="form" :rules="rules">
      <el-form-item label="删除原因" prop="comment">
        <el-input v-model="form.comment" type="textarea" placeholder="请输入删除原因" :autosize="{ minRows: 2 }"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取 消</el-button>
        <el-button type="primary" @click="onSubmit" :loading="loading">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="deleteProcess">
import { deleteProcess } from '@/api/tool/gen.js'

const emit = defineEmits(['updateList'])
const { proxy } = getCurrentInstance()
const dialogVisible = ref(false)
const form = ref({
  procInsId: '',
  comment: ''
})
const rules = {
  comment: [ { required: true, message: '请输入删除原因', trigger: 'blur' } ]
}
const loading = ref(false)

function onSubmit() {
  proxy.$refs.ruleFormRef.validate(async valid => {
    if(valid) {
      loading.value = true
      try {
        await deleteProcess(form.value)
        proxy.$modal.msgSuccess("修改成功")
        handleClose()
        emit('updateList')
      }finally {
        loading.value = false
      }
    }
  })
}

function open(row) {
  form.value.procInsId = row.processInsId
  dialogVisible.value = true
}

function handleClose() {
  form.value.procInsId = ''
  form.value.comment = ''
  dialogVisible.value = false
}

defineExpose({
  open
})
</script>

<style lang="scss" scoped>

</style>