<template>
  <nav class="sidebar">
    <ul>
      <li
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: modelValue === tab.key }"
        @click="$emit('update:modelValue', tab.key)"
      >
        {{ tab.label }}
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import type { DashboardTab } from '../../types/device'

defineProps<{
  modelValue: DashboardTab
}>()

defineEmits<{
  (e: 'update:modelValue', value: DashboardTab): void
}>()

const tabs: { key: DashboardTab; label: string }[] = [
  { key: 'main', label: '实时灯控' },
  { key: 'flow', label: '数据仪表板' },
  { key: 'settings', label: '设置' },
]
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
@media (max-width: 768px) {
  .sidebar {
    position: static;
    width: calc(100% - 24px);
    height: auto;          /* 关键：覆盖桌面端 height */
    min-height: 0;         /* 关键：不要撑高 */
    margin: 12px;
    padding: 8px;
    border-radius: 14px;

    display: block;
  }

  .sidebar ul {
    width: 100%;
    display: flex;
    flex-direction: row;   /* 关键：移动端横向排列 */
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
</style>