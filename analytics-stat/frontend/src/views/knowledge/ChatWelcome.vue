<template>
  <div class="chat-welcome">
    <div class="welcome-content">
      <!-- Logo & Title -->
      <div class="welcome-header">
        <div class="logo-container">
          <div class="logo-ring ring-1"></div>
          <div class="logo-ring ring-2"></div>
          <div class="logo-ring ring-3"></div>
          <div class="logo-core">
            <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
              <rect x="4" y="4" width="6" height="6" rx="1.5" />
              <rect x="14" y="4" width="6" height="6" rx="1.5" />
              <rect x="4" y="14" width="6" height="6" rx="1.5" />
              <rect x="14" y="14" width="6" height="6" rx="1.5" />
            </svg>
          </div>
        </div>
        <h1 class="welcome-title">{{ t('analytics.knowledge.chat_welcome_title') }}</h1>
        <p class="welcome-subtitle">{{ t('analytics.knowledge.chat_welcome_subtitle') }}</p>
      </div>

      <!-- Capabilities Grid -->
      <div class="capabilities-section">
        <div class="section-label">{{ t('analytics.knowledge.capabilities') }}</div>
        <div class="capabilities-grid">
          <div class="capability-card">
            <div class="capability-icon blue">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </div>
            <div class="capability-content">
              <h3>{{ t('analytics.knowledge.capability_search_title') }}</h3>
              <p>{{ t('analytics.knowledge.capability_search_desc') }}</p>
            </div>
          </div>
          
          <div class="capability-card">
            <div class="capability-icon purple">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
              </svg>
            </div>
            <div class="capability-content">
              <h3>{{ t('analytics.knowledge.capability_analyze_title') }}</h3>
              <p>{{ t('analytics.knowledge.capability_analyze_desc') }}</p>
            </div>
          </div>
          
          <div class="capability-card">
            <div class="capability-icon green">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
            </div>
            <div class="capability-content">
              <h3>{{ t('analytics.knowledge.capability_insights_title') }}</h3>
              <p>{{ t('analytics.knowledge.capability_insights_desc') }}</p>
            </div>
          </div>
          
          <div class="capability-card">
            <div class="capability-icon orange">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
                <circle cx="12" cy="12" r="10" />
                <line x1="2" y1="12" x2="22" y2="12" />
                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
              </svg>
            </div>
            <div class="capability-content">
              <h3>{{ t('analytics.knowledge.capability_multilingual_title') }}</h3>
              <p>{{ t('analytics.knowledge.capability_multilingual_desc') }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Start -->
      <div class="quick-start-section">
        <div class="section-label">{{ t('analytics.knowledge.quick_start') }}</div>
        <div class="quick-start-grid">
          <button 
            v-for="(item, index) in quickStarts" 
            :key="index"
            class="quick-start-card"
            @click="emit('ask', item.text)"
            :style="{ animationDelay: `${index * 0.1}s` }"
          >
            <span class="quick-start-text">{{ item.text }}</span>
            <svg class="quick-start-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Background Decoration -->
    <div class="bg-decoration">
      <div class="bg-gradient"></div>
      <div class="bg-grid"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const emit = defineEmits<{
  ask: [question: string]
}>()

const { t } = useI18n()

const quickStarts = [
  { text: t('analytics.knowledge.quick_start_1') },
  { text: t('analytics.knowledge.quick_start_2') },
  { text: t('analytics.knowledge.quick_start_3') },
  { text: t('analytics.knowledge.quick_start_4') },
]
</script>

<style scoped>
.chat-welcome {
  height: 0;
  flex: 1 1 auto;
  overflow-y: auto;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.welcome-content {
  max-width: 720px;
  width: 100%;
  position: relative;
  z-index: 1;
}

/* Header */
.welcome-header {
  text-align: center;
  margin-bottom: 48px;
}

.logo-container {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
}

.logo-ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
}

.ring-1 {
  inset: 0;
  border-color: rgba(99, 102, 241, 0.2);
  animation: rotate 20s linear infinite;
}

.ring-2 {
  inset: 8px;
  border-color: rgba(99, 102, 241, 0.15);
  animation: rotate 15s linear infinite reverse;
}

.ring-3 {
  inset: 16px;
  border-color: rgba(99, 102, 241, 0.1);
  animation: rotate 10s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.logo-core {
  position: absolute;
  inset: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.4);
  animation: pulse 3s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}

.welcome-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 12px;
  letter-spacing: -0.5px;
}

.welcome-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

/* Section Label */
.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 16px;
}

/* Capabilities */
.capabilities-section {
  margin-bottom: 40px;
}

.capabilities-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.capability-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  transition: all 0.2s;
}

.capability-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.capability-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.capability-icon.blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.capability-icon.purple {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.capability-icon.green {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.capability-icon.orange {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.capability-content {
  flex: 1;
  min-width: 0;
}

.capability-content h3 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px;
}

.capability-content p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

/* Quick Start */
.quick-start-section {
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.quick-start-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-start-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  animation: fadeInUp 0.4s ease-out both;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.quick-start-card:hover {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.02);
}

.quick-start-text {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.quick-start-card:hover .quick-start-text {
  color: #6366f1;
}

.quick-start-arrow {
  color: #94a3b8;
  transition: all 0.2s;
}

.quick-start-card:hover .quick-start-arrow {
  color: #6366f1;
  transform: translateX(4px);
}

/* Background Decoration */
.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-gradient {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 30% 20%, rgba(99, 102, 241, 0.05) 0%, transparent 50%),
              radial-gradient(ellipse at 70% 80%, rgba(139, 92, 246, 0.05) 0%, transparent 50%);
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}

/* Mobile Responsive */
@media (max-width: 640px) {
  .chat-welcome {
    padding: 24px 16px;
    justify-content: flex-start;
  }
  
  .welcome-title {
    font-size: 24px;
  }
  
  .welcome-subtitle {
    font-size: 14px;
  }
  
  .capabilities-grid {
    grid-template-columns: 1fr;
  }
  
  .capability-card {
    padding: 16px;
  }
  
  .logo-container {
    width: 64px;
    height: 64px;
  }
  
  .logo-core {
    inset: 16px;
  }
  
  .logo-core svg {
    width: 24px;
    height: 24px;
  }
}
</style>
