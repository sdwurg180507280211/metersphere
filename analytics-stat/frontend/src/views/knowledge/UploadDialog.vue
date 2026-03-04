<template>
  <n-modal
    v-model:show="visible"
    preset="dialog"
    :title="t('analytics.knowledge.upload_file')"
    style="width: 600px"
    @after-leave="handleClose"
  >
    <n-form label-placement="left" label-width="100">
      <n-form-item :label="t('analytics.knowledge.select_file')">
        <n-upload
          ref="uploadRef"
          :max="1"
          :default-upload="false"
          @change="handleFileChange"
          accept=".pdf,.doc,.docx,.txt,.md"
        >
          <n-button type="primary">{{ t('analytics.knowledge.choose_file') }}</n-button>
        </n-upload>
      </n-form-item>
      <n-form-item label="">
        <div class="upload-tip">
          {{ t('analytics.knowledge.upload_tip') }}
        </div>
      </n-form-item>

      <n-form-item :label="t('analytics.knowledge.is_public')">
        <n-switch v-model:value="isPublic" />
        <span class="form-tip">{{ t('analytics.knowledge.public_tip') }}</span>
      </n-form-item>

      <n-form-item v-if="selectedFile" label="">
        <div class="file-info">
          <div class="info-row">
            <span class="info-label">{{ t('analytics.knowledge.file_name') }}</span>
            <span class="info-value">{{ selectedFile.name }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('analytics.knowledge.file_size') }}</span>
            <span class="info-value">{{ formatFileSize(selectedFile.size) }}</span>
          </div>
        </div>
      </n-form-item>
    </n-form>

    <template #action>
      <n-button @click="handleClose">{{ t('commons.cancel') }}</n-button>
      <n-button
        type="primary"
        :loading="uploading"
        :disabled="!selectedFile"
        @click="handleUpload"
      >
        {{ t('commons.confirm') }}
      </n-button>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NForm, NFormItem, NUpload, NButton, NSwitch, useMessage, type UploadFileInfo } from 'naive-ui'
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
const message = useMessage()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const uploadRef = ref()
const { selectedFile, isPublic, uploading, setSelectedFile, upload, reset } = useKnowledgeUpload()

const handleFileChange = (options: { fileList: UploadFileInfo[] }) => {
  const file = options.fileList[0]?.file
  setSelectedFile(file || null)
}

const handleUpload = async () => {
  try {
    await upload()
    message.success(t('analytics.knowledge.upload_success'))
    emit('success')
    handleClose()
  } catch (error: any) {
    if (error?.message === EMPTY_UPLOAD_FILE_ERROR) {
      message.warning(t('analytics.knowledge.select_file_first'))
      return
    }
    message.error(resolveKnowledgeErrorMessage(error, t, 'analytics.knowledge.upload_failed'))
  }
}

const handleClose = () => {
  reset()
  uploadRef.value?.clear()
  visible.value = false
}
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.file-info {
  width: 100%;
  border: 1px solid #e5e5e5;
  border-radius: 3px;
  padding: 12px;
}

.info-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  width: 100px;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.info-value {
  flex: 1;
  font-size: 13px;
  color: #303133;
}
</style>
