<template>
  <nav class="sidebar">
    <ul>
      <li
        v-for="tab in tabs"
        :key="tab.key"
        class="sidebar-item sidebar-nav-item"
        :class="{ active: modelValue === tab.key }"
        @click="handleTabClick(tab.key)"
      >
        <span class="sidebar-nav-text">{{ tab.label }}</span>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import type { DashboardTab } from '../../types/device'

defineProps<{
  modelValue: DashboardTab
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: DashboardTab): void
}>()

const route = useRoute()
const router = useRouter()

const tabs: { key: DashboardTab; label: string }[] = [
  { key: 'main', label: '实时灯控' },
  { key: 'flow', label: '数据仪表盘' },
  { key: 'settings', label: '设置' },
  { key: 'firmware', label: '固件管理' },
]

function handleTabClick(key: DashboardTab) {
  if (route.name !== 'smartlightdashboard' || route.query.tab !== key) {
    router.push({
      path: '/smartlightdashboard',
      query: { tab: key },
    })
    return
  }

  emit('update:modelValue', key)
}
</script>

<style scoped>
.sidebar {
  width: 180px;

  position: fixed;
  left: 24px;
  top: 24px;
  z-index: 100;

  height: calc(100vh - 48px);
  min-height: auto;

  padding: 28px 10px;

  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);

  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);

  display: flex;
  flex-direction: column;
  align-items: center;
}

.sidebar ul {
  width: 100%;
  list-style: none;
  padding: 0;
  margin: 0;

  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar li {
  position: relative;
  width: 90%;
  margin: 8px auto;
  padding: 11px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, transform 0.2s;
  color: #333;
}

.sidebar li.active,
.sidebar li:hover {
  background: rgba(64, 158, 255, 0.12);
  color: var(--primary);
}

.sidebar li:hover {
  transform: translateX(2px);
}

.sidebar li.active::before {
  content: '';
  position: absolute;
  left: -2px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 60%;
  background: var(--primary);
  border-radius: 2px;
}

:global(.night-mode) .sidebar {
  background: rgba(15, 23, 42, 0.72);
  border-color: rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.35);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

:global(.night-mode) .sidebar li {
  color: rgba(226, 232, 240, 0.82);
  opacity: 1;
}

:global(.night-mode) .sidebar li:hover {
  background: rgba(59, 130, 246, 0.12);
  color: rgba(248, 250, 252, 0.96);
}

:global(.night-mode) .sidebar li.active {
  background: rgba(64, 158, 255, 0.2);
  color: #eaf2ff;
}

:global(.night-mode) .sidebar li.active::before {
  background: #60a5fa;
}

:global(.night-mode) .sidebar .sidebar-item,
:global(.night-mode) .sidebar .nav-item,
:global(.night-mode) .sidebar .menu-item,
:global(.night-mode) .sidebar .sidebar-link {
  color: rgba(226, 232, 240, 0.84) !important;
  opacity: 1 !important;
}

:global(.night-mode) .sidebar .sidebar-item *,
:global(.night-mode) .sidebar .nav-item *,
:global(.night-mode) .sidebar .menu-item *,
:global(.night-mode) .sidebar .sidebar-link * {
  color: inherit !important;
  opacity: 1 !important;
}

:global(.night-mode) .sidebar .sidebar-item:hover,
:global(.night-mode) .sidebar .nav-item:hover,
:global(.night-mode) .sidebar .menu-item:hover,
:global(.night-mode) .sidebar .sidebar-link:hover {
  background: rgba(59, 130, 246, 0.12) !important;
  color: rgba(248, 250, 252, 0.96) !important;
  opacity: 1 !important;
}

:global(.night-mode) .sidebar .sidebar-item.active,
:global(.night-mode) .sidebar .sidebar-item.router-link-active,
:global(.night-mode) .sidebar .sidebar-item.selected,
:global(.night-mode) .sidebar .sidebar-item.current,
:global(.night-mode) .sidebar .nav-item.active,
:global(.night-mode) .sidebar .nav-item.router-link-active,
:global(.night-mode) .sidebar .nav-item.selected,
:global(.night-mode) .sidebar .nav-item.current,
:global(.night-mode) .sidebar .menu-item.active,
:global(.night-mode) .sidebar .menu-item.router-link-active,
:global(.night-mode) .sidebar .menu-item.selected,
:global(.night-mode) .sidebar .menu-item.current {
  background: rgba(59, 130, 246, 0.22) !important;
  color: #ffffff !important;
  opacity: 1 !important;
}

:global(.night-mode) .sidebar .sidebar-item.disabled,
:global(.night-mode) .sidebar .sidebar-item.muted,
:global(.night-mode) .sidebar .sidebar-item.inactive,
:global(.night-mode) .sidebar .nav-item.disabled,
:global(.night-mode) .sidebar .nav-item.muted,
:global(.night-mode) .sidebar .nav-item.inactive,
:global(.night-mode) .sidebar .menu-item.disabled,
:global(.night-mode) .sidebar .menu-item.muted,
:global(.night-mode) .sidebar .menu-item.inactive {
  color: rgba(148, 163, 184, 0.65) !important;
  opacity: 1 !important;
}

:global(.app-container.night-mode) .sidebar li.sidebar-item,
:global(.app-container.night-mode) .sidebar .sidebar-item:not(.active),
:global(.firmware-page.night-mode) .sidebar li.sidebar-item,
:global(.firmware-page.night-mode) .sidebar .sidebar-item:not(.active),
:global(body:has(.app-container.night-mode)) .sidebar li.sidebar-item {
  color: rgba(226, 232, 240, 0.76) !important;
  opacity: 1 !important;
  filter: none !important;
}

:global(.app-container.night-mode) .sidebar li.sidebar-item *,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item *,
:global(body:has(.app-container.night-mode)) .sidebar li.sidebar-item * {
  color: inherit !important;
  opacity: 1 !important;
  filter: none !important;
}

:global(.app-container.night-mode) .sidebar li.sidebar-item:hover,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item:hover,
:global(body:has(.app-container.night-mode)) .sidebar li.sidebar-item:hover {
  background: rgba(59, 130, 246, 0.12) !important;
  color: rgba(248, 250, 252, 0.96) !important;
  opacity: 1 !important;
}

:global(.app-container.night-mode) .sidebar li.sidebar-item.active,
:global(.app-container.night-mode) .sidebar li.sidebar-item.router-link-active,
:global(.app-container.night-mode) .sidebar li.sidebar-item.selected,
:global(.app-container.night-mode) .sidebar li.sidebar-item.current,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item.active,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item.router-link-active,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item.selected,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item.current,
:global(body:has(.app-container.night-mode)) .sidebar li.sidebar-item.active {
  background: rgba(59, 130, 246, 0.22) !important;
  color: #ffffff !important;
  opacity: 1 !important;
}

:global(.app-container.night-mode) .sidebar li.sidebar-item.disabled,
:global(.app-container.night-mode) .sidebar li.sidebar-item.muted,
:global(.app-container.night-mode) .sidebar li.sidebar-item.inactive,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item.disabled,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item.muted,
:global(.firmware-page.night-mode) .sidebar li.sidebar-item.inactive {
  color: rgba(148, 163, 184, 0.65) !important;
  opacity: 1 !important;
}

@media (max-width: 768px) {
  .sidebar {
    position: static;
    width: calc(100% - 24px);
    height: auto;
    min-height: 0;
    margin: 12px;
    padding: 8px;
    border-radius: 14px;

    display: block;
  }

  .sidebar ul {
    width: 100%;
    display: flex;
    flex-direction: row;
    gap: 8px;
    padding: 0;
    margin: 0;
  }

  .sidebar li {
    flex: 1;
    width: auto;
    margin: 0;
    padding: 10px 6px;
    border-radius: 12px;
    text-align: center;
    font-size: 14px;
  }

  .sidebar li:hover {
    transform: none;
  }

  .sidebar li.active::before {
    top: auto;
    bottom: 0;
    left: 20%;
    width: 60%;
    height: 3px;
    transform: none;
    border-radius: 999px;
  }
}

:global(.night-mode .sidebar),
:global(.app-container.night-mode .sidebar),
:global(body:has(.app-container.night-mode) .sidebar) {
  opacity: 1 !important;
  filter: none !important;
}

:global(.night-mode .sidebar ul),
:global(.app-container.night-mode .sidebar ul),
:global(body:has(.app-container.night-mode) .sidebar ul) {
  opacity: 1 !important;
  filter: none !important;
}

:global(.night-mode .sidebar .sidebar-nav-item),
:global(.app-container.night-mode .sidebar .sidebar-nav-item),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item) {
  color: rgba(226, 232, 240, 0.82) !important;
  opacity: 1 !important;
  filter: none !important;
}

:global(.night-mode .sidebar .sidebar-nav-item .sidebar-nav-text),
:global(.night-mode .sidebar .sidebar-nav-item span),
:global(.night-mode .sidebar .sidebar-nav-item svg),
:global(.app-container.night-mode .sidebar .sidebar-nav-item .sidebar-nav-text),
:global(.app-container.night-mode .sidebar .sidebar-nav-item span),
:global(.app-container.night-mode .sidebar .sidebar-nav-item svg),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item .sidebar-nav-text),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item span),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item svg) {
  color: inherit !important;
  opacity: 1 !important;
  filter: none !important;
}

:global(.night-mode .sidebar .sidebar-nav-item:not(.active):not(.router-link-active)),
:global(.app-container.night-mode .sidebar .sidebar-nav-item:not(.active):not(.router-link-active)),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item:not(.active):not(.router-link-active)) {
  color: rgba(226, 232, 240, 0.82) !important;
  opacity: 1 !important;
}

:global(.night-mode .sidebar .sidebar-nav-item:hover),
:global(.app-container.night-mode .sidebar .sidebar-nav-item:hover),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item:hover) {
  background: rgba(59, 130, 246, 0.14) !important;
  color: rgba(248, 250, 252, 0.96) !important;
  opacity: 1 !important;
}

:global(.night-mode .sidebar .sidebar-nav-item.active),
:global(.night-mode .sidebar .sidebar-nav-item.router-link-active),
:global(.app-container.night-mode .sidebar .sidebar-nav-item.active),
:global(.app-container.night-mode .sidebar .sidebar-nav-item.router-link-active),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item.active),
:global(body:has(.app-container.night-mode) .sidebar .sidebar-nav-item.router-link-active) {
  background: rgba(59, 130, 246, 0.24) !important;
  color: #ffffff !important;
  opacity: 1 !important;
}
</style>
