<template>
  <div>
    <div class="section-header">
      <h2>已绑定设备</h2>
      <button class="refresh-btn" @click="$emit('refresh')">刷新</button>
    </div>

    <div v-if="loading" class="empty-block">设备加载中...</div>
    <div v-else-if="devices.length === 0" class="empty-block">暂无设备</div>

    <div v-else id="deviceContainer">
      <DeviceCard
        v-for="device in devices"
        :key="device.id"
        :device="device"
        :deleting="deletingId === device.id"
        @update-realtime="$emit('update-realtime', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import DeviceCard from './DeviceCard.vue'
import type { DeviceCreatePayload, DeviceItem } from '../../types/device'

defineProps<{
  devices: DeviceItem[]
  loading: boolean
  deletingId?: number | null
}>()

defineEmits<{
  (e: 'refresh'): void
  (e: 'update-realtime', value: { id: number; payload: DeviceCreatePayload }): void
  (e: 'delete', id: number): void
}>()
</script>