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
  width: 200px;
  background: var(--card-bg);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
  border-right: 1px solid #eee;
  padding: 30px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.sidebar ul {
  width: 100%;
  list-style: none;
}

.sidebar li {
  position: relative;
  width: 90%;
  margin: 8px auto;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.sidebar li.active,
.sidebar li:hover {
  background: rgba(64, 158, 255, 0.1);
  color: var(--primary);
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
  border-radius: 2px 0 0 2px;
}
@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    padding: 12px 16px;
  }

  .sidebar ul {
    display: flex;
    gap: 8px;
  }

  .sidebar li {
    flex: 1;
    margin: 0;
    border-radius: 12px;
    text-align: center;
  }

  .sidebar li.active::before {
    top: 0;
    left: 0;
    width: 100%;
    height: 4px;
    transform: none;
  }
}
</style>