<template>
  <div class="chat-welcome">
    <!-- Brand badge -->
    <div class="welcome-badge">
      <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" />
      </svg>
      <span>MeterSphere AI</span>
    </div>

    <!-- Greeting -->
    <h1 class="welcome-heading">{{ t('analytics.knowledge.chat_welcome_greeting') }}</h1>

    <!-- Feature cards -->
    <div class="feature-grid">
      <!-- Dark cards column -->
      <div class="feature-column">
        <div class="feature-card dark">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>
          <h3>{{ t('analytics.knowledge.chat_welcome_explore') }}</h3>
          <p>{{ t('analytics.knowledge.chat_welcome_explore_desc') }}</p>
        </div>
        <div class="feature-card dark">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
          </div>
          <h3>{{ t('analytics.knowledge.chat_welcome_capabilities') }}</h3>
          <p>{{ t('analytics.knowledge.chat_welcome_capabilities_desc') }}</p>
        </div>
        <div class="feature-card dark">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <h3>{{ t('analytics.knowledge.chat_welcome_limitations') }}</h3>
          <p>{{ t('analytics.knowledge.chat_welcome_limitations_desc') }}</p>
        </div>
      </div>

      <!-- Light suggestion cards column -->
      <div class="feature-column">
        <div
          v-for="keyword in hotKeywords"
          :key="keyword"
          class="feature-card light"
          @click="emit('ask', t(keyword))"
        >
          <div class="card-text">
            <span class="card-title">{{ t(keyword) }}</span>
          </div>
          <svg class="card-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 19" />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const emit = defineEmits<{
  ask: [question: string]
}>()

const { t } = useI18n()

const hotKeywords = [
  'analytics.knowledge.keyword_onboarding',
  'analytics.knowledge.keyword_permission',
  'analytics.knowledge.keyword_upload',
]
</script>

<style scoped>
.chat-welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  overflow-y: auto;
}

.welcome-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--chat-accent, #6366f1);
  margin-bottom: 16px;
}

.welcome-badge span {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.welcome-heading {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 40px;
  text-align: center;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  max-width: 640px;
  width: 100%;
}

.feature-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-card {
  padding: 16px;
  border-radius: 12px;
  transition: transform 0.15s, box-shadow 0.15s;
}

.feature-card.dark {
  background: var(--chat-dark-card-bg, #202123);
  color: #ffffff;
}

.feature-card.dark .card-icon {
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.7);
}

.feature-card.dark h3 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
}

.feature-card.dark p {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.5;
}

.feature-card.light {
  background: var(--chat-light-card-bg, #ffffff);
  border: 1px solid var(--chat-border-color, #e5e5e5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.feature-card.light:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-text {
  flex: 1;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.card-arrow {
  color: #8e8ea0;
  flex-shrink: 0;
}
</style>
