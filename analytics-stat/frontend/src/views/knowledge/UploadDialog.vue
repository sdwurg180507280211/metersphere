<template>
  <el-dialog
    v-model="visible"
    :title="t('analytics.knowledge.upload_file')"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" label-width="100px">
      <el-form-item :label="t('analytics.knowledge.select_file')">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
          accept=".pdf,.doc,.docx,.txt,.md"
        >
          <template #trigger>
            <el-button type="primary">{{ t('analytics.knowledge.choose_file') }}</el-button>
          </template>
          <template #tip>
            <div class="el-upload__tip">
              {{ t('analytics.knowledge.upload_tip') }}
            </div>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item :label="t('analytics.knowledge.is_public')">
        <el-switch v-model="form.isPublic" />
        <span class="form-tip">{{ t('analytics.knowledge.public_tip') }}</span>
      </el-form-item>

      <el-form-item v-if="selectedFile">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item :label="t('analytics.knowledge.file_name')">
            {{ selectedFile.name }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('analytics.knowledge.file_size')">
            {{ formatFileSize(selectedFile.size) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">{{ t('commons.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="uploading"
        :disabled="!selectedFile"
        @click="handleUpload"
      >
        {{ t('commons.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import { uploadFile } from '@/api/knowledge'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)

const form = ref({
  isPublic: false
})

const handleFileChange = (file: UploadFile) => {
  selectedFile.value = file.raw || null
}

const handleExceed = () => {
  ElMessage.warning(t('analytics.knowledge.upload_limit'))
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning(t('analytics.knowledge.select_file_first'))
    return
  }

  uploading.value = true
  try {
    await uploadFile(selectedFile.value, form.value.isPublic)
    ElMessage.success(t('analytics.knowledge.upload_success'))
    emit('success')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.message || t('analytics.knowledge.upload_failed'))
  } finally {
    uploading.value = false
  }
}

const handleClose = () => {
  selectedFile.value = null
  form.value.isPublic = false
  uploadRef.value?.clearFiles()
  visible.value = false
}
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
