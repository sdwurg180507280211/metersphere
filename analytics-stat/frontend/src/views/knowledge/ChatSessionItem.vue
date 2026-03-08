<template>
  <div
    class="session-item"
    :class="{ active }"
    @click="emit('select')"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
  >
    <!-- Icon -->
    <div class="session-icon">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
    </div>
    
    <!-- Content -->
    <div class="session-content">
      <span class="session-title" :title="session.title">{{ session.title }}</span>
      <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
    </div>
    
    <!-- Actions -->
    <div class="session-actions" :class="{ visible: isHovered || active }">
      <button 
        class="action-btn" 
        @click.stop="startRename"
        :title="t('commons.rename')"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
        </svg>
      </button>
      <button 
        class="action-btn delete" 
        @click.stop="confirmDelete"
        :title="t('commons.delete')"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
      </button>
    </div>
  </div>
  
  <!-- Rename Dialog -->
  <Teleport to="body">
    <div v-if="isRenaming" class="dialog-overlay" @click="cancelRename">
      <div class="dialog" @click.stop>
        <h3 class="dialog-title">{{ t('analytics.knowledge.rename_session') }}</h3>
        <input
          ref="renameInput"
          v-model="renameValue"
          type="text"
          class="dialog-input"
          @keydown.enter="confirmRename"
          @keydown.esc="cancelRename"
        />
        <div class="dialog-actions">
          <button class="dialog-btn secondary" @click="cancelRename">
            {{ t('commons.cancel') }}
          </button>
          <button class="dialog-btn primary" @click="confirmRename">
            {{ t('commons.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Teleport } from 'vue'
import type { ChatSession } from '@/composables/useChatSessionStore'

const props = defineProps<{
  session: ChatSession
  active: boolean
}>()

const emit = defineEmits<{
  select: []
  delete: []
  rename: [newTitle: string]
}>()

const { t } = useI18n()
const isHovered = ref(false)
const isRenaming = ref(false)
const renameValue = ref('')
const renameInput = ref<HTMLInputElement>()

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000)
  if (date.toDateString() === yesterday.toDateString()) {
    return t('analytics.knowledge.yesterday')
  }
  
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const startRename = () => {
  renameValue.value = props.session.title
  isRenaming.value = true
  nextTick(() => {
    renameInput.value?.focus()
    renameInput.value?.select()
  })
}

const confirmRename = () => {
  const newTitle = renameValue.value.trim()
  if (newTitle && newTitle !== props.session.title) {
    emit('rename', newTitle)
  }
  isRenaming.value = false
}

const cancelRename = () => {
  isRenaming.value = false
}

const confirmDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.session-item:hover {
  background: #f1f5f9;
}

.session-item.active {
  background: rgba(99, 102, 241, 0.1);
}

.session-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: white;
  color: #94a3b8;
  flex-shrink: 0;
  transition: all 0.15s;
}

.session-item:hover .session-icon {
  color: #6366f1;
}

.session-item.active .session-icon {
  background: rgba(99, 102, 241, 0.15);
  color: #6366f1;
}

.session-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.session-title {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item.active .session-title {
  color: #6366f1;
  font-weight: 600;
}

.session-time {
  font-size: 11px;
  color: #94a3b8;
}

/* Actions */
.session-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.session-actions.visible {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #94a3b8;
  padding: 0;
  transition: all 0.15s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  color: #64748b;
}

.action-btn.delete:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s;
}

.dialog {
  background: white;
  border-radius: 16px;
  padding: 20px;
  width: 90%;
  max-width: 320px;
  animation: scaleIn 0.2s;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px;
}

.dialog-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 16px;
  transition: all 0.2s;
}

.dialog-input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.dialog-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.dialog-btn.secondary {
  background: #f1f5f9;
  color: #64748b;
}

.dialog-btn.secondary:hover {
  background: #e2e8f0;
}

.dialog-btn.primary {
  background: #6366f1;
  color: white;
}

.dialog-btn.primary:hover {
  background: #4f46e5;
}
</style>
