<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-card">
      <h3>手动添加设备</h3>

      <label class="modal-label">设备编码</label>
      <input
        v-model.trim="form.deviceCode"
        class="modal-input"
        type="text"
        placeholder="如 lamp2"
      />

      <label class="modal-label">设备 IP</label>
      <input
        v-model.trim="form.ip"
        class="modal-input"
        type="text"
        placeholder="如 192.168.1.105"
      />

      <div class="modal-actions">
        <button class="btn-confirm" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '确认添加' }}
        </button>
        <button class="btn-cancel" :disabled="submitting" @click="$emit('close')">
          取消
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { DeviceCreatePayload } from '../../types/device'

const props = defineProps<{
  submitting?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', value: DeviceCreatePayload): void
}>()

const form = reactive<DeviceCreatePayload>({
  deviceCode: '',
  ip: '',
  brightness: 50,
  temp: 4000,
  autoMode: false,
  recommendedBrightness: 50,
  recommendedTemp: 4000,
  fabric: '',
  mainColorRgb: '',
})

function resetForm() {
  form.deviceCode = ''
  form.ip = ''
  form.brightness = 50
  form.temp = 4000
  form.autoMode = false
  form.recommendedBrightness = 50
  form.recommendedTemp = 4000
  form.fabric = ''
  form.mainColorRgb = ''
}

function submit() {
  if (!form.deviceCode) return
  emit('submit', { ...form })
}

watch(
  () => props.submitting,
  (val) => {
    if (val === false) resetForm()
  },
)
</script>