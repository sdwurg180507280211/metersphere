<template>
  <n-modal v-model:show="visible" preset="card" :title="t('analytics.knowledge.upload_file')" style="width: 600px" @after-leave="handleClose">
    <n-form label-placement="left" label-width="100">
      <n-form-item :label="t('analytics.knowledge.select_file')">
        <n-upload
          :default-upload="false"
          :max="1"
          :accept="'.pdf,.doc,.docx,.txt,.md'"
          :file-list="fileList"
          @update:file-list="handleFileListChange"
        >
          <n-button type="primary">{{ t('analytics.knowledge.choose_file') }}</n-button>
        </n-upload>
        <div class="upload-tip">{{ t('analytics.knowledge.upload_tip') }}</div>
      </n-form-item>

      <n-form-item :label="t('analytics.knowledge.is_public')">
        <n-switch v-model:value="isPublic" />
        <span class="form-tip">{{ t('analytics.knowledge.public_tip') }}</span>
      </n-form-item>

      <n-form-item v-if="selectedFile">
        <n-descriptions bordered :column="1" size="small">
          <n-descriptions-item :label="t('analytics.knowledge.file_name')">{{ selectedFile.name }}</n-descriptions-item>
          <n-descriptions-item :label="t('analytics.knowledge.file_size')">{{ formatFileSize(selectedFile.size) }}</n-descriptions-item>
        </n-descriptions>
      </n-form-item>
    </n-form>

    <template #footer>
      <div class="modal-footer">
        <n-button @click="handleClose">{{ t('commons.cancel') }}</n-button>
        <n-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">{{ t('commons.confirm') }}</n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NModal,
  NForm,
  NFormItem,
  NUpload,
  NButton,
  NSwitch,
  NDescriptions,
  NDescriptionsItem,
  useMessage,
  type UploadFileInfo,
} from 'naive-ui'
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
  success: []
}>()

const { t } = useI18n()
const message = useMessage()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const fileList = ref<UploadFileInfo[]>([])
const { selectedFile, isPublic, uploading, setSelectedFile, upload, reset } = useKnowledgeUpload()

const handleFileListChange = (list: UploadFileInfo[]) => {
  if (list.length > 1) {
    message.warning(t('analytics.knowledge.upload_limit'))
  }
  const nextList = list.slice(-1)
  fileList.value = nextList
  setSelectedFile(nextList[0]?.file || null)
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
  fileList.value = []
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

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
