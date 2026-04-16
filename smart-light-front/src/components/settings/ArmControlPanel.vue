<template>
  <div class="settings-card">
    <h2 class="settings-title">🤖 机械臂控制</h2>

    <div class="form-row">
      <label>选择设备：</label>
      <select v-model="selectedDeviceCode" class="date-input">
        <option value="">请选择设备</option>
        <option
          v-for="device in cameraDevices"
          :key="device.id"
          :value="device.chipId"
        >
          {{ device.chipId }}
        </option>
      </select>
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

const props = defineProps<{
  devices: DeviceItem[]
}>()

const selectedDeviceCode = ref('')
const submitting = ref(false)
const errorText = ref('')
const statusText = ref('请选择设备后发送控制指令')

const cameraDevices = computed(() => {
  return props.devices.filter(device =>
    device.chipId.toLowerCase().includes('cam'),
  )
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