<template>
  <el-dialog
    v-model="visible"
    :title="t('analytics.knowledge.upload_file')"
    width="600px"
    @close="handleClose"
  >
    <el-form label-width="100px">
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
        <el-switch v-model="isPublic" />
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
import {
  EMPTY_UPLOAD_FILE_ERROR,
  formatFileSize,
  useKnowledgeUpload,
} from '@/composables/useKnowledgeUpload'
import { resolveKnowledgeErrorMessage } from '@/composables/useKnowledgeErrorMessage'

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
const { selectedFile, isPublic, uploading, setSelectedFile, upload, reset } = useKnowledgeUpload()

const handleFileChange = (file: UploadFile) => {
  setSelectedFile(file.raw || null)
}

const handleExceed = () => {
  ElMessage.warning(t('analytics.knowledge.upload_limit'))
}

const handleUpload = async () => {
  try {
    await upload()
    ElMessage.success(t('analytics.knowledge.upload_success'))
    emit('success')
    handleClose()
  } catch (error: any) {
    if (error?.message === EMPTY_UPLOAD_FILE_ERROR) {
      ElMessage.warning(t('analytics.knowledge.select_file_first'))
      return
    }
    ElMessage.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.upload_failed'))
  }
}

const handleClose = () => {
  reset()
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
