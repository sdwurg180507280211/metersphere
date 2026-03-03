<template>
  <n-menu :value="activePath" :options="menuOptions" class="knowledge-menu" @update:value="handleMenuChange" />
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NMenu, NIcon, type MenuOption } from 'naive-ui'
import { KNOWLEDGE_ROUTE_PATHS } from '@/config/knowledge-route'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const iconSvg = (d: string) =>
  h(
    NIcon,
    null,
    {
      default: () =>
        h(
          'svg',
          { viewBox: '0 0 24 24', width: '16', height: '16', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' },
          [h('path', { d })],
        ),
    },
  )

const menuOptions = computed<MenuOption[]>(() => [
  {
    key: 'knowledge-group',
    label: t('commons.analytics_stat'),
    children: [
      {
        key: KNOWLEDGE_ROUTE_PATHS.knowledge,
        label: t('analytics.menu.knowledge'),
        icon: () => iconSvg('M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8a4 4 0 0 0 0-8'),
      },
      {
        key: KNOWLEDGE_ROUTE_PATHS.knowledgeChat,
        label: t('analytics.menu.knowledge_chat'),
        icon: () => iconSvg('M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'),
      },
    ],
  },
])

const activePath = computed(() => route.path)

const handleMenuChange = (key: string) => {
  if (key.startsWith('/')) {
    router.push(key)
  }
}
</script>

<style scoped>
.knowledge-menu {
  height: 100%;
  padding-top: 8px;
}
</style>
