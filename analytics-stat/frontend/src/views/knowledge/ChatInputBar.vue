<template>
  <div class="chat-input-bar">
    <!-- Quick Actions -->
    <div v-if="!props.loading && !draft.trim()" class="quick-actions">
      <button 
        v-for="action in quickActions" 
        :key="action.key"
        class="quick-action-btn"
        @click="applyQuickAction(action.text)"
      >
        <component :is="action.icon" class="action-icon" />
        <span>{{ action.label }}</span>
      </button>
    </div>

    <div class="input-container">
      <!-- Mode Switcher -->
      <div class="mode-switcher" :class="{ expanded: showModeMenu }">
        <button class="mode-btn" @click="showModeMenu = !showModeMenu">
          <svg v-if="chatMode === 'knowledge'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
            <circle cx="12" cy="12" r="10" />
            <line x1="2" y1="12" x2="22" y2="12" />
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
          </svg>
          <span class="mode-label">{{ chatMode === 'knowledge' ? t('analytics.knowledge.mode_knowledge') : t('analytics.knowledge.mode_normal') }}</span>
          <svg class="mode-arrow" :class="{ rotated: showModeMenu }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>
        
        <div v-if="showModeMenu" class="mode-menu">
          <button 
            class="mode-option" 
            :class="{ active: chatMode === 'knowledge' }"
            @click="switchMode('knowledge')"
          >
            <div class="option-icon knowledge">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <div class="option-content">
              <span class="option-title">{{ t('analytics.knowledge.mode_knowledge') }}</span>
              <span class="option-desc">{{ t('analytics.knowledge.mode_knowledge_desc') }}</span>
            </div>
            <svg v-if="chatMode === 'knowledge'" class="check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </button>
          <button 
            class="mode-option" 
            :class="{ active: chatMode === 'normal' }"
            @click="switchMode('normal')"
          >
            <div class="option-icon normal">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                <circle cx="12" cy="12" r="10" />
                <line x1="2" y1="12" x2="22" y2="12" />
                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
              </svg>
            </div>
            <div class="option-content">
              <span class="option-title">{{ t('analytics.knowledge.mode_normal') }}</span>
              <span class="option-desc">{{ t('analytics.knowledge.mode_normal_desc') }}</span>
            </div>
            <svg v-if="chatMode === 'normal'" class="check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Textarea -->
      <n-input
        ref="inputRef"
        v-model:value="draft"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 8 }"
        :placeholder="currentPlaceholder"
        class="chat-textarea"
        @keydown.enter.exact.prevent="submit"
        @keydown.enter.ctrl.exact.prevent="submit"
        @keydown.enter.meta.exact.prevent="submit"
        @keydown.enter.shift.exact="insertNewLine"
      />

      <!-- Send/Stop Button -->
      <div class="send-actions">
        <button
          v-if="!props.loading"
          class="send-btn"
          :class="{ disabled: !draft.trim() || props.disabled }"
          :disabled="!draft.trim() || props.disabled"
          @click="submit"
        >
          <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <button v-else class="stop-btn" @click="emit('stop')">
          <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
            <rect x="6" y="6" width="12" height="12" rx="2" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Footer Info -->
    <div class="input-footer">
      <div class="footer-left">
        <span class="hint">
          <kbd>Enter</kbd> / <kbd>Ctrl</kbd> + <kbd>Enter</kbd> {{ t('analytics.knowledge.send') }} · <kbd>Shift</kbd> + <kbd>Enter</kbd> {{ t('analytics.knowledge.new_line') }}
        </span>
      </div>
      <div class="footer-right">
        <span class="disclaimer">{{ t('analytics.knowledge.ai_disclaimer') }}</span>
      </div>
    </div>

    <!-- Click outside to close mode menu -->
    <div v-if="showModeMenu" class="mode-menu-overlay" @click="showModeMenu = false"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput } from 'naive-ui'

const props = defineProps<{
  loading: boolean
  chatMode: 'knowledge' | 'normal'
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [{ question: string; topK: number }]
  stop: []
  'update:chatMode': ['knowledge' | 'normal']
}>()

const { t } = useI18n()
const inputRef = ref<InstanceType<typeof NInput> | null>(null)
const draft = ref('')
const topK = ref(5)
const showModeMenu = ref(false)

// Quick actions
const LightbulbIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 14, height: 14 }, [
  h('path', { d: 'M9 18h6M10 22h4M12 2a7 7 0 0 0-7 7c0 2.5 1.5 4.5 3 6v2h8v-2c1.5-1.5 3-3.5 3-6a7 7 0 0 0-7-7z' })
])

const FileTextIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 14, height: 14 }, [
  h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
  h('polyline', { points: '14 2 14 8 20 8' }),
  h('line', { x1: 16, y1: 13, x2: 8, y2: 13 }),
  h('line', { x1: 16, y1: 17, x2: 8, y2: 17 }),
  h('polyline', { points: '10 9 9 9 8 9' })
])

const ZapIcon = () => h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, width: 14, height: 14 }, [
  h('polygon', { points: '13 2 3 14 12 14 11 22 21 10 12 10 13 2' })
])

const quickActions = [
  { key: 'explain', label: t('analytics.knowledge.quick_explain'), text: t('analytics.knowledge.quick_explain_text'), icon: LightbulbIcon },
  { key: 'summarize', label: t('analytics.knowledge.quick_summarize'), text: t('analytics.knowledge.quick_summarize_text'), icon: FileTextIcon },
  { key: 'analyze', label: t('analytics.knowledge.quick_analyze'), text: t('analytics.knowledge.quick_analyze_text'), icon: ZapIcon },
]

const currentPlaceholder = computed(() => {
  return props.chatMode === 'knowledge' 
    ? t('analytics.knowledge.chat_input_placeholder_knowledge')
    : t('analytics.knowledge.chat_input_placeholder_normal')
})

const submit = () => {
  const question = draft.value.trim()
  if (!question || props.loading || props.disabled) return
  emit('send', { question, topK: topK.value })
  draft.value = ''
}

const insertNewLine = () => {
  draft.value += '\n'
}

const switchMode = (mode: 'knowledge' | 'normal') => {
  emit('update:chatMode', mode)
  showModeMenu.value = false
}

const applyQuickAction = (text: string) => {
  draft.value = text
  inputRef.value?.focus()
}

const focus = () => {
  inputRef.value?.focus?.()
}

const restoreDraft = (value: string) => {
  draft.value = value
  focus()
}

defineExpose({ focus, restoreDraft })
</script>

<style scoped>
.chat-input-bar {
  padding: 20px 32px 24px;
  background: white;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
}

/* Quick Actions */
.quick-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.quick-action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-action-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #475569;
  transform: translateY(-1px);
}

.action-icon {
  flex-shrink: 0;
}

/* Input Container */
.input-container {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  max-width: 100%;
  width: 100%;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 10px 12px;
  transition: all 0.2s;
  position: relative;
}

.input-container:focus-within {
  background: white;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

/* Mode Switcher */
.mode-switcher {
  position: relative;
}

.mode-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
  flex-shrink: 0;
}

.mode-btn:hover {
  border-color: #cbd5e1;
  color: #475569;
}

.mode-label {
  font-size: 13px;
  font-weight: 500;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mode-arrow {
  transition: transform 0.2s;
}

.mode-arrow.rotated {
  transform: rotate(180deg);
}

.mode-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
  min-width: 240px;
  z-index: 100;
  animation: popUp 0.2s ease-out;
}

@keyframes popUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.mode-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  border: none;
  background: transparent;
  width: 100%;
  text-align: left;
  transition: background 0.15s;
}

.mode-option:hover {
  background: #f8fafc;
}

.mode-option.active {
  background: rgba(99, 102, 241, 0.08);
}

.option-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  flex-shrink: 0;
}

.option-icon.knowledge {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.option-icon.normal {
  background: rgba(14, 165, 233, 0.1);
  color: #0ea5e9;
}

.option-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.option-desc {
  font-size: 11px;
  color: #94a3b8;
}

.check-icon {
  color: #6366f1;
  flex-shrink: 0;
}

.mode-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 99;
}

/* Textarea */
.chat-textarea {
  flex: 1;
  min-width: 0;
}

.chat-textarea :deep(.n-input__textarea-el) {
  border: none !important;
  box-shadow: none !important;
  padding: 8px 4px;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  background: transparent;
  min-height: 24px;
}

.chat-textarea :deep(.n-input__border),
.chat-textarea :deep(.n-input__state-border) {
  display: none !important;
}

/* Send Actions */
.send-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border-radius: 12px;
  cursor: pointer;
  padding: 0;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.send-btn:hover:not(.disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.send-btn:active:not(.disabled) {
  transform: scale(0.95);
}

.send-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #cbd5e1;
  box-shadow: none;
}

.stop-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: #f1f5f9;
  color: #ef4444;
  border-radius: 12px;
  cursor: pointer;
  padding: 0;
  transition: all 0.2s;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
}

.stop-btn:hover {
  background: #fee2e2;
}

/* Footer */
.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
}

.footer-left, .footer-right {
  display: flex;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
}

.hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 500;
}

.disclaimer {
  font-size: 12px;
  color: #94a3b8;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .chat-input-bar {
    padding: 16px;
    gap: 10px;
  }
  
  .quick-actions {
    gap: 6px;
  }
  
  .quick-action-btn {
    padding: 6px 10px;
    font-size: 12px;
  }
  
  .mode-label {
    display: none;
  }
  
  .input-container {
    padding: 8px 10px;
  }
  
  .hint {
    display: none;
  }
  
  .disclaimer {
    font-size: 11px;
  }
}
</style>
