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