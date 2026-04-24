<template>
  <div class="settings-card">
    <h2 class="settings-title">🤖 机械臂控制</h2>

    <div class="form-row">
      <label>选择设备：</label>
        <BaseSelect
          v-model="selectedDeviceCode"
          :options="cameraDeviceOptions"
          placeholder="请选择设备"
        />
    </div>

    <div class="arm-grid">
      <button class="btn-primary" :disabled="submitting" @click="send('up')">⬆ 上</button>
      <button class="btn-primary" :disabled="submitting" @click="send('down')">⬇ 下</button>
      <button class="btn-secondary" :disabled="submitting" @click="send('left')">⟲ 人流</button>
      <button class="btn-secondary" :disabled="submitting" @click="send('right')">⟳ 服装</button>
    </div>

    <div class="result-block">
      <div v-if="errorText" class="error-text">{{ errorText }}</div>
      <div v-else class="device-meta">{{ statusText }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { armControl } from '../../api/device'
import type { DeviceItem } from '../../types/device'
import BaseSelect from '../common/BaseSelect.vue'
const props = defineProps<{
  devices: DeviceItem[]
}>()

const selectedDeviceCode = ref('')
const submitting = ref(false)
const errorText = ref('')
const statusText = ref('请选择设备后发送控制指令')

const cameraDeviceOptions = computed(() => {
  return cameraDevices.value.map(device => ({
    label: device.displayName || getDeviceCode(device) || '未知设备',
    value: getDeviceCode(device),
  }))
})

function getDeviceCode(device: Partial<DeviceItem> | any) {
  return (device?.chipId || device?.deviceCode || '').trim()
}

const cameraDevices = computed(() => {
  return (props.devices || []).filter(device => {
    const type = String(device.deviceType || '')
      .replace(/[-_\s]/g, '')
      .toLowerCase()

    return type === 'camlamp'
  })
})

async function send(direction: string) {
  errorText.value = ''

  if (!selectedDeviceCode.value) {
    errorText.value = '请先选择设备'
    return
  }

  submitting.value = true
  try {
    await armControl(selectedDeviceCode.value, direction)
    statusText.value = `已发送指令：${selectedDeviceCode.value} / ${direction}`
  } catch (error) {
    console.error('armControl error =', error)
    errorText.value = '发送控制指令失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.arm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .form-row label {
    min-width: auto;
  }

  .date-input {
    width: 100%;
    min-width: 0;
  }

  .arm-grid {
    grid-template-columns: 1fr;
  }

  .arm-grid .btn-primary,
  .arm-grid .btn-secondary {
    width: 100%;
  }
}
</style>